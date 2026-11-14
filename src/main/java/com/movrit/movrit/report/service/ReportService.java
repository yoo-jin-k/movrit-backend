package com.movrit.movrit.report.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.campaigncontent.entity.CampaignContentSubmission;
import com.movrit.movrit.campaigncontent.repository.CampaignContentSubmissionRepository;
import com.movrit.movrit.chat.service.ChatEventHub;
import com.movrit.movrit.chat.service.ChatService;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.community.entity.CommunityComment;
import com.movrit.movrit.community.entity.CommunityPost;
import com.movrit.movrit.community.repository.CommunityCommentRepository;
import com.movrit.movrit.community.repository.CommunityPostRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.report.dto.ReportDtos;
import com.movrit.movrit.report.entity.ContentReport;
import com.movrit.movrit.report.repository.ContentReportRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 신고 접수와 운영자 제재를 처리합니다.
 *
 * <ul>
 *   <li>신고: 커뮤니티 글·댓글(로그인 회원), 채팅 메시지(그 방 참여자), 캠페인 콘텐츠(그 캠페인 업체). 내 글은 신고할 수 없고, 같은 대상은 한 번만
 *       신고합니다. 접수되면 운영자에게 알립니다.
 *   <li>처리: DISMISS(기각) · WARN(작성자에게 경고 알림) · HIDE(대상 가리기) · SUSPEND(대상 가리기 + 작성자 이용 정지). 조치하면 같은
 *       대상의 다른 접수 신고도 함께 처리하고, 신고한 회원에게 결과를 알립니다.
 *   <li>캠페인 콘텐츠는 외부 게시물이라 가릴 수 없어 경고·이용 정지만 할 수 있습니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ReportService {
  static final Set<String> TARGETS =
      Set.of("COMMUNITY_POST", "COMMUNITY_COMMENT", "CHAT_MESSAGE", "CAMPAIGN_CONTENT");
  static final Set<String> REASONS = Set.of("SPAM", "ABUSE", "SEXUAL", "PRIVACY", "FRAUD", "OTHER");
  static final Map<String, String> TARGET_LABELS =
      Map.of(
          "COMMUNITY_POST", "커뮤니티 글",
          "COMMUNITY_COMMENT", "커뮤니티 댓글",
          "CHAT_MESSAGE", "채팅 메시지",
          "CAMPAIGN_CONTENT", "캠페인 콘텐츠");

  private final ContentReportRepository reportRepository;
  private final CommunityPostRepository postRepository;
  private final CommunityCommentRepository commentRepository;
  private final CampaignContentSubmissionRepository contentRepository;
  private final ChatService chatService;
  private final ChatEventHub chatEventHub;
  private final CampaignOwnershipService campaignOwnership;
  private final MemberService memberService;
  private final MemberRepository memberRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  /** 신고 대상 확인 결과입니다. */
  private record Target(Long ownerMemberId, String snapshot) {}

  /**
   * 신고를 접수합니다.
   *
   * @throws ResponseStatusException 형식 오류·내 글 신고는 400, 볼 수 없는 대상은 404, 이미 신고했으면 409
   */
  @Transactional
  public ReportDtos.Created create(Long memberId, ReportDtos.CreateRequest request) {
    String type = request.targetType().trim();
    String reason = request.reason().trim();
    if (!TARGETS.contains(type)) throw badRequest("신고할 수 없는 대상입니다.");
    if (!REASONS.contains(reason)) throw badRequest("신고 사유를 골라 주세요.");
    Target target = target(memberId, type, request.targetId());
    if (memberId.equals(target.ownerMemberId())) throw badRequest("내가 쓴 내용은 신고할 수 없습니다.");
    if (reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
        memberId, type, request.targetId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 내용입니다. 운영자가 확인하고 있습니다.");
    }
    String detail =
        request.detail() == null || request.detail().isBlank() ? null : request.detail().strip();
    ContentReport report =
        reportRepository.save(
            new ContentReport(
                memberId,
                type,
                request.targetId(),
                target.ownerMemberId(),
                target.snapshot(),
                reason,
                detail));
    memberRepository
        .findAllByRoleAndStatus(MemberRole.ADMIN, MemberStatus.ACTIVE)
        .forEach(
            admin ->
                notificationService.send(
                    admin.getId(),
                    "REPORT",
                    "새 신고가 접수되었습니다.",
                    TARGET_LABELS.get(type) + " · " + preview(target.snapshot()),
                    // 운영자 알림은 '신고·제재' 화면으로 이동합니다.
                    "ADMIN_REPORT",
                    report.getId()));
    return new ReportDtos.Created(report.getId(), report.getStatus());
  }

  /** 운영자용 최근 신고 목록(최대 200건)입니다. */
  @Transactional(readOnly = true)
  public List<ReportDtos.Item> list(Long adminMemberId) {
    requireAdmin(adminMemberId);
    return reportRepository.findTop200ByOrderByIdDesc().stream()
        .map(
            report ->
                ReportDtos.Item.from(
                    report,
                    name(report.getTargetMemberId()),
                    name(report.getReporterMemberId()),
                    reportRepository.countByTargetTypeAndTargetId(
                        report.getTargetType(), report.getTargetId())))
        .toList();
  }

  /**
   * 운영자가 신고를 처리합니다.
   *
   * @throws ResponseStatusException 운영자가 아니면 403, 이미 처리했거나 할 수 없는 조치면 409
   */
  @Transactional
  public ReportDtos.Item handle(
      Long adminMemberId, Long reportId, ReportDtos.HandleRequest request) {
    requireAdmin(adminMemberId);
    ContentReport report =
        reportRepository
            .findById(reportId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."));
    if (!report.isOpen()) throw conflict("이미 처리한 신고입니다.");
    String action = request.action().trim();
    String note =
        request.note() == null || request.note().isBlank() ? null : request.note().strip();
    String label = TARGET_LABELS.get(report.getTargetType());
    Long owner = report.getTargetMemberId();
    switch (action) {
      case "DISMISS" -> {}
      case "WARN" -> {
        if (owner == null) throw conflict("작성자를 알 수 없어 경고할 수 없습니다.");
      }
      case "HIDE" -> hideTarget(report, true);
      case "SUSPEND" -> {
        if (owner == null) throw conflict("작성자를 알 수 없어 이용 정지할 수 없습니다.");
        hideTarget(report, false);
        memberService.updateStatus(adminMemberId, owner, MemberStatus.SUSPENDED);
        refreshTokenRepository.findByMemberId(owner).ifPresent(token -> token.revoke());
        // 열려 있는 채팅 실시간 연결도 닫습니다(커밋 뒤). 다시 연결하면 정지 상태라 403입니다.
        chatEventHub.disconnect(owner);
      }
      default -> throw badRequest("처리 방식을 확인해 주세요.");
    }
    // 조치하면 같은 대상의 다른 접수 신고도 함께 닫습니다. 기각은 이 신고만 닫습니다.
    List<ContentReport> closing =
        action.equals("DISMISS")
            ? List.of(report)
            : reportRepository.findAllByTargetTypeAndTargetIdAndStatus(
                report.getTargetType(), report.getTargetId(), ContentReport.OPEN);
    for (ContentReport item : closing) {
      item.handle(action, note, adminMemberId);
      notificationService.send(
          item.getReporterMemberId(),
          "REPORT",
          action.equals("DISMISS") ? "신고를 검토했습니다." : "신고하신 내용을 조치했습니다.",
          action.equals("DISMISS")
              ? label + "을(를) 확인했으나 운영 정책 위반으로 보기 어려워 그대로 둡니다."
              : label + "에 대해 운영 정책에 따라 조치했습니다. 알려 주셔서 감사합니다.",
          "REPORT",
          item.getId());
    }
    if (!action.equals("DISMISS") && owner != null && !action.equals("SUSPEND")) {
      notificationService.send(
          owner,
          "REPORT",
          action.equals("WARN") ? "운영 정책 경고가 도착했습니다." : "운영 정책에 따라 내용이 가려졌습니다.",
          label + (note == null ? "" : " · " + note),
          "REPORT",
          report.getId());
    }
    auditLogService.record(
        adminMemberId,
        "CONTENT_REPORT_" + action,
        report.getTargetType(),
        report.getTargetId(),
        "{\"status\":\"OPEN\"}",
        "{\"resolution\":\"" + action + "\"}",
        note);
    return ReportDtos.Item.from(
        report,
        name(owner),
        name(report.getReporterMemberId()),
        reportRepository.countByTargetTypeAndTargetId(
            report.getTargetType(), report.getTargetId()));
  }

  /** 대상을 가립니다. required가 true인데 가릴 수 없는 대상(캠페인 콘텐츠)이면 409입니다. */
  private void hideTarget(ContentReport report, boolean required) {
    Long id = report.getTargetId();
    switch (report.getTargetType()) {
      case "COMMUNITY_POST" -> postRepository.findById(id).ifPresent(CommunityPost::hideByAdmin);
      // 댓글은 지우지 않고 가립니다. 운영자가 커뮤니티 관리 화면에서 다시 보이게 할 수 있습니다.
      case "COMMUNITY_COMMENT" ->
          commentRepository
              .findById(id)
              .filter(comment -> comment.getDeletedAt() == null)
              .ifPresent(CommunityComment::hideByAdmin);
      case "CHAT_MESSAGE" -> chatService.hideMessage(id);
      default -> {
        if (required) throw conflict("캠페인 콘텐츠는 외부 게시물이라 가릴 수 없습니다. 경고 또는 이용 정지를 골라 주세요.");
      }
    }
  }

  /** 신고 대상을 확인하고 작성자·내용을 돌려줍니다. 신고하는 사람이 볼 수 있는 대상만 받습니다. */
  private Target target(Long memberId, String type, Long id) {
    return switch (type) {
      case "COMMUNITY_POST" -> {
        CommunityPost post =
            postRepository.findById(id).filter(CommunityPost::visible).orElseThrow(this::notFound);
        yield new Target(
            post.getAuthor() == null ? null : post.getAuthor().getId(),
            cut("[" + post.getTitle() + "] " + post.getBody()));
      }
      case "COMMUNITY_COMMENT" -> {
        CommunityComment comment =
            commentRepository
                .findById(id)
                .filter(
                    value ->
                        value.getDeletedAt() == null
                            && !value.isHidden()
                            && value.getPost().visible())
                .orElseThrow(this::notFound);
        yield new Target(
            comment.getAuthor() == null ? null : comment.getAuthor().getId(),
            cut(comment.getBody()));
      }
      case "CHAT_MESSAGE" -> {
        ChatService.ReportTarget target = chatService.reportTarget(memberId, id);
        yield new Target(target.ownerMemberId(), target.snapshot());
      }
      default -> {
        CampaignContentSubmission content =
            contentRepository.findById(id).orElseThrow(this::notFound);
        // 캠페인 콘텐츠는 그 캠페인을 운영하는 업체만 신고할 수 있습니다.
        campaignOwnership.requireOwner(memberId, content.getApplication().getCampaign());
        yield new Target(
            content.getApplication().getCreator().getMember().getId(),
            cut(
                content.getApplication().getCampaign().getTitle()
                    + " · "
                    + content.getContentUrl()
                    + (content.getNote() == null ? "" : " · " + content.getNote())));
      }
    };
  }

  private String name(Long memberId) {
    if (memberId == null) return "알 수 없음";
    return memberRepository
        .findById(memberId)
        .map(
            member ->
                member.getNickname() != null && !member.getNickname().isBlank()
                    ? member.getNickname()
                    : member.getName())
        .orElse("알 수 없음");
  }

  private void requireAdmin(Long memberId) {
    Member member = memberService.getMember(memberId);
    if (!member.isAdmin())
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "운영자만 할 수 있습니다.");
  }

  private static String cut(String value) {
    return value == null ? null : value.length() > 1900 ? value.substring(0, 1900) : value;
  }

  private static String preview(String value) {
    return value == null ? "" : value.length() > 40 ? value.substring(0, 40) + "…" : value;
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "신고할 내용을 찾을 수 없습니다.");
  }

  private static ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}

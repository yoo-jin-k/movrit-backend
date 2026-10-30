package com.movrit.movrit.campaigncontent.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.dto.CampaignContentDtos;
import com.movrit.movrit.campaigncontent.entity.CampaignContentLink;
import com.movrit.movrit.campaigncontent.entity.CampaignContentSubmission;
import com.movrit.movrit.campaigncontent.entity.ContentSubmissionStatus;
import com.movrit.movrit.campaigncontent.repository.CampaignContentLinkRepository;
import com.movrit.movrit.campaigncontent.repository.CampaignContentSubmissionRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 캠페인 콘텐츠 제출과 검토를 처리합니다.
 *
 * <p>참여자는 게시물 링크를 제출·재제출하고, 브랜드는 확인 또는 수정 요청을, 운영자는 최종 승인 또는 수정 요청을 합니다. 상태가 바뀔 때마다 상대방에게 알림을 보내고
 * 감사 로그를 남깁니다. 동시에 검토해 결과가 덮어써지지 않도록 모든 변경은 화면이 가진 버전(expectedVersion)을 확인합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignContentService {
  private final CampaignApplicationRepository applicationRepository;
  private final CampaignContentSubmissionRepository submissionRepository;
  private final CampaignContentLinkRepository linkRepository;
  private final CampaignOwnershipService campaignOwnership;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;
  private final MemberService memberService;
  private final SellerProfileService sellerProfileService;

  /**
   * 참여자가 대표 링크 하나로 콘텐츠를 제출하거나 다시 제출합니다.
   *
   * <p>본인 신청만 가능하며, 처음 제출은 캠페인의 콘텐츠 제출 기간 안에서만 받습니다. 수정 요청을 받은 재제출은 기간이 지나도 허용합니다. 처음 제출이면 신청을 '콘텐츠
   * 제출' 단계로 바꾸고, 재제출이면 버전을 확인한 뒤 내용을 교체합니다. 제출 후 브랜드 담당자에게 알림을 보냅니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 링크 형식이 잘못되면 400, 제출 기간이 아니거나 상태·버전이 맞지 않으면 409
   */
  @Transactional
  public CampaignContentDtos.Response submit(
      Long memberId, Long applicationId, CampaignContentDtos.SubmitRequest request) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    CampaignContentSubmission submission =
        submissionRepository.findByApplicationId(applicationId).orElse(null);
    validateLink(
        com.movrit.movrit.campaigncontent.entity.ContentChannel.OTHER, request.contentUrl());
    requireSubmissionPeriod(application, submission);

    if (submission == null) {
      transitionApplication(application, CampaignApplication::markContentSubmitted);
      submission =
          submissionRepository.save(
              CampaignContentSubmission.builder()
                  .application(application)
                  .contentUrl(request.contentUrl().trim())
                  .note(normalize(request.note()))
                  .status(ContentSubmissionStatus.SUBMITTED)
                  .revisionCount(0)
                  .submittedAt(LocalDateTime.now())
                  .build());
    } else {
      requireVersion(submission, request.expectedVersion());
      transitionSubmission(
          submission,
          value -> value.resubmit(request.contentUrl().trim(), normalize(request.note())));
      transitionApplication(application, CampaignApplication::markContentSubmitted);
    }

    Long sellerMemberId = sellerMemberId(application);
    notificationService.send(
        sellerMemberId,
        "CAMPAIGN_CONTENT",
        "캠페인 콘텐츠가 제출되었습니다.",
        application.getCreator().getNickname() + "님이 콘텐츠를 제출하셨습니다.",
        "CAMPAIGN_CONTENT",
        submission.getId());
    auditLogService.record(
        memberId,
        "CAMPAIGN_CONTENT_SUBMITTED",
        "CAMPAIGN_CONTENT",
        submission.getId(),
        null,
        "{\"status\":\"SUBMITTED\"}",
        null);
    return flushedResponse(submission);
  }

  /**
   * 참여자가 채널별 링크 여러 개(최대 5개)를 한 번에 제출합니다.
   *
   * <p>링크마다 형식을 검사하고 같은 주소의 중복을 거절합니다. 첫 번째 링크로 {@link #submit}을 수행해 상태를 바꾼 뒤, 이전 채널 링크를 모두 지우고 새
   * 목록으로 교체합니다.
   *
   * @throws ResponseStatusException 중복 주소나 형식 오류는 400, 그 밖의 거절 사유는 {@link #submit}과 같습니다
   */
  @Transactional
  public CampaignContentDtos.Response submitLinks(
      Long memberId, Long applicationId, CampaignContentDtos.PackageSubmitRequest request) {
    Set<String> urls = new HashSet<>();
    for (CampaignContentDtos.LinkRequest link : request.links()) {
      validateLink(link.channelType(), link.contentUrl());
      if (!urls.add(link.contentUrl().trim())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 콘텐츠 URL을 중복 제출할 수 없습니다.");
      }
    }
    CampaignContentDtos.LinkRequest first = request.links().get(0);
    submit(
        memberId,
        applicationId,
        new CampaignContentDtos.SubmitRequest(
            first.contentUrl(), request.note(), request.expectedVersion()));
    CampaignContentSubmission submission =
        submissionRepository.findByApplicationId(applicationId).orElseThrow();
    linkRepository.deleteAllBySubmissionId(submission.getId());
    linkRepository.saveAll(
        request.links().stream()
            .map(
                link ->
                    CampaignContentLink.builder()
                        .submission(submission)
                        .channelType(link.channelType())
                        .contentUrl(link.contentUrl().trim())
                        .build())
            .toList());
    return flushedResponse(submission);
  }

  /** 로그인한 참여자가 제출한 콘텐츠 목록을 최신순으로 조회합니다. */
  public List<CampaignContentDtos.Response> findMine(Long memberId) {
    return submissionRepository.findAllByApplicationCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(this::response)
        .toList();
  }

  /** 내 캠페인 작업공간에서 신청별 최신 콘텐츠 검수 상태를 합칠 때 사용합니다. */
  public CampaignContentDtos.Response findMineByApplication(Long memberId, Long applicationId) {
    CampaignContentSubmission submission =
        submissionRepository.findByApplicationId(applicationId).orElse(null);
    if (submission == null) return null;
    if (!submission.getApplication().getCreator().getMember().getId().equals(memberId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "콘텐츠 제출 내역을 찾을 수 없습니다.");
    }
    return response(submission);
  }

  /**
   * 승인된 업체 회원이 자기 브랜드 캠페인에 제출된 콘텐츠를 최신순으로 조회합니다.
   *
   * @throws ResponseStatusException 승인된 업체가 아니면 거절합니다
   */
  public List<CampaignContentDtos.Response> findSellerContents(Long memberId) {
    sellerProfileService.getApprovedSeller(memberId);
    return submissionRepository
        .findAllByApplicationCampaignBrandSellerMemberIdOrderByIdDesc(memberId)
        .stream()
        .map(this::response)
        .toList();
  }

  /**
   * 브랜드(캠페인 소유 업체)가 참여자에게 콘텐츠 수정을 요청합니다.
   *
   * <p>검토 대기 중인 제출만 가능하며, 제출과 신청을 모두 '수정 요청' 상태로 바꾸고 참여자에게 알립니다.
   *
   * @throws ResponseStatusException 소유 캠페인이 아니면 거절, 상태나 버전이 맞지 않으면 409
   */
  @Transactional
  public CampaignContentDtos.Response requestRevision(
      Long memberId, Long submissionId, CampaignContentDtos.ReviewRequest request) {
    CampaignContentSubmission submission = getOwnedSubmission(memberId, submissionId);
    requireVersion(submission, request.expectedVersion());
    transitionSubmission(submission, value -> value.requestRevision(request.comment().trim()));
    transitionApplication(submission.getApplication(), CampaignApplication::requestContentRevision);
    notifyCreatorAndAudit(
        memberId,
        submission,
        "콘텐츠 수정 요청이 도착했습니다.",
        "CAMPAIGN_CONTENT_REVISION_REQUESTED",
        "REVISION_REQUESTED",
        request.comment().trim());
    return flushedResponse(submission);
  }

  /**
   * 브랜드(캠페인 소유 업체)가 콘텐츠 확인을 마칩니다.
   *
   * <p>제출 상태만 '브랜드 확인 완료'로 바뀌고 신청 상태는 그대로입니다. 캠페인 완료는 운영자 최종 승인에서 처리합니다.
   *
   * @throws ResponseStatusException 소유 캠페인이 아니면 거절, 상태나 버전이 맞지 않으면 409
   */
  @Transactional
  public CampaignContentDtos.Response approve(
      Long memberId, Long submissionId, CampaignContentDtos.ApproveRequest request) {
    CampaignContentSubmission submission = getOwnedSubmission(memberId, submissionId);
    requireVersion(submission, request.expectedVersion());
    transitionSubmission(submission, value -> value.approveByBrand(normalize(request.comment())));
    notifyCreatorAndAudit(
        memberId,
        submission,
        "브랜드 콘텐츠 검토가 완료되었습니다.",
        "CAMPAIGN_CONTENT_BRAND_APPROVED",
        "BRAND_APPROVED",
        normalize(request.comment()));
    return flushedResponse(submission);
  }

  /**
   * 운영자(ADMIN)가 전체 콘텐츠 제출 목록을 최신순으로 조회합니다.
   *
   * @throws ResponseStatusException 운영자가 아니면 거절합니다
   */
  public List<CampaignContentDtos.Response> findOperatorContents(Long memberId) {
    memberService.getAdmin(memberId);
    return submissionRepository.findAllByOrderByIdDesc().stream().map(this::response).toList();
  }

  /**
   * 운영자가 콘텐츠 수정을 요청합니다. 검토 대기 또는 브랜드 확인 완료 상태에서 가능하며, 신청도 '수정 요청'으로 바뀝니다.
   *
   * @throws ResponseStatusException 운영자가 아니면 거절, 상태나 버전이 맞지 않으면 409
   */
  @Transactional
  public CampaignContentDtos.Response requestRevisionByOperator(
      Long memberId, Long submissionId, CampaignContentDtos.ReviewRequest request) {
    memberService.getAdmin(memberId);
    CampaignContentSubmission submission = getSubmission(submissionId);
    requireVersion(submission, request.expectedVersion());
    transitionSubmission(
        submission, value -> value.requestRevisionByOperator(request.comment().trim()));
    transitionApplication(submission.getApplication(), CampaignApplication::requestContentRevision);
    notifyCreatorAndAudit(
        memberId,
        submission,
        "운영자 콘텐츠 수정 요청이 도착했습니다.",
        "CAMPAIGN_CONTENT_OPERATOR_REVISION_REQUESTED",
        "REVISION_REQUESTED",
        request.comment().trim());
    return flushedResponse(submission);
  }

  /**
   * 운영자가 콘텐츠를 최종 승인합니다.
   *
   * <p>제출은 '최종 승인', 신청은 '완료'로 바뀌며 참여자에게 완료 알림을 보냅니다. 완료 횟수는 이 두 상태를 함께 만족한 신청만 셉니다.
   *
   * @throws ResponseStatusException 운영자가 아니면 거절, 상태나 버전이 맞지 않으면 409
   */
  @Transactional
  public CampaignContentDtos.Response approveByOperator(
      Long memberId, Long submissionId, CampaignContentDtos.ApproveRequest request) {
    memberService.getAdmin(memberId);
    CampaignContentSubmission submission = getSubmission(submissionId);
    requireVersion(submission, request.expectedVersion());
    transitionSubmission(
        submission, value -> value.approveByOperator(normalize(request.comment())));
    transitionApplication(submission.getApplication(), CampaignApplication::completeContentReview);
    notifyCreatorAndAudit(
        memberId,
        submission,
        "콘텐츠 최종 검토가 완료되었습니다.",
        "CAMPAIGN_CONTENT_OPERATOR_APPROVED",
        "APPROVED",
        normalize(request.comment()));
    return flushedResponse(submission);
  }

  /** 제출 기록을 찾고, 요청한 업체 회원이 해당 캠페인 상품의 소유자인지 확인합니다. */
  private CampaignContentSubmission getOwnedSubmission(Long memberId, Long submissionId) {
    CampaignContentSubmission submission = getSubmission(submissionId);
    campaignOwnership.requireOwner(memberId, submission.getApplication().getCampaign());
    return submission;
  }

  private CampaignContentSubmission getSubmission(Long submissionId) {
    return submissionRepository
        .findById(submissionId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "콘텐츠 제출 내역을 찾을 수 없습니다."));
  }

  /** 검토 결과를 참여자에게 알리고, 누가 어떤 상태로 바꿨는지 감사 로그에 남깁니다. */
  private void notifyCreatorAndAudit(
      Long actorMemberId,
      CampaignContentSubmission submission,
      String title,
      String action,
      String status,
      String reason) {
    Long creatorMemberId = submission.getApplication().getCreator().getMember().getId();
    notificationService.send(
        creatorMemberId,
        "CAMPAIGN_CONTENT",
        title,
        submission.getApplication().getCampaign().getTitle(),
        "CAMPAIGN_CONTENT",
        submission.getId());
    auditLogService.record(
        actorMemberId,
        action,
        "CAMPAIGN_CONTENT",
        submission.getId(),
        "{\"status\":\"SUBMITTED\"}",
        "{\"status\":\"" + status + "\"}",
        reason);
  }

  /** 엔티티의 상태 규칙 위반(IllegalStateException)을 409 응답으로 바꿔 화면에 사유를 전달합니다. */
  private void transitionApplication(
      CampaignApplication application, Consumer<CampaignApplication> action) {
    try {
      action.accept(application);
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
    }
  }

  /** 제출 상태 규칙 위반을 409 응답으로 바꿉니다. */
  private void transitionSubmission(
      CampaignContentSubmission submission, Consumer<CampaignContentSubmission> action) {
    try {
      action.accept(submission);
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
    }
  }

  private CampaignContentDtos.Response response(CampaignContentSubmission submission) {
    return new CampaignContentDtos.Response(
        submission.getId(),
        submission.getApplication().getId(),
        submission.getApplication().getCreator().getNickname(),
        submission.getApplication().getCampaign().getTitle(),
        submission.getContentUrl(),
        submission.getNote(),
        submission.getStatus(),
        submission.getRevisionCount(),
        submission.getVersion(),
        submission.getReviewComment(),
        submission.getSubmittedAt(),
        submission.getReviewedAt(),
        linkRepository.findAllBySubmissionIdOrderByIdAsc(submission.getId()).stream()
            .map(
                link ->
                    new CampaignContentDtos.LinkResponse(
                        link.getChannelType(), link.getContentUrl()))
            .toList());
  }

  private CampaignContentDtos.Response flushedResponse(CampaignContentSubmission submission) {
    // @Version 값은 flush 시 증가하므로 저장 응답을 만들기 전에 최신 값을 확정합니다.
    submissionRepository.flush();
    return response(submission);
  }

  private Long sellerMemberId(CampaignApplication application) {
    return application.getCampaign().getBrand().getSeller().getMember().getId();
  }

  /**
   * 콘텐츠 제출 기간을 확인합니다.
   *
   * <p>수정 요청에 대한 재제출은 기간과 관계없이 허용하고, 처음 제출만 시작·마감 시각을 검사합니다. 일정이 비어 있으면 그 경계는 검사하지 않습니다.
   */
  private void requireSubmissionPeriod(
      CampaignApplication application, CampaignContentSubmission submission) {
    if (submission != null && submission.getStatus() == ContentSubmissionStatus.REVISION_REQUESTED)
      return;
    LocalDateTime start = application.getCampaign().getContentSubmissionStartAt();
    LocalDateTime end = application.getCampaign().getContentSubmissionEndAt();
    LocalDateTime now = LocalDateTime.now();
    if ((start != null && now.isBefore(start)) || (end != null && now.isAfter(end))) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 콘텐츠 제출 기간이 아닙니다.");
    }
  }

  /** 주소 형식만 검사하며 외부 사이트에 접속하거나 게시물을 수집하지 않습니다. */
  private void validateLink(
      com.movrit.movrit.campaigncontent.entity.ContentChannel channel, String value) {
    try {
      URI uri = URI.create(value.trim());
      String scheme = uri.getScheme();
      String host = uri.getHost();
      if ((!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)))
          || host == null
          || uri.getUserInfo() != null
          || blockedHost(host)) {
        throw invalidLink();
      }
      String normalizedHost = host.toLowerCase(Locale.ROOT);
      String path = uri.getPath() == null ? "" : uri.getPath();
      if (channel == com.movrit.movrit.campaigncontent.entity.ContentChannel.INSTAGRAM
          && (!(normalizedHost.equals("instagram.com")
                  || normalizedHost.equals("www.instagram.com"))
              || !(path.startsWith("/p/")
                  || path.startsWith("/reel/")
                  || path.startsWith("/tv/")))) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Instagram은 instagram.com의 게시물 또는 릴스 URL을 입력해 주세요.");
      }
    } catch (IllegalArgumentException exception) {
      throw invalidLink();
    }
  }

  /** 내 컴퓨터(localhost)나 사내망 주소처럼 외부에 공개되지 않은 주소를 막습니다. */
  private boolean blockedHost(String host) {
    String value = host.toLowerCase(Locale.ROOT);
    if (value.equals("localhost")
        || value.equals("::1")
        || value.equals("0.0.0.0")
        || value.endsWith(".local")
        || value.startsWith("127.")
        || value.startsWith("10.")
        || value.startsWith("192.168.")) return true;
    if (value.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")) return true;
    return false;
  }

  private ResponseStatusException invalidLink() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "외부 공개 콘텐츠의 http 또는 https URL을 입력해 주세요.");
  }

  /** 화면이 마지막으로 본 버전과 현재 버전이 다르면 거절해, 다른 사람의 검토 결과를 모르고 덮어쓰지 않게 합니다. */
  private void requireVersion(CampaignContentSubmission submission, Long expectedVersion) {
    if (!java.util.Objects.equals(submission.getVersion(), expectedVersion)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "콘텐츠 제출 상태가 변경되었습니다. 다시 조회해 주세요.");
    }
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}

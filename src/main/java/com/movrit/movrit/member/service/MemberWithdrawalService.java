package com.movrit.movrit.member.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.member.dto.MemberWithdrawalDtos;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 본인 회원 탈퇴를 처리합니다.
 *
 * <ul>
 *   <li>탈퇴 전 확인: 선정·참여 중·콘텐츠 제출·수정 요청 단계의 캠페인이 있거나(참여자), 공개 중인 캠페인이 있으면(업체) 탈퇴할 수 없습니다. 운영자 계정도 탈퇴할
 *       수 없습니다.
 *   <li>확인 문구 '탈퇴합니다'를 정확히 입력해야 합니다.
 *   <li>탈퇴하면 상태를 WITHDRAWN으로 바꾸고 이메일·닉네임·이름·연락처·비밀번호·사진을 지웁니다. 소셜 로그인 연결과 로그인 유지 토큰도 끊습니다.
 *   <li>캠페인·주문 기록은 업체 정산과 법적 보관을 위해 남지만 화면에는 '탈퇴한 회원'으로 보입니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {
  static final String CONFIRM_TEXT = "탈퇴합니다";
  private static final List<CampaignApplicationStatus> IN_PROGRESS =
      List.of(
          CampaignApplicationStatus.SELECTED,
          CampaignApplicationStatus.ACTIVE,
          CampaignApplicationStatus.CONTENT_SUBMITTED,
          CampaignApplicationStatus.REVISION_REQUESTED);

  private final MemberRepository memberRepository;
  private final MemberSocialAccountRepository socialAccountRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final CampaignRepository campaignRepository;
  private final AuditLogService auditLogService;

  /** 지금 탈퇴할 수 있는지와, 막는 이유 목록입니다(화면에서 미리 안내). */
  @Transactional(readOnly = true)
  public MemberWithdrawalDtos.Eligibility check(Long memberId) {
    Member member = active(memberId);
    return new MemberWithdrawalDtos.Eligibility(blockers(member).isEmpty(), blockers(member));
  }

  /**
   * 탈퇴합니다.
   *
   * @throws ResponseStatusException 확인 문구가 다르면 400, 진행 중인 캠페인·공개 캠페인이 있거나 운영자면 409
   */
  @Transactional
  public void withdraw(Long memberId, String confirmText) {
    if (!CONFIRM_TEXT.equals(confirmText == null ? "" : confirmText.trim())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "확인 문구 '탈퇴합니다'를 정확히 입력해 주세요.");
    }
    Member member =
        memberRepository
            .findByIdForUpdate(memberId)
            .filter(value -> value.getDeletedAt() == null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    List<String> reasons = blockers(member);
    if (!reasons.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ", reasons));
    }
    // 감사 기록은 회원 정보를 지우기 전에 남깁니다(기록 시 회원 조회가 필요합니다).
    auditLogService.record(
        memberId,
        "MEMBER_WITHDRAWN",
        "MEMBER",
        memberId,
        "{\"status\":\"ACTIVE\"}",
        "{\"status\":\"WITHDRAWN\"}",
        null);

    socialAccountRepository.deleteAll(socialAccountRepository.findAllByMemberId(memberId));
    refreshTokenRepository.findByMemberId(memberId).ifPresent(token -> token.revoke());
    member.anonymizeForWithdrawal();
  }

  private Member active(Long memberId) {
    return memberRepository
        .findById(memberId)
        .filter(value -> value.getDeletedAt() == null)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
  }

  /** 탈퇴를 막는 이유들입니다. 비어 있으면 탈퇴할 수 있습니다. */
  private List<String> blockers(Member member) {
    List<String> reasons = new ArrayList<>();
    if (member.isAdmin()) reasons.add("운영자 계정은 탈퇴할 수 없습니다.");
    long inProgress =
        applicationRepository.countByCreatorMemberIdAndStatusIn(member.getId(), IN_PROGRESS);
    if (inProgress > 0) {
      reasons.add("진행 중인 캠페인이 " + inProgress + "건 있습니다. 완료되거나 브랜드가 참여를 취소한 뒤 탈퇴할 수 있습니다.");
    }
    long openCampaigns = campaignRepository.countByBrandSellerMemberIdAndOpenTrue(member.getId());
    if (openCampaigns > 0) {
      reasons.add("공개 중인 캠페인이 " + openCampaigns + "개 있습니다. 캠페인을 마감한 뒤 탈퇴할 수 있습니다.");
    }
    return reasons;
  }
}

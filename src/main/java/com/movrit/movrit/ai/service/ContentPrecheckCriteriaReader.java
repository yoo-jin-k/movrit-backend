package com.movrit.movrit.ai.service;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.entity.ContentChannel;
import com.movrit.movrit.collaboration.entity.Campaign;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 제출 전 점검에 쓸 캠페인 기준을 읽습니다.
 *
 * <p>AI 호출(최대 40초) 동안 DB 연결을 잡고 있지 않도록, 필요한 값만 짧은 읽기 트랜잭션에서 꺼내 {@link Criteria}로 돌려줍니다.
 *
 * <ul>
 *   <li>권한: 본인 신청만 읽습니다. 다른 회원의 신청이거나 없는 신청이면 404입니다.
 *   <li>단계: 참여 진행 중(ACTIVE)이거나 수정 요청(REVISION_REQUESTED)을 받은 신청만 점검합니다. 그 밖의 단계는 409입니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ContentPrecheckCriteriaReader {
  private final CampaignApplicationRepository applications;

  /**
   * 점검 기준입니다. 업체가 쓴 글이므로 프롬프트에서는 자료로만 넘깁니다.
   *
   * @param campaignTitle 캠페인 제목
   * @param campaignType 캠페인 종류 이름(PRODUCT 등)
   * @param contentChannel 캠페인이 정한 채널(없으면 null)
   * @param mission 참여 미션
   * @param submissionRequirement 제출 요건
   * @param caution 주의사항
   */
  public record Criteria(
      String campaignTitle,
      String campaignType,
      ContentChannel contentChannel,
      String mission,
      String submissionRequirement,
      String caution) {}

  /**
   * 본인 신청의 캠페인 기준을 읽습니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 점검할 수 없는 단계면 409
   */
  @Transactional(readOnly = true)
  public Criteria read(Long memberId, Long applicationId) {
    CampaignApplication application =
        applications
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    CampaignApplicationStatus status = application.getStatus();
    if (status != CampaignApplicationStatus.ACTIVE
        && status != CampaignApplicationStatus.REVISION_REQUESTED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "콘텐츠를 준비하는 단계(참여 중·수정 요청)에서만 점검할 수 있습니다.");
    }
    Campaign campaign = application.getCampaign();
    return new Criteria(
        campaign.getTitle(),
        campaign.getCampaignType() == null ? null : campaign.getCampaignType().name(),
        campaign.getContentChannel(),
        campaign.getMission(),
        campaign.getSubmissionRequirement(),
        campaign.getCaution());
  }
}

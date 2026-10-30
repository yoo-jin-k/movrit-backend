package com.movrit.movrit.campaigncontent.repository;

import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaigncontent.entity.CampaignContentSubmission;
import com.movrit.movrit.campaigncontent.entity.ContentSubmissionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 캠페인 콘텐츠 제출 기록을 저장하고 조회합니다. 신청 한 건에 제출 기록은 하나입니다. */
public interface CampaignContentSubmissionRepository
    extends JpaRepository<CampaignContentSubmission, Long> {
  Optional<CampaignContentSubmission> findByApplicationId(Long applicationId);

  /** 참여자 본인이 제출한 콘텐츠를 최신순으로 조회합니다. */
  List<CampaignContentSubmission> findAllByApplicationCreatorMemberIdOrderByIdDesc(Long memberId);

  /** 업체 회원이 소유한 브랜드의 캠페인에 제출된 콘텐츠를 최신순으로 조회합니다. */
  List<CampaignContentSubmission>
      findAllByApplicationCampaignPreorderProductBrandSellerMemberIdOrderByIdDesc(Long memberId);

  /** 운영자 검토 화면용으로 전체 제출 기록을 최신순으로 조회합니다. */
  List<CampaignContentSubmission> findAllByOrderByIdDesc();

  /** 참여자 포트폴리오용: 특정 참여자의 완료된 신청 중 최종 승인된 콘텐츠를 검토일 최신순으로 조회합니다. */
  @EntityGraph(attributePaths = {"application", "application.campaign"})
  List<CampaignContentSubmission>
      findAllByApplicationCreatorIdAndApplicationStatusAndStatusOrderByReviewedAtDesc(
          Long creatorId,
          CampaignApplicationStatus applicationStatus,
          ContentSubmissionStatus status);
}

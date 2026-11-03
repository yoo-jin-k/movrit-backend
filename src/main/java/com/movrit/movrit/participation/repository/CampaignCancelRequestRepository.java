package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.CampaignCancelRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 협의 취소 요청 저장소입니다. */
public interface CampaignCancelRequestRepository
    extends JpaRepository<CampaignCancelRequest, Long> {
  /** 신청의 가장 최근 요청입니다(확인 중이거나 마지막으로 처리된 요청). */
  Optional<CampaignCancelRequest> findFirstByApplicationIdOrderByIdDesc(Long applicationId);

  boolean existsByApplicationIdAndStatus(Long applicationId, String status);

  /** 운영자 확인 목록: 확인 중인 요청을 먼저, 오래된 순으로 보여 줍니다. */
  List<CampaignCancelRequest> findTop200ByOrderByStatusDescCreatedAtAsc();
}

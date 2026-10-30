package com.movrit.movrit.collaboration.repository;

import com.movrit.movrit.collaboration.entity.CampaignDetailImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 캠페인 상세 이미지를 저장하고 표시 순서대로 조회합니다. */
public interface CampaignDetailImageRepository extends JpaRepository<CampaignDetailImage, Long> {
  /** 캠페인 상세 이미지를 보여줄 순서대로 조회합니다. */
  List<CampaignDetailImage> findAllByCampaignIdOrderByDisplayOrderAsc(Long campaignId);

  /** 캠페인에 등록된 상세 이미지 개수입니다. 등록 가능 장수 제한을 확인할 때 사용합니다. */
  long countByCampaignId(Long campaignId);
}

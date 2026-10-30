package com.movrit.movrit.campaignmedia.repository;

import com.movrit.movrit.campaignmedia.entity.CampaignImageAsset;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 업로드된 캠페인 이미지 정보를 저장하고 조회합니다. */
public interface CampaignImageAssetRepository extends JpaRepository<CampaignImageAsset, String> {
  /** 캠페인에 올린 이미지 파일 기록을 모두 조회합니다. */
  List<CampaignImageAsset> findAllByCampaignId(Long campaignId);

  /** 이미지를 잠근 상태로 가져옵니다. 같은 이미지를 두 캠페인이 동시에 가져가 연결하는 일을 막습니다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select asset from CampaignImageAsset asset where asset.id = :id")
  Optional<CampaignImageAsset> findByIdForUpdate(@Param("id") String id);
}

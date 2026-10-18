package com.movrit.movrit.campaignmedia.repository;

import com.movrit.movrit.campaignmedia.entity.CampaignImageAsset;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CampaignImageAssetRepository extends JpaRepository<CampaignImageAsset, String> {
  List<CampaignImageAsset> findAllByCampaignId(Long campaignId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select asset from CampaignImageAsset asset where asset.id = :id")
  Optional<CampaignImageAsset> findByIdForUpdate(@Param("id") String id);
}

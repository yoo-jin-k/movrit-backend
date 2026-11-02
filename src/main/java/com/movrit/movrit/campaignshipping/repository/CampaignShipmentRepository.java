package com.movrit.movrit.campaignshipping.repository;

import com.movrit.movrit.campaignshipping.entity.CampaignShipment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 발송 정보 저장소입니다(키: 신청 ID). */
public interface CampaignShipmentRepository extends JpaRepository<CampaignShipment, Long> {
  List<CampaignShipment> findAllByApplicationIdIn(Collection<Long> applicationIds);
}

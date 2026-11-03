package com.movrit.movrit.participation.service;

import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.participation.dto.ParticipationChangeDtos;
import com.movrit.movrit.participation.entity.CampaignUnselectedRecord;
import com.movrit.movrit.participation.repository.CampaignUnselectedRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 업체 미선정 기록 조회입니다. 업체는 자기 기록만, 운영자는 전체(최근 200건)를 봅니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnselectedRecordService {
  private final CampaignUnselectedRecordRepository repository;
  private final CampaignRepository campaignRepository;
  private final MemberService memberService;

  /** 업체 본인의 미선정 기록과 횟수입니다. */
  public ParticipationChangeDtos.UnselectedSummary mine(Long sellerMemberId) {
    return summary(repository.findAllBySellerMemberIdOrderByRecordedAtDesc(sellerMemberId));
  }

  /** 운영자 전체 미선정 기록입니다. */
  public ParticipationChangeDtos.UnselectedSummary all(Long adminMemberId) {
    memberService.getAdmin(adminMemberId);
    return summary(repository.findTop200ByOrderByRecordedAtDesc());
  }

  private ParticipationChangeDtos.UnselectedSummary summary(
      List<CampaignUnselectedRecord> records) {
    List<ParticipationChangeDtos.Unselected> views =
        records.stream()
            .map(
                record ->
                    campaignRepository
                        .findById(record.getCampaignId())
                        .map(
                            campaign ->
                                new ParticipationChangeDtos.Unselected(
                                    campaign.getId(),
                                    campaign.getTitle(),
                                    campaign.getBrand().getName(),
                                    record.getApplicationCount(),
                                    record.getRecordedAt()))
                        .orElse(null))
            .filter(java.util.Objects::nonNull)
            .toList();
    return new ParticipationChangeDtos.UnselectedSummary(views.size(), views);
  }
}

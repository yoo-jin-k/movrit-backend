package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.campaignapplication.entity.*;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.Location;
import com.movrit.movrit.campaignmedia.entity.*;
import com.movrit.movrit.campaignmedia.repository.*;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.product.service.ProductService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignVisitEvidenceService {
  private final CampaignVisitHistoryRepository histories;
  private final CampaignVisitSnapshotRepository snapshots;
  private final CampaignMediaStateRepository states;
  private final CampaignApplicationRepository applications;
  private final CampaignRepository campaigns;
  private final ProductService products;
  private final SellerProfileService sellers;
  private final NotificationService notifications;
  private final ObjectMapper json;
  private static final Set<CampaignApplicationStatus> AFFECTED =
      EnumSet.of(
          CampaignApplicationStatus.SUBMITTED,
          CampaignApplicationStatus.SELECTED,
          CampaignApplicationStatus.ACTIVE,
          CampaignApplicationStatus.CONTENT_SUBMITTED,
          CampaignApplicationStatus.REVISION_REQUESTED);

  /** 호출자가 캠페인 잠금을 보유한 동일 트랜잭션에서 주소 이력과 내부 알림을 저장합니다. */
  @Transactional
  public void recordChange(
      Long actor, Campaign campaign, String before, String after, long version) {
    if (Objects.equals(location(before), location(after))) return;
    histories.save(
        CampaignVisitHistory.builder()
            .campaignId(campaign.getId())
            .actorMemberId(actor)
            .presentationVersion(version)
            .beforeLocationJson(before)
            .afterLocationJson(after)
            .legacyPlaceName(before == null ? campaign.getVisitPlaceName() : null)
            .legacyAddress(before == null ? campaign.getVisitAddress() : null)
            .changedAt(LocalDateTime.now())
            .build());
    notifyAffected(campaign.getId());
  }

  /** 구 주소 저장 경로의 자유 문자열도 파싱 없이 전후 원문을 보존합니다. */
  @Transactional
  public void recordLegacyChange(
      Long actor, Campaign campaign, String nextPlace, String nextAddress) {
    if (Objects.equals(campaign.getVisitPlaceName(), nextPlace)
        && Objects.equals(campaign.getVisitAddress(), nextAddress)) return;
    histories.save(
        CampaignVisitHistory.builder()
            .campaignId(campaign.getId())
            .actorMemberId(actor)
            .presentationVersion(0)
            .legacyPlaceName(campaign.getVisitPlaceName())
            .legacyAddress(campaign.getVisitAddress())
            .legacyAfterPlaceName(nextPlace)
            .legacyAfterAddress(nextAddress)
            .changedAt(LocalDateTime.now())
            .build());
    notifyAffected(campaign.getId());
  }

  private void notifyAffected(Long campaignId) {
    // 종료·취소·거절 및 배송 참여는 제외하며 주소 원문은 알림에 노출하지 않습니다.
    Set<Long> recipients = new HashSet<>();
    for (var application : applications.findAllByCampaignId(campaignId)) {
      if (application.getFulfillmentType() == FulfillmentType.VISIT
          && AFFECTED.contains(application.getStatus()))
        recipients.add(application.getCreator().getMember().getId());
    }
    for (Long recipient : recipients)
      notifications.send(
          recipient,
          "CAMPAIGN_VISIT_CHANGED",
          "캠페인 방문 장소가 변경되었습니다.",
          "캠페인 상세에서 변경된 방문 정보를 확인해 주세요.",
          "CAMPAIGN",
          campaignId);
  }

  /** 신규 방문 신청의 현재 값만 저장하며 과거 신청을 소급 생성하거나 덮어쓰지 않습니다. */
  @Transactional
  public void capture(CampaignApplication application) {
    if (application.getFulfillmentType() != FulfillmentType.VISIT) return;
    if (snapshots.existsById(application.getId()))
      throw new IllegalStateException("방문 신청 증빙은 덮어쓸 수 없습니다.");
    Campaign campaign = application.getCampaign();
    CampaignMediaState state = states.findById(campaign.getId()).orElse(null);
    snapshots.saveAndFlush(
        CampaignVisitSnapshot.builder()
            .applicationId(application.getId())
            .campaignId(campaign.getId())
            .presentationVersion(state == null ? 0 : state.getVersion())
            .locationJson(state == null ? null : state.getVisitLocationJson())
            .visitGuide(campaign.getVisitGuide())
            .legacyPlaceName(campaign.getVisitPlaceName())
            .legacyAddress(campaign.getVisitAddress())
            .capturedAt(LocalDateTime.now())
            .build());
  }

  public record SnapshotView(
      boolean available,
      long presentationVersion,
      Location visitLocation,
      String visitGuide,
      String legacyPlaceName,
      String legacyAddress,
      LocalDateTime capturedAt) {}

  public record HistoryView(
      Long id,
      long presentationVersion,
      Location beforeLocation,
      Location afterLocation,
      String legacyPlaceName,
      String legacyAddress,
      String legacyAfterPlaceName,
      String legacyAfterAddress,
      LocalDateTime changedAt) {}

  public SnapshotView snapshot(Long memberId, Long applicationId) {
    var app =
        applications.findById(applicationId).orElseThrow(CampaignVisitEvidenceService::missing);
    if (!app.getCreator().getMember().getId().equals(memberId))
      requireOwner(memberId, app.getCampaign());
    var saved = snapshots.findById(applicationId).orElse(null);
    return saved == null
        ? new SnapshotView(false, 0, null, null, null, null, null)
        : new SnapshotView(
            true,
            saved.getPresentationVersion(),
            location(saved.getLocationJson()),
            saved.getVisitGuide(),
            saved.getLegacyPlaceName(),
            saved.getLegacyAddress(),
            saved.getCapturedAt());
  }

  public List<HistoryView> history(Long memberId, Long campaignId) {
    var campaign =
        campaigns.findById(campaignId).orElseThrow(CampaignVisitEvidenceService::missing);
    requireOwner(memberId, campaign);
    return histories.findAllByCampaignIdOrderByIdDesc(campaignId).stream()
        .map(
            h ->
                new HistoryView(
                    h.getId(),
                    h.getPresentationVersion(),
                    location(h.getBeforeLocationJson()),
                    location(h.getAfterLocationJson()),
                    h.getLegacyPlaceName(),
                    h.getLegacyAddress(),
                    h.getLegacyAfterPlaceName(),
                    h.getLegacyAfterAddress(),
                    h.getChangedAt()))
        .toList();
  }

  private void requireOwner(Long memberId, Campaign campaign) {
    sellers.getApprovedSeller(memberId);
    products.getOwnedProduct(memberId, campaign.getPreorder().getProduct().getId());
  }

  private Location location(String value) {
    return value == null ? null : json.readValue(value, Location.class);
  }

  private static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 정보를 찾을 수 없습니다.");
  }
}

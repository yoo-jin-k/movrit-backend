package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.Location;
import com.movrit.movrit.campaignmedia.entity.CampaignMediaState;
import com.movrit.movrit.campaignmedia.entity.CampaignVisitHistory;
import com.movrit.movrit.campaignmedia.entity.CampaignVisitSnapshot;
import com.movrit.movrit.campaignmedia.repository.CampaignMediaStateRepository;
import com.movrit.movrit.campaignmedia.repository.CampaignVisitHistoryRepository;
import com.movrit.movrit.campaignmedia.repository.CampaignVisitSnapshotRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/**
 * 방문형 캠페인의 장소 변경 기록과 신청 당시 방문 정보 사본을 관리합니다. 장소가 바뀌면 영향을 받는 방문 참여자에게 알림을 보내고, 기록은 업체(소유자)와 해당 참여자만 볼
 * 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignVisitEvidenceService {
  private final CampaignVisitHistoryRepository histories;
  private final CampaignVisitSnapshotRepository snapshots;
  private final CampaignMediaStateRepository states;
  private final CampaignApplicationRepository applications;
  private final CampaignRepository campaigns;
  private final CampaignOwnershipService campaignOwnership;
  private final SellerProfileService sellers;
  private final NotificationService notifications;
  private final ObjectMapper json;

  /** 장소 변경 알림을 받을 신청 상태입니다. 아직 진행 중인 신청만 포함하고 완료·취소·미선정은 제외합니다. */
  private static final Set<CampaignApplicationStatus> AFFECTED =
      EnumSet.of(
          CampaignApplicationStatus.SUBMITTED,
          CampaignApplicationStatus.SELECTED,
          CampaignApplicationStatus.ACTIVE,
          CampaignApplicationStatus.CONTENT_SUBMITTED,
          CampaignApplicationStatus.REVISION_REQUESTED);

  /**
   * 새 이미지 관리 방식에서 방문 장소가 실제로 바뀌었을 때 변경 기록을 남기고 방문 참여자에게 알립니다. 장소가 같으면 아무것도 하지 않습니다. 호출하는 쪽이 캠페인을 잠근
   * 같은 트랜잭션 안에서 불러야 합니다.
   */
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

  /** 예전 방식(장소 이름·주소 칸)으로 방문 장소가 바뀌었을 때 변경 전후 값을 그대로 기록하고 참여자에게 알립니다. 값이 같으면 아무것도 하지 않습니다. */
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

  /** 진행 중인 방문형 참여자에게 '방문 장소 변경' 알림을 한 번씩 보냅니다. */
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

  /**
   * 방문형 신청이 접수되는 순간의 방문 장소·안내를 사본으로 저장합니다. 배송형 신청은 저장하지 않으며, 같은 신청의 사본이 이미 있으면 덮어쓰지 않고 오류로 처리합니다.
   */
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

  /** 신청 당시 방문 정보 응답입니다. available이 false면 저장된 사본이 없는 신청입니다. */
  public record SnapshotView(
      boolean available,
      long presentationVersion,
      Location visitLocation,
      String visitGuide,
      String legacyPlaceName,
      String legacyAddress,
      LocalDateTime capturedAt) {}

  /** 방문 장소 변경 기록 한 건의 응답입니다. */
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

  /** 신청 당시 방문 정보를 조회합니다. 신청자 본인 또는 캠페인 소유 업체만 볼 수 있습니다. */
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

  /** 캠페인 방문 장소 변경 기록을 최신순으로 조회합니다. 승인된 소유 업체만 볼 수 있습니다. */
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

  /** 승인된 업체이면서 해당 캠페인 상품의 소유자인지 확인합니다. */
  private void requireOwner(Long memberId, Campaign campaign) {
    sellers.getApprovedSeller(memberId);
    campaignOwnership.requireOwner(memberId, campaign);
  }

  private Location location(String value) {
    return value == null ? null : json.readValue(value, Location.class);
  }

  private static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 정보를 찾을 수 없습니다.");
  }
}

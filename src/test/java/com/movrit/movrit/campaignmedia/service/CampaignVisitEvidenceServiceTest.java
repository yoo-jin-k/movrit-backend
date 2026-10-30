package com.movrit.movrit.campaignmedia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.entity.*;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignmedia.entity.*;
import com.movrit.movrit.campaignmedia.repository.*;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class CampaignVisitEvidenceServiceTest {
  private final CampaignVisitHistoryRepository histories =
      mock(CampaignVisitHistoryRepository.class);
  private final CampaignVisitSnapshotRepository snapshots =
      mock(CampaignVisitSnapshotRepository.class);
  private final CampaignMediaStateRepository states = mock(CampaignMediaStateRepository.class);
  private final CampaignApplicationRepository applications =
      mock(CampaignApplicationRepository.class);
  private final NotificationService notifications = mock(NotificationService.class);
  private final SellerProfileService sellers = mock(SellerProfileService.class);
  private final CampaignVisitEvidenceService service =
      new CampaignVisitEvidenceService(
          histories,
          snapshots,
          states,
          applications,
          mock(CampaignRepository.class),
          mock(CampaignOwnershipService.class),
          sellers,
          notifications,
          new ObjectMapper());
  private final Campaign campaign =
      Campaign.builder()
          .brand(Brand.builder().id(99L).name("테스트 브랜드").build())
          .id(10L)
          .visitGuide("예약 후 방문해 주세요.")
          .build();
  private static final String OLD =
      "{\"venueName\":\"장소\",\"postalCode\":null,\"address1\":\"이전 주소\",\"address2\":null}";
  private static final String NEXT = OLD.replace("이전 주소", "변경 주소");

  @Test
  void unchangedAddressAndImageOnlyEditProduceNoHistoryOrNotification() {
    service.recordChange(1L, campaign, OLD, OLD.replace(":", ": "), 2L);
    service.recordChange(1L, campaign, null, null, 3L);
    verifyNoInteractions(histories, applications, notifications);
  }

  @Test
  void onlyOngoingVisitParticipantsReceiveOneInternalNotice() {
    List<CampaignApplication> rows = new ArrayList<>();
    for (var status : CampaignApplicationStatus.values())
      rows.add(application((long) status.ordinal() + 1, status, FulfillmentType.VISIT));
    rows.add(application(99L, CampaignApplicationStatus.ACTIVE, FulfillmentType.DELIVERY));
    rows.add(application(1L, CampaignApplicationStatus.SUBMITTED, FulfillmentType.VISIT));
    when(applications.findAllByCampaignId(10L)).thenReturn(rows);
    service.recordChange(100L, campaign, OLD, NEXT, 7L);
    ArgumentCaptor<CampaignVisitHistory> saved =
        ArgumentCaptor.forClass(CampaignVisitHistory.class);
    verify(histories).save(saved.capture());
    assertEquals(OLD, saved.getValue().getBeforeLocationJson());
    assertEquals(NEXT, saved.getValue().getAfterLocationJson());
    assertEquals(7, saved.getValue().getPresentationVersion());
    verify(notifications, times(5))
        .send(
            anyLong(),
            eq("CAMPAIGN_VISIT_CHANGED"),
            anyString(),
            anyString(),
            eq("CAMPAIGN"),
            eq(10L));
    verify(notifications, never())
        .send(eq(99L), anyString(), anyString(), anyString(), anyString(), anyLong());
  }

  @Test
  void visitSnapshotCapturesVersionAndDoesNotOverwriteExistingEvidence() {
    var app = application(1L, CampaignApplicationStatus.SUBMITTED, FulfillmentType.VISIT);
    when(states.findById(10L))
        .thenReturn(
            Optional.of(
                CampaignMediaState.builder()
                    .campaignId(10L)
                    .version(3)
                    .visitLocationJson(OLD)
                    .build()));
    service.capture(app);
    ArgumentCaptor<CampaignVisitSnapshot> saved =
        ArgumentCaptor.forClass(CampaignVisitSnapshot.class);
    verify(snapshots).saveAndFlush(saved.capture());
    assertEquals(OLD, saved.getValue().getLocationJson());
    assertEquals(3, saved.getValue().getPresentationVersion());
    assertEquals(campaign.getVisitGuide(), saved.getValue().getVisitGuide());
    when(snapshots.existsById(1L)).thenReturn(true);
    assertThrows(IllegalStateException.class, () -> service.capture(app));
    verify(snapshots, times(1)).saveAndFlush(any());
  }

  @Test
  void deliveryDoesNotCreateVisitEvidenceAndLegacyIsNotInferred() {
    service.capture(application(1L, CampaignApplicationStatus.SUBMITTED, FulfillmentType.DELIVERY));
    verifyNoInteractions(snapshots, states);
    service.capture(application(2L, CampaignApplicationStatus.SUBMITTED, FulfillmentType.VISIT));
    ArgumentCaptor<CampaignVisitSnapshot> saved =
        ArgumentCaptor.forClass(CampaignVisitSnapshot.class);
    verify(snapshots).saveAndFlush(saved.capture());
    assertNull(saved.getValue().getLocationJson());
    assertEquals(0, saved.getValue().getPresentationVersion());
  }

  @Test
  void pastApplicationReadReportsUnavailableWithoutBackfill() {
    when(applications.findById(1L))
        .thenReturn(
            Optional.of(
                application(1L, CampaignApplicationStatus.COMPLETED, FulfillmentType.VISIT)));
    assertFalse(service.snapshot(1L, 1L).available());
    verify(snapshots, never()).saveAndFlush(any());
  }

  @Test
  void unrelatedReaderMustPassSellerAuthorizationBeforeSnapshotLookup() {
    when(applications.findById(1L))
        .thenReturn(
            Optional.of(
                application(1L, CampaignApplicationStatus.SUBMITTED, FulfillmentType.VISIT)));
    doThrow(new IllegalStateException("업체 권한이 필요합니다.")).when(sellers).getApprovedSeller(99L);
    assertThrows(IllegalStateException.class, () -> service.snapshot(99L, 1L));
    verifyNoInteractions(snapshots);
  }

  @Test
  void legacyAddressChangesPreserveRawTextWithoutParsing() {
    campaign.updatePresentation("이전 장소", "이전 자유 주소");
    service.recordLegacyChange(1L, campaign, "다음 장소", "다음 자유 주소");
    ArgumentCaptor<CampaignVisitHistory> saved =
        ArgumentCaptor.forClass(CampaignVisitHistory.class);
    verify(histories).save(saved.capture());
    assertEquals("이전 자유 주소", saved.getValue().getLegacyAddress());
    assertEquals("다음 자유 주소", saved.getValue().getLegacyAfterAddress());
    assertNull(saved.getValue().getBeforeLocationJson());
    assertNull(saved.getValue().getAfterLocationJson());
    service.recordLegacyChange(1L, campaign, "이전 장소", "이전 자유 주소");
    verify(histories, times(1)).save(any());
  }

  private CampaignApplication application(
      Long memberId, CampaignApplicationStatus status, FulfillmentType type) {
    return CampaignApplication.builder()
        .id(memberId)
        .campaign(campaign)
        .creator(Creator.builder().member(Member.builder().id(memberId).build()).build())
        .status(status)
        .fulfillmentType(type)
        .build();
  }
}

package com.movrit.movrit.campaignmedia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos.*;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignapplication.service.CampaignApplicationService;
import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.*;
import com.movrit.movrit.campaignmedia.entity.*;
import com.movrit.movrit.campaignmedia.repository.*;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.service.ProductService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class CampaignMediaServiceTest {
  private static final String FIRST = "00000000-0000-0000-0000-000000000001";
  private static final String SECOND = "00000000-0000-0000-0000-000000000002";
  private final CampaignImageAssetRepository assets = mock(CampaignImageAssetRepository.class);
  private final CampaignMediaStateRepository states = mock(CampaignMediaStateRepository.class);
  private final CampaignRepository campaigns = mock(CampaignRepository.class);
  private final ProductService products = mock(ProductService.class);
  private final SellerProfileService sellers = mock(SellerProfileService.class);
  private final CampaignApplicationService forms = mock(CampaignApplicationService.class);
  private final CampaignApplicationRepository applications =
      mock(CampaignApplicationRepository.class);
  private final CampaignPrivateImageStorage storage = mock(CampaignPrivateImageStorage.class);
  private final CampaignMediaService service =
      new CampaignMediaService(
          assets,
          states,
          campaigns,
          products,
          sellers,
          forms,
          applications,
          storage,
          new ObjectMapper(),
          mock(CampaignVisitEvidenceService.class));
  @TempDir Path folder;

  @Test
  void anotherOwnerCannotPreviewOrResolvePrivatePath() {
    when(assets.findById(FIRST)).thenReturn(Optional.of(asset(FIRST, 2L, null)));
    assertStatus(404, () -> service.preview(1L, FIRST));
    verifyNoInteractions(storage);
  }

  @Test
  void revokedSellerIsRejectedBeforeAssetLookup() {
    doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "승인 후 이용할 수 있습니다."))
        .when(sellers)
        .getApprovedSeller(1L);
    assertStatus(403, () -> service.preview(1L, FIRST));
    verifyNoInteractions(assets, storage);
  }

  @Test
  void unattachedOrUnreferencedAssetIsNeverPublic() {
    when(assets.findById(FIRST)).thenReturn(Optional.of(asset(FIRST, 1L, null)));
    assertStatus(404, () -> service.content(FIRST));
    Campaign campaign = campaign(false);
    when(assets.findById(FIRST)).thenReturn(Optional.of(asset(FIRST, 1L, 10L)));
    when(campaigns.findById(10L)).thenReturn(Optional.of(campaign));
    when(states.findById(10L)).thenReturn(Optional.of(state(SECOND, 2)));
    assertStatus(404, () -> service.content(FIRST));
    verifyNoInteractions(storage);
  }

  @Test
  void closedRecruitmentStillHasPublicImageButDeletedProductDoesNot() throws Exception {
    Campaign campaign = campaign(false);
    when(assets.findById(FIRST)).thenReturn(Optional.of(asset(FIRST, 1L, 10L)));
    when(campaigns.findById(10L)).thenReturn(Optional.of(campaign));
    when(states.findById(10L)).thenReturn(Optional.of(state(FIRST, 2)));
    Path file = Files.write(folder.resolve("image.png"), new byte[] {1});
    when(storage.resolve(anyString())).thenReturn(file);
    assertEquals(file, service.content(FIRST).path());
    campaign.getPreorder().getProduct().softDelete();
    assertStatus(404, () -> service.content(FIRST));
  }

  @Test
  void versionConflictHappensBeforeAssetWrites() {
    owned(FulfillmentType.DELIVERY);
    when(states.findById(10L)).thenReturn(Optional.of(state(FIRST, 3)));
    assertStatus(409, () -> service.apply(1L, 10L, input(FIRST), 2L, false));
    verifyNoInteractions(assets);
  }

  @Test
  void wrongOwnerCannotBeAttachedAndExistingAssetRemainsUnchanged() {
    owned(FulfillmentType.DELIVERY);
    var foreign = asset(FIRST, 2L, null);
    when(assets.findByIdForUpdate(FIRST)).thenReturn(Optional.of(foreign));
    assertStatus(404, () -> service.apply(1L, 10L, input(FIRST), 0L, false));
    assertNull(foreign.getCampaignId());
    verify(states, never()).saveAndFlush(any());
    verify(assets, never()).flush();
  }

  @Test
  void replacesReferencesAndReturnsIncrementedVersion() {
    owned(FulfillmentType.DELIVERY);
    var previous = asset(FIRST, 1L, 10L);
    var next = asset(SECOND, 1L, null);
    var state = state(FIRST, 3);
    when(states.findById(10L)).thenReturn(Optional.of(state));
    when(assets.findAllByCampaignId(10L)).thenReturn(List.of(previous));
    when(assets.findByIdForUpdate(FIRST)).thenReturn(Optional.of(previous));
    when(assets.findByIdForUpdate(SECOND)).thenReturn(Optional.of(next));
    when(states.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var result = service.apply(1L, 10L, input(SECOND), 3L, false);
    assertEquals(4, result.version());
    assertNull(previous.getCampaignId());
    assertEquals(10L, next.getCampaignId());
    assertEquals(SECOND, result.representativeImage().assetId());
    assertTrue(result.representativeImage().previewPath().endsWith("/preview"));
  }

  @Test
  void appliedCampaignCannotChangeFulfillmentType() {
    owned(FulfillmentType.VISIT);
    when(applications.existsByCampaignId(10L)).thenReturn(true);
    var next = new FormSaveRequest(List.of(FulfillmentType.DELIVERY), List.of(), List.of());
    assertStatus(409, () -> service.preflightUpdate(1L, 10L, input(FIRST), 0L, next));
    verifyNoInteractions(assets);
  }

  @Test
  void creationRequiresRepresentativeAndVisitLocationButLegacyImageEditDoesNot() {
    assertStatus(400, () -> CampaignMediaService.normalize(input(null), null, true, false));
    assertStatus(400, () -> CampaignMediaService.normalize(input(FIRST), null, true, true));
    assertNull(CampaignMediaService.normalize(input(null), null, false, true).visitLocation());
    var retained = state(null, 1);
    retained.replace(null, "[]", "{\"venueName\":\"기존\"}");
    assertStatus(400, () -> CampaignMediaService.normalize(input(null), retained, false, true));
  }

  @Test
  void validatesPostalCodeDuplicatesAndUnicodeDescriptionBoundary() {
    var location = new Location("매장", "12345", "주소", null);
    assertNotNull(
        CampaignMediaService.normalize(
            new PresentationInput(FIRST, List.of(), location), null, true, true));
    assertStatus(
        400,
        () ->
            CampaignMediaService.normalize(
                new PresentationInput(FIRST, List.of(), new Location("매장", "ABC12", "주소", null)),
                null,
                true,
                true));
    assertStatus(
        400,
        () ->
            CampaignMediaService.normalize(
                new PresentationInput(FIRST, List.of(new DetailInput(FIRST, null)), null),
                null,
                true,
                false));
    assertDoesNotThrow(
        () ->
            CampaignMediaService.normalize(
                new PresentationInput(
                    FIRST, List.of(new DetailInput(SECOND, "😀".repeat(200))), null),
                null,
                true,
                false));
    assertStatus(
        400,
        () ->
            CampaignMediaService.normalize(
                new PresentationInput(
                    FIRST, List.of(new DetailInput(SECOND, "😀".repeat(201))), null),
                null,
                true,
                false));
  }

  @Test
  void databaseFailureCleansNewPrivateFileWithoutIssuingPublicUrl() {
    var file = new MockMultipartFile("file", "image.png", "image/png", new byte[] {1});
    when(storage.store(file))
        .thenReturn(new CampaignPrivateImageStorage.StoredImage("key.png", "image/png", 12, 1, 1));
    when(assets.saveAndFlush(any())).thenThrow(new IllegalStateException("검수용 DB 실패"));
    assertThrows(IllegalStateException.class, () -> service.upload(1L, file));
    verify(storage).remove("key.png");
  }

  @Test
  void deliveryPublicResponseContainsNoRetainedLocation() {
    owned(FulfillmentType.DELIVERY);
    CampaignMediaState state = state(FIRST, 1);
    state.replace(FIRST, "[]", "{\"venueName\":\"이전\",\"address1\":\"주소\"}");
    state.replace(FIRST, "[]", null);
    when(states.findById(10L)).thenReturn(Optional.of(state));
    assertNull(service.publicPresentation(10L).visitLocation());
    assertNotNull(service.manage(1L, 10L).retainedVisitLocation());
  }

  @Test
  void jsonAddressOmissionPreservesWhileExplicitNullDeactivatesAtomically() {
    ObjectMapper mapper = new ObjectMapper();
    var omitted =
        mapper.readValue(
            "{\"representativeAssetId\":\"" + FIRST + "\",\"detailImages\":[]}",
            PresentationInput.class);
    var cleared =
        mapper.readValue(
            "{\"representativeAssetId\":\""
                + FIRST
                + "\",\"detailImages\":[],\"visitLocation\":null}",
            PresentationInput.class);
    assertFalse(omitted.visitLocationProvided());
    assertTrue(cleared.visitLocationProvided());
    var current = state(FIRST, 1);
    current.replace(FIRST, "[]", "{\"venueName\":\"매장\",\"address1\":\"주소\"}");
    assertEquals(
        "매장",
        CampaignMediaService.normalize(omitted, current, false, true).visitLocation().venueName());
    assertStatus(400, () -> CampaignMediaService.normalize(omitted, current, false, false));
    assertNull(CampaignMediaService.normalize(cleared, current, false, false).visitLocation());
  }

  @Test
  void newlyUploadedAssetHasOnlyAuthenticatedPreviewAndNoPublicUrl() {
    var file = new MockMultipartFile("file", "image.png", "image/png", new byte[] {1});
    when(storage.store(file))
        .thenReturn(new CampaignPrivateImageStorage.StoredImage("key.png", "image/png", 12, 1, 1));
    when(assets.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var response = service.upload(1L, file);
    assertEquals("READY_UNATTACHED", response.state());
    assertNull(response.publicUrl());
    assertTrue(response.previewPath().endsWith("/preview"));
  }

  private Campaign owned(FulfillmentType type) {
    Campaign campaign = campaign(true);
    when(campaigns.findByIdForUpdate(10L)).thenReturn(Optional.of(campaign));
    when(campaigns.findById(10L)).thenReturn(Optional.of(campaign));
    when(forms.getForm(10L))
        .thenReturn(new FormResponse(10L, 1L, List.of(type), List.of(), List.of()));
    return campaign;
  }

  private Campaign campaign(boolean open) {
    Product product = Product.builder().id(20L).build();
    return Campaign.builder()
        .id(10L)
        .preorder(Preorder.builder().id(30L).product(product).build())
        .open(open)
        .build();
  }

  private CampaignImageAsset asset(String id, Long owner, Long campaign) {
    return CampaignImageAsset.builder()
        .id(id)
        .ownerMemberId(owner)
        .campaignId(campaign)
        .storageKey(id + ".png")
        .contentType("image/png")
        .byteSize(12)
        .width(1)
        .height(1)
        .build();
  }

  private CampaignMediaState state(String representative, long version) {
    return CampaignMediaState.builder()
        .campaignId(10L)
        .version(version)
        .representativeAssetId(representative)
        .detailImagesJson("[]")
        .build();
  }

  private PresentationInput input(String representative) {
    return PresentationInput.withoutVisit(representative, List.of());
  }

  private void assertStatus(int code, Runnable action) {
    assertEquals(
        code, assertThrows(ResponseStatusException.class, action::run).getStatusCode().value());
  }
}

package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos.FormSaveRequest;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignapplication.service.CampaignApplicationService;
import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.*;
import com.movrit.movrit.campaignmedia.entity.*;
import com.movrit.movrit.campaignmedia.repository.*;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 캠페인 이미지(대표 1장·상세 최대 5장)와 방문 장소를 관리하는 새 방식 서비스입니다. 업체가 이미지를 먼저 올려 두고, 캠페인 저장 시 선택한 이미지만 캠페인에
 * 연결합니다. 이미지 파일은 비공개 폴더에 두고, 공개된 캠페인에 실제로 연결된 이미지만 누구나 볼 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignMediaService {
  private final CampaignImageAssetRepository assets;
  private final CampaignMediaStateRepository states;
  private final CampaignRepository campaigns;
  private final CampaignOwnershipService campaignOwnership;
  private final SellerProfileService sellers;
  private final CampaignApplicationService forms;
  private final CampaignApplicationRepository applications;
  private final CampaignPrivateImageStorage storage;
  private final ObjectMapper json;
  private final CampaignVisitEvidenceService visitEvidence;

  /**
   * 승인된 업체가 캠페인에 쓸 이미지를 미리 올립니다. 아직 어느 캠페인에도 연결되지 않은 상태로 저장됩니다. DB 저장이 실패하거나 트랜잭션이 취소되면 방금 저장한 파일도
   * 함께 지웁니다.
   */
  @Transactional
  public AssetResponse upload(Long memberId, MultipartFile file) {
    sellers.getApprovedSeller(memberId);
    var image = storage.store(file);
    // DB 저장이 실패하면 새 표시본도 정리하며 기존 파일은 건드리지 않습니다.
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status != STATUS_COMMITTED) storage.remove(image.key());
            }
          });
    }
    try {
      var asset =
          assets.saveAndFlush(
              CampaignImageAsset.builder()
                  .id(UUID.randomUUID().toString())
                  .ownerMemberId(memberId)
                  .storageKey(image.key())
                  .contentType(image.contentType())
                  .byteSize(image.byteSize())
                  .width(image.width())
                  .height(image.height())
                  .build());
      return new AssetResponse(
          asset.getId(),
          "READY_UNATTACHED",
          asset.getContentType(),
          asset.getByteSize(),
          asset.getWidth(),
          asset.getHeight(),
          previewPath(asset.getId()),
          null);
    } catch (RuntimeException exception) {
      storage.remove(image.key());
      throw exception;
    }
  }

  /** 내려줄 이미지 파일 경로와 형식입니다. */
  public record ImageFile(Path path, String contentType) {}

  /** 업체가 자기가 올린 이미지를 미리보기로 봅니다. 다른 업체의 이미지는 존재를 드러내지 않도록 404로 응답합니다. */
  public ImageFile preview(Long memberId, String assetId) {
    sellers.getApprovedSeller(memberId);
    var asset = asset(assetId);
    if (!asset.getOwnerMemberId().equals(memberId)) throw missing();
    return file(asset);
  }

  /** 누구나 볼 수 있는 공개 이미지 주소입니다. 공개된 캠페인에 현재 연결된 이미지만 내려주고, 연결이 끊겼거나 비공개인 경우는 404로 응답합니다. */
  public ImageFile content(String assetId) {
    var asset = asset(assetId);
    if (asset.getCampaignId() == null) throw missing();
    var campaign =
        campaigns.findById(asset.getCampaignId()).orElseThrow(CampaignMediaService::missing);
    var state = states.findById(campaign.getId()).orElseThrow(CampaignMediaService::missing);
    if (!CampaignPublicPolicy.visible(campaign) || !referencedIds(state).contains(assetId))
      throw missing();
    return file(asset);
  }

  private ImageFile file(CampaignImageAsset asset) {
    Path path = storage.resolve(asset.getStorageKey());
    if (!Files.isRegularFile(path)) throw missing();
    return new ImageFile(path, asset.getContentType());
  }

  /**
   * 캠페인 '한 번에 저장' 수정에서 본문·신청서를 바꾸기 전에 이미지·방문 정보 변경이 가능한지 미리 확인합니다.
   *
   * <p>캠페인을 잠근 뒤 확인하며, 거절되는 경우는 다음과 같습니다. 신청 이력이 있는데 참여 유형(배송/방문)을 바꿈, 방문형을 추가하면서 장소를 안 넣음, 유형을
   * 바꾸면서 이미지 정보를 함께 보내지 않음, 다른 화면에서 이미지가 먼저 바뀜.
   */
  /** configured의 본문/폼 변경보다 먼저 잠금·기대 버전·유형 변경을 검사합니다. */
  @Transactional
  public void preflightUpdate(
      Long memberId,
      Long campaignId,
      PresentationInput input,
      Long expectedVersion,
      FormSaveRequest nextForm) {
    Campaign campaign = owned(memberId, campaignId, true);
    CampaignMediaState current = states.findById(campaignId).orElse(null);
    List<FulfillmentType> before = forms.getForm(campaignId).allowedFulfillmentTypes();
    List<FulfillmentType> after = nextForm == null ? before : nextForm.allowedFulfillmentTypes();
    if (after == null) throw bad("캠페인 참여 유형을 입력해 주세요.");
    boolean changed = !new HashSet<>(before).equals(new HashSet<>(after));
    if (changed && applications.existsByCampaignId(campaignId)) {
      throw conflict("신청 이력이 있는 캠페인의 참여 유형은 변경할 수 없습니다.");
    }
    if (changed
        && after.contains(FulfillmentType.VISIT)
        && !before.contains(FulfillmentType.VISIT)
        && (input == null || input.visitLocation() == null)) {
      throw bad("방문 유형을 추가할 때 장소명과 기본 주소를 입력해 주세요.");
    }
    if (changed && current != null && input == null) {
      throw bad("참여 유형을 변경할 때 이미지와 방문 정보를 함께 확인해 주세요.");
    }
    if (input != null) {
      checkVersion(current, expectedVersion);
      normalize(input, current, false, after.contains(FulfillmentType.VISIT));
    }
  }

  /**
   * 선택한 이미지와 방문 장소를 캠페인에 실제로 저장합니다. '한 번에 저장' 트랜잭션 안에서만 호출합니다.
   *
   * <p>요청에 포함된 이미지는 캠페인에 연결하고 빠진 이미지는 연결을 끊습니다. 다른 업체의 이미지나 다른 캠페인에 이미 연결된 이미지는 쓸 수 없습니다. 수정이면 방문
   * 장소 변경 기록을 남기고, 예전 화면을 위해 대표·상세 이미지 주소도 함께 맞춥니다.
   */
  /** 같은 configured 트랜잭션 안에서만 호출하여 본문·폼·조건과 자산참조를 함께 저장합니다. */
  @Transactional
  public ManagedPresentation apply(
      Long memberId,
      Long campaignId,
      PresentationInput input,
      Long expectedVersion,
      boolean creating) {
    Campaign campaign = owned(memberId, campaignId, true);
    CampaignMediaState current = states.findById(campaignId).orElse(null);
    if (!creating) checkVersion(current, expectedVersion);
    boolean visit =
        forms.getForm(campaignId).allowedFulfillmentTypes().contains(FulfillmentType.VISIT);
    PresentationInput normalized = normalize(input, current, creating, visit);
    Set<String> requested = inputIds(normalized);
    // 캠페인 잠금 후 자산ID 정렬 순서로 잠가 다른 캠페인과의 교차 연결 경쟁도 방지합니다.
    Set<String> allIds = new TreeSet<>(requested);
    assets.findAllByCampaignId(campaignId).forEach(asset -> allIds.add(asset.getId()));
    List<CampaignImageAsset> locked = new ArrayList<>();
    for (String id : allIds) {
      var asset = assets.findByIdForUpdate(id).orElseThrow(CampaignMediaService::missing);
      if (!asset.getOwnerMemberId().equals(memberId)) throw missing();
      if (asset.getCampaignId() != null && !asset.getCampaignId().equals(campaignId)) {
        throw conflict("다른 캠페인에 연결된 이미지는 사용할 수 없습니다.");
      }
      locked.add(asset);
    }
    for (var asset : locked) {
      if (requested.contains(asset.getId())) asset.attach(campaignId);
      else asset.detach();
    }
    CampaignMediaState state =
        current == null
            ? CampaignMediaState.builder()
                .campaignId(campaignId)
                .version(0)
                .legacyRepresentativeUrl(campaign.getRepresentativeImageUrl())
                .legacyDetailUrl(campaign.getDetailImageUrl())
                .build()
            : current;
    String previousLocation = state.getVisitLocationJson();
    state.replace(
        normalized.representativeAssetId(),
        json.writeValueAsString(normalized.detailImages()),
        normalized.visitLocation() == null
            ? null
            : json.writeValueAsString(normalized.visitLocation()));
    assets.flush();
    state = states.saveAndFlush(state);
    // 구 화면에는 파일 경로 대신 공개 조건을 매번 검사하는 URL만 전달합니다.
    campaign.updateRepresentativeImage(
        state.getRepresentativeAssetId() == null
            ? state.getLegacyRepresentativeUrl()
            : publicPath(state.getRepresentativeAssetId()));
    campaign.updateDetailImage(
        normalized.detailImages().isEmpty()
            ? null
            : publicPath(normalized.detailImages().get(0).assetId()));
    if (!creating)
      visitEvidence.recordChange(
          memberId, campaign, previousLocation, state.getVisitLocationJson(), state.getVersion());
    Location location = normalized.visitLocation();
    // 레거시 자유 주소는 이미지 단독 변경이나 배송 전환 때문에 삭제하지 않습니다.
    // 배송 전용 공개 응답은 별도 정책으로 숨기고 소유자 이력은 보존합니다.
    if (location != null) campaign.updatePresentation(location.venueName(), location.address1());
    return managed(campaign, state, visit);
  }

  /** 업체 편집 화면에 보여줄 현재 이미지·방문 장소 구성과 버전을 조회합니다. 소유 업체만 볼 수 있습니다. */
  public ManagedPresentation manage(Long memberId, Long campaignId) {
    Campaign campaign = owned(memberId, campaignId, false);
    boolean visit =
        forms.getForm(campaignId).allowedFulfillmentTypes().contains(FulfillmentType.VISIT);
    return managed(campaign, states.findById(campaignId).orElse(null), visit);
  }

  /**
   * 사용자 캠페인 상세에 보여줄 이미지와 방문 장소를 조회합니다. 새 방식으로 전환 전인 캠페인은 예전 이미지 주소를 그대로 보여주고, 방문형이 아니면 장소를 숨깁니다.
   */
  public PublicPresentation publicPresentation(Long campaignId) {
    Campaign campaign = campaigns.findById(campaignId).orElseThrow(CampaignMediaService::missing);
    if (!CampaignPublicPolicy.visible(campaign)) throw missing();
    boolean visit =
        forms.getForm(campaignId).allowedFulfillmentTypes().contains(FulfillmentType.VISIT);
    var state = states.findById(campaignId).orElse(null);
    if (state == null)
      return new PublicPresentation(
          campaign.getRepresentativeImageUrl(),
          campaign.getDetailImageUrl() == null
              ? List.of()
              : List.of(new PublicImage(campaign.getDetailImageUrl(), null)),
          null);
    return new PublicPresentation(
        state.getRepresentativeAssetId() == null
            ? state.getLegacyRepresentativeUrl()
            : publicPath(state.getRepresentativeAssetId()),
        details(state).stream()
            .map(detail -> new PublicImage(publicPath(detail.assetId()), detail.altText()))
            .toList(),
        visit ? location(state.getVisitLocationJson()) : null);
  }

  /** 업체 편집 화면용 응답을 만듭니다. 캠페인이 공개 상태일 때만 공개 이미지 주소를 함께 넣습니다. */
  private ManagedPresentation managed(Campaign campaign, CampaignMediaState state, boolean visit) {
    if (state == null)
      return new ManagedPresentation(
          0,
          null,
          List.of(),
          null,
          null,
          campaign.getRepresentativeImageUrl(),
          campaign.getDetailImageUrl(),
          campaign.getVisitGuide());
    return new ManagedPresentation(
        state.getVersion(),
        state.getRepresentativeAssetId() == null
            ? null
            : managedImage(
                state.getRepresentativeAssetId(), null, CampaignPublicPolicy.visible(campaign)),
        details(state).stream()
            .map(
                detail ->
                    managedImage(
                        detail.assetId(), detail.altText(), CampaignPublicPolicy.visible(campaign)))
            .toList(),
        visit ? location(state.getVisitLocationJson()) : null,
        location(state.getRetainedVisitLocationJson()),
        state.getLegacyRepresentativeUrl(),
        state.getLegacyDetailUrl(),
        campaign.getVisitGuide());
  }

  private ManagedImage managedImage(String id, String alt, boolean open) {
    return new ManagedImage(id, previewPath(id), open ? publicPath(id) : null, alt);
  }

  /**
   * 화면에서 보낸 이미지·방문 장소 입력을 검사하고 저장할 형태로 정리합니다.
   *
   * <p>상세 이미지는 최대 5장, 같은 이미지는 한 번만 쓸 수 있습니다. 처음 만들 때나 이미 대표 이미지가 있을 때는 대표 이미지를 비울 수 없습니다. 배송 전용
   * 캠페인은 방문 주소를 받지 않고, 방문형은 장소명·기본 주소가 필수이며 우편번호는 숫자 5자리입니다. 방문 장소를 보내지 않았으면 기존 장소를 그대로 유지합니다.
   */
  static PresentationInput normalize(
      PresentationInput input, CampaignMediaState current, boolean creating, boolean visit) {
    if (input == null || input.detailImages() == null || input.detailImages().size() > 5) {
      throw bad("대표 이미지와 상세 이미지 목록을 확인해 주세요.");
    }
    String representative = normalizedId(input.representativeAssetId());
    if (representative == null
        && (creating || current != null && current.getRepresentativeAssetId() != null)) {
      throw bad("대표 이미지를 한 장 선택해 주세요.");
    }
    List<DetailInput> details = new ArrayList<>();
    Set<String> used = new HashSet<>();
    if (representative != null) used.add(representative);
    for (DetailInput detail : input.detailImages()) {
      if (detail == null) throw bad("상세 이미지 항목을 확인해 주세요.");
      String id = normalizedId(detail.assetId());
      if (id == null || !used.add(id)) throw bad("이미지를 중복 없이 선택해 주세요.");
      details.add(new DetailInput(id, text(detail.altText(), 200)));
    }
    Location location = input.visitLocation();
    if (!input.visitLocationProvided()
        && current != null
        && current.getVisitLocationJson() != null) {
      location = new ObjectMapper().readValue(current.getVisitLocationJson(), Location.class);
    }
    if (!visit && location != null) throw bad("배송 전용 캠페인에는 방문 주소를 입력할 수 없습니다.");
    if (visit
        && location == null
        && (creating
            || current != null
                && (current.getVisitLocationJson() != null
                    || current.getRetainedVisitLocationJson() != null))) {
      throw bad("방문 장소명과 기본 주소를 입력해 주세요.");
    }
    if (visit && location != null) {
      location =
          new Location(
              text(location.venueName(), 100),
              text(location.postalCode(), 5),
              text(location.address1(), 255),
              text(location.address2(), 255));
      if (location.venueName() == null || location.address1() == null)
        throw bad("방문 장소명과 기본 주소를 입력해 주세요.");
      if (location.postalCode() != null && !location.postalCode().matches("[0-9]{5}")) {
        throw bad("우편번호는 국내 숫자 5자리로 입력해 주세요.");
      }
    }
    return new PresentationInput(representative, List.copyOf(details), location);
  }

  /** 이미지 ID가 올바른 UUID 형식인지 확인합니다. 비어 있으면 '선택 안 함'으로 봅니다. */
  private static String normalizedId(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      String id = UUID.fromString(value).toString();
      if (!id.equals(value)) throw new IllegalArgumentException();
      return id;
    } catch (IllegalArgumentException exception) {
      throw bad("이미지 식별자가 올바르지 않습니다.");
    }
  }

  /** 앞뒤 공백을 지우고 글자 수 제한을 확인합니다. 빈 값은 null로 바꿉니다. */
  private static String text(String value, int maximum) {
    String trimmed = value == null ? null : value.trim();
    if (trimmed == null || trimmed.isEmpty()) return null;
    if (trimmed.codePointCount(0, trimmed.length()) > maximum) throw bad("입력 가능한 글자 수를 초과했습니다.");
    return trimmed;
  }

  private Set<String> inputIds(PresentationInput input) {
    Set<String> ids = new HashSet<>();
    if (input.representativeAssetId() != null) ids.add(input.representativeAssetId());
    input.detailImages().forEach(detail -> ids.add(detail.assetId()));
    return ids;
  }

  private Set<String> referencedIds(CampaignMediaState state) {
    return inputIds(new PresentationInput(state.getRepresentativeAssetId(), details(state), null));
  }

  private List<DetailInput> details(CampaignMediaState state) {
    return json.readValue(state.getDetailImagesJson(), new TypeReference<List<DetailInput>>() {});
  }

  private Location location(String value) {
    return value == null ? null : json.readValue(value, Location.class);
  }

  private CampaignImageAsset asset(String id) {
    return assets.findById(id).orElseThrow(CampaignMediaService::missing);
  }

  /** 승인된 업체이면서 캠페인 상품의 소유자인지 확인합니다. 저장 작업이면 캠페인을 잠가 동시 수정을 막습니다. */
  private Campaign owned(Long memberId, Long campaignId, boolean lock) {
    sellers.getApprovedSeller(memberId);
    Campaign campaign =
        (lock ? campaigns.findByIdForUpdate(campaignId) : campaigns.findById(campaignId))
            .orElseThrow(CampaignMediaService::missing);
    campaignOwnership.requireOwner(memberId, campaign);
    return campaign;
  }

  /** 화면이 알고 있는 이미지 구성 버전이 현재와 같은지 확인합니다. 다르면 다른 화면에서 먼저 바뀐 것이므로 409로 거절합니다. */
  static void checkVersion(CampaignMediaState state, Long expected) {
    long current = state == null ? 0 : state.getVersion();
    if (expected == null || expected != current)
      throw conflict("이미지 또는 방문 정보가 변경되었습니다. 최신 내용을 확인해 주세요.");
  }

  /** 업체 전용 미리보기 주소입니다. */
  private static String previewPath(String id) {
    return "/api/campaign-image-assets/" + id + "/preview";
  }

  /** 공개 이미지 주소입니다. 요청할 때마다 공개 조건을 다시 검사합니다. */
  private static String publicPath(String id) {
    return "/api/campaign-image-assets/" + id + "/content";
  }

  private static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지 또는 캠페인을 찾을 수 없습니다.");
  }

  private static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}

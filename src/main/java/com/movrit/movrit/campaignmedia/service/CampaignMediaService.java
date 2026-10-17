package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.*;
import com.movrit.movrit.campaignmedia.entity.*;
import com.movrit.movrit.campaignmedia.repository.*;
import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos.FormSaveRequest;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignapplication.service.CampaignApplicationService;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.product.service.ProductService;
import com.movrit.movrit.seller.service.SellerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class CampaignMediaService {
    private final CampaignImageAssetRepository assets;
    private final CampaignMediaStateRepository states;
    private final CampaignRepository campaigns;
    private final ProductService products;
    private final SellerProfileService sellers;
    private final CampaignApplicationService forms;
    private final CampaignApplicationRepository applications;
    private final CampaignPrivateImageStorage storage;
    private final ObjectMapper json;

    @Transactional
    public AssetResponse upload(Long memberId, MultipartFile file) {
        sellers.getApprovedSeller(memberId);
        var image = storage.store(file);
        // DB 저장이 실패하면 새 표시본도 정리하며 기존 파일은 건드리지 않습니다.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) storage.remove(image.key());
                }
            });
        }
        try {
            var asset = assets.saveAndFlush(CampaignImageAsset.builder().id(UUID.randomUUID().toString())
                    .ownerMemberId(memberId).storageKey(image.key()).contentType(image.contentType())
                    .byteSize(image.byteSize()).width(image.width()).height(image.height()).build());
            return new AssetResponse(asset.getId(), "READY_UNATTACHED", asset.getContentType(),
                    asset.getByteSize(), asset.getWidth(), asset.getHeight(), previewPath(asset.getId()), null);
        } catch (RuntimeException exception) {
            storage.remove(image.key());
            throw exception;
        }
    }

    public record ImageFile(Path path, String contentType) { }
    public ImageFile preview(Long memberId, String assetId) {
        sellers.getApprovedSeller(memberId);
        var asset = asset(assetId);
        if (!asset.getOwnerMemberId().equals(memberId)) throw missing();
        return file(asset);
    }
    public ImageFile content(String assetId) {
        var asset = asset(assetId);
        if (asset.getCampaignId() == null) throw missing();
        var campaign = campaigns.findById(asset.getCampaignId()).orElseThrow(CampaignMediaService::missing);
        var state = states.findById(campaign.getId()).orElseThrow(CampaignMediaService::missing);
        if (!CampaignPublicPolicy.visible(campaign) || !referencedIds(state).contains(assetId)) throw missing();
        return file(asset);
    }
    private ImageFile file(CampaignImageAsset asset) {
        Path path = storage.resolve(asset.getStorageKey());
        if (!Files.isRegularFile(path)) throw missing();
        return new ImageFile(path, asset.getContentType());
    }

    /** configured의 본문/폼 변경보다 먼저 잠금·기대 버전·유형 변경을 검사합니다. */
    @Transactional
    public void preflightUpdate(Long memberId, Long campaignId, PresentationInput input,
                                Long expectedVersion, FormSaveRequest nextForm) {
        Campaign campaign = owned(memberId, campaignId, true);
        CampaignMediaState current = states.findById(campaignId).orElse(null);
        List<FulfillmentType> before = forms.getForm(campaignId).allowedFulfillmentTypes();
        List<FulfillmentType> after = nextForm == null ? before : nextForm.allowedFulfillmentTypes();
        if (after == null) throw bad("캠페인 참여 유형을 입력해 주세요.");
        boolean changed = !new HashSet<>(before).equals(new HashSet<>(after));
        if (changed && applications.existsByCampaignId(campaignId)) {
            throw conflict("신청 이력이 있는 캠페인의 참여 유형은 변경할 수 없습니다.");
        }
        if (changed && after.contains(FulfillmentType.VISIT) && !before.contains(FulfillmentType.VISIT)
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

    /** 같은 configured 트랜잭션 안에서만 호출하여 본문·폼·조건과 자산참조를 함께 저장합니다. */
    @Transactional
    public ManagedPresentation apply(Long memberId, Long campaignId, PresentationInput input,
                                      Long expectedVersion, boolean creating) {
        Campaign campaign = owned(memberId, campaignId, true);
        CampaignMediaState current = states.findById(campaignId).orElse(null);
        if (!creating) checkVersion(current, expectedVersion);
        boolean visit = forms.getForm(campaignId).allowedFulfillmentTypes().contains(FulfillmentType.VISIT);
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
            if (requested.contains(asset.getId())) asset.attach(campaignId); else asset.detach();
        }
        CampaignMediaState state = current == null ? CampaignMediaState.builder().campaignId(campaignId)
                .version(0).legacyRepresentativeUrl(campaign.getRepresentativeImageUrl())
                .legacyDetailUrl(campaign.getDetailImageUrl()).build() : current;
        state.replace(normalized.representativeAssetId(), json.writeValueAsString(normalized.detailImages()),
                normalized.visitLocation() == null ? null : json.writeValueAsString(normalized.visitLocation()));
        assets.flush();
        state = states.saveAndFlush(state);
        // 구 화면에는 파일 경로 대신 공개 조건을 매번 검사하는 URL만 전달합니다.
        campaign.updateRepresentativeImage(state.getRepresentativeAssetId() == null
                ? state.getLegacyRepresentativeUrl() : publicPath(state.getRepresentativeAssetId()));
        campaign.updateDetailImage(normalized.detailImages().isEmpty() ? null : publicPath(normalized.detailImages().get(0).assetId()));
        Location location = normalized.visitLocation();
        // 레거시 자유 주소는 이미지 단독 변경이나 배송 전환 때문에 삭제하지 않습니다.
        // 배송 전용 공개 응답은 별도 정책으로 숨기고 소유자 이력은 보존합니다.
        if (location != null) campaign.updatePresentation(location.venueName(), location.address1());
        return managed(campaign, state, visit);
    }

    public ManagedPresentation manage(Long memberId, Long campaignId) {
        Campaign campaign = owned(memberId, campaignId, false);
        boolean visit = forms.getForm(campaignId).allowedFulfillmentTypes().contains(FulfillmentType.VISIT);
        return managed(campaign, states.findById(campaignId).orElse(null), visit);
    }
    public PublicPresentation publicPresentation(Long campaignId) {
        Campaign campaign = campaigns.findById(campaignId).orElseThrow(CampaignMediaService::missing);
        if (!CampaignPublicPolicy.visible(campaign)) throw missing();
        boolean visit = forms.getForm(campaignId).allowedFulfillmentTypes().contains(FulfillmentType.VISIT);
        var state = states.findById(campaignId).orElse(null);
        if (state == null) return new PublicPresentation(campaign.getRepresentativeImageUrl(),
                campaign.getDetailImageUrl() == null ? List.of() : List.of(new PublicImage(campaign.getDetailImageUrl(), null)), null);
        return new PublicPresentation(state.getRepresentativeAssetId() == null ? state.getLegacyRepresentativeUrl()
                : publicPath(state.getRepresentativeAssetId()), details(state).stream()
                .map(detail -> new PublicImage(publicPath(detail.assetId()), detail.altText())).toList(),
                visit ? location(state.getVisitLocationJson()) : null);
    }
    private ManagedPresentation managed(Campaign campaign, CampaignMediaState state, boolean visit) {
        if (state == null) return new ManagedPresentation(0, null, List.of(), null, null,
                campaign.getRepresentativeImageUrl(), campaign.getDetailImageUrl(), campaign.getVisitGuide());
        return new ManagedPresentation(state.getVersion(), state.getRepresentativeAssetId() == null ? null
                : managedImage(state.getRepresentativeAssetId(), null, CampaignPublicPolicy.visible(campaign)),
                details(state).stream().map(detail -> managedImage(detail.assetId(), detail.altText(), CampaignPublicPolicy.visible(campaign))).toList(),
                visit ? location(state.getVisitLocationJson()) : null,
                location(state.getRetainedVisitLocationJson()), state.getLegacyRepresentativeUrl(),
                state.getLegacyDetailUrl(), campaign.getVisitGuide());
    }
    private ManagedImage managedImage(String id, String alt, boolean open) {
        return new ManagedImage(id, previewPath(id), open ? publicPath(id) : null, alt);
    }

    static PresentationInput normalize(PresentationInput input, CampaignMediaState current, boolean creating, boolean visit) {
        if (input == null || input.detailImages() == null || input.detailImages().size() > 5) {
            throw bad("대표 이미지와 상세 이미지 목록을 확인해 주세요.");
        }
        String representative = normalizedId(input.representativeAssetId());
        if (representative == null && (creating || current != null && current.getRepresentativeAssetId() != null)) {
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
        if (!input.visitLocationProvided() && current != null && current.getVisitLocationJson() != null) {
            location = new ObjectMapper().readValue(current.getVisitLocationJson(), Location.class);
        }
        if (!visit && location != null) throw bad("배송 전용 캠페인에는 방문 주소를 입력할 수 없습니다.");
        if (visit && location == null && (creating || current != null
                && (current.getVisitLocationJson() != null || current.getRetainedVisitLocationJson() != null))) {
            throw bad("방문 장소명과 기본 주소를 입력해 주세요.");
        }
        if (visit && location != null) {
            location = new Location(text(location.venueName(), 100), text(location.postalCode(), 5),
                    text(location.address1(), 255), text(location.address2(), 255));
            if (location.venueName() == null || location.address1() == null) throw bad("방문 장소명과 기본 주소를 입력해 주세요.");
            if (location.postalCode() != null && !location.postalCode().matches("[0-9]{5}")) {
                throw bad("우편번호는 국내 숫자 5자리로 입력해 주세요.");
            }
        }
        return new PresentationInput(representative, List.copyOf(details), location);
    }
    private static String normalizedId(String value) {
        if (value == null || value.isBlank()) return null;
        try { String id = UUID.fromString(value).toString(); if (!id.equals(value)) throw new IllegalArgumentException(); return id; }
        catch (IllegalArgumentException exception) { throw bad("이미지 식별자가 올바르지 않습니다."); }
    }
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
        return json.readValue(state.getDetailImagesJson(), new TypeReference<List<DetailInput>>() { });
    }
    private Location location(String value) { return value == null ? null : json.readValue(value, Location.class); }
    private CampaignImageAsset asset(String id) { return assets.findById(id).orElseThrow(CampaignMediaService::missing); }
    private Campaign owned(Long memberId, Long campaignId, boolean lock) {
        sellers.getApprovedSeller(memberId);
        Campaign campaign = (lock ? campaigns.findByIdForUpdate(campaignId) : campaigns.findById(campaignId))
                .orElseThrow(CampaignMediaService::missing);
        products.getOwnedProduct(memberId, campaign.getPreorder().getProduct().getId());
        return campaign;
    }
    static void checkVersion(CampaignMediaState state, Long expected) {
        long current = state == null ? 0 : state.getVersion();
        if (expected == null || expected != current) throw conflict("이미지 또는 방문 정보가 변경되었습니다. 최신 내용을 확인해 주세요.");
    }
    private static String previewPath(String id) { return "/api/campaign-image-assets/" + id + "/preview"; }
    private static String publicPath(String id) { return "/api/campaign-image-assets/" + id + "/content"; }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지 또는 캠페인을 찾을 수 없습니다."); }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}

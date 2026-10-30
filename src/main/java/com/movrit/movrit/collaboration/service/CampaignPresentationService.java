package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignmedia.repository.CampaignMediaStateRepository;
import com.movrit.movrit.campaignmedia.service.CampaignMediaService;
import com.movrit.movrit.campaignmedia.service.CampaignPublicPolicy;
import com.movrit.movrit.collaboration.dto.CampaignPresentationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignDetailImage;
import com.movrit.movrit.collaboration.repository.CampaignDetailImageRepository;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.product.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 캠페인 화면에 보여줄 대표·상세 이미지와 방문 장소를 관리하는 예전 방식(개별 API) 서비스입니다. 새 이미지 관리 방식(CampaignMediaService)으로 전환된
 * 캠페인은 이 서비스로 수정할 수 없고, 조회만 새 방식 결과로 대신 보여줍니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignPresentationService {
  private final CampaignRepository campaignRepository;
  private final CampaignDetailImageRepository detailImageRepository;
  private final ProductService productService;
  private final CampaignImageStorageService fileStorageService;
  private final CampaignMediaStateRepository mediaStates;
  private final CampaignApplicationFormRepository forms;
  private final CampaignMediaService mediaService;
  private final com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService visitEvidence;

  /**
   * 사용자에게 공개된 캠페인의 이미지와 방문 장소를 조회합니다. 새 방식으로 전환된 캠페인은 새 방식 데이터를 예전 응답 형태로 바꿔 보여주고, 방문형이 아닌 캠페인은 방문
   * 장소를 숨깁니다.
   */
  public CampaignPresentationDtos.Response find(Long campaignId) {
    Campaign campaign = get(campaignId);
    CampaignPublicPolicy.requireVisible(campaign);
    if (mediaStates.existsById(campaignId)) {
      var current = mediaService.publicPresentation(campaignId);
      var details =
          java.util.stream.IntStream.range(0, current.detailImages().size())
              .mapToObj(
                  index ->
                      new CampaignPresentationDtos.DetailImage(
                          null,
                          current.detailImages().get(index).imageUrl(),
                          index,
                          current.detailImages().get(index).altText()))
              .toList();
      return new CampaignPresentationDtos.Response(
          campaignId,
          current.representativeImageUrl(),
          details.isEmpty() ? null : details.get(0).imageUrl(),
          details,
          current.visitLocation() == null ? null : current.visitLocation().venueName(),
          current.visitLocation() == null ? null : current.visitLocation().address1());
    }
    var result = response(campaign);
    boolean visit =
        forms.findByCampaignId(campaignId).map(CampaignPublicPolicy::includesVisit).orElse(false);
    return new CampaignPresentationDtos.Response(
        result.campaignId(),
        result.representativeImageUrl(),
        result.detailImageUrl(),
        result.detailImages(),
        visit ? result.visitPlaceName() : null,
        visit ? result.visitAddress() : null);
  }

  /** 업체가 방문 장소 이름과 주소를 저장합니다. 바꾸기 전 값은 방문 이력으로 남깁니다. */
  @Transactional
  public CampaignPresentationDtos.Response saveLocation(
      Long memberId, Long campaignId, CampaignPresentationDtos.SaveRequest request) {
    Campaign campaign = owned(memberId, campaignId);
    String nextPlace = normalize(request.visitPlaceName());
    String nextAddress = normalize(request.visitAddress());
    visitEvidence.recordLegacyChange(memberId, campaign, nextPlace, nextAddress);
    campaign.updatePresentation(nextPlace, nextAddress);
    return response(campaign);
  }

  /** 업체가 목록 카드에 쓸 대표 이미지를 올립니다. 이미 있으면 새 이미지로 바뀝니다. */
  @Transactional
  public CampaignPresentationDtos.Response uploadRepresentative(
      Long memberId, Long campaignId, MultipartFile file) {
    Campaign campaign = owned(memberId, campaignId);
    campaign.updateRepresentativeImage(
        fileStorageService.storeImage(file, "campaigns/" + campaignId + "/representative"));
    return response(campaign);
  }

  /** 업체가 상세 이미지를 한 장 추가합니다. 최대 5장, 설명 문구는 200자까지입니다. 첫 번째로 올린 이미지는 예전 단일 상세 이미지 칸에도 함께 저장합니다. */
  @Transactional
  public CampaignPresentationDtos.Response uploadDetail(
      Long memberId, Long campaignId, MultipartFile file, String altText) {
    Campaign campaign = owned(memberId, campaignId);
    String description = normalize(altText);
    if (description != null && description.codePointCount(0, description.length()) > 200) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 설명은 200자 이하여야 합니다.");
    }
    long count = detailImageRepository.countByCampaignId(campaignId);
    if (count >= 5)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상세 이미지는 최대 5장까지 등록할 수 있습니다.");
    String url = fileStorageService.storeImage(file, "campaigns/" + campaignId + "/detail");
    detailImageRepository.save(
        CampaignDetailImage.builder()
            .campaign(campaign)
            .imageUrl(url)
            .displayOrder((int) count)
            .altText(description)
            .build());
    if (count == 0) campaign.updateDetailImage(url);
    return response(campaign);
  }

  /** 업체가 상세 이미지 표시 순서를 바꿉니다. 현재 등록된 이미지 ID 전체를 빠짐없이, 중복 없이 보내야 합니다. */
  @Transactional
  public CampaignPresentationDtos.Response reorderDetails(
      Long memberId, Long campaignId, CampaignPresentationDtos.ReorderRequest request) {
    Campaign campaign = owned(memberId, campaignId);
    List<CampaignDetailImage> images =
        detailImageRepository.findAllByCampaignIdOrderByDisplayOrderAsc(campaignId);
    List<Long> ids = request.detailImageIds();
    if (ids == null
        || ids.size() != images.size()
        || ids.stream().distinct().count() != ids.size()
        || !images.stream()
            .map(CampaignDetailImage::getId)
            .collect(java.util.stream.Collectors.toSet())
            .equals(new java.util.HashSet<>(ids))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 상세 이미지 전체를 중복 없이 순서대로 보내 주세요.");
    }
    // 같은 캠페인 안에서 순서 번호가 겹치면 저장이 막히므로, 먼저 모두 음수 임시 번호로 옮긴 뒤 새 순서를 매깁니다.
    for (CampaignDetailImage image : images) image.reorder(-(image.getDisplayOrder() + 1));
    detailImageRepository.flush();
    for (int i = 0; i < ids.size(); i++) {
      final Long id = ids.get(i);
      final int order = i;
      images.stream()
          .filter(image -> image.getId().equals(id))
          .findFirst()
          .orElseThrow()
          .reorder(order);
    }
    campaign.updateDetailImage(
        ids.isEmpty()
            ? null
            : images.stream()
                .filter(i -> i.getId().equals(ids.get(0)))
                .findFirst()
                .orElseThrow()
                .getImageUrl());
    return response(campaign);
  }

  /** 업체가 대표 이미지를 비웁니다. 저장된 파일은 지우지 않고 연결만 해제합니다. */
  @Transactional
  public CampaignPresentationDtos.Response clearRepresentative(Long memberId, Long campaignId) {
    Campaign campaign = owned(memberId, campaignId);
    campaign.updateRepresentativeImage(null);
    return response(campaign);
  }

  /** 업체가 상세 이미지를 모두 비웁니다. */
  @Transactional
  public CampaignPresentationDtos.Response clearDetail(Long memberId, Long campaignId) {
    Campaign campaign = owned(memberId, campaignId);
    detailImageRepository.deleteAll(
        detailImageRepository.findAllByCampaignIdOrderByDisplayOrderAsc(campaignId));
    campaign.updateDetailImage(null);
    return response(campaign);
  }

  /**
   * 수정할 캠페인을 잠금 상태로 가져오고 소유 업체인지 확인합니다. 새 이미지 관리 방식으로 전환된 캠페인이면 예전 방식 수정을 막아 두 방식의 데이터가 엇갈리지 않게
   * 합니다.
   */
  private Campaign owned(Long memberId, Long campaignId) {
    Campaign campaign =
        campaignRepository
            .findByIdForUpdate(campaignId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    productService.getOwnedProduct(memberId, campaign.getPreorder().getProduct().getId());
    if (mediaStates.existsById(campaignId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "새 이미지 관리 방식으로 전환된 캠페인입니다. 최신 정보를 조회하고 함께 저장해 주세요.");
    }
    return campaign;
  }

  private Campaign get(Long campaignId) {
    return campaignRepository
        .findById(campaignId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
  }

  /** 캠페인의 현재 이미지·방문 장소를 응답 형태로 만듭니다. */
  private CampaignPresentationDtos.Response response(Campaign campaign) {
    List<CampaignPresentationDtos.DetailImage> details =
        detailImageRepository.findAllByCampaignIdOrderByDisplayOrderAsc(campaign.getId()).stream()
            .map(
                image ->
                    new CampaignPresentationDtos.DetailImage(
                        image.getId(),
                        image.getImageUrl(),
                        image.getDisplayOrder(),
                        image.getAltText()))
            .toList();
    return new CampaignPresentationDtos.Response(
        campaign.getId(),
        campaign.getRepresentativeImageUrl(),
        campaign.getDetailImageUrl(),
        details,
        campaign.getVisitPlaceName(),
        campaign.getVisitAddress());
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}

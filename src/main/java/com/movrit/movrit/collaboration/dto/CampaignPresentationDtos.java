package com.movrit.movrit.collaboration.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/** 캠페인 및 협업의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class CampaignPresentationDtos {
  private CampaignPresentationDtos() {}

  /**
   * 캠페인 및 협업의 저장 요청을 전달합니다.
   *
   * @param visitPlaceName 방문 장소명입니다. 크기 제약은 max = 200입니다.
   * @param visitAddress 방문 주소입니다. 크기 제약은 max = 500입니다.
   */
  public record SaveRequest(
      @Size(max = 200) String visitPlaceName, @Size(max = 500) String visitAddress) {}

  /**
   * 캠페인 및 협업의 조회 및 처리 결과를 전달합니다.
   *
   * @param campaignId 캠페인 식별자입니다.
   * @param representativeImageUrl 대표 이미지 주소입니다.
   * @param detailImageUrl 기존 단일 상세 이미지 주소입니다.
   * @param detailImages 표시 순서대로 전달하는 상세 이미지 목록입니다.
   * @param visitPlaceName 방문 장소명입니다.
   * @param visitAddress 방문 주소입니다.
   */
  public record Response(
      Long campaignId,
      String representativeImageUrl,
      String detailImageUrl,
      List<DetailImage> detailImages,
      String visitPlaceName,
      String visitAddress) {}

  /**
   * 캠페인 및 협업의 상세 이미지 항목을 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param imageUrl 이미지 표시 주소입니다.
   * @param displayOrder 이미지 표시 순서입니다.
   * @param altText 이미지 대체 설명입니다.
   */
  public record DetailImage(Long id, String imageUrl, Integer displayOrder, String altText) {}

  /**
   * 캠페인 및 협업의 상세 이미지 순서 변경 요청을 전달합니다.
   *
   * @param detailImageIds 원하는 표시 순서대로 전달하는 상세 이미지 식별자 목록입니다. 크기 제약은 max = 5입니다.
   */
  public record ReorderRequest(@Size(max = 5) List<Long> detailImageIds) {}
}

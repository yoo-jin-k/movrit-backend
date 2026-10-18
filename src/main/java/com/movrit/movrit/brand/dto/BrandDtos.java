package com.movrit.movrit.brand.dto;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.brand.entity.BrandStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 브랜드의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class BrandDtos {
  private BrandDtos() {}

  /**
   * 브랜드의 생성 요청을 전달합니다.
   *
   * @param name 표시 이름입니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 max = 100입니다.
   * @param description 상세 설명입니다. 크기 제약은 max = 2000입니다.
   */
  public record CreateRequest(
      @NotBlank @Size(max = 100) String name, @Size(max = 2000) String description) {}

  /**
   * 브랜드의 수정 요청을 전달합니다.
   *
   * @param name 표시 이름입니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 max = 100입니다.
   * @param description 상세 설명입니다. 크기 제약은 max = 2000입니다.
   */
  public record UpdateRequest(
      @NotBlank @Size(max = 100) String name, @Size(max = 2000) String description) {}

  /**
   * 브랜드의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param sellerId 업체 프로필 식별자입니다.
   * @param name 표시 이름입니다.
   * @param description 상세 설명입니다.
   * @param logoUrl 브랜드 로고 주소입니다.
   * @param coverUrl 브랜드 커버 이미지 주소입니다.
   * @param status 현재 업무 상태입니다.
   */
  public record Response(
      Long id,
      Long sellerId,
      String name,
      String description,
      String logoUrl,
      String coverUrl,
      BrandStatus status) {
    /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
    public static Response from(Brand brand) {
      return new Response(
          brand.getId(),
          brand.getSeller().getId(),
          brand.getName(),
          brand.getDescription(),
          brand.getLogoUrl(),
          brand.getCoverUrl(),
          brand.getStatus());
    }
  }
}

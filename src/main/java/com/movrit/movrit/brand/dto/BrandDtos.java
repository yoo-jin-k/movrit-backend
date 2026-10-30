package com.movrit.movrit.brand.dto;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.brand.entity.BrandStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 브랜드 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
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
    /** 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
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

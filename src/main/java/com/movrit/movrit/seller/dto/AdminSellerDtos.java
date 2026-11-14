package com.movrit.movrit.seller.dto;

import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.entity.SellerStatus;
import java.time.LocalDateTime;

/** 운영자 업체 목록 화면의 응답 형식입니다. 정산 계좌 정보는 목록에 내려주지 않습니다(상세 화면에서만 확인). */
public final class AdminSellerDtos {

  private AdminSellerDtos() {}

  /**
   * 운영자 업체 목록의 한 줄입니다.
   *
   * @param createdAt 업체 등록 신청 시각(신청일 정렬 기준)
   * @param updatedAt 마지막 변경 시각(심사 처리·신청서 수정 포함)
   */
  public record ListItem(
      Long id,
      Long memberId,
      String businessName,
      String brandName,
      String businessNumber,
      String representativeName,
      SellerStatus status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    /** 엔티티를 목록 응답으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
    public static ListItem from(SellerProfile seller) {
      return new ListItem(
          seller.getId(),
          seller.getMember().getId(),
          seller.getBusinessName(),
          seller.getBrandName(),
          seller.getBusinessNumber(),
          seller.getRepresentativeName(),
          seller.getStatus(),
          seller.getCreatedAt(),
          seller.getUpdatedAt());
    }
  }
}

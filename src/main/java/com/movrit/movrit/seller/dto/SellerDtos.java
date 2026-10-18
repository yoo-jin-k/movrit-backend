package com.movrit.movrit.seller.dto;

import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.entity.SellerStatus;
import jakarta.validation.constraints.NotBlank;

public final class SellerDtos {
  private SellerDtos() {}

  /**
   * 판매자 등록 신청 값만 받습니다. memberId와 status를 클라이언트가 보내지 않는 이유는 로그인 토큰과 서버 정책으로 결정해야 다른 회원 명의로 신청하거나 임의로
   * APPROVED 상태를 만드는 것을 막을 수 있기 때문입니다.
   */
  public record ApplyRequest(
      @NotBlank String businessName,
      @NotBlank String brandName,
      @NotBlank String businessNumber,
      @NotBlank String representativeName,
      String settlementBankCode,
      String settlementAccountMasked) {}

  public record Response(
      Long id,
      Long memberId,
      String businessName,
      String brandName,
      String businessNumber,
      String representativeName,
      SellerStatus status,
      String settlementBankCode,
      String settlementAccountMasked) {
    public static Response from(SellerProfile seller) {
      return new Response(
          seller.getId(),
          seller.getMember().getId(),
          seller.getBusinessName(),
          seller.getBrandName(),
          seller.getBusinessNumber(),
          seller.getRepresentativeName(),
          seller.getStatus(),
          seller.getSettlementBankCode(),
          seller.getSettlementAccountMasked());
    }
  }
}

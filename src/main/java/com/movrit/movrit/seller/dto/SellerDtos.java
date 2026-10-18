package com.movrit.movrit.seller.dto;

import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.entity.SellerStatus;
import jakarta.validation.constraints.NotBlank;

/** 업체의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class SellerDtos {
  private SellerDtos() {}

  /**
   * 판매자 등록 신청 값만 받습니다. memberId와 status를 클라이언트가 보내지 않는 이유는 로그인 토큰과 서버 정책으로 결정해야 다른 회원 명의로 신청하거나 임의로
   * APPROVED 상태를 만드는 것을 막을 수 있기 때문입니다.
   *
   * <p>업체의 신청 요청을 전달합니다.
   *
   * @param businessName 사업자 상호입니다. 빈 문자열을 허용하지 않습니다.
   * @param brandName 브랜드명입니다. 빈 문자열을 허용하지 않습니다.
   * @param businessNumber 사업자등록번호입니다. 빈 문자열을 허용하지 않습니다.
   * @param representativeName 대표자 이름입니다. 빈 문자열을 허용하지 않습니다.
   * @param settlementBankCode 정산 은행 코드입니다.
   * @param settlementAccountMasked 마스킹된 정산 계좌 표시값이며 원문 계좌번호와 구분합니다.
   */
  public record ApplyRequest(
      @NotBlank String businessName,
      @NotBlank String brandName,
      @NotBlank String businessNumber,
      @NotBlank String representativeName,
      String settlementBankCode,
      String settlementAccountMasked) {}

  /**
   * 업체의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param memberId 회원 식별자입니다.
   * @param businessName 사업자 상호입니다.
   * @param brandName 브랜드명입니다.
   * @param businessNumber 사업자등록번호입니다.
   * @param representativeName 대표자 이름입니다.
   * @param status 현재 업무 상태입니다.
   * @param settlementBankCode 정산 은행 코드입니다.
   * @param settlementAccountMasked 마스킹된 정산 계좌 표시값이며 원문 계좌번호와 구분합니다.
   */
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
    /** 엔티티에서 공개할 응답 필드만 복사합니다. 이 변환 메서드 자체는 조회 권한을 판정하지 않습니다. */
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

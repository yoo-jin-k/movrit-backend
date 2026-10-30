package com.movrit.movrit.memberaddress.dto;

import com.movrit.movrit.memberaddress.entity.MemberAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 배송지 주소록 API의 요청·응답 형식을 모아 둡니다. */
public final class MemberAddressDtos {
  private MemberAddressDtos() {}

  /** 배송지를 추가하거나 고칠 때 보내는 값입니다. 기본 배송지로 지정할지도 함께 보냅니다. */
  public record SaveRequest(
      @NotBlank @Size(max = 30) String label,
      @NotBlank @Size(max = 50) String recipientName,
      @NotBlank @Pattern(regexp = "^[0-9+\\-]{8,20}$", message = "전화번호 형식이 올바르지 않습니다.")
          String recipientPhone,
      @NotBlank @Pattern(regexp = "^[0-9]{5,6}$", message = "우편번호 형식이 올바르지 않습니다.")
          String postalCode,
      @NotBlank @Size(max = 300) String address1,
      @Size(max = 300) String address2,
      boolean defaultAddress) {}

  /** 화면에 보여줄 배송지 한 건입니다. */
  public record Response(
      Long id,
      String label,
      String recipientName,
      String recipientPhone,
      String postalCode,
      String address1,
      String address2,
      boolean defaultAddress) {
    public static Response from(MemberAddress address) {
      return new Response(
          address.getId(),
          address.getLabel(),
          address.getRecipientName(),
          address.getRecipientPhone(),
          address.getPostalCode(),
          address.getAddress1(),
          address.getAddress2(),
          address.isDefaultAddress());
    }
  }
}

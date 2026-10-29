package com.movrit.movrit.memberaddress.dto;

import com.movrit.movrit.memberaddress.entity.MemberAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class MemberAddressDtos {
  private MemberAddressDtos() {}

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

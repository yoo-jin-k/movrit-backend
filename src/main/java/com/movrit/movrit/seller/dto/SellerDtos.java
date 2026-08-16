package com.movrit.movrit.seller.dto;

import com.movrit.movrit.seller.entity.SellerProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class SellerDtos {
    private SellerDtos() {}

    public record CreateRequest(
            @NotNull Long memberId,
            @NotBlank String businessName,
            String brandName,
            String businessNumber,
            String representativeName,
            @NotBlank String status,
            String settlementBankCode,
            String settlementAccountMasked
    ) {}

    public record UpdateRequest(
            @NotBlank String businessName,
            String brandName,
            String businessNumber,
            String representativeName,
            @NotBlank String status,
            String settlementBankCode,
            String settlementAccountMasked
    ) {}

    public record Response(
            Long id,
            Long memberId,
            String businessName,
            String brandName,
            String businessNumber,
            String representativeName,
            String status,
            String settlementBankCode,
            String settlementAccountMasked
    ) {
        public static Response from(SellerProfile seller) {
            return new Response(seller.getId(), seller.getMember().getId(), seller.getBusinessName(), seller.getBrandName(),
                    seller.getBusinessNumber(), seller.getRepresentativeName(), seller.getStatus(),
                    seller.getSettlementBankCode(), seller.getSettlementAccountMasked());
        }
    }
}

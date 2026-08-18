package com.movrit.movrit.seller.dto;

import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.entity.SellerStatus;
import jakarta.validation.constraints.NotBlank;

public final class SellerDtos {
    private SellerDtos() {}

    public record ApplyRequest(
            @NotBlank String businessName,
            @NotBlank String brandName,
            @NotBlank String businessNumber,
            @NotBlank String representativeName,
            String settlementBankCode,
            String settlementAccountMasked
    ) {}

    public record CreateRequest(
            @NotBlank String businessName,
            @NotBlank String brandName,
            @NotBlank String businessNumber,
            @NotBlank String representativeName,
            String settlementBankCode,
            String settlementAccountMasked
    ) {}

    public record UpdateRequest(
            @NotBlank String businessName,
            @NotBlank String brandName,
            @NotBlank String businessNumber,
            @NotBlank String representativeName,
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
            SellerStatus status,
            String settlementBankCode,
            String settlementAccountMasked
    ) {
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
                    seller.getSettlementAccountMasked()
            );
        }
    }
}

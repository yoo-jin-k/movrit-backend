package com.movrit.movrit.brand.dto;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.brand.entity.BrandStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class BrandDtos {
    private BrandDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 2000) String description
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 2000) String description
    ) {}

    public record Response(
            Long id,
            Long sellerId,
            String name,
            String description,
            String logoUrl,
            String coverUrl,
            BrandStatus status
    ) {
        public static Response from(Brand brand) {
            return new Response(
                    brand.getId(),
                    brand.getSeller().getId(),
                    brand.getName(),
                    brand.getDescription(),
                    brand.getLogoUrl(),
                    brand.getCoverUrl(),
                    brand.getStatus()
            );
        }
    }
}

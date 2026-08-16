package com.movrit.movrit.creator.dto;

import com.movrit.movrit.creator.entity.Creator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class CreatorDtos {
    private CreatorDtos() {}

    public record CreateRequest(
            @NotNull Long memberId,
            @NotBlank String nickname,
            String bio,
            String primaryCategory,
            @NotBlank String status
    ) {}

    public record UpdateRequest(
            @NotBlank String nickname,
            String bio,
            String primaryCategory,
            @NotBlank String status
    ) {}

    public record Response(
            Long id,
            Long memberId,
            String nickname,
            String bio,
            String primaryCategory,
            String status
    ) {
        public static Response from(Creator creator) {
            return new Response(
                    creator.getId(), creator.getMember().getId(), creator.getNickname(),
                    creator.getBio(), creator.getPrimaryCategory(), creator.getStatus()
            );
        }
    }
}

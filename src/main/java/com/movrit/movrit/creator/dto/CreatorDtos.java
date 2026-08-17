package com.movrit.movrit.creator.dto;
import com.movrit.movrit.creator.entity.Creator;
import jakarta.validation.constraints.*;

public final class CreatorDtos {
    private CreatorDtos() {}
    public record SaveRequest(@NotBlank @Size(max=100) String nickname, @Size(max=2000) String bio,
                              @Size(max=100) String primaryCategory, @Size(max=500) String snsUrl,
                              @NotNull @Min(0) Long followerCount) {}
    public record Response(Long id, Long memberId, String nickname, String bio, String primaryCategory,
                           String snsUrl, Long followerCount, String status) {
        public static Response from(Creator c) { return new Response(c.getId(),c.getMember().getId(),c.getNickname(),c.getBio(),c.getPrimaryCategory(),c.getSnsUrl(),c.getFollowerCount(),c.getStatus()); }
    }
}

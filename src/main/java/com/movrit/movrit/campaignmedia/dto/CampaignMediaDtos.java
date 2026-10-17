package com.movrit.movrit.campaignmedia.dto;

import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class CampaignMediaDtos {
    private CampaignMediaDtos() { }
    public record DetailInput(@NotNull String assetId, String altText) { }
    public record Location(String venueName, String postalCode, String address1, String address2) { }
    /** 주소 생략과 명시적 null을 구분하며 이미지 참조와 같은 저장 요청에 포함합니다. */
    public static final class PresentationInput {
        @JsonProperty private String representativeAssetId;
        @JsonProperty @NotNull @Size(max = 5) private List<@Valid DetailInput> detailImages;
        @Valid private Location visitLocation;
        @JsonIgnore private boolean visitLocationProvided;
        public PresentationInput() { }
        @JsonCreator(mode = JsonCreator.Mode.DISABLED)
        public PresentationInput(String representative, List<DetailInput> details, Location location) {
            representativeAssetId = representative; detailImages = details;
            visitLocation = location; visitLocationProvided = true;
        }
        public static PresentationInput withoutVisit(String representative, List<DetailInput> details) {
            PresentationInput input = new PresentationInput(representative, details, null);
            input.visitLocationProvided = false;
            return input;
        }
        public String representativeAssetId() { return representativeAssetId; }
        public List<DetailInput> detailImages() { return detailImages; }
        @JsonProperty("visitLocation") public Location visitLocation() { return visitLocation; }
        @JsonSetter("visitLocation") public void setVisitLocation(Location location) {
            visitLocation = location; visitLocationProvided = true;
        }
        @JsonIgnore public boolean visitLocationProvided() { return visitLocationProvided; }
    }
    public record AssetResponse(String assetId, String state, String contentType, long byteSize,
            int width, int height, String previewPath, String publicUrl) { }
    public record ManagedImage(String assetId, String previewPath, String publicUrl, String altText) { }
    public record ManagedPresentation(long version, ManagedImage representativeImage,
            List<ManagedImage> detailImages, Location visitLocation, Location retainedVisitLocation,
            String legacyRepresentativeImageUrl, String legacyDetailImageUrl, String legacyVisitGuide) { }
    public record PublicImage(String imageUrl, String altText) { }
    public record PublicPresentation(String representativeImageUrl, List<PublicImage> detailImages,
            Location visitLocation) { }
}

package com.movrit.movrit.campaignmedia.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 캠페인 이미지와 방문 주소의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class CampaignMediaDtos {
  private CampaignMediaDtos() {}

  /**
   * 캠페인 이미지와 방문 주소의 상세 이미지 참조를 전달합니다.
   *
   * @param assetId 업로드된 이미지 자산 식별자이며 임의 외부 URL과 구분합니다. null을 허용하지 않습니다.
   * @param altText 이미지 대체 설명입니다.
   */
  public record DetailInput(@NotNull String assetId, String altText) {}

  /**
   * 캠페인 이미지와 방문 주소의 방문 장소 정보를 전달합니다.
   *
   * @param venueName 방문 장소 이름입니다.
   * @param postalCode 우편번호입니다.
   * @param address1 기본 주소입니다.
   * @param address2 상세 주소입니다.
   */
  public record Location(String venueName, String postalCode, String address1, String address2) {}

  /** 주소 생략과 명시적 null을 구분하며 이미지 참조와 같은 저장 요청에 포함합니다. */
  public static final class PresentationInput {
    /** 업로드된 대표 이미지 자산 식별자입니다. */
    @JsonProperty private String representativeAssetId;

    /** 표시 순서대로 전달하는 상세 이미지 목록입니다. null을 허용하지 않습니다. 크기 제약은 max = 5입니다. */
    @JsonProperty
    @NotNull
    @Size(max = 5)
    private List<@Valid DetailInput> detailImages;

    /** 방문 주소이며 생략과 명시적 null의 의미를 구분해야 합니다. */
    @Valid private Location visitLocation;

    /** 주소 필드의 전달 여부이며, 생략 시 기존 주소 유지 판단에 사용합니다. */
    @JsonIgnore private boolean visitLocationProvided;

    /** JSON 역직렬화 시 주소가 생략된 상태로 시작합니다. 주소 setter 호출 여부로 명시적인 null과 구분합니다. */
    public PresentationInput() {}

    /** 내부 호출에서 이미지와 주소를 명시적으로 지정합니다. location이 null이어도 주소 필드를 전달한 것으로 기록합니다. */
    @JsonCreator(mode = JsonCreator.Mode.DISABLED)
    public PresentationInput(String representative, List<DetailInput> details, Location location) {
      representativeAssetId = representative;
      detailImages = details;
      visitLocation = location;
      visitLocationProvided = true;
    }

    /** 이미지만 변경하는 요청을 구성합니다. 기존 주소를 유지할 수 있도록 주소 전달 여부를 false로 둡니다. */
    public static PresentationInput withoutVisit(String representative, List<DetailInput> details) {
      PresentationInput input = new PresentationInput(representative, details, null);
      input.visitLocationProvided = false;
      return input;
    }

    /** 대표 이미지로 연결할 업로드 자산 ID를 반환합니다. */
    public String representativeAssetId() {
      return representativeAssetId;
    }

    /** 요청자가 지정한 표시 순서의 상세 이미지 참조를 반환합니다. */
    public List<DetailInput> detailImages() {
      return detailImages;
    }

    /** 요청에 담긴 방문 주소를 반환합니다. null만으로 주소 생략 여부를 판정하지 않습니다. */
    @JsonProperty("visitLocation")
    public Location visitLocation() {
      return visitLocation;
    }

    /** JSON에 주소 필드가 명시되었음을 기록합니다. null 전달도 주소 변경 의사로 구분합니다. */
    @JsonSetter("visitLocation")
    public void setVisitLocation(Location location) {
      visitLocation = location;
      visitLocationProvided = true;
    }

    /** 서비스가 기존 주소 유지와 주소 변경을 구분하도록 전달 여부를 반환합니다. JSON 응답에는 노출하지 않습니다. */
    @JsonIgnore
    public boolean visitLocationProvided() {
      return visitLocationProvided;
    }
  }

  /**
   * 캠페인 이미지와 방문 주소의 이미지 업로드 결과를 전달합니다.
   *
   * @param assetId 업로드된 이미지 자산 식별자이며 임의 외부 URL과 구분합니다.
   * @param state 이미지 자산의 저장 및 연결 상태입니다.
   * @param contentType 이미지의 MIME 유형입니다.
   * @param byteSize 이미지 파일 크기이며 단위는 바이트입니다.
   * @param width 이미지 가로 크기이며 단위는 픽셀입니다.
   * @param height 이미지 세로 크기이며 단위는 픽셀입니다.
   * @param previewPath 권한 확인이 필요한 이미지 미리보기 경로입니다.
   * @param publicUrl 공개 이미지 경로입니다.
   */
  public record AssetResponse(
      String assetId,
      String state,
      String contentType,
      long byteSize,
      int width,
      int height,
      String previewPath,
      String publicUrl) {}

  /**
   * 캠페인 이미지와 방문 주소의 관리 화면 이미지 정보를 전달합니다.
   *
   * @param assetId 업로드된 이미지 자산 식별자이며 임의 외부 URL과 구분합니다.
   * @param previewPath 권한 확인이 필요한 이미지 미리보기 경로입니다.
   * @param publicUrl 공개 이미지 경로입니다.
   * @param altText 이미지 대체 설명입니다.
   */
  public record ManagedImage(
      String assetId, String previewPath, String publicUrl, String altText) {}

  /**
   * 캠페인 이미지와 방문 주소의 관리 화면 표시 정보를 전달합니다.
   *
   * @param version 조회한 데이터 버전이며 수정 요청의 동시성 검사에 사용됩니다.
   * @param representativeImage 관리용 대표 이미지 정보입니다.
   * @param detailImages 표시 순서대로 전달하는 상세 이미지 목록입니다.
   * @param visitLocation 방문 주소이며 생략과 명시적 null의 의미를 구분해야 합니다.
   * @param retainedVisitLocation 관리 화면에서 보존된 방문 주소입니다.
   * @param legacyRepresentativeImageUrl 기존 방식으로 저장된 대표 이미지 주소입니다.
   * @param legacyDetailImageUrl 기존 방식으로 저장된 상세 이미지 주소입니다.
   * @param legacyVisitGuide 기존 방식으로 저장된 방문 안내입니다.
   */
  public record ManagedPresentation(
      long version,
      ManagedImage representativeImage,
      List<ManagedImage> detailImages,
      Location visitLocation,
      Location retainedVisitLocation,
      String legacyRepresentativeImageUrl,
      String legacyDetailImageUrl,
      String legacyVisitGuide) {}

  /**
   * 캠페인 이미지와 방문 주소의 공개 이미지 정보를 전달합니다.
   *
   * @param imageUrl 이미지 표시 주소입니다.
   * @param altText 이미지 대체 설명입니다.
   */
  public record PublicImage(String imageUrl, String altText) {}

  /**
   * 캠페인 이미지와 방문 주소의 사용자 화면 표시 정보를 전달합니다.
   *
   * @param representativeImageUrl 대표 이미지 주소입니다.
   * @param detailImages 표시 순서대로 전달하는 상세 이미지 목록입니다.
   * @param visitLocation 방문 주소이며 생략과 명시적 null의 의미를 구분해야 합니다.
   */
  public record PublicPresentation(
      String representativeImageUrl, List<PublicImage> detailImages, Location visitLocation) {}
}

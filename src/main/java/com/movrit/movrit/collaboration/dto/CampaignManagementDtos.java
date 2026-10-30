package com.movrit.movrit.collaboration.dto;

import com.movrit.movrit.campaigncontent.entity.ContentChannel;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignSelectionMode;
import com.movrit.movrit.collaboration.entity.CampaignType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** 캠페인 및 협업 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
public final class CampaignManagementDtos {
  private CampaignManagementDtos() {}

  /**
   * 캠페인 및 협업의 수정 요청을 전달합니다.
   *
   * @param title 제목입니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 max = 255입니다.
   * @param description 상세 설명입니다. 크기 제약은 max = 10000입니다.
   * @param category 분류값입니다. 크기 제약은 max = 100입니다.
   * @param minFollowerCount 기존 캠페인의 최소 팔로워 필드이며 신규 채널별 조건과 구분합니다. null을 허용하지 않습니다. 최솟값은 0입니다.
   * @param recruitmentCount 모집 인원입니다. null을 허용하지 않습니다. 최솟값은 1입니다.
   * @param rewardDescription 제공 혜택 설명입니다. 크기 제약은 max = 5000입니다.
   * @param mission 참여자가 수행할 미션입니다. 크기 제약은 max = 10000입니다.
   * @param channelGuide 채널별 작성 안내입니다. 크기 제약은 max = 5000입니다.
   * @param scheduleGuide 일정 안내입니다. 크기 제약은 max = 5000입니다.
   * @param deliveryGuide 배송 안내입니다. 크기 제약은 max = 5000입니다.
   * @param visitGuide 방문 안내입니다. 크기 제약은 max = 5000입니다.
   * @param caution 참여 시 주의사항입니다. 크기 제약은 max = 5000입니다.
   * @param submissionRequirement 콘텐츠 제출 요구사항입니다. 크기 제약은 max = 5000입니다.
   * @param expectedScheduleVersion 수정하기 전에 조회한 캠페인 일정 버전입니다. null을 허용하지 않습니다.
   * @param startAt 모집 시작 시각입니다.
   * @param endAt 모집 종료 시각입니다.
   * @param selectionAnnouncedAt 참여자 선정 발표 시각입니다.
   * @param experienceStartAt 체험 시작 시각입니다.
   * @param experienceEndAt 체험 종료 시각입니다.
   * @param contentSubmissionStartAt 콘텐츠 등록 시작 시각입니다.
   * @param contentSubmissionEndAt 콘텐츠 등록 종료 시각입니다.
   * @param resultsAnnouncedAt 결과 발표 시각입니다.
   * @param campaignType 캠페인 유형입니다. 비우면 기존 값을 유지합니다.
   * @param region 지역 캠페인의 광역 지역입니다. 비우면 기존 값을 유지합니다.
   * @param contentChannel 주요 콘텐츠 채널입니다. 비우면 기존 값을 유지합니다.
   */
  public record UpdateRequest(
      @NotBlank @Size(max = 255) String title,
      @Size(max = 10000) String description,
      @Size(max = 100) String category,
      @NotNull @Min(0) Long minFollowerCount,
      @NotNull @Min(1) Integer recruitmentCount,
      CampaignSelectionMode selectionMode,
      @Size(max = 5000) String rewardDescription,
      @Size(max = 10000) String mission,
      @Size(max = 5000) String channelGuide,
      @Size(max = 5000) String scheduleGuide,
      @Size(max = 5000) String deliveryGuide,
      @Size(max = 5000) String visitGuide,
      @Size(max = 5000) String caution,
      @Size(max = 5000) String submissionRequirement,
      @NotNull Long expectedScheduleVersion,
      @NotNull LocalDateTime startAt,
      @NotNull LocalDateTime endAt,
      LocalDateTime selectionAnnouncedAt,
      LocalDateTime experienceStartAt,
      LocalDateTime experienceEndAt,
      LocalDateTime contentSubmissionStartAt,
      LocalDateTime contentSubmissionEndAt,
      LocalDateTime resultsAnnouncedAt,
      CampaignType campaignType,
      String region,
      ContentChannel contentChannel) {}

  /**
   * 캠페인 및 협업의 캠페인 상세 응답을 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param preorderId 연결된 프리오더 식별자입니다.
   * @param preorderTitle 프리오더 제목입니다.
   * @param productId 상품 식별자입니다.
   * @param productName 상품명입니다.
   * @param brandName 브랜드명입니다.
   * @param title 제목입니다.
   * @param description 상세 설명입니다.
   * @param category 분류값입니다.
   * @param minFollowerCount 기존 캠페인의 최소 팔로워 필드이며 신규 채널별 조건과 구분합니다.
   * @param recruitmentCount 모집 인원입니다.
   * @param rewardDescription 제공 혜택 설명입니다.
   * @param mission 참여자가 수행할 미션입니다.
   * @param channelGuide 채널별 작성 안내입니다.
   * @param scheduleGuide 일정 안내입니다.
   * @param deliveryGuide 배송 안내입니다.
   * @param visitGuide 방문 안내입니다.
   * @param representativeImageUrl 대표 이미지 주소입니다.
   * @param detailImageUrl 기존 단일 상세 이미지 주소입니다.
   * @param visitPlaceName 방문 장소명입니다.
   * @param visitAddress 방문 주소입니다.
   * @param caution 참여 시 주의사항입니다.
   * @param submissionRequirement 콘텐츠 제출 요구사항입니다.
   * @param startAt 모집 시작 시각입니다.
   * @param endAt 모집 종료 시각입니다.
   * @param selectionAnnouncedAt 참여자 선정 발표 시각입니다.
   * @param experienceStartAt 체험 시작 시각입니다.
   * @param experienceEndAt 체험 종료 시각입니다.
   * @param contentSubmissionStartAt 콘텐츠 등록 시작 시각입니다.
   * @param contentSubmissionEndAt 콘텐츠 등록 종료 시각입니다.
   * @param resultsAnnouncedAt 결과 발표 시각입니다.
   * @param scheduleVersion 현재 캠페인 일정 버전입니다.
   * @param open 현재 모집 또는 진행 가능 상태입니다.
   * @param campaignType 캠페인 유형(제품·지역·기자단)입니다.
   * @param region 지역 캠페인의 광역 지역입니다.
   * @param contentChannel 주요 콘텐츠 채널입니다.
   * @param premier 운영자 지정 프리미어 여부입니다.
   */
  public record DetailResponse(
      Long id,
      Long preorderId,
      String preorderTitle,
      Long productId,
      String productName,
      String brandName,
      String title,
      String description,
      String category,
      Long minFollowerCount,
      Integer recruitmentCount,
      CampaignSelectionMode selectionMode,
      String rewardDescription,
      String mission,
      String channelGuide,
      String scheduleGuide,
      String deliveryGuide,
      String visitGuide,
      String representativeImageUrl,
      String detailImageUrl,
      String visitPlaceName,
      String visitAddress,
      String caution,
      String submissionRequirement,
      LocalDateTime startAt,
      LocalDateTime endAt,
      LocalDateTime selectionAnnouncedAt,
      LocalDateTime experienceStartAt,
      LocalDateTime experienceEndAt,
      LocalDateTime contentSubmissionStartAt,
      LocalDateTime contentSubmissionEndAt,
      LocalDateTime resultsAnnouncedAt,
      Long scheduleVersion,
      boolean open,
      CampaignType campaignType,
      String region,
      ContentChannel contentChannel,
      boolean premier) {
    /**
     * 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. 방문 정보의 포함 여부는 호출한 오버로드와 includeVisit
     * 인자로 구분합니다.
     */
    public static DetailResponse from(Campaign campaign) {
      return from(campaign, true);
    }

    /**
     * 공개 조회에서는 배송 전용 캠페인의 방문 주소·안내를 제외합니다. 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다.
     * 방문 정보의 포함 여부는 호출한 오버로드와 includeVisit 인자로 구분합니다.
     */
    public static DetailResponse from(Campaign campaign, boolean includeVisit) {
      return new DetailResponse(
          campaign.getId(),
          // 프리오더 없이 운영하는 캠페인은 프리오더·상품 정보를 비워 응답합니다.
          campaign.getPreorder() == null ? null : campaign.getPreorder().getId(),
          campaign.getPreorder() == null ? null : campaign.getPreorder().getTitle(),
          campaign.getPreorder() == null ? null : campaign.getPreorder().getProduct().getId(),
          campaign.getPreorder() == null ? null : campaign.getPreorder().getProduct().getName(),
          campaign.getBrand().getName(),
          campaign.getTitle(),
          campaign.getDescription(),
          campaign.getCategory(),
          campaign.getMinFollowerCount(),
          campaign.getRecruitmentCount(),
          campaign.getSelectionMode(),
          campaign.getRewardDescription(),
          campaign.getMission(),
          campaign.getChannelGuide(),
          campaign.getScheduleGuide(),
          campaign.getDeliveryGuide(),
          includeVisit ? campaign.getVisitGuide() : null,
          campaign.getRepresentativeImageUrl(),
          campaign.getDetailImageUrl(),
          includeVisit ? campaign.getVisitPlaceName() : null,
          includeVisit ? campaign.getVisitAddress() : null,
          campaign.getCaution(),
          campaign.getSubmissionRequirement(),
          campaign.getStartAt(),
          campaign.getEndAt(),
          campaign.getSelectionAnnouncedAt(),
          campaign.getExperienceStartAt(),
          campaign.getExperienceEndAt(),
          campaign.getContentSubmissionStartAt(),
          campaign.getContentSubmissionEndAt(),
          campaign.getResultsAnnouncedAt(),
          campaign.getScheduleVersion(),
          campaign.isOpen(),
          campaign.getCampaignType(),
          campaign.getRegion(),
          campaign.getContentChannel(),
          campaign.isPremier());
    }
  }

  /** 캠페인 신청 당사자에게 공개할 브랜드 담당자 연락처입니다. */
  public record ContactResponse(String name, String phone) {}

  /** 업체가 캠페인별 공개 담당자 연락처를 저장합니다. */
  public record ContactSaveRequest(
      @NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 30) String phone) {}
}

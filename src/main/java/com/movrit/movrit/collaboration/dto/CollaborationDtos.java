package com.movrit.movrit.collaboration.dto;

import com.movrit.movrit.campaigncontent.entity.ContentChannel;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignSelectionMode;
import com.movrit.movrit.collaboration.entity.CampaignType;
import com.movrit.movrit.collaboration.entity.Collaboration;
import com.movrit.movrit.collaboration.entity.CollaborationStatus;
import com.movrit.movrit.collaboration.entity.CollaborationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** 캠페인 및 협업 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
public final class CollaborationDtos {
  private CollaborationDtos() {}

  /**
   * 캠페인 및 협업의 기존 캠페인 생성 요청을 전달합니다.
   *
   * @param preorderId 함께 판매할 프리오더 식별자입니다. 프리오더 없이 체험단만 운영하면 비워 둡니다.
   * @param brandId 캠페인을 운영할 본인 브랜드 식별자입니다. 프리오더를 고르면 그 브랜드로 정해지고, 브랜드가 하나뿐이면 생략할 수 있습니다.
   * @param title 제목입니다. 빈 문자열을 허용하지 않습니다.
   * @param description 상세 설명입니다.
   * @param category 분류값입니다.
   * @param minFollowerCount 기존 캠페인의 최소 팔로워 필드이며 신규 채널별 조건과 구분합니다. null을 허용하지 않습니다. 최솟값은 0입니다.
   * @param recruitmentCount 모집 인원입니다. null을 허용하지 않습니다. 최솟값은 1입니다.
   * @param rewardDescription 제공 혜택 설명입니다.
   * @param startAt 모집 시작 시각입니다.
   * @param endAt 모집 종료 시각입니다.
   * @param campaignType 캠페인 유형(제품·지역·기자단)입니다. 비우면 제품으로 저장합니다.
   * @param region 지역 캠페인의 광역 지역(서울·경기 등)입니다. 지역 캠페인이 아니면 무시합니다.
   * @param contentChannel 주로 콘텐츠를 올릴 채널입니다(선택).
   */
  public record CampaignRequest(
      Long preorderId,
      Long brandId,
      @NotBlank String title,
      String description,
      String category,
      @NotNull @Min(0) Long minFollowerCount,
      @NotNull @Min(1) Integer recruitmentCount,
      CampaignSelectionMode selectionMode,
      String rewardDescription,
      @NotNull LocalDateTime startAt,
      @NotNull LocalDateTime endAt,
      CampaignType campaignType,
      String region,
      ContentChannel contentChannel) {}

  /**
   * 캠페인 및 협업의 기존 캠페인 목록 응답을 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param preorderId 연결된 프리오더 식별자입니다.
   * @param preorderTitle 프리오더 제목입니다.
   * @param brandName 브랜드명입니다.
   * @param title 제목입니다.
   * @param description 상세 설명입니다.
   * @param category 분류값입니다.
   * @param minFollowerCount 기존 캠페인의 최소 팔로워 필드이며 신규 채널별 조건과 구분합니다.
   * @param recruitmentCount 모집 인원입니다.
   * @param rewardDescription 제공 혜택 설명입니다.
   * @param representativeImageUrl 캠페인에 직접 연결된 대표 이미지 주소입니다.
   * @param startAt 모집 시작 시각입니다.
   * @param endAt 모집 종료 시각입니다.
   * @param open 현재 모집 또는 진행 가능 상태입니다.
   */
  public record CampaignResponse(
      Long id,
      Long preorderId,
      String preorderTitle,
      String brandName,
      String title,
      String description,
      String category,
      Long minFollowerCount,
      Integer recruitmentCount,
      CampaignSelectionMode selectionMode,
      String rewardDescription,
      String representativeImageUrl,
      LocalDateTime startAt,
      LocalDateTime endAt,
      boolean open,
      Long applicationCount,
      CampaignType campaignType,
      String region,
      ContentChannel contentChannel,
      boolean premier) {
    /**
     * 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다.
     *
     * <p>신청 수를 집계하지 않는 응답(등록·업체 목록)은 applicationCount를 null로 둡니다.
     */
    public static CampaignResponse from(Campaign c) {
      return from(c, null);
    }

    /**
     * 공개 목록처럼 신청 수를 함께 보여줄 때 사용합니다.
     *
     * @param applicationCount 취소를 제외한 신청 수입니다. 집계하지 않았으면 null입니다.
     */
    public static CampaignResponse from(Campaign c, Long applicationCount) {
      return new CampaignResponse(
          c.getId(),
          c.getPreorder() == null ? null : c.getPreorder().getId(),
          c.getPreorder() == null ? null : c.getPreorder().getTitle(),
          c.getBrand().getName(),
          c.getTitle(),
          c.getDescription(),
          c.getCategory(),
          c.getMinFollowerCount(),
          c.getRecruitmentCount(),
          c.getSelectionMode(),
          c.getRewardDescription(),
          c.getRepresentativeImageUrl(),
          c.getStartAt(),
          c.getEndAt(),
          c.isOpen(),
          applicationCount,
          c.getCampaignType(),
          c.getRegion(),
          c.getContentChannel(),
          c.isPremier());
    }
  }

  /**
   * 캠페인 및 협업의 신청 요청을 전달합니다.
   *
   * @param message 신청 또는 안내 메시지입니다. 빈 문자열을 허용하지 않습니다.
   */
  public record ApplyRequest(@NotBlank String message) {}

  /**
   * 캠페인 및 협업의 협업 제안 요청을 전달합니다.
   *
   * @param preorderId 연결된 프리오더 식별자입니다. null을 허용하지 않습니다.
   * @param creatorId 기존 채널 프로필 식별자이며, 회원 ID와 구분합니다. null을 허용하지 않습니다.
   * @param message 신청 또는 안내 메시지입니다. 빈 문자열을 허용하지 않습니다.
   */
  public record ProposeRequest(
      @NotNull Long preorderId, @NotNull Long creatorId, @NotBlank String message) {}

  /**
   * 캠페인 및 협업의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param preorderId 연결된 프리오더 식별자입니다.
   * @param preorderTitle 프리오더 제목입니다.
   * @param creatorId 기존 채널 프로필 식별자이며, 회원 ID와 구분합니다.
   * @param creatorNickname 참여자의 채널 프로필 표시 이름입니다.
   * @param type 데이터 또는 질문 유형입니다.
   * @param status 현재 업무 상태입니다.
   * @param message 신청 또는 안내 메시지입니다.
   * @param referralCode 추천 추적 코드입니다.
   * @param referralUrl 추천 코드가 포함된 연결 주소입니다.
   */
  public record Response(
      Long id,
      Long preorderId,
      String preorderTitle,
      Long creatorId,
      String creatorNickname,
      CollaborationType type,
      CollaborationStatus status,
      String message,
      String referralCode,
      String referralUrl) {
    /** 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
    public static Response from(Collaboration c) {
      String url =
          c.getReferralCode() == null
              ? null
              : "/preorders/" + c.getPreorder().getId() + "?ref=" + c.getReferralCode();
      return new Response(
          c.getId(),
          c.getPreorder().getId(),
          c.getPreorder().getTitle(),
          c.getCreator().getId(),
          c.getCreator().getNickname(),
          c.getType(),
          c.getStatus(),
          c.getMessage(),
          c.getReferralCode(),
          url);
    }
  }

  /**
   * 캠페인 및 협업의 추천 링크 클릭 기록을 전달합니다.
   *
   * @param visitorKey 방문 식별값입니다.
   * @param userAgent 요청 클라이언트 정보입니다.
   */
  public record ClickRequest(String visitorKey, String userAgent) {}

  /**
   * 캠페인 및 협업의 추천 성과 집계 응답을 전달합니다.
   *
   * @param collaborationId 협업 식별자입니다.
   * @param creatorNickname 참여자의 채널 프로필 표시 이름입니다.
   * @param preorderTitle 프리오더 제목입니다.
   * @param clicks 추천 링크 클릭 수입니다.
   * @param paidOrders 결제된 주문 수입니다.
   * @param paidQuantity 결제된 수량입니다.
   * @param gmv 집계 거래 금액입니다.
   * @param conversionRate 집계된 전환율입니다.
   */
  public record PerformanceResponse(
      Long collaborationId,
      String creatorNickname,
      String preorderTitle,
      long clicks,
      long paidOrders,
      long paidQuantity,
      java.math.BigDecimal gmv,
      double conversionRate) {}
}

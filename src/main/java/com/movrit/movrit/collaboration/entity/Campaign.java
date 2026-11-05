package com.movrit.movrit.collaboration.entity;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaigncontent.entity.ContentChannel;
import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브랜드가 등록하는 체험단 캠페인(모집 공고)입니다. 모집 내용·참여 안내·일정·선정 방식·공개 여부를 담고, 운영 브랜드에 직접 속합니다(프리오더 연결은 선택이며 연결하면
 * 프리오더 하나에 캠페인 하나). 일정을 바꿀 때마다 scheduleVersion이 1씩 올라가 동시에 다른 화면에서 고친 내용을 덮어쓰지 않게 합니다.
 */
@Entity
@Table(name = "collaboration_campaign")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Campaign extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 캠페인을 운영하는 브랜드입니다. 소유 확인(수정·선정·검수 권한)은 이 브랜드의 판매자를 기준으로 합니다. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private Brand brand;

  /** 함께 판매하는 프리오더입니다(선택). 프리오더 없이 체험단 캠페인만 운영할 수도 있습니다. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "preorder_id", unique = true)
  private Preorder preorder;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(length = 100)
  private String category;

  /** 캠페인 유형(제품·지역·기자단)입니다. 탐색 메뉴 분류에 사용합니다. */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "campaign_type", nullable = false, length = 20)
  private CampaignType campaignType = CampaignType.PRODUCT;

  /** 지역 캠페인의 방문 광역 지역(서울·경기 등)입니다. 제품·기자단 캠페인은 비워 둡니다. */
  @Column(length = 20)
  private String region;

  /** 주로 콘텐츠를 올릴 채널입니다(탐색 화면의 미디어 필터). 정하지 않으면 비워 둡니다. */
  @Enumerated(EnumType.STRING)
  @Column(name = "content_channel", length = 20)
  private ContentChannel contentChannel;

  /** 운영자가 지정하는 프리미어 캠페인 여부입니다. 업체 화면에서는 바꿀 수 없습니다. */
  @Builder.Default
  @Column(nullable = false)
  private boolean premier = false;

  /** 기존 단일 팔로워 조건 값입니다. 현재 신청 조건은 CampaignEligibilityRequirement에서 관리합니다. */
  @Column(name = "min_follower_count", nullable = false)
  private Long minFollowerCount;

  @Column(name = "recruitment_count", nullable = false)
  private Integer recruitmentCount;

  /** 기존 캠페인은 브랜드 직접 선정으로 유지합니다. */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "selection_mode", nullable = false, length = 20)
  private CampaignSelectionMode selectionMode = CampaignSelectionMode.BRAND_DIRECT;

  @Column(name = "reward_description", columnDefinition = "TEXT")
  private String rewardDescription;

  @Column(columnDefinition = "TEXT")
  private String mission;

  @Column(name = "channel_guide", columnDefinition = "TEXT")
  private String channelGuide;

  @Column(name = "schedule_guide", columnDefinition = "TEXT")
  private String scheduleGuide;

  @Column(name = "delivery_guide", columnDefinition = "TEXT")
  private String deliveryGuide;

  @Column(name = "visit_guide", columnDefinition = "TEXT")
  private String visitGuide;

  @Column(name = "representative_image_url", length = 1000)
  private String representativeImageUrl;

  @Column(name = "detail_image_url", length = 1000)
  private String detailImageUrl;

  @Column(name = "visit_place_name", length = 200)
  private String visitPlaceName;

  @Column(name = "visit_address", length = 500)
  private String visitAddress;

  @Column(columnDefinition = "TEXT")
  private String caution;

  @Column(name = "submission_requirement", columnDefinition = "TEXT")
  private String submissionRequirement;

  @Column(name = "start_at", nullable = false)
  private LocalDateTime startAt;

  @Column(name = "end_at", nullable = false)
  private LocalDateTime endAt;

  @Column(name = "selection_announced_at")
  private LocalDateTime selectionAnnouncedAt;

  @Column(name = "experience_start_at")
  private LocalDateTime experienceStartAt;

  @Column(name = "experience_end_at")
  private LocalDateTime experienceEndAt;

  @Column(name = "content_submission_start_at")
  private LocalDateTime contentSubmissionStartAt;

  @Column(name = "content_submission_end_at")
  private LocalDateTime contentSubmissionEndAt;

  @Column(name = "results_announced_at")
  private LocalDateTime resultsAnnouncedAt;

  @Column(name = "contact_name", length = 100)
  private String contactName;

  @Column(name = "contact_phone", length = 30)
  private String contactPhone;

  /** 일정 수정 버전입니다. 수정 요청의 기대 버전과 다르면 저장을 거절합니다. */
  @Builder.Default
  @Column(name = "schedule_version", nullable = false)
  private Long scheduleVersion = 1L;

  /** 사용자 목록에 공개할지 여부입니다. 모집 기간과는 별개로 업체·운영자가 마감할 수 있습니다. */
  @Column(nullable = false)
  private boolean open;

  /** 운영자가 숨긴 시각입니다. 값이 있으면 소유 업체·운영자·신청자 외에는 상세를 볼 수 없습니다(업체 마감과 구분). */
  @Column(name = "admin_hidden_at")
  private LocalDateTime adminHiddenAt;

  /**
   * 업체가 모집 내용·참여 안내·일정을 한 번에 수정합니다. 선정 방식은 현재 값을 그대로 유지합니다. 검증과 버전 확인은 아래 선정 방식을 받는 update와 같습니다.
   */
  public void update(
      String title,
      String description,
      String category,
      Long minFollowerCount,
      Integer recruitmentCount,
      String rewardDescription,
      String mission,
      String channelGuide,
      String scheduleGuide,
      String deliveryGuide,
      String visitGuide,
      String caution,
      String submissionRequirement,
      Long expectedScheduleVersion,
      LocalDateTime startAt,
      LocalDateTime endAt,
      LocalDateTime selectionAnnouncedAt,
      LocalDateTime experienceStartAt,
      LocalDateTime experienceEndAt,
      LocalDateTime contentSubmissionStartAt,
      LocalDateTime contentSubmissionEndAt,
      LocalDateTime resultsAnnouncedAt) {
    update(
        title,
        description,
        category,
        minFollowerCount,
        recruitmentCount,
        selectionMode == null ? CampaignSelectionMode.BRAND_DIRECT : selectionMode,
        rewardDescription,
        mission,
        channelGuide,
        scheduleGuide,
        deliveryGuide,
        visitGuide,
        caution,
        submissionRequirement,
        expectedScheduleVersion,
        startAt,
        endAt,
        selectionAnnouncedAt,
        experienceStartAt,
        experienceEndAt,
        contentSubmissionStartAt,
        contentSubmissionEndAt,
        resultsAnnouncedAt);
  }

  /**
   * 선정 방식까지 포함해 모집 내용·참여 안내·일정을 한 번에 수정합니다.
   *
   * <p>expectedScheduleVersion이 현재 버전과 다르면(다른 화면에서 먼저 수정됨) 거절합니다. 일정 순서(모집 → 선정 발표 → 체험 → 콘텐츠 등록 →
   * 결과 발표)가 맞지 않아도 거절하며, 저장에 성공하면 일정 버전을 1 올립니다.
   */
  public void update(
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
      String caution,
      String submissionRequirement,
      Long expectedScheduleVersion,
      LocalDateTime startAt,
      LocalDateTime endAt,
      LocalDateTime selectionAnnouncedAt,
      LocalDateTime experienceStartAt,
      LocalDateTime experienceEndAt,
      LocalDateTime contentSubmissionStartAt,
      LocalDateTime contentSubmissionEndAt,
      LocalDateTime resultsAnnouncedAt) {
    if (scheduleVersion == null || !scheduleVersion.equals(expectedScheduleVersion)) {
      throw new IllegalArgumentException("캠페인 일정이 변경되었습니다. 최신 일정을 다시 확인해 주세요.");
    }
    validateSchedule(
        startAt,
        endAt,
        selectionAnnouncedAt,
        experienceStartAt,
        experienceEndAt,
        contentSubmissionStartAt,
        contentSubmissionEndAt,
        resultsAnnouncedAt);
    this.title = title;
    this.description = description;
    this.category = category;
    this.minFollowerCount = minFollowerCount;
    this.recruitmentCount = recruitmentCount;
    this.selectionMode = selectionMode;
    this.rewardDescription = rewardDescription;
    this.mission = mission;
    this.channelGuide = channelGuide;
    this.scheduleGuide = scheduleGuide;
    this.deliveryGuide = deliveryGuide;
    this.visitGuide = visitGuide;
    this.caution = caution;
    this.submissionRequirement = submissionRequirement;
    this.startAt = startAt;
    this.endAt = endAt;
    this.selectionAnnouncedAt = selectionAnnouncedAt;
    this.experienceStartAt = experienceStartAt;
    this.experienceEndAt = experienceEndAt;
    this.contentSubmissionStartAt = contentSubmissionStartAt;
    this.contentSubmissionEndAt = contentSubmissionEndAt;
    this.resultsAnnouncedAt = resultsAnnouncedAt;
    this.scheduleVersion += 1L;
  }

  /**
   * 새 캠페인에 참여 안내와 세부 일정을 채웁니다. 등록 직후 한 번 사용하며 일정 버전은 올리지 않습니다.
   *
   * <p>일정 순서(모집 종료 → 선정 발표 → 체험 → 콘텐츠 등록 → 결과 발표)가 맞지 않으면 IllegalArgumentException으로 거절합니다.
   */
  public void describe(
      String mission,
      String channelGuide,
      String scheduleGuide,
      String deliveryGuide,
      String visitGuide,
      String caution,
      String submissionRequirement,
      LocalDateTime selectionAnnouncedAt,
      LocalDateTime experienceStartAt,
      LocalDateTime experienceEndAt,
      LocalDateTime contentSubmissionStartAt,
      LocalDateTime contentSubmissionEndAt,
      LocalDateTime resultsAnnouncedAt) {
    validateSchedule(
        startAt,
        endAt,
        selectionAnnouncedAt,
        experienceStartAt,
        experienceEndAt,
        contentSubmissionStartAt,
        contentSubmissionEndAt,
        resultsAnnouncedAt);
    this.mission = mission;
    this.channelGuide = channelGuide;
    this.scheduleGuide = scheduleGuide;
    this.deliveryGuide = deliveryGuide;
    this.visitGuide = visitGuide;
    this.caution = caution;
    this.submissionRequirement = submissionRequirement;
    this.selectionAnnouncedAt = selectionAnnouncedAt;
    this.experienceStartAt = experienceStartAt;
    this.experienceEndAt = experienceEndAt;
    this.contentSubmissionStartAt = contentSubmissionStartAt;
    this.contentSubmissionEndAt = contentSubmissionEndAt;
    this.resultsAnnouncedAt = resultsAnnouncedAt;
  }

  /** 캠페인을 마감해 사용자 목록에서 내립니다(open = false). */
  public void close() {
    open = false;
  }

  /** 운영자가 캠페인을 숨깁니다. 목록에서 내리고(open = false) 숨긴 시각을 남겨 상세 주소로도 일반 회원에게 보이지 않게 합니다. */
  public void hideByAdmin() {
    open = false;
    adminHiddenAt = LocalDateTime.now();
  }

  /** 운영자가 숨긴 캠페인인지 확인합니다. */
  public boolean isAdminHidden() {
    return adminHiddenAt != null;
  }

  /** 운영자가 숨긴(마감한) 캠페인을 다시 사용자 목록에 공개합니다(open = true). 모집 기간은 바꾸지 않습니다. */
  public void reopen() {
    open = true;
    adminHiddenAt = null;
  }

  /** 방문형 캠페인의 방문 장소 이름과 주소를 바꿉니다. */
  public void updatePresentation(String visitPlaceName, String visitAddress) {
    this.visitPlaceName = visitPlaceName;
    this.visitAddress = visitAddress;
  }

  /** 목록 카드에 보여줄 대표 이미지 주소를 바꿉니다. */
  public void updateRepresentativeImage(String imageUrl) {
    this.representativeImageUrl = imageUrl;
  }

  /** 예전 단일 상세 이미지 주소를 바꿉니다. 여러 장 상세 이미지는 CampaignDetailImage를 사용합니다. */
  public void updateDetailImage(String imageUrl) {
    this.detailImageUrl = imageUrl;
  }

  /** 선정된 참여자에게만 보여줄 캠페인 담당자 이름과 연락처를 저장합니다. 회원·대표자 전화번호를 자동으로 공개하지 않도록 따로 입력받습니다. */
  public void updateContact(String contactName, String contactPhone) {
    this.contactName = contactName;
    this.contactPhone = contactPhone;
  }

  /** 캠페인 일정이 모집 → 선정 발표 → 체험 → 콘텐츠 등록 → 결과 발표 순서인지 확인합니다. 입력하지 않은 선택 일정은 비교에서 제외합니다. */
  private void validateSchedule(
      LocalDateTime recruitmentStartAt,
      LocalDateTime recruitmentEndAt,
      LocalDateTime selectionAt,
      LocalDateTime experienceStart,
      LocalDateTime experienceEnd,
      LocalDateTime submissionStart,
      LocalDateTime submissionEnd,
      LocalDateTime resultsAt) {
    if (!recruitmentStartAt.isBefore(recruitmentEndAt)) {
      throw new IllegalArgumentException("모집 종료일은 시작일보다 뒤여야 합니다.");
    }
    if (selectionAt != null && selectionAt.isBefore(recruitmentEndAt)) {
      throw new IllegalArgumentException("선정 발표일은 모집 종료일보다 빠를 수 없습니다.");
    }
    if (experienceStart != null && selectionAt != null && experienceStart.isBefore(selectionAt)) {
      throw new IllegalArgumentException("체험 시작일은 선정 발표일보다 빠를 수 없습니다.");
    }
    if (experienceStart != null
        && experienceEnd != null
        && !experienceStart.isBefore(experienceEnd)) {
      throw new IllegalArgumentException("체험 종료일은 체험 시작일보다 뒤여야 합니다.");
    }
    if (submissionStart != null
        && submissionEnd != null
        && !submissionStart.isBefore(submissionEnd)) {
      throw new IllegalArgumentException("콘텐츠 등록 종료일은 시작일보다 뒤여야 합니다.");
    }
    if (resultsAt != null && submissionEnd != null && resultsAt.isBefore(submissionEnd)) {
      throw new IllegalArgumentException("결과 발표일은 콘텐츠 등록 종료일보다 빠를 수 없습니다.");
    }
  }

  /** 탐색 메뉴에서 고를 수 있는 광역 지역 목록입니다. 프론트 지역 필터(REGIONS)와 같은 값을 씁니다. */
  public static final java.util.Set<String> REGIONS =
      java.util.Set.of(
          "서울", "경기", "인천", "부산", "대구", "대전", "광주", "울산", "강원", "충청", "전라", "경상", "제주");

  /**
   * 캠페인 유형·지역·주요 채널을 정합니다. 값이 null이면 기존 값을 유지합니다.
   *
   * <p>지역은 지역(LOCAL) 캠페인에서만 의미가 있어, 다른 유형으로 바꾸면 지역을 비웁니다. 지역 이름은 REGIONS 목록 안의 값만 받습니다.
   *
   * @throws IllegalArgumentException 목록에 없는 지역 이름이면 거절합니다
   */
  public void classify(CampaignType type, String regionName, ContentChannel channel) {
    if (type != null) this.campaignType = type;
    if (channel != null) this.contentChannel = channel;
    if (this.campaignType != CampaignType.LOCAL) {
      this.region = null;
      return;
    }
    if (regionName != null) {
      String trimmed = regionName.trim();
      if (trimmed.isEmpty()) {
        this.region = null;
      } else if (!REGIONS.contains(trimmed)) {
        throw new IllegalArgumentException("지원하지 않는 지역입니다: " + trimmed);
      } else {
        this.region = trimmed;
      }
    }
  }
}

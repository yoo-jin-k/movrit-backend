package com.movrit.movrit.campaignapplication.repository;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.collaboration.entity.CampaignSelectionMode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 캠페인 신청을 저장·조회하고, 목록 화면에 필요한 집계(신청 수, 완료 횟수, 채널 지표)를 제공합니다. */
public interface CampaignApplicationRepository extends JpaRepository<CampaignApplication, Long> {
  /** 참여자별 완료 캠페인 수 집계 결과입니다. */
  interface CompletedCampaignCount {
    Long getCreatorId();

    Long getCompletedCount();
  }

  /** 참여자 검토 화면에 보여줄 회원 채널 지표 한 줄입니다. */
  interface CreatorMetricSummary {
    Long getMemberId();

    String getChannelType();

    String getMetricType();

    Long getMetricValue();

    String getMeasurementPeriod();

    java.time.LocalDateTime getMeasuredAt();

    String getVerificationStatus();
  }

  /** 캠페인별 유효 신청 수 집계 결과입니다. 공개 목록 카드의 "신청 N / 모집 M명" 표시에 사용합니다. */
  interface CampaignApplicationCount {
    Long getCampaignId();

    Long getApplicationCount();
  }

  /**
   * 여러 캠페인의 신청 수를 한 번의 쿼리로 집계합니다.
   *
   * <p>취소(CANCELLED)된 신청은 경쟁률에 포함하지 않습니다. 목록 조회 시 캠페인마다 count 쿼리를 반복하지 않도록 캠페인 ID 묶음으로 그룹 집계합니다.
   */
  @Query(
      "select application.campaign.id as campaignId, count(application) as applicationCount"
          + " from CampaignApplication application"
          + " where application.campaign.id in :campaignIds"
          + " and application.status <> com.movrit.movrit.campaignapplication.entity"
          + ".CampaignApplicationStatus.CANCELLED"
          + " group by application.campaign.id")
  List<CampaignApplicationCount> countActiveApplicationsByCampaignIds(
      @Param("campaignIds") Collection<Long> campaignIds);

  /** 같은 크리에이터가 이 캠페인에 이미 신청했는지 확인합니다. */
  boolean existsByCampaignIdAndCreatorId(Long campaignId, Long creatorId);

  /** 참여자 포트폴리오 화면용으로 신청 한 건과 캠페인·브랜드·업체 소유자 정보를 한 번에 읽습니다. 소유 업체 권한 확인에 필요한 연관을 함께 가져옵니다. */
  @EntityGraph(
      attributePaths = {
        "creator",
        "creator.member",
        "campaign",
        "campaign.preorder",
        "campaign.brand",
        "campaign.brand.seller",
        "campaign.brand.seller.member"
      })
  @Query("select application from CampaignApplication application where application.id = :id")
  Optional<CampaignApplication> findPortfolioById(@Param("id") Long id);

  /** 같은 회원이 이미 이 캠페인에 신청했는지 확인합니다(중복 신청 방지). */
  boolean existsByCampaignIdAndCreatorMemberId(Long campaignId, Long memberId);

  /** 이 회원이 캠페인 신청을 특정 상태·사유 머리말로 끝냈는지 확인합니다(예: 취소 + "[신청 철회]"). */
  boolean existsByCampaignIdAndCreatorMemberIdAndStatusAndCancellationReasonStartingWith(
      Long campaignId, Long memberId, CampaignApplicationStatus status, String reasonPrefix);

  /** 콘텐츠 마감이 (now, until] 사이에 있는 진행 중 신청입니다(마감 알림용). */
  @org.springframework.data.jpa.repository.Query(
      "select a from CampaignApplication a join fetch a.campaign c join fetch a.creator cr"
          + " join fetch cr.member where a.status in :statuses"
          + " and coalesce(a.contentDeadlineExtendedUntil, c.contentSubmissionEndAt) > :now"
          + " and coalesce(a.contentDeadlineExtendedUntil, c.contentSubmissionEndAt) <= :until")
  List<CampaignApplication> findContentDeadlineDue(
      @org.springframework.data.repository.query.Param("statuses")
          java.util.Collection<CampaignApplicationStatus> statuses,
      @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,
      @org.springframework.data.repository.query.Param("until") java.time.LocalDateTime until);

  /**
   * 콘텐츠 제출 마감(연장 반영)이 from 이후 now 이전에 지난 신청을 찾습니다. 마감 미제출 자동 처리에 씁니다.
   *
   * <p>from을 두어 기능을 켜기 전에 이미 오래 지난 신청(보존 중인 점검 데이터 등)은 건드리지 않습니다.
   */
  @org.springframework.data.jpa.repository.Query(
      "select a from CampaignApplication a join fetch a.campaign c join fetch a.creator cr"
          + " join fetch cr.member where a.status in :statuses"
          + " and coalesce(a.contentDeadlineExtendedUntil, c.contentSubmissionEndAt) > :from"
          + " and coalesce(a.contentDeadlineExtendedUntil, c.contentSubmissionEndAt) <= :now")
  List<CampaignApplication> findContentDeadlinePassed(
      @org.springframework.data.repository.query.Param("statuses")
          java.util.Collection<CampaignApplicationStatus> statuses,
      @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
      @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

  /** 캠페인 신청 수(취소·미선정 포함)입니다. */
  long countByCampaignId(Long campaignId);

  /**
   * 업체가 한 번이라도 선정한 신청 수입니다. 선정하면 응답 시각이 남고, 선정 취소하면 지워지므로 미선정(REJECTED)만 빼고 셉니다. 선정 뒤 취소된 참여도
   * 포함합니다.
   */
  @org.springframework.data.jpa.repository.Query(
      "select count(a) from CampaignApplication a where a.campaign.id = :campaignId"
          + " and a.respondedAt is not null and a.status <> com.movrit.movrit.campaignapplication"
          + ".entity.CampaignApplicationStatus.REJECTED")
  long countEverSelected(
      @org.springframework.data.repository.query.Param("campaignId") Long campaignId);

  /** 캠페인에 신청 이력이 하나라도 있는지 확인합니다. 신청 이력이 생긴 뒤에는 선정 방식을 바꿀 수 없습니다. */
  boolean existsByCampaignId(Long campaignId);

  /** 캠페인의 신청 전체를 조회합니다. */
  List<CampaignApplication> findAllByCampaignId(Long campaignId);

  /** 신청 ID와 회원 ID가 모두 일치할 때만 찾습니다. 본인 신청만 다루도록 권한 확인을 겸합니다. */
  Optional<CampaignApplication> findByIdAndCreatorMemberId(Long id, Long memberId);

  /** 회원 본인의 신청을 최근 순으로 조회합니다. */
  List<CampaignApplication> findAllByCreatorMemberIdOrderByIdDesc(Long memberId);

  @EntityGraph(
      attributePaths = {
        "creator",
        "creator.member",
        "campaign",
        "campaign.preorder",
        "campaign.brand"
      })
  /** 업체 회원이 소유한 브랜드의 캠페인에 들어온 신청을 최신순으로 조회합니다(업체 참여자 관리 화면). */
  List<CampaignApplication> findAllByCampaignBrandSellerMemberIdOrderByIdDesc(Long memberId);

  /** 선정 방식별 신청 목록입니다. 운영자 선정(OPERATOR) 캠페인의 신청을 운영자 화면에 보여줄 때 사용합니다. */
  @EntityGraph(attributePaths = {"creator", "campaign", "campaign.preorder"})
  List<CampaignApplication> findAllByCampaignSelectionModeOrderByIdDesc(
      CampaignSelectionMode selectionMode);

  /** 캠페인에서 특정 상태들에 해당하는 신청 수를 셉니다. 선정 시 모집 인원을 넘지 않는지 확인할 때 사용합니다. */
  long countByCampaignIdAndStatusIn(Long campaignId, List<CampaignApplicationStatus> statuses);

  /** 회원이 참여자로서 진행 중인 신청 수입니다(탈퇴 가능 여부 확인용). */
  long countByCreatorMemberIdAndStatusIn(Long memberId, List<CampaignApplicationStatus> statuses);

  /** 캠페인·상태별 신청 수입니다(운영자 캠페인 관리 목록용, 한 번에 집계합니다). */
  interface CampaignStatusCount {
    Long getCampaignId();

    CampaignApplicationStatus getStatus();

    Long getTotal();
  }

  @Query(
      "select a.campaign.id as campaignId, a.status as status, count(a) as total"
          + " from CampaignApplication a group by a.campaign.id, a.status")
  List<CampaignStatusCount> countAllByCampaignAndStatus();

  @Query(
      value =
          """
          SELECT application.creator_id AS creatorId, COUNT(application.id) AS completedCount
          FROM campaign_application application
          JOIN campaign_content_submission submission
            ON submission.application_id = application.id
          WHERE application.creator_id IN (:creatorIds)
            AND application.status = 'COMPLETED'
            AND submission.status = 'APPROVED'
          GROUP BY application.creator_id
          """,
      nativeQuery = true)
  /** 참여자별 완료 캠페인 수를 셉니다. 신청이 완료(COMPLETED)이면서 콘텐츠가 최종 승인(APPROVED)된 건만 1건으로 셉니다. */
  List<CompletedCampaignCount> countCompletedCampaignsByCreatorIds(
      @Param("creatorIds") Collection<Long> creatorIds);

  @Query(
      value =
          """
          SELECT metric.member_id AS memberId,
                 metric.channel_type AS channelType,
                 metric.metric_type AS metricType,
                 metric.metric_value AS metricValue,
                 metric.measurement_period AS measurementPeriod,
                 metric.measured_at AS measuredAt,
                 metric.verification_status AS verificationStatus
          FROM member_channel_metric metric
          WHERE metric.member_id IN (:memberIds)
            AND metric.metric_type <> 'DAILY_VISITORS'
          ORDER BY metric.member_id, metric.id
          """,
      nativeQuery = true)
  /**
   * 여러 회원의 채널 지표를 한 번에 읽습니다. 참여자 목록에서 회원마다 따로 조회하지 않기 위해 사용합니다. 블로그 방문자 수는 신청 당시 값만 상세에서 보여주므로 여기서
   * 제외합니다.
   */
  List<CreatorMetricSummary> findMetricSummariesByMemberIds(
      @Param("memberIds") Collection<Long> memberIds);
}

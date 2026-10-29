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

public interface CampaignApplicationRepository extends JpaRepository<CampaignApplication, Long> {
  interface CompletedCampaignCount {
    Long getCreatorId();

    Long getCompletedCount();
  }

  interface CreatorMetricSummary {
    Long getMemberId();

    String getChannelType();

    String getMetricType();

    Long getMetricValue();

    String getMeasurementPeriod();

    java.time.LocalDateTime getMeasuredAt();

    String getVerificationStatus();
  }

  boolean existsByCampaignIdAndCreatorId(Long campaignId, Long creatorId);

  @EntityGraph(
      attributePaths = {
        "creator",
        "creator.member",
        "campaign",
        "campaign.preorder",
        "campaign.preorder.product",
        "campaign.preorder.product.brand",
        "campaign.preorder.product.brand.seller",
        "campaign.preorder.product.brand.seller.member"
      })
  @Query("select application from CampaignApplication application where application.id = :id")
  Optional<CampaignApplication> findPortfolioById(@Param("id") Long id);

  boolean existsByCampaignIdAndCreatorMemberId(Long campaignId, Long memberId);

  boolean existsByCampaignId(Long campaignId);

  List<CampaignApplication> findAllByCampaignId(Long campaignId);

  Optional<CampaignApplication> findByIdAndCreatorMemberId(Long id, Long memberId);

  List<CampaignApplication> findAllByCreatorMemberIdOrderByIdDesc(Long memberId);

  @EntityGraph(
      attributePaths = {
        "creator",
        "creator.member",
        "campaign",
        "campaign.preorder",
        "campaign.preorder.product",
        "campaign.preorder.product.brand"
      })
  List<CampaignApplication> findAllByCampaignPreorderProductBrandSellerMemberIdOrderByIdDesc(
      Long memberId);

  @EntityGraph(attributePaths = {"creator", "campaign", "campaign.preorder"})
  List<CampaignApplication> findAllByCampaignSelectionModeOrderByIdDesc(
      CampaignSelectionMode selectionMode);

  long countByCampaignIdAndStatusIn(Long campaignId, List<CampaignApplicationStatus> statuses);

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
          ORDER BY metric.member_id, metric.id
          """,
      nativeQuery = true)
  List<CreatorMetricSummary> findMetricSummariesByMemberIds(
      @Param("memberIds") Collection<Long> memberIds);
}

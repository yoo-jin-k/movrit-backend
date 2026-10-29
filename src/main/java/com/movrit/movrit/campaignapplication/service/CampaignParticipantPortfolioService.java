package com.movrit.movrit.campaignapplication.service;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.dto.CampaignParticipantDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.entity.CampaignContentLink;
import com.movrit.movrit.campaigncontent.entity.CampaignContentSubmission;
import com.movrit.movrit.campaigncontent.entity.ContentSubmissionStatus;
import com.movrit.movrit.campaigncontent.repository.CampaignContentLinkRepository;
import com.movrit.movrit.campaigncontent.repository.CampaignContentSubmissionRepository;
import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.product.service.ProductService;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignParticipantPortfolioService {
  private final CampaignApplicationRepository applicationRepository;
  private final CampaignApplicationFormRepository formRepository;
  private final CampaignContentSubmissionRepository submissionRepository;
  private final CampaignContentLinkRepository linkRepository;
  private final MemberChannelMetricRepository metricRepository;
  private final ProductService productService;
  private final ObjectMapper objectMapper;

  /** 승인된 업체가 소유한 캠페인의 신청자 포트폴리오만 조회합니다. */
  public CampaignParticipantDtos.DetailResponse find(Long sellerMemberId, Long applicationId) {
    CampaignApplication application =
        applicationRepository
            .findPortfolioById(applicationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    productService.getOwnedProduct(
        sellerMemberId, application.getCampaign().getPreorder().getProduct().getId());

    Creator creator = application.getCreator();
    List<MemberChannelMetric> metrics =
        metricRepository.findAllByMemberIdOrderById(creator.getMember().getId());
    List<CampaignContentSubmission> completed =
        submissionRepository
            .findAllByApplicationCreatorIdAndApplicationStatusAndStatusOrderByReviewedAtDesc(
                creator.getId(),
                CampaignApplicationStatus.COMPLETED,
                ContentSubmissionStatus.APPROVED);
    Map<Long, List<CampaignContentLink>> linksBySubmission = linksBySubmission(completed);

    return new CampaignParticipantDtos.DetailResponse(
        application.getId(),
        application.getCampaign().getId(),
        profile(creator),
        metrics.stream().map(this::metric).toList(),
        answers(application),
        evaluate(application, metrics),
        completed.size(),
        completed.stream()
            .map(submission -> completedCampaign(submission, linksBySubmission))
            .toList());
  }

  private CampaignParticipantDtos.Profile profile(Creator creator) {
    return new CampaignParticipantDtos.Profile(
        creator.getId(),
        creator.getNickname(),
        creator.getMember().getNickname(),
        java.util.Objects.equals(creator.getNickname(), creator.getMember().getNickname()),
        creator.getBio(),
        creator.getPrimaryCategory(),
        creator.getSnsUrl(),
        creator.getStatus());
  }

  private CampaignParticipantDtos.ChannelMetric metric(MemberChannelMetric metric) {
    return new CampaignParticipantDtos.ChannelMetric(
        metric.getChannelType(),
        metric.getMetricType(),
        metricName(metric.getMetricType()),
        metric.getValue(),
        metric.getMeasurementPeriod(),
        metric.getMeasuredAt(),
        metric.getVerificationStatus(),
        "UNKNOWN");
  }

  private CampaignParticipantDtos.RequirementEvaluation evaluate(
      CampaignApplication application, List<MemberChannelMetric> metrics) {
    CampaignEligibilityDtos.RequirementResponse requirement = readRequirement(application);
    if (requirement == null) {
      boolean knownNoRequirement = application.getEligibilityRequirementVersion() == null;
      return new CampaignParticipantDtos.RequirementEvaluation(
          null,
          null,
          null,
          knownNoRequirement,
          knownNoRequirement
              ? "NO_REQUIREMENT_AT_APPLICATION"
              : "APPLICATION_REQUIREMENT_SNAPSHOT_UNAVAILABLE",
          knownNoRequirement
              ? "NO_REQUIREMENT_AT_APPLICATION"
              : "APPLICATION_SNAPSHOT_UNAVAILABLE");
    }
    MemberChannelMetric actual =
        metrics.stream()
            .filter(
                metric ->
                    metric.getChannelType().equals(requirement.channelType())
                        && metric.getMetricType().equals(requirement.metricType()))
            .findFirst()
            .orElse(null);
    String reason = null;
    if (actual == null) reason = "CHANNEL_METRIC_REQUIRED";
    else if (actual.getVerificationStatus() != MetricVerificationStatus.VERIFIED)
      reason = "CHANNEL_METRIC_UNVERIFIED";
    else if (requirement.measurementPeriod() != null
        && !requirement.measurementPeriod().isBlank()
        && !java.util.Objects.equals(
            requirement.measurementPeriod(), actual.getMeasurementPeriod()))
      reason = "MEASUREMENT_PERIOD_MISMATCH";
    else if (actual.getValue() < requirement.minimumValue())
      reason = "CHANNEL_METRIC_BELOW_MINIMUM";
    return new CampaignParticipantDtos.RequirementEvaluation(
        requirement,
        actual == null ? null : actual.getValue(),
        actual == null ? null : actual.getVerificationStatus(),
        reason == null,
        reason,
        "CURRENT_METRIC_AGAINST_APPLICATION_SNAPSHOT");
  }

  private CampaignParticipantDtos.CompletedCampaign completedCampaign(
      CampaignContentSubmission submission,
      Map<Long, List<CampaignContentLink>> linksBySubmission) {
    List<CampaignContentLink> links = linksBySubmission.getOrDefault(submission.getId(), List.of());
    List<CampaignParticipantDtos.PublicContentLink> publicLinks = new ArrayList<>();
    if (links.isEmpty()) {
      if (isPublicHttpUrl(submission.getContentUrl())) {
        publicLinks.add(
            new CampaignParticipantDtos.PublicContentLink("LEGACY", submission.getContentUrl()));
      }
    } else {
      links.forEach(
          link -> {
            if (isPublicHttpUrl(link.getContentUrl())) {
              publicLinks.add(
                  new CampaignParticipantDtos.PublicContentLink(
                      link.getChannelType().name(), link.getContentUrl()));
            }
          });
    }
    return new CampaignParticipantDtos.CompletedCampaign(
        submission.getApplication().getCampaign().getId(),
        submission.getApplication().getCampaign().getTitle(),
        submission.getApplication().getCampaign().getCategory(),
        submission.getReviewedAt(),
        List.copyOf(publicLinks));
  }

  private Map<Long, List<CampaignContentLink>> linksBySubmission(
      List<CampaignContentSubmission> submissions) {
    if (submissions.isEmpty()) return Map.of();
    Map<Long, List<CampaignContentLink>> result = new HashMap<>();
    linkRepository
        .findAllBySubmissionIdInOrderBySubmissionIdAscIdAsc(
            submissions.stream().map(CampaignContentSubmission::getId).toList())
        .forEach(
            link ->
                result
                    .computeIfAbsent(link.getSubmission().getId(), key -> new ArrayList<>())
                    .add(link));
    return result;
  }

  private List<CampaignParticipantDtos.ApplicationAnswer> answers(CampaignApplication application) {
    List<CampaignApplicationDtos.Answer> answers =
        read(application.getAnswersJson(), new TypeReference<>() {});
    CampaignApplicationForm form =
        formRepository.findByCampaignId(application.getCampaign().getId()).orElse(null);
    Map<String, String> labels = new HashMap<>();
    String labelSource = "UNAVAILABLE";
    if (form != null && java.util.Objects.equals(form.getVersion(), application.getFormVersion())) {
      List<CampaignApplicationDtos.QuestionDefinition> questions =
          read(form.getQuestionsJson(), new TypeReference<>() {});
      questions.forEach(question -> labels.put(question.id(), question.label()));
      labelSource = "CURRENT_FORM_VERSION_MATCH";
    }
    String resolvedLabelSource = labelSource;
    return answers.stream()
        .map(
            answer ->
                new CampaignParticipantDtos.ApplicationAnswer(
                    answer.questionId(),
                    labels.get(answer.questionId()),
                    answer.value(),
                    resolvedLabelSource))
        .toList();
  }

  private CampaignEligibilityDtos.RequirementResponse readRequirement(
      CampaignApplication application) {
    if (application.getEligibilityRequirementSnapshotJson() == null) return null;
    return read(
        application.getEligibilityRequirementSnapshotJson(),
        new TypeReference<CampaignEligibilityDtos.RequirementResponse>() {});
  }

  private <T> T read(String json, TypeReference<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JacksonException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "신청자 정보를 읽지 못했습니다.");
    }
  }

  private String metricName(String metricType) {
    return switch (metricType) {
      case "FOLLOWER_COUNT" -> "팔로워 수";
      case "AVERAGE_VISITORS" -> "평균 방문자 수";
      case "SUBSCRIBER_COUNT" -> "구독자 수";
      default -> metricType;
    };
  }

  /** 저장된 주소만 형식 검사하며 외부 콘텐츠에는 접속하지 않습니다. */
  private boolean isPublicHttpUrl(String value) {
    try {
      URI uri = URI.create(value);
      String scheme = uri.getScheme();
      String host = uri.getHost();
      if (host == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)))
        return false;
      String normalized = host.toLowerCase();
      return !normalized.equals("localhost")
          && !normalized.endsWith(".local")
          && !normalized.equals("127.0.0.1")
          && !normalized.equals("0.0.0.0")
          && !normalized.equals("::1")
          && !normalized.startsWith("10.")
          && !normalized.startsWith("192.168.")
          && !normalized.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
}

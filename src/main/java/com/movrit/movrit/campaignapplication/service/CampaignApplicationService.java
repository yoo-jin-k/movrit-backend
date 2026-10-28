package com.movrit.movrit.campaignapplication.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.service.CampaignContentService;
import com.movrit.movrit.campaigneligibility.service.CampaignEligibilityPolicyService;
import com.movrit.movrit.campaigneligibility.service.MemberChannelMetricService;
import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.service.CreatorService;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.product.service.ProductService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
public class CampaignApplicationService {
  private final CampaignRepository campaignRepository;
  private final CampaignApplicationFormRepository formRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final CreatorService creatorService;
  private final MemberService memberService;
  private final CampaignEligibilityPolicyService eligibilityPolicyService;
  private final MemberChannelMetricService memberChannelMetricService;
  private final SellerProfileService sellerProfileService;
  private final ProductService productService;
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;
  private final com.movrit.movrit.campaignmedia.service.CampaignVisitEvidenceService visitEvidence;
  private final CampaignContentService campaignContentService;

  @Transactional
  public CampaignApplicationDtos.FormResponse saveForm(
      Long sellerMemberId, Long campaignId, CampaignApplicationDtos.FormSaveRequest request) {
    return saveForm(sellerMemberId, campaignId, request, null, false);
  }

  @Transactional
  public CampaignApplicationDtos.FormResponse saveFormWithExpectedVersion(
      Long sellerMemberId,
      Long campaignId,
      CampaignApplicationDtos.FormSaveRequest request,
      Long expectedVersion) {
    return saveForm(sellerMemberId, campaignId, request, expectedVersion, true);
  }

  private CampaignApplicationDtos.FormResponse saveForm(
      Long sellerMemberId,
      Long campaignId,
      CampaignApplicationDtos.FormSaveRequest request,
      Long expectedVersion,
      boolean requireExpectedVersion) {
    Campaign campaign = getOwnedCampaign(sellerMemberId, campaignId);
    validateDefinition(request);

    CampaignApplicationForm form =
        formRepository
            .findByCampaignId(campaignId)
            .orElseGet(
                () ->
                    CampaignApplicationForm.builder()
                        .campaign(campaign)
                        .allowedFulfillmentTypesJson(write(request.allowedFulfillmentTypes()))
                        .questionsJson(write(request.questions()))
                        .agreementsJson(write(request.agreements()))
                        .version(1L)
                        .build());
    if (form.getId() != null) {
      if (requireExpectedVersion && !form.getVersion().equals(expectedVersion)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 신청서가 변경되었습니다. 다시 조회해 주세요.");
      }
      form.replace(
          write(request.allowedFulfillmentTypes()),
          write(request.questions()),
          write(request.agreements()));
    }
    return formResponse(formRepository.save(form));
  }

  public CampaignApplicationDtos.FormResponse getForm(Long campaignId) {
    getCampaign(campaignId);
    return formResponse(getFormEntity(campaignId));
  }

  public CampaignApplicationDtos.EligibilityResponse getEligibility(
      Long memberId, Long campaignId) {
    Campaign campaign = getCampaign(campaignId);
    CampaignApplicationForm form = getFormEntity(campaignId);
    Member member = memberService.getMember(memberId);
    Creator creator = creatorService.findOptionalByMember(memberId).orElse(null);
    CampaignEligibilityPolicyService.Evaluation evaluation =
        eligibilityPolicyService.evaluate(memberId, campaignId);
    List<String> reasons = campaignEligibilityReasons(member, campaign, evaluation);
    boolean alreadyApplied =
        applicationRepository.existsByCampaignIdAndCreatorMemberId(campaignId, memberId);
    if (alreadyApplied) {
      reasons.add("ALREADY_APPLIED");
    }
    return new CampaignApplicationDtos.EligibilityResponse(
        campaignId,
        form.getVersion(),
        "EMAIL_AND_CAMPAIGN_REQUIREMENT",
        member.isEmailVerified(),
        false,
        creator != null && hasChannel(creator),
        memberChannelMetricService.findMine(memberId).profileExists(),
        evaluation.requirement(),
        evaluation.metricEvaluation(),
        alreadyApplied,
        reasons.isEmpty(),
        List.copyOf(reasons));
  }

  @Transactional
  public CampaignApplicationDtos.Response apply(
      Long memberId, Long campaignId, CampaignApplicationDtos.CreateRequest request) {
    // 주소 변경과 신청 증빙 저장은 같은 캠페인 잠금으로 순서를 보장합니다.
    campaignRepository
        .findByIdForUpdate(campaignId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    Campaign campaign = getOpenCampaign(campaignId);
    CampaignApplicationForm form = getFormEntity(campaignId);
    Member member = memberService.getMember(memberId);

    if (!form.getVersion().equals(request.expectedFormVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "신청 조건이 변경되었습니다. 최신 신청서를 다시 확인해 주세요.");
    }
    CampaignEligibilityPolicyService.Evaluation eligibility = validateEligibility(member, campaign);
    validateExpectedRequirement(request, eligibility.requirement());
    if (applicationRepository.existsByCampaignIdAndCreatorMemberId(campaignId, memberId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
    }
    validateApplication(form, request);
    Creator creator = creatorService.getOrCreateCandidate(memberId);

    CampaignApplication application =
        CampaignApplication.builder()
            .campaign(campaign)
            .creator(creator)
            .fulfillmentType(request.fulfillmentType())
            .status(CampaignApplicationStatus.SUBMITTED)
            .message(request.message().trim())
            .recipientName(
                contactValue(request, CampaignApplicationDtos.ShippingContact::recipientName))
            .recipientPhone(
                contactValue(request, CampaignApplicationDtos.ShippingContact::recipientPhone))
            .postalCode(contactValue(request, CampaignApplicationDtos.ShippingContact::postalCode))
            .address1(contactValue(request, CampaignApplicationDtos.ShippingContact::address1))
            .address2(contactValue(request, CampaignApplicationDtos.ShippingContact::address2))
            .answersJson(write(request.answers()))
            .agreementAcceptancesJson(write(request.agreementAcceptances()))
            .formVersion(form.getVersion())
            .eligibilityRequirementSnapshotJson(
                eligibility.requirement() == null ? null : write(eligibility.requirement()))
            .eligibilityRequirementVersion(
                eligibility.requirement() == null ? null : eligibility.requirement().version())
            // 본인 인증 공급자가 정해지기 전에는 인증 완료로 간주하지 않습니다.
            .identityVerificationConfigured(false)
            .identityVerified(false)
            .emailVerified(member.isEmailVerified())
            .channelReady(hasChannel(creator))
            .submittedAt(LocalDateTime.now())
            .build();
    try {
      CampaignApplication saved = applicationRepository.saveAndFlush(application);
      visitEvidence.capture(saved);
      Long sellerMemberId =
          campaign.getPreorder().getProduct().getBrand().getSeller().getMember().getId();
      notificationService.send(
          sellerMemberId,
          "CAMPAIGN_APPLICATION",
          "새 캠페인 신청이 접수되었습니다.",
          creator.getNickname() + "님이 캠페인에 신청하셨습니다.",
          "CAMPAIGN_APPLICATION",
          saved.getId());
      auditLogService.record(
          memberId,
          "CAMPAIGN_APPLICATION_SUBMITTED",
          "CAMPAIGN_APPLICATION",
          saved.getId(),
          null,
          "{\"status\":\"SUBMITTED\"}",
          null);
      return response(saved);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
    }
  }

  public List<CampaignApplicationDtos.Response> findMine(Long memberId) {
    return applicationRepository.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(this::response)
        .toList();
  }

  public List<CampaignApplicationDtos.WorkspaceResponse> findMyWorkspaces(Long memberId) {
    return applicationRepository.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(
            application -> {
              Campaign campaign = application.getCampaign();
              boolean contactVisible =
                  application.getStatus() != CampaignApplicationStatus.SUBMITTED
                      && application.getStatus() != CampaignApplicationStatus.REJECTED
                      && application.getStatus() != CampaignApplicationStatus.CANCELLED;
              CampaignManagementDtos.ContactResponse contact =
                  !contactVisible
                          || campaign.getContactName() == null
                          || campaign.getContactPhone() == null
                      ? null
                      : new CampaignManagementDtos.ContactResponse(
                          campaign.getContactName(), campaign.getContactPhone());
              return new CampaignApplicationDtos.WorkspaceResponse(
                  response(application),
                  CampaignManagementDtos.DetailResponse.from(
                      campaign, application.getFulfillmentType() == FulfillmentType.VISIT),
                  contact,
                  campaignContentService.findMineByApplication(memberId, application.getId()));
            })
        .toList();
  }

  public List<CampaignApplicationDtos.Response> findSellerApplications(Long memberId) {
    sellerProfileService.getApprovedSeller(memberId);
    return applicationRepository
        .findAllByCampaignPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
        .stream()
        .map(this::sellerResponse)
        .toList();
  }

  @Transactional
  public CampaignApplicationDtos.Response select(Long memberId, Long applicationId) {
    CampaignApplication application = getSellerApplication(memberId, applicationId);
    Campaign lockedCampaign =
        campaignRepository
            .findByIdForUpdate(application.getCampaign().getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    long selectedCount =
        applicationRepository.countByCampaignIdAndStatusIn(
            lockedCampaign.getId(),
            List.of(
                CampaignApplicationStatus.SELECTED,
                CampaignApplicationStatus.ACTIVE,
                CampaignApplicationStatus.CONTENT_SUBMITTED,
                CampaignApplicationStatus.REVISION_REQUESTED,
                CampaignApplicationStatus.COMPLETED));
    if (selectedCount >= lockedCampaign.getRecruitmentCount()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 모집 인원이 모두 선정되었습니다.");
    }
    CampaignApplicationDtos.Response response =
        transition(application, CampaignApplication::select);
    notifyCreatorAndAudit(
        memberId, application, "신청이 선정되었습니다.", "CAMPAIGN_APPLICATION_SELECTED", "SELECTED", null);
    return response;
  }

  @Transactional
  public CampaignApplicationDtos.Response reject(Long memberId, Long applicationId) {
    CampaignApplication application = getSellerApplication(memberId, applicationId);
    CampaignApplicationDtos.Response response =
        transition(application, CampaignApplication::reject);
    notifyCreatorAndAudit(
        memberId,
        application,
        "신청이 선정되지 않았습니다.",
        "CAMPAIGN_APPLICATION_REJECTED",
        "REJECTED",
        null);
    return response;
  }

  @Transactional
  public CampaignApplicationDtos.Response activate(Long memberId, Long applicationId) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    CampaignApplicationDtos.Response response =
        transition(application, CampaignApplication::activate);
    Long sellerMemberId =
        application
            .getCampaign()
            .getPreorder()
            .getProduct()
            .getBrand()
            .getSeller()
            .getMember()
            .getId();
    notificationService.send(
        sellerMemberId,
        "CAMPAIGN_APPLICATION",
        "캠페인 참여가 확정되었습니다.",
        application.getCreator().getNickname() + "님이 참여를 확정하셨습니다.",
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        memberId,
        "CAMPAIGN_APPLICATION_ACTIVATED",
        "CAMPAIGN_APPLICATION",
        application.getId(),
        "{\"status\":\"SELECTED\"}",
        "{\"status\":\"ACTIVE\"}",
        null);
    return response;
  }

  @Transactional
  public CampaignApplicationDtos.Response cancel(
      Long memberId, Long applicationId, CampaignApplicationDtos.CancelRequest request) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    CampaignApplicationStatus previousStatus = application.getStatus();
    CampaignApplicationDtos.Response response =
        transition(application, value -> value.cancel(request.reason().trim()));
    Long sellerMemberId =
        application
            .getCampaign()
            .getPreorder()
            .getProduct()
            .getBrand()
            .getSeller()
            .getMember()
            .getId();
    notificationService.send(
        sellerMemberId,
        "CAMPAIGN_APPLICATION",
        "캠페인 참여가 취소되었습니다.",
        application.getCreator().getNickname() + "님이 참여를 취소하셨습니다.",
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        memberId,
        "CAMPAIGN_APPLICATION_CANCELLED",
        "CAMPAIGN_APPLICATION",
        application.getId(),
        "{\"status\":\"" + previousStatus + "\"}",
        "{\"status\":\"CANCELLED\"}",
        request.reason().trim());
    return response;
  }

  @Transactional
  public CampaignApplicationDtos.Response confirmExperience(Long memberId, Long applicationId) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    CampaignApplicationDtos.Response response =
        transition(application, CampaignApplication::confirmExperience);
    Long sellerMemberId =
        application
            .getCampaign()
            .getPreorder()
            .getProduct()
            .getBrand()
            .getSeller()
            .getMember()
            .getId();
    notificationService.send(
        sellerMemberId,
        "CAMPAIGN_EXPERIENCE",
        "체험 완료가 확인되었습니다.",
        application.getCreator().getNickname() + "님이 체험 완료를 확인하셨습니다.",
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        memberId,
        "CAMPAIGN_EXPERIENCE_CONFIRMED",
        "CAMPAIGN_APPLICATION",
        application.getId(),
        null,
        "{\"experienceConfirmed\":true}",
        null);
    return response;
  }

  private void validateDefinition(CampaignApplicationDtos.FormSaveRequest request) {
    requireUniqueIds(
        request.questions().stream().map(CampaignApplicationDtos.QuestionDefinition::id).toList(),
        "질문 ID");
    requireUniqueIds(
        request.agreements().stream().map(CampaignApplicationDtos.AgreementDefinition::id).toList(),
        "동의 문서 ID");
    boolean invalidChoice =
        request.questions().stream()
            .anyMatch(
                question ->
                    question.type() == CampaignApplicationDtos.QuestionType.SINGLE_CHOICE
                        && question.options().isEmpty());
    if (invalidChoice) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단일 선택 질문에는 선택지가 필요합니다.");
    }
  }

  private CampaignEligibilityPolicyService.Evaluation validateEligibility(
      Member member, Campaign campaign) {
    if (!member.isEmailVerified()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이메일 소유 확인 후 캠페인에 신청할 수 있습니다.");
    }
    CampaignEligibilityPolicyService.Evaluation evaluation =
        eligibilityPolicyService.evaluate(member.getId(), campaign.getId());
    String reason = evaluation.blockingReason();
    if (reason == null) return evaluation;
    String message =
        switch (reason) {
          case "CHANNEL_METRIC_REQUIRED" -> "캠페인 신청에 필요한 채널 지표를 먼저 등록해 주세요.";
          case "CHANNEL_METRIC_UNVERIFIED" -> "캠페인 신청에 필요한 채널 지표가 아직 확인되지 않았습니다.";
          case "CHANNEL_METRIC_BELOW_MINIMUM" -> "캠페인의 채널 지표 조건을 충족하지 못했습니다.";
          default -> "캠페인 신청 조건을 확인해 주세요.";
        };
    throw new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  private void validateExpectedRequirement(
      CampaignApplicationDtos.CreateRequest request,
      com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos.RequirementResponse
          current) {
    Long currentId = current == null ? null : current.id();
    Long currentVersion = current == null ? null : current.version();
    if (!java.util.Objects.equals(request.expectedEligibilityRequirementId(), currentId)
        || !java.util.Objects.equals(
            request.expectedEligibilityRequirementVersion(), currentVersion)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "캠페인 신청 조건이 변경되었습니다. 최신 조건을 다시 확인해 주세요.");
    }
  }

  private List<String> campaignEligibilityReasons(
      Member member, Campaign campaign, CampaignEligibilityPolicyService.Evaluation evaluation) {
    List<String> reasons = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    if (!campaign.isOpen()
        || now.isBefore(campaign.getStartAt())
        || !now.isBefore(campaign.getEndAt())) {
      reasons.add("CAMPAIGN_NOT_OPEN");
    }
    if (!member.isEmailVerified()) {
      reasons.add("EMAIL_VERIFICATION_REQUIRED");
    }
    if (evaluation.blockingReason() != null) reasons.add(evaluation.blockingReason());
    return reasons;
  }

  private boolean hasChannel(Creator creator) {
    return creator.getSnsUrl() != null && !creator.getSnsUrl().isBlank();
  }

  private void validateApplication(
      CampaignApplicationForm form, CampaignApplicationDtos.CreateRequest request) {
    List<FulfillmentType> allowedTypes =
        read(form.getAllowedFulfillmentTypesJson(), new TypeReference<>() {});
    if (!allowedTypes.contains(request.fulfillmentType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이 캠페인에서 지원하지 않는 참여 방식입니다.");
    }
    validateShippingContact(request);

    List<CampaignApplicationDtos.QuestionDefinition> questions =
        read(form.getQuestionsJson(), new TypeReference<>() {});
    Map<String, String> answers = new HashMap<>();
    for (CampaignApplicationDtos.Answer answer : request.answers()) {
      if (answers.put(answer.questionId(), answer.value()) != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 질문에 답변을 두 번 보낼 수 없습니다.");
      }
    }
    Set<String> knownQuestionIds =
        questions.stream()
            .map(CampaignApplicationDtos.QuestionDefinition::id)
            .collect(java.util.stream.Collectors.toSet());
    if (!knownQuestionIds.containsAll(answers.keySet())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신청서에 없는 질문 답변이 포함되어 있습니다.");
    }
    boolean missingRequiredAnswer =
        questions.stream()
            .filter(CampaignApplicationDtos.QuestionDefinition::required)
            .anyMatch(question -> isBlank(answers.get(question.id())));
    if (missingRequiredAnswer) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 질문에 모두 답변해 주세요.");
    }

    for (CampaignApplicationDtos.QuestionDefinition question : questions) {
      String value = answers.get(question.id());
      if (value == null) {
        continue;
      }
      if (question.maxLength() != null && value.length() > question.maxLength()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            question.label() + " 답변은 " + question.maxLength() + "자 이내로 입력해 주세요.");
      }
      if (question.type() == CampaignApplicationDtos.QuestionType.SINGLE_CHOICE
          && !question.options().contains(value)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문의 선택지에 없는 답변입니다.");
      }
    }

    validateAgreements(form, request.agreementAcceptances());
  }

  private void validateShippingContact(CampaignApplicationDtos.CreateRequest request) {
    CampaignApplicationDtos.ShippingContact contact = request.shippingContact();
    if (request.fulfillmentType() == FulfillmentType.VISIT) {
      if (contact != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방문형 캠페인은 배송 주소를 수집하지 않습니다.");
      }
      return;
    }
    if (contact == null
        || isBlank(contact.recipientName())
        || isBlank(contact.recipientPhone())
        || isBlank(contact.postalCode())
        || isBlank(contact.address1())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송형 캠페인의 수령 정보를 모두 입력해 주세요.");
    }
  }

  private void validateAgreements(
      CampaignApplicationForm form, List<CampaignApplicationDtos.AgreementAcceptance> acceptances) {
    List<CampaignApplicationDtos.AgreementDefinition> agreements =
        read(form.getAgreementsJson(), new TypeReference<>() {});
    Map<String, CampaignApplicationDtos.AgreementAcceptance> acceptedById = new HashMap<>();
    for (CampaignApplicationDtos.AgreementAcceptance acceptance : acceptances) {
      if (acceptedById.put(acceptance.agreementId(), acceptance) != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 동의 문서를 두 번 제출할 수 없습니다.");
      }
    }
    for (CampaignApplicationDtos.AgreementDefinition agreement : agreements) {
      CampaignApplicationDtos.AgreementAcceptance acceptance = acceptedById.get(agreement.id());
      if (agreement.required()
          && (acceptance == null
              || !acceptance.accepted()
              || !agreement.version().equals(acceptance.version()))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 동의 문서의 최신 버전에 동의해 주세요.");
      }
    }
  }

  private void requireUniqueIds(List<String> ids, String label) {
    if (new HashSet<>(ids).size() != ids.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "가 중복되었습니다.");
    }
  }

  private Campaign getOpenCampaign(Long id) {
    Campaign campaign = getCampaign(id);
    LocalDateTime now = LocalDateTime.now();
    if (!campaign.isOpen()
        || now.isBefore(campaign.getStartAt())
        || !now.isBefore(campaign.getEndAt())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 신청할 수 없는 캠페인입니다.");
    }
    return campaign;
  }

  private Campaign getOwnedCampaign(Long memberId, Long id) {
    Campaign campaign = getCampaign(id);
    productService.getOwnedProduct(memberId, campaign.getPreorder().getProduct().getId());
    return campaign;
  }

  private Campaign getCampaign(Long id) {
    return campaignRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
  }

  private CampaignApplicationForm getFormEntity(Long campaignId) {
    return formRepository
        .findByCampaignId(campaignId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인 신청서가 아직 구성되지 않았습니다."));
  }

  private CampaignApplication getSellerApplication(Long memberId, Long applicationId) {
    CampaignApplication application =
        applicationRepository
            .findById(applicationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    getOwnedCampaign(memberId, application.getCampaign().getId());
    return application;
  }

  private CampaignApplicationDtos.FormResponse formResponse(CampaignApplicationForm form) {
    return new CampaignApplicationDtos.FormResponse(
        form.getCampaign().getId(),
        form.getVersion(),
        read(form.getAllowedFulfillmentTypesJson(), new TypeReference<>() {}),
        read(form.getQuestionsJson(), new TypeReference<>() {}),
        read(form.getAgreementsJson(), new TypeReference<>() {}));
  }

  private CampaignApplicationDtos.Response response(CampaignApplication application) {
    return response(application, false);
  }

  private CampaignApplicationDtos.Response sellerResponse(CampaignApplication application) {
    boolean hideContact =
        application.getStatus() == CampaignApplicationStatus.SUBMITTED
            || application.getStatus() == CampaignApplicationStatus.REJECTED;
    return response(application, hideContact);
  }

  private CampaignApplicationDtos.Response response(
      CampaignApplication application, boolean hideContact) {
    CampaignApplicationDtos.ShippingContact contact =
        application.getFulfillmentType() == FulfillmentType.DELIVERY && !hideContact
            ? new CampaignApplicationDtos.ShippingContact(
                application.getRecipientName(),
                application.getRecipientPhone(),
                application.getPostalCode(),
                application.getAddress1(),
                application.getAddress2())
            : null;
    List<String> notices = new ArrayList<>();
    if (!application.isIdentityVerificationConfigured()) notices.add("본인 인증 기능이 아직 구성되지 않았습니다.");
    if (!application.isChannelReady()) notices.add("활동 채널 정보를 먼저 등록해 주세요.");
    return new CampaignApplicationDtos.Response(
        application.getId(),
        application.getCampaign().getId(),
        application.getCampaign().getPreorder().getId(),
        application.getCreator().getId(),
        application.getCreator().getNickname(),
        application.getFulfillmentType(),
        application.getStatus(),
        application.getMessage(),
        contact,
        read(application.getAnswersJson(), new TypeReference<>() {}),
        read(application.getAgreementAcceptancesJson(), new TypeReference<>() {}),
        application.getFormVersion(),
        application.getEligibilityRequirementSnapshotJson() == null
            ? null
            : read(application.getEligibilityRequirementSnapshotJson(), new TypeReference<>() {}),
        application.getEligibilityRequirementVersion(),
        application.isIdentityVerificationConfigured(),
        application.isIdentityVerified(),
        application.isEmailVerified(),
        application.isChannelReady(),
        notices,
        application.getSubmittedAt(),
        application.getRespondedAt(),
        application.getCancelledAt(),
        application.getCancellationReason(),
        application.getExperienceConfirmedAt());
  }

  private CampaignApplicationDtos.Response transition(
      CampaignApplication application, Consumer<CampaignApplication> action) {
    try {
      action.accept(application);
      return response(application);
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
    }
  }

  private void notifyCreatorAndAudit(
      Long actorMemberId,
      CampaignApplication application,
      String title,
      String action,
      String nextStatus,
      String reason) {
    notificationService.send(
        application.getCreator().getMember().getId(),
        "CAMPAIGN_APPLICATION",
        title,
        application.getCampaign().getTitle(),
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        actorMemberId,
        action,
        "CAMPAIGN_APPLICATION",
        application.getId(),
        "{\"status\":\"SUBMITTED\"}",
        "{\"status\":\"" + nextStatus + "\"}",
        reason);
  }

  private String contactValue(
      CampaignApplicationDtos.CreateRequest request,
      Function<CampaignApplicationDtos.ShippingContact, String> getter) {
    return request.shippingContact() == null
        ? null
        : normalize(getter.apply(request.shippingContact()));
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "신청서 데이터를 저장하지 못했습니다.");
    }
  }

  private <T> T read(String json, TypeReference<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JacksonException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "신청서 데이터를 읽지 못했습니다.");
    }
  }
}

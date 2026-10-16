package com.movrit.movrit.campaignapplication.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.entity.*;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.service.CreatorService;
import com.movrit.movrit.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignApplicationService {
    private final CampaignRepository campaignRepository;
    private final CampaignApplicationFormRepository formRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final CreatorService creatorService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CampaignApplicationDtos.FormResponse saveForm(
            Long sellerMemberId,
            Long campaignId,
            CampaignApplicationDtos.FormSaveRequest request
    ) {
        Campaign campaign = getOwnedCampaign(sellerMemberId, campaignId);
        validateDefinition(request);

        CampaignApplicationForm form = formRepository.findByCampaignId(campaignId)
                .orElseGet(() -> CampaignApplicationForm.builder()
                        .campaign(campaign)
                        .allowedFulfillmentTypesJson(write(request.allowedFulfillmentTypes()))
                        .questionsJson(write(request.questions()))
                        .agreementsJson(write(request.agreements()))
                        .version(1L)
                        .build());
        if (form.getId() != null) {
            form.replace(
                    write(request.allowedFulfillmentTypes()),
                    write(request.questions()),
                    write(request.agreements())
            );
        }
        return formResponse(formRepository.save(form));
    }

    public CampaignApplicationDtos.FormResponse getForm(Long campaignId) {
        getCampaign(campaignId);
        return formResponse(getFormEntity(campaignId));
    }

    @Transactional
    public CampaignApplicationDtos.Response apply(
            Long memberId,
            Long campaignId,
            CampaignApplicationDtos.CreateRequest request
    ) {
        Campaign campaign = getOpenCampaign(campaignId);
        CampaignApplicationForm form = getFormEntity(campaignId);
        Creator creator = creatorService.getByMember(memberId);

        if (applicationRepository.existsByCampaignIdAndCreatorId(campaignId, creator.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
        }
        validateApplication(form, request);

        CampaignApplication application = CampaignApplication.builder()
                .campaign(campaign)
                .creator(creator)
                .fulfillmentType(request.fulfillmentType())
                .status(CampaignApplicationStatus.SUBMITTED)
                .message(request.message().trim())
                .recipientName(contactValue(request, CampaignApplicationDtos.ShippingContact::recipientName))
                .recipientPhone(contactValue(request, CampaignApplicationDtos.ShippingContact::recipientPhone))
                .postalCode(contactValue(request, CampaignApplicationDtos.ShippingContact::postalCode))
                .address1(contactValue(request, CampaignApplicationDtos.ShippingContact::address1))
                .address2(contactValue(request, CampaignApplicationDtos.ShippingContact::address2))
                .answersJson(write(request.answers()))
                .agreementAcceptancesJson(write(request.agreementAcceptances()))
                .formVersion(form.getVersion())
                // 본인 인증 공급자가 정해지기 전에는 인증 완료로 간주하지 않습니다.
                .identityVerificationConfigured(false)
                .identityVerified(false)
                .channelReady(creator.getSnsUrl() != null && !creator.getSnsUrl().isBlank())
                .submittedAt(LocalDateTime.now())
                .build();
        try {
            return response(applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 캠페인에 신청하셨습니다.");
        }
    }

    public List<CampaignApplicationDtos.Response> findMine(Long memberId) {
        return applicationRepository.findAllByCreatorMemberIdOrderByIdDesc(memberId)
                .stream()
                .map(this::response)
                .toList();
    }

    public List<CampaignApplicationDtos.Response> findSellerApplications(Long memberId) {
        return applicationRepository
                .findAllByCampaignPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
                .stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public CampaignApplicationDtos.Response select(Long memberId, Long applicationId) {
        CampaignApplication application = getSellerApplication(memberId, applicationId);
        long selectedCount = applicationRepository.countByCampaignIdAndStatusIn(
                application.getCampaign().getId(),
                List.of(
                        CampaignApplicationStatus.SELECTED,
                        CampaignApplicationStatus.ACTIVE,
                        CampaignApplicationStatus.CONTENT_SUBMITTED,
                        CampaignApplicationStatus.REVISION_REQUESTED,
                        CampaignApplicationStatus.COMPLETED
                )
        );
        if (selectedCount >= application.getCampaign().getRecruitmentCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 모집 인원이 모두 선정되었습니다.");
        }
        return transition(application, CampaignApplication::select);
    }

    @Transactional
    public CampaignApplicationDtos.Response reject(Long memberId, Long applicationId) {
        return transition(getSellerApplication(memberId, applicationId), CampaignApplication::reject);
    }

    @Transactional
    public CampaignApplicationDtos.Response activate(Long memberId, Long applicationId) {
        CampaignApplication application = applicationRepository
                .findByIdAndCreatorMemberId(applicationId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
        return transition(application, CampaignApplication::activate);
    }

    @Transactional
    public CampaignApplicationDtos.Response cancel(
            Long memberId,
            Long applicationId,
            CampaignApplicationDtos.CancelRequest request
    ) {
        CampaignApplication application = applicationRepository
                .findByIdAndCreatorMemberId(applicationId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
        return transition(application, value -> value.cancel(request.reason().trim()));
    }

    private void validateDefinition(CampaignApplicationDtos.FormSaveRequest request) {
        requireUniqueIds(request.questions().stream().map(CampaignApplicationDtos.QuestionDefinition::id).toList(), "질문 ID");
        requireUniqueIds(request.agreements().stream().map(CampaignApplicationDtos.AgreementDefinition::id).toList(), "동의 문서 ID");
    }

    private void validateApplication(
            CampaignApplicationForm form,
            CampaignApplicationDtos.CreateRequest request
    ) {
        List<FulfillmentType> allowedTypes = read(form.getAllowedFulfillmentTypesJson(), new TypeReference<>() {});
        if (!allowedTypes.contains(request.fulfillmentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이 캠페인에서 지원하지 않는 참여 방식입니다.");
        }
        validateShippingContact(request);

        List<CampaignApplicationDtos.QuestionDefinition> questions = read(
                form.getQuestionsJson(),
                new TypeReference<>() {}
        );
        Map<String, String> answers = new HashMap<>();
        for (CampaignApplicationDtos.Answer answer : request.answers()) {
            if (answers.put(answer.questionId(), answer.value()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 질문에 답변을 두 번 보낼 수 없습니다.");
            }
        }
        Set<String> knownQuestionIds = questions.stream()
                .map(CampaignApplicationDtos.QuestionDefinition::id)
                .collect(java.util.stream.Collectors.toSet());
        if (!knownQuestionIds.containsAll(answers.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신청서에 없는 질문 답변이 포함되어 있습니다.");
        }
        boolean missingRequiredAnswer = questions.stream()
                .filter(CampaignApplicationDtos.QuestionDefinition::required)
                .anyMatch(question -> isBlank(answers.get(question.id())));
        if (missingRequiredAnswer) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 질문에 모두 답변해 주세요.");
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
            CampaignApplicationForm form,
            List<CampaignApplicationDtos.AgreementAcceptance> acceptances
    ) {
        List<CampaignApplicationDtos.AgreementDefinition> agreements = read(
                form.getAgreementsJson(),
                new TypeReference<>() {}
        );
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
        if (!campaign.isOpen() || now.isBefore(campaign.getStartAt()) || !now.isBefore(campaign.getEndAt())) {
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
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }

    private CampaignApplicationForm getFormEntity(Long campaignId) {
        return formRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인 신청서가 아직 구성되지 않았습니다."));
    }

    private CampaignApplication getSellerApplication(Long memberId, Long applicationId) {
        CampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
        getOwnedCampaign(memberId, application.getCampaign().getId());
        return application;
    }

    private CampaignApplicationDtos.FormResponse formResponse(CampaignApplicationForm form) {
        return new CampaignApplicationDtos.FormResponse(
                form.getCampaign().getId(),
                form.getVersion(),
                read(form.getAllowedFulfillmentTypesJson(), new TypeReference<>() {}),
                read(form.getQuestionsJson(), new TypeReference<>() {}),
                read(form.getAgreementsJson(), new TypeReference<>() {})
        );
    }

    private CampaignApplicationDtos.Response response(CampaignApplication application) {
        CampaignApplicationDtos.ShippingContact contact = application.getFulfillmentType() == FulfillmentType.DELIVERY
                ? new CampaignApplicationDtos.ShippingContact(
                        application.getRecipientName(),
                        application.getRecipientPhone(),
                        application.getPostalCode(),
                        application.getAddress1(),
                        application.getAddress2()
                )
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
                application.isIdentityVerificationConfigured(),
                application.isIdentityVerified(),
                application.isChannelReady(),
                notices,
                application.getSubmittedAt(),
                application.getRespondedAt(),
                application.getCancelledAt(),
                application.getCancellationReason()
        );
    }

    private CampaignApplicationDtos.Response transition(
            CampaignApplication application,
            Consumer<CampaignApplication> action
    ) {
        try {
            action.accept(application);
            return response(application);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    private String contactValue(
            CampaignApplicationDtos.CreateRequest request,
            Function<CampaignApplicationDtos.ShippingContact, String> getter
    ) {
        return request.shippingContact() == null ? null : normalize(getter.apply(request.shippingContact()));
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

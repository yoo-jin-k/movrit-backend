package com.movrit.movrit.campaignapplication.service;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;

/**
 * 캠페인 신청서 양식(체험 방식·질문·동의 문서)을 저장하고 조회합니다.
 *
 * <p>양식은 캠페인 소유 업체만 만들거나 고칠 수 있고, 고칠 때마다 버전이 올라갑니다. 공개 조회는 로그인 없이 가능합니다. 신청 처리 자체는 {@link
 * CampaignApplicationService}가 맡습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignApplicationFormService {
  private final CampaignRepository campaignRepository;
  private final CampaignApplicationFormRepository formRepository;
  private final CampaignOwnershipService campaignOwnership;
  private final CampaignApplicationJson json;

  /**
   * 캠페인 소유 업체가 신청서 양식을 만들거나 고칩니다. 버전 확인 없이 저장하는 기존 경로입니다.
   *
   * @throws ResponseStatusException 소유 캠페인이 아니거나 질문·동의 문서 ID가 중복되면 거절합니다
   */
  @Transactional
  public CampaignApplicationDtos.FormResponse saveForm(
      Long sellerMemberId, Long campaignId, CampaignApplicationDtos.FormSaveRequest request) {
    return saveForm(sellerMemberId, campaignId, request, null, false);
  }

  /**
   * 신청서 양식을 저장하되, 화면이 본 버전과 현재 버전이 같을 때만 저장합니다. 다른 사람이 먼저 고친 내용을 덮어쓰지 않기 위해 사용합니다.
   *
   * @throws ResponseStatusException 버전이 다르면 409
   */
  @Transactional
  public CampaignApplicationDtos.FormResponse saveFormWithExpectedVersion(
      Long sellerMemberId,
      Long campaignId,
      CampaignApplicationDtos.FormSaveRequest request,
      Long expectedVersion) {
    return saveForm(sellerMemberId, campaignId, request, expectedVersion, true);
  }

  /**
   * 캠페인의 공개 신청서 양식을 조회합니다. 로그인하지 않아도 볼 수 있습니다.
   *
   * @throws ResponseStatusException 캠페인이나 양식이 없으면 404
   */
  public CampaignApplicationDtos.FormResponse getForm(Long campaignId) {
    campaignRepository
        .findById(campaignId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    return formResponse(getFormEntity(campaignId));
  }

  /** 신청 처리에서 쓰는 양식 엔티티를 찾습니다. 아직 양식을 만들지 않은 캠페인은 404로 안내합니다. */
  CampaignApplicationForm getFormEntity(Long campaignId) {
    return formRepository
        .findByCampaignId(campaignId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인 신청서가 아직 구성되지 않았습니다."));
  }

  /** 양식이 허용하는 체험 방식(배송형·방문형) 목록입니다. */
  List<FulfillmentType> allowedTypes(CampaignApplicationForm form) {
    return json.read(form.getAllowedFulfillmentTypesJson(), new TypeReference<>() {});
  }

  /** 양식의 질문 목록입니다. */
  List<CampaignApplicationDtos.QuestionDefinition> questions(CampaignApplicationForm form) {
    return json.read(form.getQuestionsJson(), new TypeReference<>() {});
  }

  /** 양식의 동의 문서 목록입니다. */
  List<CampaignApplicationDtos.AgreementDefinition> agreements(CampaignApplicationForm form) {
    return json.read(form.getAgreementsJson(), new TypeReference<>() {});
  }

  /** 양식이 없으면 버전 1로 새로 만들고, 있으면 내용을 교체하면서 버전을 올립니다. */
  private CampaignApplicationDtos.FormResponse saveForm(
      Long sellerMemberId,
      Long campaignId,
      CampaignApplicationDtos.FormSaveRequest request,
      Long expectedVersion,
      boolean requireExpectedVersion) {
    Campaign campaign =
        campaignRepository
            .findById(campaignId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    campaignOwnership.requireOwner(sellerMemberId, campaign);
    validateDefinition(request);
    requireMatchingFulfillment(campaign, request.allowedFulfillmentTypes());

    CampaignApplicationForm form =
        formRepository
            .findByCampaignId(campaignId)
            .orElseGet(
                () ->
                    CampaignApplicationForm.builder()
                        .campaign(campaign)
                        .allowedFulfillmentTypesJson(json.write(request.allowedFulfillmentTypes()))
                        .questionsJson(json.write(request.questions()))
                        .agreementsJson(json.write(request.agreements()))
                        .version(1L)
                        .build());
    if (form.getId() != null) {
      if (requireExpectedVersion && !form.getVersion().equals(expectedVersion)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "캠페인 신청서가 변경되었습니다. 다시 조회해 주세요.");
      }
      form.replace(
          json.write(request.allowedFulfillmentTypes()),
          json.write(request.questions()),
          json.write(request.agreements()));
    }
    return formResponse(formRepository.save(form));
  }

  /**
   * 구매평 캠페인은 '구매형'만, 다른 캠페인은 배송형·방문형만, 선착순 캠페인은 방문형만 허용합니다.
   *
   * <p>구매형은 배송 주소를 받지 않고 구매 인증으로 진행하므로, 다른 유형과 섞이면 진행 단계가 맞지 않습니다.
   */
  private static void requireMatchingFulfillment(
      Campaign campaign, java.util.List<FulfillmentType> types) {
    boolean purchaseCampaign =
        campaign.getCampaignType()
            == com.movrit.movrit.collaboration.entity.CampaignType.PURCHASE_REVIEW;
    boolean onlyPurchase = types.stream().allMatch(type -> type == FulfillmentType.PURCHASE);
    boolean hasPurchase = types.contains(FulfillmentType.PURCHASE);
    if (purchaseCampaign && !onlyPurchase) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "구매평 캠페인은 '직접 구매' 방식만 고를 수 있습니다.");
    }
    if (!purchaseCampaign && hasPurchase) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'직접 구매' 방식은 구매평 캠페인에서만 쓸 수 있습니다.");
    }
    // 선착순은 신청과 동시에 선정되어 바로 방문 일정을 잡는 방식이라 방문형에서만 씁니다.
    boolean firstCome =
        campaign.getSelectionMode()
            == com.movrit.movrit.collaboration.entity.CampaignSelectionMode.FIRST_COME;
    if (firstCome && !types.stream().allMatch(type -> type == FulfillmentType.VISIT)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선착순 캠페인은 '방문형' 방식만 고를 수 있습니다.");
    }
  }

  /** 신청서 양식 자체를 검사합니다. 질문·동의 문서 ID 중복과 선택지 없는 단일 선택 질문을 거절합니다. */
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

  /** 같은 ID가 두 번 나오면 거절합니다. */
  private void requireUniqueIds(List<String> ids, String label) {
    if (new HashSet<>(ids).size() != ids.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "가 중복되었습니다.");
    }
  }

  /** 엔티티의 JSON 값을 화면용 양식 응답으로 바꿉니다. */
  private CampaignApplicationDtos.FormResponse formResponse(CampaignApplicationForm form) {
    return new CampaignApplicationDtos.FormResponse(
        form.getCampaign().getId(),
        form.getVersion(),
        allowedTypes(form),
        questions(form),
        agreements(form));
  }
}

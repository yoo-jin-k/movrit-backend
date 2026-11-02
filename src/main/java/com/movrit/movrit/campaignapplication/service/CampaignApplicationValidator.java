package com.movrit.movrit.campaignapplication.service;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 참여자가 보낸 신청서 내용이 캠페인 양식에 맞는지 검사합니다.
 *
 * <p>허용된 체험 방식인지, 배송 정보, 질문 답변(중복·누락·글자 수·선택지), 필수 동의 문서 순서로 확인하고, 문제가 있으면 400으로 사유를 안내합니다. 자격(이메일
 * 확인·채널 지표)과 모집 기간 검사는 {@link CampaignApplicationService}가 맡습니다.
 */
@Component
@RequiredArgsConstructor
class CampaignApplicationValidator {
  private final CampaignApplicationFormService forms;

  /** 신청서 전체를 검사합니다. */
  void validateApplication(
      CampaignApplicationForm form, CampaignApplicationDtos.CreateRequest request) {
    if (!forms.allowedTypes(form).contains(request.fulfillmentType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이 캠페인에서 지원하지 않는 참여 방식입니다.");
    }
    validateShippingContact(request);
    validateAnswers(forms.questions(form), request.answers());
    validateAgreements(forms.agreements(form), request.agreementAcceptances());
  }

  /** 배송형은 수령인·연락처·우편번호·주소를 모두 요구하고, 방문형·구매형은 배송 주소를 받지 않습니다. */
  private void validateShippingContact(CampaignApplicationDtos.CreateRequest request) {
    CampaignApplicationDtos.ShippingContact contact = request.shippingContact();
    if (request.fulfillmentType() == FulfillmentType.VISIT
        || request.fulfillmentType() == FulfillmentType.PURCHASE) {
      if (contact != null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            request.fulfillmentType() == FulfillmentType.VISIT
                ? "방문형 캠페인은 배송 주소를 수집하지 않습니다."
                : "구매평 캠페인은 직접 구매하므로 배송 주소를 받지 않습니다.");
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

  /** 질문 답변을 검사합니다. 같은 질문 중복 답변, 없는 질문, 필수 답변 누락, 글자 수 초과, 선택지에 없는 답변을 거절합니다. */
  private void validateAnswers(
      List<CampaignApplicationDtos.QuestionDefinition> questions,
      List<CampaignApplicationDtos.Answer> submitted) {
    Map<String, String> answers = new HashMap<>();
    for (CampaignApplicationDtos.Answer answer : submitted) {
      if (answers.put(answer.questionId(), answer.value()) != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 질문에 답변을 두 번 보낼 수 없습니다.");
      }
    }
    Set<String> knownQuestionIds =
        questions.stream()
            .map(CampaignApplicationDtos.QuestionDefinition::id)
            .collect(Collectors.toSet());
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
  }

  /** 필수 동의 문서마다 최신 버전에 동의했는지 확인합니다. 같은 문서를 두 번 보내면 거절합니다. */
  private void validateAgreements(
      List<CampaignApplicationDtos.AgreementDefinition> agreements,
      List<CampaignApplicationDtos.AgreementAcceptance> acceptances) {
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

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

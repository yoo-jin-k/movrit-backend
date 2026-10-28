package com.movrit.movrit.campaignapplication.dto;

import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaigncontent.dto.CampaignContentDtos;
import com.movrit.movrit.campaigneligibility.dto.CampaignEligibilityDtos;
import com.movrit.movrit.collaboration.dto.CampaignManagementDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** 캠페인 신청의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class CampaignApplicationDtos {
  private CampaignApplicationDtos() {}

  /** 신청서 질문의 입력 방식을 구분합니다. 단문·장문·단일 선택 형태를 지원합니다. */
  public enum QuestionType {
    TEXT,
    TEXTAREA,
    SINGLE_CHOICE
  }

  /**
   * 캠페인 신청의 신청서 질문 정의를 전달합니다.
   *
   * @param id 신청서 내 항목 식별자이며 답변 및 동의 항목 연결에 사용합니다.
   * @param label 질문 표시 문구입니다. 빈 문자열을 허용하지 않습니다.
   * @param required 필수 입력 또는 필수 동의 여부입니다.
   * @param type 데이터 또는 질문 유형입니다. null을 허용하지 않습니다.
   * @param options 단일 선택 질문의 선택지 목록입니다. 빈 문자열을 허용하지 않습니다.
   * @param maxLength 답변의 최대 길이입니다. 입력하는 경우 양수여야 합니다.
   */
  public record QuestionDefinition(
      @NotBlank String id,
      @NotBlank String label,
      boolean required,
      @NotNull QuestionType type,
      @NotNull List<@NotBlank String> options,
      @Positive Integer maxLength) {
    /** 누락된 유형과 선택지를 기본값으로 정규화하고 선택지 목록을 외부에서 변경하지 못하도록 복사합니다. */
    public QuestionDefinition {
      type = type == null ? QuestionType.TEXT : type;
      options = options == null ? List.of() : List.copyOf(options);
    }
  }

  /**
   * 캠페인 신청의 동의 항목 정의를 전달합니다.
   *
   * @param id 신청서 내 항목 식별자이며 답변 및 동의 항목 연결에 사용합니다.
   * @param title 제목입니다. 빈 문자열을 허용하지 않습니다.
   * @param version 동의 문서 버전이며 현재 정의와 제출 버전의 일치 여부 확인에 사용합니다.
   * @param required 필수 입력 또는 필수 동의 여부입니다.
   * @param body 동의 항목 본문입니다. 빈 문자열을 허용하지 않습니다. 크기 제약은 max = 20000입니다.
   */
  public record AgreementDefinition(
      @NotBlank String id,
      @NotBlank String title,
      @NotBlank String version,
      boolean required,
      @NotBlank @Size(max = 20000) String body) {}

  /**
   * 캠페인 신청의 신청서 구성 저장 요청을 전달합니다.
   *
   * @param allowedFulfillmentTypes 업체가 허용한 참여 방식 목록입니다. null을 허용하지 않습니다. 빈 목록을 허용하지 않습니다.
   * @param questions 신청서 질문 정의 목록입니다. null을 허용하지 않습니다.
   * @param agreements 신청 동의 항목 정의 목록입니다. null을 허용하지 않습니다.
   */
  public record FormSaveRequest(
      @NotEmpty List<@NotNull FulfillmentType> allowedFulfillmentTypes,
      @NotNull List<@Valid QuestionDefinition> questions,
      @NotNull List<@Valid AgreementDefinition> agreements) {}

  /**
   * 캠페인 신청의 신청서 구성 응답을 전달합니다.
   *
   * @param campaignId 캠페인 식별자입니다.
   * @param version 조회한 데이터 버전이며 수정 요청의 동시성 검사에 사용됩니다.
   * @param allowedFulfillmentTypes 업체가 허용한 참여 방식 목록입니다.
   * @param questions 신청서 질문 정의 목록입니다.
   * @param agreements 신청 동의 항목 정의 목록입니다.
   */
  public record FormResponse(
      Long campaignId,
      Long version,
      List<FulfillmentType> allowedFulfillmentTypes,
      List<QuestionDefinition> questions,
      List<AgreementDefinition> agreements) {}

  /**
   * 캠페인 신청의 배송 수령 정보를 전달합니다.
   *
   * @param recipientName 수령인 이름입니다.
   * @param recipientPhone 수령인 연락처입니다.
   * @param postalCode 우편번호입니다.
   * @param address1 기본 주소입니다.
   * @param address2 상세 주소입니다.
   */
  public record ShippingContact(
      String recipientName,
      String recipientPhone,
      String postalCode,
      String address1,
      String address2) {}

  /**
   * 캠페인 신청의 질문 답변을 전달합니다.
   *
   * @param questionId 답변 대상 질문 식별자입니다. 빈 문자열을 허용하지 않습니다.
   * @param value 질문에 대한 답변 값입니다.
   */
  public record Answer(@NotBlank String questionId, String value) {}

  /**
   * 캠페인 신청의 동의 항목 제출값을 전달합니다.
   *
   * @param agreementId 동의 대상 항목 식별자입니다. 빈 문자열을 허용하지 않습니다.
   * @param version 동의 문서 버전이며 현재 정의와 제출 버전의 일치 여부 확인에 사용합니다.
   * @param accepted 동의 여부입니다.
   */
  public record AgreementAcceptance(
      @NotBlank String agreementId, @NotBlank String version, boolean accepted) {}

  /**
   * 캠페인 신청의 생성 요청을 전달합니다.
   *
   * @param message 신청 또는 안내 메시지입니다. 빈 문자열을 허용하지 않습니다.
   * @param expectedFormVersion 신청자가 확인한 신청서 버전이며 오래된 신청서 제출을 방지합니다. null을 허용하지 않습니다.
   * @param expectedEligibilityRequirementId 신청자가 확인한 참여 조건 식별자이며 조건 추가·삭제 여부 확인에 사용합니다.
   * @param expectedEligibilityRequirementVersion 신청자가 확인한 참여 조건 버전입니다.
   * @param fulfillmentType 참여자가 선택한 배송 또는 방문 방식입니다. null을 허용하지 않습니다.
   * @param shippingContact 배송 참여자의 수령 정보이며 개인정보 노출을 제한해야 합니다.
   * @param answers 질문별 제출 답변 목록입니다. null을 허용하지 않습니다.
   * @param agreementAcceptances 동의 항목별 버전과 동의 결과입니다. null을 허용하지 않습니다.
   */
  public record CreateRequest(
      @NotBlank String message,
      @NotNull Long expectedFormVersion,
      Long expectedEligibilityRequirementId,
      Long expectedEligibilityRequirementVersion,
      @NotNull FulfillmentType fulfillmentType,
      @Valid ShippingContact shippingContact,
      @NotNull List<@Valid Answer> answers,
      @NotNull List<@Valid AgreementAcceptance> agreementAcceptances) {}

  /**
   * 캠페인 신청의 취소 요청을 전달합니다.
   *
   * @param reason 처리 또는 판정 사유입니다. 빈 문자열을 허용하지 않습니다.
   */
  public record CancelRequest(@NotBlank String reason) {}

  /**
   * 캠페인 신청의 신청 가능 여부 응답을 전달합니다.
   *
   * @param campaignId 캠페인 식별자입니다.
   * @param formVersion 신청서 버전입니다.
   * @param applicationEligibilityPolicy 서버가 적용한 캠페인 신청 자격 정책 코드입니다.
   * @param emailVerified 이메일 소유 확인 여부이며 휴대폰 본인인증과 구분합니다.
   * @param identityVerified 외부 본인인증 여부이며 이메일 인증과 구분합니다.
   * @param channelReady 채널 준비 상태입니다.
   * @param channelProfileExists 채널 프로필 존재 여부입니다.
   * @param campaignRequirement 해당 캠페인에 설정된 참여 조건입니다.
   * @param metricEvaluation 사용자 지표와 캠페인 조건을 비교한 결과입니다.
   * @param alreadyApplied 기존 신청 존재 여부입니다.
   * @param canApply 현재 서버 기준의 신청 가능 여부이며 실제 신청 시 다시 검증합니다.
   * @param blockingReasons 신청을 막는 사유 코드 목록입니다.
   */
  public record EligibilityResponse(
      Long campaignId,
      Long formVersion,
      String applicationEligibilityPolicy,
      boolean emailVerified,
      boolean identityVerified,
      boolean channelReady,
      boolean channelProfileExists,
      CampaignEligibilityDtos.RequirementResponse campaignRequirement,
      CampaignEligibilityDtos.MetricEvaluation metricEvaluation,
      boolean alreadyApplied,
      boolean canApply,
      List<String> blockingReasons) {}

  /**
   * 캠페인 신청의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param campaignId 캠페인 식별자입니다.
   * @param preorderId 연결된 프리오더 식별자입니다.
   * @param creatorId 기존 채널 프로필 식별자이며, 회원 ID와 구분합니다.
   * @param creatorNickname 참여자의 채널 프로필 표시 이름입니다.
   * @param fulfillmentType 참여자가 선택한 배송 또는 방문 방식입니다.
   * @param status 현재 업무 상태입니다.
   * @param message 신청 또는 안내 메시지입니다.
   * @param shippingContact 배송 참여자의 수령 정보이며 개인정보 노출을 제한해야 합니다.
   * @param answers 질문별 제출 답변 목록입니다.
   * @param agreementAcceptances 동의 항목별 버전과 동의 결과입니다.
   * @param formVersion 신청서 버전입니다.
   * @param eligibilityRequirementSnapshot 신청 당시 참여 조건 사본이며 이후 조건 변경과 구분합니다.
   * @param eligibilityRequirementVersion 신청 당시 참여 조건 버전입니다.
   * @param identityVerificationConfigured 외부 본인인증 설정 여부입니다.
   * @param identityVerified 외부 본인인증 여부이며 이메일 인증과 구분합니다.
   * @param emailVerified 이메일 소유 확인 여부이며 휴대폰 본인인증과 구분합니다.
   * @param channelReady 채널 준비 상태입니다.
   * @param readinessNotices 인증 및 채널 준비 관련 안내 목록입니다.
   * @param submittedAt 제출 시각입니다.
   * @param respondedAt 신청 응답 시각입니다.
   * @param cancelledAt 취소 시각입니다.
   * @param cancellationReason 취소 사유입니다.
   * @param experienceConfirmedAt 체험 확인 시각입니다.
   */
  public record Response(
      Long id,
      Long campaignId,
      Long preorderId,
      Long creatorId,
      String creatorNickname,
      FulfillmentType fulfillmentType,
      CampaignApplicationStatus status,
      String message,
      ShippingContact shippingContact,
      List<Answer> answers,
      List<AgreementAcceptance> agreementAcceptances,
      Long formVersion,
      CampaignEligibilityDtos.RequirementResponse eligibilityRequirementSnapshot,
      Long eligibilityRequirementVersion,
      boolean identityVerificationConfigured,
      boolean identityVerified,
      boolean emailVerified,
      boolean channelReady,
      List<String> readinessNotices,
      LocalDateTime submittedAt,
      LocalDateTime respondedAt,
      LocalDateTime cancelledAt,
      String cancellationReason,
      LocalDateTime experienceConfirmedAt) {}

  /** 마이페이지에서 신청·일정·안내·연락처·콘텐츠 검수 상태를 함께 표시하는 읽기 전용 응답입니다. */
  public record WorkspaceResponse(
      Response application,
      CampaignManagementDtos.DetailResponse campaign,
      CampaignManagementDtos.ContactResponse brandContact,
      CampaignContentDtos.Response content) {}
}

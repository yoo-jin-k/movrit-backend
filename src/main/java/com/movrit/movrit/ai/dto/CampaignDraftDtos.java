package com.movrit.movrit.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** AI 캠페인 초안 요청·응답 형식입니다. */
public final class CampaignDraftDtos {
  private CampaignDraftDtos() {}

  /**
   * 초안을 만들 때 참고할 캠페인 정보입니다. 업체가 작성 화면에 이미 넣은 값만 보냅니다.
   *
   * @param title 캠페인 제목(제품·매장 이름이 들어간 제목). 필수입니다.
   * @param campaignType 캠페인 종류(PRODUCT·LOCAL·REPORTER·PURCHASE_REVIEW)
   * @param category 카테고리(예: 뷰티, 맛집)
   * @param contentChannel 콘텐츠 채널(BLOG·INSTAGRAM·YOUTUBE·CLIP·OTHER)
   * @param rewardDescription 참여자에게 제공하는 내역
   * @param highlights 제품·매장의 특징이나 꼭 넣고 싶은 내용(업체가 자유롭게 적음)
   */
  public record DraftRequest(
      @NotBlank(message = "캠페인 제목을 먼저 입력해 주세요.") @Size(max = 100) String title,
      @Size(max = 30) String campaignType,
      @Size(max = 40) String category,
      @Size(max = 20) String contentChannel,
      @Size(max = 500) String rewardDescription,
      @Size(max = 1000, message = "특징은 1,000자 이내로 적어 주세요.") String highlights) {}

  /**
   * AI가 만든 초안입니다. 저장하지 않으며, 업체가 화면에서 읽고 고친 뒤 캠페인과 함께 저장합니다.
   *
   * @param description 캠페인 소개
   * @param mission 참여 미션
   * @param submissionRequirement 콘텐츠 제출 요건
   * @param caution 주의사항
   * @param live 실제 AI로 만들었으면 true, 키가 없어 견본을 돌려줬으면 false
   * @param model 답한 모델 이름
   * @param remainingToday 오늘 더 만들 수 있는 초안 수
   */
  public record DraftResponse(
      String description,
      String mission,
      String submissionRequirement,
      String caution,
      boolean live,
      String model,
      int remainingToday) {}
}

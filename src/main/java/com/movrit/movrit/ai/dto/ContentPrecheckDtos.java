package com.movrit.movrit.ai.dto;

import com.movrit.movrit.campaigncontent.entity.ContentChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 제출 전 콘텐츠 점검 요청·응답 형식입니다. */
public final class ContentPrecheckDtos {
  private ContentPrecheckDtos() {}

  /** 본문 최대 길이입니다. 블로그 긴 글도 대부분 들어가고, 한 번의 비용이 커지지 않게 막습니다. */
  public static final int CONTENT_MAX = 8000;

  /**
   * 참여자가 올리기 전에 점검할 콘텐츠입니다. 점검 기준(미션·제출 요건·주의사항)은 화면에서 받지 않고 서버가 캠페인에서 읽습니다.
   *
   * @param channelType 올릴 채널(BLOG·INSTAGRAM·YOUTUBE·CLIP·OTHER). 비어 있으면 캠페인 채널을 씁니다.
   * @param content 게시할 글 본문이나 영상 설명·자막. 필수이며 8,000자까지 받습니다. 저장하지 않습니다.
   */
  public record PrecheckRequest(
      ContentChannel channelType,
      @NotBlank(message = "점검할 본문을 붙여 넣어 주세요.")
          @Size(max = CONTENT_MAX, message = "본문은 8,000자 이내로 붙여 넣어 주세요.")
          String content) {}

  /** 기준 한 항목의 판단입니다. */
  public enum CheckStatus {
    /** 본문에서 기준을 지킨 것을 확인했습니다. */
    MET,
    /** 본문에서 기준에 해당하는 내용을 찾지 못했습니다. */
    MISSING,
    /** 본문만으로는 판단하기 어렵습니다(사진 수 등). */
    UNCLEAR
  }

  /**
   * 점검 기준 한 항목의 결과입니다.
   *
   * @param criterion 캠페인 안내에서 가져온 기준(예: "사진 3장 이상")
   * @param status 판단 결과
   * @param comment 참여자에게 주는 짧은 안내(무엇을 더 넣으면 되는지)
   */
  public record CheckItem(String criterion, CheckStatus status, String comment) {}

  /**
   * 점검 결과입니다. 저장하지 않으며, 참여자가 참고해 본문을 고친 뒤 직접 제출합니다.
   *
   * @param summary 전체 한 줄 요약
   * @param items 기준별 결과(최대 8개)
   * @param disclosureFound 서버 규칙으로 본문에서 협찬 표시 문구(협찬·제공받아·광고 등)를 찾았으면 true
   * @param live 실제 AI로 점검했으면 true, 키가 없어 견본을 돌려줬으면 false
   * @param model 점검한 모델 이름
   * @param remainingToday 오늘 더 점검할 수 있는 횟수
   */
  public record PrecheckResponse(
      String summary,
      List<CheckItem> items,
      boolean disclosureFound,
      boolean live,
      String model,
      int remainingToday) {}
}

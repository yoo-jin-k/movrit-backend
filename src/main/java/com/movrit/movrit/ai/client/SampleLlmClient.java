package com.movrit.movrit.ai.client;

/**
 * API 키가 없을 때 쓰는 견본 응답입니다. 외부로 아무것도 보내지 않고, 정해진 형식의 초안 JSON을 돌려줍니다.
 *
 * <p>키 없이도 로컬 개발·시연·테스트가 돌아가게 하려는 용도입니다. 화면에는 '견본 초안'으로 표시되고, 사용량 기록·하루 횟수에는 넣지 않습니다.
 */
public class SampleLlmClient implements LlmClient {

  /** 입력과 상관없이 같은 견본 초안을 돌려줍니다. 토큰은 0이고 잘리지 않습니다. */
  @Override
  public LlmResult complete(String systemPrompt, String userPrompt, int maxTokens) {
    String text =
        """
        {
          "description": "견본 초안입니다. 제품의 특징과 체험 포인트를 참여자가 이해하기 쉽게 소개해 주세요.",
          "mission": "1. 제품을 받은 뒤 일주일 이상 사용해 주세요.\\n2. 사용 전후 느낌을 사진과 함께 기록해 주세요.\\n3. 지정한 채널에 솔직한 후기를 올려 주세요.",
          "submissionRequirement": "사진 3장 이상, 본문 500자 이상, 업체가 정한 필수 키워드를 포함해 주세요.",
          "caution": "협찬받은 콘텐츠임을 본문 첫 부분에 표시해 주세요. 사실과 다른 효능을 적지 않습니다."
        }
        """;
    return new LlmResult(text, "sample", 0, 0, false);
  }

  /** 실제 AI를 부르지 않으므로 false입니다. */
  @Override
  public boolean live() {
    return false;
  }
}

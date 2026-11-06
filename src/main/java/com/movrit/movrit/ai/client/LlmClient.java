package com.movrit.movrit.ai.client;

/**
 * 대규모 언어 모델(LLM)에 글을 요청하는 약속입니다. 기능 코드(예: 캠페인 초안)는 이 약속만 알고, 어느 회사 모델인지는 모릅니다.
 *
 * <p>회사를 바꿀 때는 이 약속을 지키는 연결 클래스(예: {@link ClaudeLlmClient})만 새로 만들고 설정에서 고릅니다. 기능 코드·화면·테스트는 그대로
 * 둡니다.
 */
public interface LlmClient {

  /**
   * 지시문과 요청을 보내고 모델이 쓴 글을 받습니다.
   *
   * @param systemPrompt 모델의 역할과 지켜야 할 규칙
   * @param userPrompt 이번 요청 내용(사용자 입력이 들어갑니다)
   * @param maxTokens 답의 최대 길이(토큰)
   * @throws LlmException 시간 초과·인증 실패·응답 형식 오류 등으로 글을 받지 못했을 때
   */
  LlmResult complete(String systemPrompt, String userPrompt, int maxTokens);

  /** 지금 실제 AI를 부르는지 알려줍니다. 견본으로 동작하면 false입니다. */
  boolean live();

  /**
   * 모델이 쓴 글과 사용량입니다.
   *
   * @param text 모델이 쓴 글
   * @param model 답한 모델 이름(견본이면 "sample")
   * @param inputTokens 보낸 글의 토큰 수(비용 기록용)
   * @param outputTokens 받은 글의 토큰 수(비용 기록용)
   */
  record LlmResult(String text, String model, int inputTokens, int outputTokens) {}

  /** 모델에서 글을 받지 못했을 때 쓰는 예외입니다. 메시지에는 키나 요청 원문을 넣지 않습니다. */
  class LlmException extends RuntimeException {
    public LlmException(String message) {
      super(message);
    }
  }
}

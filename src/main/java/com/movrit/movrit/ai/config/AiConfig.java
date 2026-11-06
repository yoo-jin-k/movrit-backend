package com.movrit.movrit.ai.config;

import com.movrit.movrit.ai.client.ClaudeLlmClient;
import com.movrit.movrit.ai.client.LlmClient;
import com.movrit.movrit.ai.client.SampleLlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 어떤 LLM 연결을 쓸지 정합니다. 키(movrit.ai.anthropic-api-key ← 환경 변수 ANTHROPIC_API_KEY)가 있으면 Claude, 없으면 견본
 * 응답을 씁니다.
 *
 * <p>다른 회사 모델로 바꿀 때는 {@link LlmClient}를 구현한 클래스를 하나 추가하고 여기서 고르면 됩니다.
 */
@Slf4j
@Configuration
public class AiConfig {

  @Bean
  public LlmClient llmClient(
      @Value("${movrit.ai.anthropic-api-key:}") String apiKey,
      @Value("${movrit.ai.model:claude-haiku-5-5}") String model) {
    if (apiKey == null || apiKey.isBlank()) {
      log.info("AI 초안: API 키가 없어 견본 응답으로 동작합니다.");
      return new SampleLlmClient();
    }
    log.info("AI 초안: Claude({})를 사용합니다.", model);
    return new ClaudeLlmClient(apiKey.trim(), model);
  }
}

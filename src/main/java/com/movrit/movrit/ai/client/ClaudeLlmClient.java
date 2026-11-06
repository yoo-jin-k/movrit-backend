package com.movrit.movrit.ai.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/**
 * Anthropic Claude의 Messages API(POST https://api.anthropic.com/v1/messages)로 글을 요청합니다.
 *
 * <ul>
 *   <li>키는 환경 변수로 받은 값을 요청 헤더(x-api-key)에만 씁니다. 로그·예외 메시지에는 남기지 않습니다.
 *   <li>연결 5초·응답 40초를 넘기면 실패로 봅니다. 화면은 실패 안내를 보여주고 다른 기능은 그대로 동작합니다.
 *   <li>응답의 content 중 text 조각을 이어 붙여 돌려주고, usage의 토큰 수를 함께 돌려줍니다.
 * </ul>
 */
@Slf4j
public class ClaudeLlmClient implements LlmClient {
  static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
  static final String API_VERSION = "2023-06-01";

  private final RestClient http;
  private final String apiKey;
  private final String model;

  public ClaudeLlmClient(String apiKey, String model) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(40));
    this.http = RestClient.builder().requestFactory(factory).build();
    this.apiKey = apiKey; // secret-check: allow (환경 변수에서 받은 값을 넘겨받습니다)
    this.model = model;
  }

  @Override
  public LlmResult complete(String systemPrompt, String userPrompt, int maxTokens) {
    Map<String, Object> body =
        Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", systemPrompt,
            "messages", List.of(Map.of("role", "user", "content", userPrompt)));
    try {
      JsonNode response =
          http.post()
              .uri(ENDPOINT)
              .header("x-api-key", apiKey)
              .header("anthropic-version", API_VERSION)
              .header("content-type", "application/json")
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      return parse(response);
    } catch (RestClientResponseException exception) {
      // 401(키 오류)·429(요청 많음)·529(과부하) 등입니다. 상태 코드만 남깁니다.
      log.warn("Claude 요청 실패: status={}", exception.getStatusCode().value());
      throw new LlmException("AI 응답을 받지 못했습니다. (상태 " + exception.getStatusCode().value() + ")");
    } catch (RestClientException exception) {
      log.warn("Claude 요청 실패: error={}", exception.getClass().getSimpleName());
      throw new LlmException("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }
  }

  @Override
  public boolean live() {
    return true;
  }

  /** 응답에서 글과 토큰 수를 꺼냅니다. 글 조각이 하나도 없으면 실패로 봅니다. */
  static LlmResult parse(JsonNode response) {
    if (response == null || !response.path("content").isArray()) {
      throw new LlmException("AI 응답 형식을 확인할 수 없습니다.");
    }
    StringBuilder text = new StringBuilder();
    for (JsonNode block : response.path("content")) {
      if ("text".equals(block.path("type").asString(""))) {
        text.append(block.path("text").asString(""));
      }
    }
    if (text.isEmpty()) {
      throw new LlmException("AI가 빈 답을 보냈습니다. 다시 시도해 주세요.");
    }
    JsonNode usage = response.path("usage");
    return new LlmResult(
        text.toString(),
        response.path("model").asString(""),
        usage.path("input_tokens").asInt(0),
        usage.path("output_tokens").asInt(0));
  }
}

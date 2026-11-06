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
 *   <li>응답의 content 중 text 조각을 이어 붙여 돌려주고, usage의 토큰 수와 잘림 여부(stop_reason=max_tokens)를 함께 돌려줍니다.
 * </ul>
 *
 * <p>변경 이력(코드 리뷰 반영):
 *
 * <ul>
 *   <li>실패 안내: 고치기 전에는 "AI 응답을 받지 못했습니다. (상태 401)"처럼 외부 상태 코드를 업체 화면에 그대로 보였고, 키 오류도 warn 로그라 놓치기
 *       쉬웠습니다. 고친 후에는 401·403은 설정 문제(CONFIGURATION)로 error 로그를 남기고 업체에는 "지금은 쓸 수 없습니다"만, 429·5xx는
 *       BUSY로 "잠시 후 다시"를 안내합니다. 상태 코드는 로그에만 남깁니다.
 *   <li>잘림 확인: 고치기 전에는 stop_reason을 보지 않아, 답이 잘리면 JSON 형식 오류로만 보였습니다. 고친 후에는 truncated로 알려 줍니다.
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

  /** Claude에 요청을 보내고 결과를 돌려줍니다. 실패하면 종류를 나눈 {@link LlmException}을 던집니다. */
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
      throw failure(exception.getStatusCode().value());
    } catch (RestClientException exception) {
      log.warn("Claude 요청 실패: error={}", exception.getClass().getSimpleName());
      throw new LlmException(Failure.UNAVAILABLE, "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }
  }

  /** 실제 Claude를 부르므로 항상 true입니다. */
  @Override
  public boolean live() {
    return true;
  }

  /**
   * 외부 상태 코드를 실패 종류로 바꿉니다. 상태 코드는 로그에만 남기고 사용자 안내에는 넣지 않습니다.
   *
   * <ul>
   *   <li>401·403: 키가 틀리거나 만료됨 → CONFIGURATION, error 로그(운영자가 키를 확인해야 함)
   *   <li>429·5xx(529 과부하 포함): 요청 많음·서버 바쁨 → BUSY
   *   <li>그 밖의 4xx: 요청 형식 문제 → UNAVAILABLE
   * </ul>
   */
  static LlmException failure(int status) {
    if (status == 401 || status == 403) {
      log.error("Claude 키 확인 필요: status={} (ANTHROPIC_API_KEY를 확인해 주세요)", status);
      return new LlmException(Failure.CONFIGURATION, "지금은 AI 초안을 쓸 수 없습니다. 운영자에게 문의해 주세요.");
    }
    if (status == 429 || status >= 500) {
      log.warn("Claude 요청 많음·과부하: status={}", status);
      return new LlmException(Failure.BUSY, "AI 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
    }
    log.warn("Claude 요청 거절: status={}", status);
    return new LlmException(Failure.UNAVAILABLE, "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.");
  }

  /** 응답에서 글, 토큰 수, 잘림 여부를 꺼냅니다. 글 조각이 하나도 없으면 실패로 봅니다. */
  static LlmResult parse(JsonNode response) {
    if (response == null || !response.path("content").isArray()) {
      throw new LlmException(Failure.UNAVAILABLE, "AI 응답 형식을 확인할 수 없습니다.");
    }
    StringBuilder text = new StringBuilder();
    for (JsonNode block : response.path("content")) {
      if ("text".equals(block.path("type").asString(""))) {
        text.append(block.path("text").asString(""));
      }
    }
    if (text.isEmpty()) {
      throw new LlmException(Failure.UNAVAILABLE, "AI가 빈 답을 보냈습니다. 다시 시도해 주세요.");
    }
    JsonNode usage = response.path("usage");
    return new LlmResult(
        text.toString(),
        response.path("model").asString(""),
        usage.path("input_tokens").asInt(0),
        usage.path("output_tokens").asInt(0),
        "max_tokens".equals(response.path("stop_reason").asString("")));
  }
}

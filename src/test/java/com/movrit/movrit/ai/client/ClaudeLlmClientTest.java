package com.movrit.movrit.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Claude 응답에서 글·토큰 수·잘림 여부를 꺼내는 규칙과 실패 종류 구분을 네트워크 없이 확인합니다. */
class ClaudeLlmClientTest {
  private final JsonMapper json = JsonMapper.builder().build();

  @Test
  void textBlocksAndUsageAreRead() {
    var result =
        ClaudeLlmClient.parse(
            json.readTree(
                """
                {"model":"claude-haiku-5-5","stop_reason":"end_turn",
                 "content":[{"type":"text","text":"{\\"a\\":"},{"type":"text","text":"1}"}],
                 "usage":{"input_tokens":23,"output_tokens":31}}
                """));

    assertThat(result.text()).isEqualTo("{\"a\":1}");
    assertThat(result.model()).isEqualTo("claude-haiku-5-5");
    assertThat(result.inputTokens()).isEqualTo(23);
    assertThat(result.outputTokens()).isEqualTo(31);
    assertThat(result.truncated()).isFalse();
  }

  @Test
  void maxTokensStopIsMarkedTruncated() {
    var result =
        ClaudeLlmClient.parse(
            json.readTree(
                """
                {"model":"m","stop_reason":"max_tokens","content":[{"type":"text","text":"{\\"a\\""}],"usage":{}}
                """));

    assertThat(result.truncated()).isTrue();
  }

  @Test
  void emptyOrMalformedResponseFails() {
    assertThatThrownBy(() -> ClaudeLlmClient.parse(json.readTree("{\"content\":[]}")))
        .isInstanceOf(LlmClient.LlmException.class);
    assertThatThrownBy(() -> ClaudeLlmClient.parse(json.readTree("{\"error\":{}}")))
        .isInstanceOf(LlmClient.LlmException.class);
  }

  @Test
  void statusCodesBecomeFailureKindsWithoutLeakingCodes() {
    assertThat(ClaudeLlmClient.failure(401).failure()).isEqualTo(LlmClient.Failure.CONFIGURATION);
    assertThat(ClaudeLlmClient.failure(403).failure()).isEqualTo(LlmClient.Failure.CONFIGURATION);
    assertThat(ClaudeLlmClient.failure(429).failure()).isEqualTo(LlmClient.Failure.BUSY);
    assertThat(ClaudeLlmClient.failure(529).failure()).isEqualTo(LlmClient.Failure.BUSY);
    assertThat(ClaudeLlmClient.failure(400).failure()).isEqualTo(LlmClient.Failure.UNAVAILABLE);
    for (int status : new int[] {400, 401, 429, 529}) {
      assertThat(ClaudeLlmClient.failure(status).getMessage())
          .doesNotContain(String.valueOf(status));
    }
  }
}

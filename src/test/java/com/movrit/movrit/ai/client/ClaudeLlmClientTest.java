package com.movrit.movrit.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Claude 응답에서 글과 토큰 수를 꺼내는 규칙을 네트워크 없이 확인합니다. */
class ClaudeLlmClientTest {
  private final JsonMapper json = JsonMapper.builder().build();

  @Test
  void textBlocksAndUsageAreRead() {
    var result =
        ClaudeLlmClient.parse(
            json.readTree(
                """
                {"model":"claude-haiku-5-5","content":[{"type":"text","text":"{\\"a\\":"},{"type":"text","text":"1}"}],
                 "usage":{"input_tokens":23,"output_tokens":31}}
                """));

    assertThat(result.text()).isEqualTo("{\"a\":1}");
    assertThat(result.model()).isEqualTo("claude-haiku-5-5");
    assertThat(result.inputTokens()).isEqualTo(23);
    assertThat(result.outputTokens()).isEqualTo(31);
  }

  @Test
  void emptyOrMalformedResponseFails() {
    assertThatThrownBy(() -> ClaudeLlmClient.parse(json.readTree("{\"content\":[]}")))
        .isInstanceOf(LlmClient.LlmException.class);
    assertThatThrownBy(() -> ClaudeLlmClient.parse(json.readTree("{\"error\":{}}")))
        .isInstanceOf(LlmClient.LlmException.class);
  }
}

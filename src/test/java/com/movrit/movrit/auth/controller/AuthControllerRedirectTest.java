package com.movrit.movrit.auth.controller;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthControllerRedirectTest {

  @Test
  void localQaFrontendRedirectIsAllowed() {
    AuthController controller = controller("http://localhost:5174");

    assertThatCode(controller::validateFrontendBaseUrl).doesNotThrowAnyException();
  }

  @Test
  void arbitraryFrontendRedirectIsRejected() {
    AuthController controller = controller("https://example.invalid");

    assertThatThrownBy(controller::validateFrontendBaseUrl)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("허용되지 않은 프론트 OAuth 리디렉션 주소입니다.");
  }

  private AuthController controller(String frontendBaseUrl) {
    AuthController controller = new AuthController(null, null, null, null, null, null, null);
    ReflectionTestUtils.setField(controller, "frontendBaseUrl", frontendBaseUrl);
    // 설정 기본값(application.properties의 movrit.security.allowed-origins)과 같은 허용 목록입니다.
    ReflectionTestUtils.setField(
        controller,
        "allowedFrontendOrigins",
        java.util.List.of("http://localhost:5173", "http://localhost:5174"));
    return controller;
  }
}

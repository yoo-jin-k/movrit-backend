package com.movrit.movrit.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.movrit.movrit.auth.service.GoogleAuthService;
import com.movrit.movrit.auth.service.KakaoAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

/** 소셜 토큰 교환이 외부 오류로 실패하면 500 화면 대신 로그인 화면으로 돌려보내는지 확인합니다. */
class AuthControllerSocialFailureTest {

  private final GoogleAuthService google = mock(GoogleAuthService.class);
  private final KakaoAuthService kakao = mock(KakaoAuthService.class);
  private final AuthController controller =
      new AuthController(google, null, kakao, null, null, null, null);

  {
    ReflectionTestUtils.setField(controller, "frontendBaseUrl", "http://localhost:5174");
  }

  @Test
  void kakaoTokenRejectionRedirectsToLoginWithProvider() throws Exception {
    // Client Secret 누락처럼 Kakao가 401을 돌려주는 상황입니다.
    when(kakao.getAccessToken("code-1"))
        .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "", null, null, null));
    MockHttpServletResponse response = new MockHttpServletResponse();

    controller.kakaoCallback("code-1", null, response);

    assertThat(response.getRedirectedUrl())
        .isEqualTo("http://localhost:5174/login?socialError=KAKAO");
  }

  @Test
  void googleTokenRejectionRedirectsToLoginWithProvider() throws Exception {
    when(google.getAccessToken("code-2"))
        .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "", null, null, null));
    MockHttpServletResponse response = new MockHttpServletResponse();

    controller.googleCallback("code-2", null, response);

    assertThat(response.getRedirectedUrl())
        .isEqualTo("http://localhost:5174/login?socialError=GOOGLE");
  }
}

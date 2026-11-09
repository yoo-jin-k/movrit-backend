package com.movrit.movrit.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.movrit.movrit.ai.dto.ContentPrecheckDtos;
import com.movrit.movrit.ai.service.ContentPrecheckService;
import com.movrit.movrit.common.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 제출 전 점검 API의 로그인 확인과 입력 검사를 확인합니다. 권한·횟수·AI 호출은 서비스 테스트가 맡습니다. */
@WebMvcTest(ContentPrecheckController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=movrit-security-test-secret-key-123456789")
class ContentPrecheckControllerTest {
  private static final String URL = "/api/campaign-contents/ai/applications/11/precheck";
  private static final String BODY = "{\"channelType\":\"BLOG\",\"content\":\"협찬받아 쓴 후기입니다.\"}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ContentPrecheckService service;

  @Test
  void loginIsRequired() throws Exception {
    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isUnauthorized());
    verify(service, never()).precheck(any(), any(), any());
  }

  @Test
  void blankOrTooLongContentIsRejectedBeforeCallingService() throws Exception {
    mockMvc
        .perform(
            post(URL)
                .with(jwt().jwt(token -> token.subject("42")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" \"}"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post(URL)
                .with(jwt().jwt(token -> token.subject("42")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"content\":\"" + "가".repeat(ContentPrecheckDtos.CONTENT_MAX + 1) + "\"}"))
        .andExpect(status().isBadRequest());
    verify(service, never()).precheck(any(), any(), any());
  }

  @Test
  void memberIdComesFromToken() throws Exception {
    when(service.precheck(eq(42L), eq(11L), any()))
        .thenReturn(
            new ContentPrecheckDtos.PrecheckResponse(
                "좋습니다.",
                List.of(
                    new ContentPrecheckDtos.CheckItem(
                        "협찬 표시", ContentPrecheckDtos.CheckStatus.MET, "")),
                true,
                true,
                "model-x",
                9));

    mockMvc
        .perform(
            post(URL)
                .with(jwt().jwt(token -> token.subject("42")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].status").value("MET"))
        .andExpect(jsonPath("$.disclosureFound").value(true))
        .andExpect(jsonPath("$.remainingToday").value(9));
  }
}

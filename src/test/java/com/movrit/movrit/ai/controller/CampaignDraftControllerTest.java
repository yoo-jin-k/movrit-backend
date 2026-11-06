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

import com.movrit.movrit.ai.dto.CampaignDraftDtos;
import com.movrit.movrit.ai.service.CampaignDraftService;
import com.movrit.movrit.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** AI 초안 API의 로그인 확인과 입력 검사를 확인합니다. 권한·횟수·AI 호출은 서비스 테스트가 맡습니다. */
@WebMvcTest(CampaignDraftController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=movrit-security-test-secret-key-123456789")
class CampaignDraftControllerTest {
  private static final String BODY =
      "{\"title\":\"시카 앰플 체험단\",\"campaignType\":\"PRODUCT\",\"highlights\":\"민감 피부용\"}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CampaignDraftService service;

  @Test
  void loginIsRequired() throws Exception {
    mockMvc
        .perform(
            post("/api/campaigns/ai/draft").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isUnauthorized());
    verify(service, never()).draft(any(), any());
  }

  @Test
  void emptyTitleIsRejectedBeforeCallingService() throws Exception {
    mockMvc
        .perform(
            post("/api/campaigns/ai/draft")
                .with(jwt().jwt(token -> token.subject("42")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\" \"}"))
        .andExpect(status().isBadRequest());
    verify(service, never()).draft(any(), any());
  }

  @Test
  void memberIdComesFromToken() throws Exception {
    when(service.draft(eq(42L), any()))
        .thenReturn(
            new CampaignDraftDtos.DraftResponse(
                "소개", "1. 사용", "사진 3장", "협찬 표시", true, "claude-haiku-5-5", 19));

    mockMvc
        .perform(
            post("/api/campaigns/ai/draft")
                .with(jwt().jwt(token -> token.subject("42")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("소개"))
        .andExpect(jsonPath("$.remainingToday").value(19));
  }
}

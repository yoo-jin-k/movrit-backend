package com.movrit.movrit.seller.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.movrit.movrit.common.config.SecurityConfig;
import com.movrit.movrit.seller.dto.AdminSellerDtos;
import com.movrit.movrit.seller.entity.SellerStatus;
import com.movrit.movrit.seller.service.AdminSellerQueryService;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

/** 운영자 업체 목록 API의 로그인 확인·조건 전달·잘못된 값 처리를 확인합니다. 정렬 자체는 서비스 테스트가 맡습니다. */
@WebMvcTest(AdminSellerController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=movrit-security-test-secret-key-123456789")
class AdminSellerControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SellerProfileService sellerProfileService;
  @MockitoBean private AdminSellerQueryService adminSellerQueryService;

  @Test
  void loginIsRequired() throws Exception {
    mockMvc.perform(get("/api/admin/sellers")).andExpect(status().isUnauthorized());
    verify(adminSellerQueryService, never()).findForAdmin(any(), any(), any());
  }

  @Test
  void statusAndSortArePassedWithMemberIdFromToken() throws Exception {
    when(adminSellerQueryService.findForAdmin(42L, SellerStatus.PENDING, "oldest"))
        .thenReturn(
            List.of(
                new AdminSellerDtos.ListItem(
                    5L,
                    9L,
                    "루미 스튜디오",
                    "LUMI",
                    "123-45-67890",
                    "김대표",
                    SellerStatus.PENDING,
                    LocalDateTime.of(2026, 11, 1, 10, 30, 15),
                    LocalDateTime.of(2026, 11, 1, 10, 30, 15))));

    mockMvc
        .perform(
            get("/api/admin/sellers")
                .param("status", "PENDING")
                .param("sort", "oldest")
                .with(jwt().jwt(token -> token.subject("42"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(5))
        .andExpect(jsonPath("$[0].status").value("PENDING"))
        .andExpect(jsonPath("$[0].createdAt").value("2026-11-01T10:30:15"))
        // 목록에는 정산 계좌 정보를 내려주지 않습니다.
        .andExpect(jsonPath("$[0].settlementAccountMasked").doesNotExist());
  }

  @Test
  void withoutConditionsServiceGetsNulls() throws Exception {
    when(adminSellerQueryService.findForAdmin(eq(42L), isNull(), isNull())).thenReturn(List.of());

    mockMvc
        .perform(get("/api/admin/sellers").with(jwt().jwt(token -> token.subject("42"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void unknownStatusIsBadRequestWithoutCallingService() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/sellers")
                .param("status", "UNKNOWN")
                .with(jwt().jwt(token -> token.subject("42"))))
        .andExpect(status().isBadRequest());
    verify(adminSellerQueryService, never()).findForAdmin(any(), any(), any());
  }

  @Test
  void nonAdminGetsForbidden() throws Exception {
    when(adminSellerQueryService.findForAdmin(eq(7L), any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."));

    mockMvc
        .perform(get("/api/admin/sellers").with(jwt().jwt(token -> token.subject("7"))))
        .andExpect(status().isForbidden());
  }
}

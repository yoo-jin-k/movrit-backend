package com.movrit.movrit.common.config;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.movrit.movrit.collaboration.controller.CampaignManagementController;
import com.movrit.movrit.collaboration.controller.CollaborationController;
import com.movrit.movrit.collaboration.service.CampaignManagementService;
import com.movrit.movrit.collaboration.service.CampaignSearchService;
import com.movrit.movrit.collaboration.service.CollaborationService;
import com.movrit.movrit.memberavatar.controller.MemberAvatarController;
import com.movrit.movrit.memberavatar.service.MemberAvatarService;
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

@WebMvcTest({
  CollaborationController.class,
  CampaignManagementController.class,
  MemberAvatarController.class
})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=movrit-security-test-secret-key-123456789")
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CollaborationService collaborationService;

  @MockitoBean private CampaignManagementService campaignManagementService;

  @MockitoBean private CampaignSearchService campaignSearchService;

  @MockitoBean private MemberAvatarService memberAvatarService;

  @Test
  void anonymousSellerCampaignRequestIsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/api/collaborations/campaigns/seller"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void campaignSearchIsPublic() throws Exception {
    mockMvc
        .perform(get("/api/collaborations/campaigns/search").param("q", "앰플"))
        .andExpect(status().isOk());
  }

  @Test
  void numericCampaignDetailRemainsPublic() throws Exception {
    mockMvc.perform(get("/api/collaborations/campaigns/9")).andExpect(status().isOk());
  }

  @Test
  void authenticatedSellerCampaignRequestReachesController() throws Exception {
    when(collaborationService.sellerCampaigns(42L)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/collaborations/campaigns/seller")
                .with(jwt().jwt(token -> token.subject("42"))))
        .andExpect(status().isOk());
  }

  @Test
  void qaFrontendOriginIsAllowed() throws Exception {
    mockMvc
        .perform(
            options("/api/collaborations/campaigns/10")
                .header("Origin", "http://localhost:5174")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"));
  }

  @Test
  void qaFrontendLoopbackOriginIsAllowed() throws Exception {
    mockMvc
        .perform(
            options("/api/auth/login")
                .header("Origin", "http://127.0.0.1:5174")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5174"));
  }

  @Test
  void publicAvatarContentDoesNotRequireLogin() throws Exception {
    when(memberAvatarService.findPublic(1L, 2L))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc.perform(get("/api/members/1/avatar?v=2")).andExpect(status().isNotFound());
  }

  @Test
  void privateUploadFoldersAreNotPublic() throws Exception {
    // 구매 인증·취소 증빙·채팅 첨부·채널 확인 자료·프로필 사진은 /uploads 로 열 수 없습니다.
    for (String folder :
        List.of(
            "purchase-proof",
            "cancel-evidence",
            "chat-attachments",
            "channel-evidence",
            "member-avatars")) {
      mockMvc.perform(get("/uploads/" + folder + "/a.png")).andExpect(status().isUnauthorized());
    }
  }

  @Test
  void unknownOriginIsRejected() throws Exception {
    mockMvc
        .perform(
            options("/api/collaborations/campaigns/search")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }

  @Test
  void securityHeadersAreSent() throws Exception {
    mockMvc
        .perform(get("/api/collaborations/campaigns/search"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
  }

  @Test
  void shortJwtSecretIsRejected() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> SecurityConfig.requireStrongJwtSecret("short-secret"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining("short-secret");
  }
}

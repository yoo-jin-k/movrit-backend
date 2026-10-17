package com.movrit.movrit.common.config;

import com.movrit.movrit.collaboration.controller.CampaignManagementController;
import com.movrit.movrit.collaboration.controller.CollaborationController;
import com.movrit.movrit.collaboration.service.CampaignManagementService;
import com.movrit.movrit.collaboration.service.CollaborationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CollaborationController.class, CampaignManagementController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=movrit-security-test-secret-key-123456789")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollaborationService collaborationService;

    @MockitoBean
    private CampaignManagementService campaignManagementService;

    @Test
    void anonymousSellerCampaignRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/collaborations/campaigns/seller"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void numericCampaignDetailRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/collaborations/campaigns/9"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedSellerCampaignRequestReachesController() throws Exception {
        when(collaborationService.sellerCampaigns(42L)).thenReturn(List.of());

        mockMvc.perform(get("/api/collaborations/campaigns/seller")
                        .with(jwt().jwt(token -> token.subject("42"))))
                .andExpect(status().isOk());
    }

    @Test
    void qaFrontendOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/collaborations/campaigns/10")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"));
    }
}

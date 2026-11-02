package com.movrit.movrit.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.movrit.movrit.collaboration.controller.CollaborationController;
import com.movrit.movrit.collaboration.service.CampaignSearchService;
import com.movrit.movrit.collaboration.service.CollaborationService;
import com.movrit.movrit.community.controller.CommunityAdminController;
import com.movrit.movrit.community.service.CommunityAdminService;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocJavadocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API 문서(Swagger) 확인 테스트입니다.
 *
 * <p>로그인 없이 문서를 열 수 있는지, 서비스·운영자 그룹이 분류대로 나뉘는지, 메서드 Javadoc이 설명으로 들어가는지 봅니다.
 */
@WebMvcTest({CollaborationController.class, CommunityAdminController.class})
@Import({SecurityConfig.class, OpenApiConfig.class})
@ImportAutoConfiguration({
  SpringDocConfigProperties.class,
  SpringDocConfiguration.class,
  SpringDocJavadocConfiguration.class,
  SpringDocWebMvcConfiguration.class,
  MultipleOpenApiSupportConfiguration.class
})
@TestPropertySource(properties = "jwt.secret=movrit-security-test-secret-key-123456789")
class OpenApiDocsTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CollaborationService collaborationService;

  @MockitoBean private CampaignSearchService campaignSearchService;

  @MockitoBean private CommunityAdminService communityAdminService;

  /** 서비스 그룹 문서는 로그인 없이 열리고, 공개 검색 API의 분류와 Javadoc 설명이 담깁니다. 운영자 API는 빠집니다. */
  @Test
  void serviceGroupIsPublicAndDescribed() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs/1-service"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("MOVRIT API"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(
            jsonPath("$.paths['/api/collaborations/campaigns/search'].get.tags[0]")
                .value("공통 · 캠페인 목록·상세"))
        .andExpect(
            jsonPath("$.paths['/api/collaborations/campaigns/search'].get.summary").isNotEmpty())
        .andExpect(jsonPath("$.paths['/api/admin/community/posts']").doesNotExist());
  }

  /** 운영자 그룹에는 운영자 분류의 API만 들어갑니다. */
  @Test
  void adminGroupHoldsOnlyAdminApis() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs/2-admin"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/admin/community/posts'].get.summary")
                .value("운영자가 숨김 글을 포함한 커뮤니티 글 목록을 조회합니다."))
        .andExpect(jsonPath("$.paths['/api/collaborations/campaigns/search']").doesNotExist());
  }
}

package com.movrit.movrit.common.config;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * API 문서(Swagger) 설정입니다.
 *
 * <ul>
 *   <li>화면: {@code /swagger-ui.html}, 원본 JSON: {@code /v3/api-docs/{그룹}}
 *   <li>API 설명은 각 컨트롤러 메서드의 Javadoc 주석에서 자동으로 가져옵니다.
 *   <li>분류는 컨트롤러의 {@code @Tag} 이름 앞부분(공통·참여자·업체·운영자·이전 기능)으로 묶습니다.
 *   <li>그룹은 주소로 나눕니다. {@code /api/admin/**}는 운영자 API, 이전 기능 분류는 이전 기능 API, 나머지는 서비스 API입니다.
 *   <li>로그인이 필요한 API는 화면 오른쪽 위 'Authorize'에 로그인 응답의 accessToken을 넣고 실행합니다.
 * </ul>
 */
@Configuration
public class OpenApiConfig {

  /** 문서에 표시할 JWT 인증 방식의 이름입니다. */
  private static final String BEARER = "bearerAuth";

  /** 프리오더·주문·결제 등 지금 화면에서 쓰지 않는 이전 기능 분류의 앞부분입니다. */
  private static final String LEGACY_PREFIX = "이전 기능";

  /** 운영자 전용 API 주소입니다. 운영자 화면이 쓰는 API는 모두 이 아래에 있습니다. */
  private static final String ADMIN_PATHS = "/api/admin/**";

  /** 문서 제목·사용 방법과 JWT 인증 방식을 정합니다. */
  @Bean
  public OpenAPI movritOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("MOVRIT API")
                .version("v1")
                .description(
                    """
                    브랜드가 캠페인을 등록하고 참여자가 신청·체험·콘텐츠 제출을 진행하는 MOVRIT의 백엔드 API입니다.

                    **사용 방법**
                    1. `공통 · 로그인·소셜 인증`의 `POST /api/auth/login`으로 로그인합니다.
                    2. 응답의 `accessToken` 값을 오른쪽 위 **Authorize**에 넣습니다.
                    3. 이후 요청에는 `Authorization: Bearer {토큰}` 헤더가 자동으로 붙습니다.

                    목록·상세 등 공개 API는 토큰 없이도 호출할 수 있습니다. \
                    권한(참여자·업체·운영자)과 상태 검증은 서버의 서비스 계층에서 매번 확인합니다.
                    """))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("로그인 응답의 accessToken")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER));
  }

  /** 지금 서비스에서 쓰는 전체 API(이전 기능 제외)입니다. 문서 화면에서 가장 먼저 보입니다. */
  @Bean
  public GroupedOpenApi serviceApi() {
    return GroupedOpenApi.builder()
        .group("1-service")
        .displayName("서비스 API (공통·참여자·업체)")
        .pathsToExclude(ADMIN_PATHS)
        .addOpenApiMethodFilter(tagStartsWith(LEGACY_PREFIX).negate()::test)
        .build();
  }

  /** 운영자 화면에서 쓰는 API입니다(공지·패널티 관리처럼 다른 분류에 속한 운영자 API도 함께 보입니다). */
  @Bean
  public GroupedOpenApi adminApi() {
    return GroupedOpenApi.builder()
        .group("2-admin")
        .displayName("운영자 API")
        .pathsToMatch(ADMIN_PATHS)
        .build();
  }

  /** 초기 프리오더 구조에서 남아 있는 API입니다. 데이터 보존을 위해 남겨 두었고 현재 화면에서는 쓰지 않습니다. */
  @Bean
  public GroupedOpenApi legacyApi() {
    return GroupedOpenApi.builder()
        .group("3-legacy")
        .displayName("이전 기능 (프리오더·주문·결제)")
        .addOpenApiMethodFilter(tagStartsWith(LEGACY_PREFIX)::test)
        .build();
  }

  /** 메서드가 속한 컨트롤러의 {@code @Tag} 이름이 주어진 말로 시작하는지 확인합니다. */
  private static Predicate<Method> tagStartsWith(String prefix) {
    return method -> {
      Tag tag = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Tag.class);
      return tag != null && tag.name().startsWith(prefix);
    };
  }
}

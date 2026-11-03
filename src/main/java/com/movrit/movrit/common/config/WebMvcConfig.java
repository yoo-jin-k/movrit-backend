package com.movrit.movrit.common.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 공통 설정입니다. 로컬 저장소에 올린 파일 중 공개 이미지만 /uploads 주소로 내려줍니다.
 *
 * <p>업로드 폴더에는 비공개 파일(구매 인증·취소 증빙·채팅 첨부·채널 확인 자료·프로필 사진)도 함께 있습니다. 이 파일들은 권한을 확인하는 API로만 내려가야 하므로
 * /uploads 에는 공개 폴더({@link #PUBLIC_FOLDERS})만 연결합니다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  /** 로그인 없이 볼 수 있는 공개 이미지 폴더입니다(캠페인 대표·상세 이미지, 브랜드 로고·표지, 상품 이미지). */
  public static final java.util.List<String> PUBLIC_FOLDERS =
      java.util.List.of("campaigns", "brands", "products");

  private final String uploadDir;

  /** 파일 업로드 폴더 경로(app.upload-dir, 기본값 uploads)를 받아 둡니다. */
  public WebMvcConfig(@Value("${app.upload-dir:uploads}") String uploadDir) {
    this.uploadDir = uploadDir;
  }

  /**
   * 공개 폴더의 이미지만 /uploads/{폴더}/** 주소로 열 수 있게 연결합니다. 다른 폴더는 연결하지 않으므로 주소를 알아도 열리지 않습니다. 운영에서 외부 저장소로
   * 바꾸더라도 저장 로직은 별도 서비스에 있어 이 설정만 교체하면 됩니다.
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path root = Path.of(uploadDir).toAbsolutePath().normalize();
    for (String folder : PUBLIC_FOLDERS) {
      registry
          .addResourceHandler("/uploads/" + folder + "/**")
          .addResourceLocations(root.resolve(folder).toUri().toString());
    }
  }
}

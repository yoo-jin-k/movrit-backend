package com.movrit.movrit.common.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 웹 MVC 공통 설정입니다. 로컬 저장소에 올린 파일을 /uploads 주소로 내려줍니다. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final String uploadDir;

  /** 파일 업로드 폴더 경로(app.upload-dir, 기본값 uploads)를 받아 둡니다. */
  public WebMvcConfig(@Value("${app.upload-dir:uploads}") String uploadDir) {
    this.uploadDir = uploadDir;
  }

  /**
   * 저장된 이미지를 /uploads/** 주소로 바로 열 수 있게 연결합니다. 운영에서 외부 저장소로 바꾸더라도 저장 로직은 별도 서비스에 있어 이 설정만 교체하면 됩니다.
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
    registry.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}

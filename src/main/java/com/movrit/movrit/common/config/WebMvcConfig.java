package com.movrit.movrit.common.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final String uploadDir;

  public WebMvcConfig(@Value("${app.upload-dir:uploads}") String uploadDir) {
    this.uploadDir = uploadDir;
  }

  /**
   * 로컬 개발 환경에서는 저장된 이미지를 /uploads/** 주소로 바로 확인합니다. 운영 환경에서는 S3 같은 외부 스토리지로 교체할 수 있도록 파일 저장 로직을 별도
   * 서비스로 분리했습니다.
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
    registry.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}

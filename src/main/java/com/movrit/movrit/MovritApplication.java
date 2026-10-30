package com.movrit.movrit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** MOVRIT 백엔드(Spring Boot) 실행 시작점입니다. 예약 작업(@EnableScheduling)도 함께 켭니다. */
@SpringBootApplication
@EnableScheduling
public class MovritApplication {

  /** 스프링 애플리케이션을 시작합니다. */
  public static void main(String[] args) {
    SpringApplication.run(MovritApplication.class, args);
  }
}

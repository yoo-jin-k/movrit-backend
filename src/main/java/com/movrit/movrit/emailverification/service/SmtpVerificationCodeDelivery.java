package com.movrit.movrit.emailverification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** movrit.email.delivery=smtp일 때 사용하는 실제 메일 발송 구현입니다. 스프링 메일 설정(SMTP 서버)으로 인증 번호 메일을 보냅니다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "movrit.email.delivery", havingValue = "smtp")
public class SmtpVerificationCodeDelivery implements VerificationCodeDelivery {
  private final JavaMailSender mailSender;

  @Value("${movrit.email.from}")
  private String from;

  /** 발신 주소(movrit.email.from)로 인증 번호와 유효 시간을 담은 메일을 보냅니다. */
  @Override
  public void send(String email, String code, int expiresInMinutes) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(email);
    message.setSubject("[MOVRIT] 이메일 인증 번호입니다.");
    message.setText("인증 번호는 " + code + "입니다. " + expiresInMinutes + "분 안에 입력해 주세요.");
    mailSender.send(message);
  }
}

package com.movrit.movrit.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI(LLM) 호출 한 번의 사용량입니다. 하루 사용 횟수 제한과 토큰(비용) 집계에 씁니다.
 *
 * <p>요청 원문과 AI 답은 저장하지 않습니다. 누가(회원), 어떤 기능에서, 어떤 모델로, 토큰을 얼마나 썼고 결과가 어땠는지만 남깁니다.
 *
 * <p>변경 이력(V26): 고치기 전에는 운영 감사 기록(audit_log)에 AI_CAMPAIGN_DRAFT로 함께 남겨, 업체가 초안을 자주 만들면 운영자 화면의 '최근
 * 운영 기록 200건'이 AI 기록으로 밀려났습니다. 고친 후에는 이 전용 테이블에 남기고 감사 기록에는 남기지 않습니다.
 */
@Entity
@Table(name = "ai_usage_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiUsageLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  /** 기능 이름입니다(예: CAMPAIGN_DRAFT). */
  @Column(nullable = false, length = 40)
  private String feature;

  @Column(nullable = false, length = 80)
  private String model;

  @Column(name = "input_tokens", nullable = false)
  private int inputTokens;

  @Column(name = "output_tokens", nullable = false)
  private int outputTokens;

  /** 결과입니다: SUCCESS · INVALID_ANSWER · TRUNCATED. */
  @Column(nullable = false, length = 20)
  private String outcome;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}

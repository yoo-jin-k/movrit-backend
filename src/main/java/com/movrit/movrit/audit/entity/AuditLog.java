package com.movrit.movrit.audit.entity;

import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 운영자·업체가 운영 상태를 바꾼 기록(누가, 무엇을, 어떤 대상을, 변경 전후)을 남기는 감사 로그입니다. */
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_member_id")
  private Member actor;

  @Column(nullable = false)
  private String action;

  @Column(name = "target_type", nullable = false)
  private String targetType;

  @Column(name = "target_id")
  private Long targetId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "before_data", columnDefinition = "jsonb")
  private String beforeData;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "after_data", columnDefinition = "jsonb")
  private String afterData;

  @Column(length = 500)
  private String reason;

  @Column(name = "ip_hash")
  private String ipHash;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}

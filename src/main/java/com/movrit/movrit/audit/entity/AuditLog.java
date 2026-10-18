package com.movrit.movrit.audit.entity;

import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  private String reason;

  @Column(name = "ip_hash")
  private String ipHash;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}

package com.movrit.movrit.audit.repository;

import com.movrit.movrit.audit.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 운영 감사 기록을 저장하고 조회합니다. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  /** 운영자 화면에 보여줄 최근 감사 기록 200건을 최신순으로 조회합니다. */
  List<AuditLog> findTop200ByOrderByIdDesc();

  /** 한 회원이 어떤 기록(예: AI 초안 사용)을 특정 시각 이후 몇 번 남겼는지 셉니다. 하루 사용 횟수 제한에 씁니다. */
  long countByActorIdAndActionAndCreatedAtGreaterThanEqual(
      Long actorId, String action, LocalDateTime from);
}

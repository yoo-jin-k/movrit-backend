package com.movrit.movrit.audit.repository;

import com.movrit.movrit.audit.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 운영 감사 기록을 저장하고 조회합니다. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  /** 운영자 화면에 보여줄 최근 감사 기록 200건을 최신순으로 조회합니다. */
  List<AuditLog> findTop200ByOrderByIdDesc();
}

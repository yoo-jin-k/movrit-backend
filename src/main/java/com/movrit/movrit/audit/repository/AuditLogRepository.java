package com.movrit.movrit.audit.repository;

import com.movrit.movrit.audit.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findTop200ByOrderByIdDesc();
}

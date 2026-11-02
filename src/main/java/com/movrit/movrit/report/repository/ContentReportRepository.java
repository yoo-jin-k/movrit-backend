package com.movrit.movrit.report.repository;

import com.movrit.movrit.report.entity.ContentReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 신고 저장소입니다. */
public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {
  boolean existsByReporterMemberIdAndTargetTypeAndTargetId(
      Long reporterMemberId, String targetType, Long targetId);

  List<ContentReport> findTop200ByOrderByIdDesc();

  List<ContentReport> findAllByTargetTypeAndTargetIdAndStatus(
      String targetType, Long targetId, String status);

  long countByTargetTypeAndTargetId(String targetType, Long targetId);
}

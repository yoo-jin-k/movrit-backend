package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.DeadlineReminder;
import org.springframework.data.jpa.repository.JpaRepository;

/** 마감 알림 발송 기록 저장소입니다. */
public interface DeadlineReminderRepository extends JpaRepository<DeadlineReminder, Long> {
  boolean existsByApplicationIdAndStage(Long applicationId, String stage);
}

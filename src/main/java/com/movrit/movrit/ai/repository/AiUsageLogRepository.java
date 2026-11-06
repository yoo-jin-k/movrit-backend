package com.movrit.movrit.ai.repository;

import com.movrit.movrit.ai.entity.AiUsageLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

/** AI 사용량 기록을 저장하고 셉니다. */
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

  /** 한 회원이 어떤 기능을 특정 시각 이후 몇 번 썼는지 셉니다. 하루 사용 횟수 제한에 씁니다. */
  long countByMemberIdAndFeatureAndCreatedAtGreaterThanEqual(
      Long memberId, String feature, LocalDateTime from);
}

package com.movrit.movrit.contentmetric.repository;

import com.movrit.movrit.contentmetric.entity.CampaignContentMetric;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 콘텐츠 성과 기록을 저장하고 조회합니다. */
public interface CampaignContentMetricRepository
    extends JpaRepository<CampaignContentMetric, Long> {

  /** 제출 한 건의 기록을 게시물별·최근 날짜순으로 읽습니다. */
  List<CampaignContentMetric> findAllBySubmissionIdOrderByMeasuredOnDescIdDesc(Long submissionId);

  /** 여러 제출 건의 기록을 한 번에 읽습니다(캠페인 성과 요약). */
  List<CampaignContentMetric> findAllBySubmissionIdInOrderByMeasuredOnDescIdDesc(
      Collection<Long> submissionIds);

  /** 같은 게시물·같은 날짜 기록이 있으면 그 줄을 고칩니다. */
  Optional<CampaignContentMetric> findBySubmissionIdAndContentUrlAndMeasuredOn(
      Long submissionId, String contentUrl, LocalDate measuredOn);
}

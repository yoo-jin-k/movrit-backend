package com.movrit.movrit.campaigncontent.repository;

import com.movrit.movrit.campaigncontent.entity.CampaignContentLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignContentLinkRepository extends JpaRepository<CampaignContentLink, Long> {
  List<CampaignContentLink> findAllBySubmissionIdOrderByIdAsc(Long submissionId);

  void deleteAllBySubmissionId(Long submissionId);
}

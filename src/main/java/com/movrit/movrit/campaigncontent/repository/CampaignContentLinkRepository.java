package com.movrit.movrit.campaigncontent.repository;

import com.movrit.movrit.campaigncontent.entity.CampaignContentLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignContentLinkRepository extends JpaRepository<CampaignContentLink, Long> {
  List<CampaignContentLink> findAllBySubmissionIdOrderByIdAsc(Long submissionId);

  List<CampaignContentLink> findAllBySubmissionIdInOrderBySubmissionIdAscIdAsc(
      List<Long> submissionIds);

  void deleteAllBySubmissionId(Long submissionId);
}

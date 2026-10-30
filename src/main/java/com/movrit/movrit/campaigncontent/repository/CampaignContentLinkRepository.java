package com.movrit.movrit.campaigncontent.repository;

import com.movrit.movrit.campaigncontent.entity.CampaignContentLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 콘텐츠 제출 한 건에 딸린 채널별 게시물 링크를 저장하고 조회합니다. */
public interface CampaignContentLinkRepository extends JpaRepository<CampaignContentLink, Long> {
  /** 콘텐츠 제출 한 건에 담긴 링크를 입력한 순서대로 조회합니다. */
  List<CampaignContentLink> findAllBySubmissionIdOrderByIdAsc(Long submissionId);

  /** 여러 제출 건의 링크를 한 번에 읽습니다. 목록 화면에서 제출 건마다 따로 조회하지 않기 위해 사용합니다. */
  List<CampaignContentLink> findAllBySubmissionIdInOrderBySubmissionIdAscIdAsc(
      List<Long> submissionIds);

  /** 재제출 시 이전 링크 목록을 모두 지우고 새 목록으로 바꿀 때 사용합니다. */
  void deleteAllBySubmissionId(Long submissionId);
}

package com.movrit.movrit.community.repository;

import com.movrit.movrit.community.entity.CommunityComment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 게시글 댓글을 조회합니다. */
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

  @EntityGraph(attributePaths = "author")
  List<CommunityComment> findAllByPostIdAndDeletedAtIsNullOrderByIdAsc(Long postId);

  /** 운영자 상세용: 지운 댓글까지 모두 가져옵니다. */
  @EntityGraph(attributePaths = "author")
  List<CommunityComment> findAllByPostIdOrderByIdAsc(Long postId);

  /** 운영자 목록에서 글마다 가린 댓글 수를 보여 줍니다. */
  long countByPostIdAndHiddenTrueAndDeletedAtIsNull(Long postId);
}

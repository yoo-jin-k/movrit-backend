package com.movrit.movrit.community.repository;

import com.movrit.movrit.community.entity.CommunityComment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 게시글 댓글을 조회합니다. */
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

  @EntityGraph(attributePaths = "author")
  List<CommunityComment> findAllByPostIdAndDeletedAtIsNullOrderByIdAsc(Long postId);
}

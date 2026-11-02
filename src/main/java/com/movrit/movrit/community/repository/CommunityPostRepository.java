package com.movrit.movrit.community.repository;

import com.movrit.movrit.community.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 커뮤니티 게시글을 조회합니다. */
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

  /** 게시판 목록입니다. 분류·검색어(제목·내용)는 비어 있으면 거르지 않습니다. 지운 글·숨긴 글은 빼고 최신순입니다. */
  @EntityGraph(attributePaths = "author")
  @Query(
      "select p from CommunityPost p where p.board = :board and p.deletedAt is null and p.hidden = false"
          + " and (:category is null or p.category = :category)"
          + " and (:keyword is null or lower(p.title) like :keyword or lower(p.body) like :keyword)"
          + " order by p.id desc")
  Page<CommunityPost> search(
      @Param("board") String board,
      @Param("category") String category,
      @Param("keyword") String keyword,
      Pageable pageable);
}

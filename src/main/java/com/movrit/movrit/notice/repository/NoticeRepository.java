package com.movrit.movrit.notice.repository;

import com.movrit.movrit.notice.entity.Notice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 공지사항을 조회합니다. */
public interface NoticeRepository extends JpaRepository<Notice, Long> {
  /** 사용자 목록: 게시된 공지를 고정 공지 먼저, 그다음 최근 게시 순으로 조회합니다. */
  List<Notice> findAllByPublishedTrueOrderByPinnedDescPublishedAtDesc();

  /** 사용자 상세: 게시된 공지만 찾습니다. */
  Optional<Notice> findByIdAndPublishedTrue(Long id);

  /** 운영자 목록: 게시 여부와 관계없이 최근 작성 순으로 조회합니다. */
  List<Notice> findAllByOrderByIdDesc();
}

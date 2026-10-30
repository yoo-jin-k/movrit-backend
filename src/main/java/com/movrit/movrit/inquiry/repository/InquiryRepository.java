package com.movrit.movrit.inquiry.repository;

import com.movrit.movrit.inquiry.entity.Inquiry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 1:1 문의를 조회합니다. 회원 본인 문의와 운영자 전체 문의 조회를 구분합니다. */
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
  /** 회원 본인의 문의를 최근 순으로 조회합니다. */
  List<Inquiry> findAllByMemberIdOrderByIdDesc(Long memberId);

  /** 운영자용으로 모든 문의를 최근 순으로 조회합니다. */
  List<Inquiry> findAllByOrderByIdDesc();

  /** 문의 ID와 작성자를 함께 조건으로 걸어 본인 문의만 조회합니다. */
  Optional<Inquiry> findByIdAndMemberId(Long id, Long memberId);
}

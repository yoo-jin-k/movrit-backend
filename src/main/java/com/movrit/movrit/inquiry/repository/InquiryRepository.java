package com.movrit.movrit.inquiry.repository;

import com.movrit.movrit.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findAllByMemberIdOrderByIdDesc(Long memberId);
    List<Inquiry> findAllByOrderByIdDesc();
    Optional<Inquiry> findByIdAndMemberId(Long id, Long memberId);
}

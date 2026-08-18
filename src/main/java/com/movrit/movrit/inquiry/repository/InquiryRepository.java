package com.movrit.movrit.inquiry.repository;

import com.movrit.movrit.inquiry.entity.Inquiry;
import com.movrit.movrit.inquiry.entity.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findAllByMemberIdOrderByIdDesc(Long memberId);
    Optional<Inquiry> findByIdAndMemberId(Long id, Long memberId);
    List<Inquiry> findAllByStatusOrderByIdDesc(InquiryStatus status);
    List<Inquiry> findAllByOrderByIdDesc();
}

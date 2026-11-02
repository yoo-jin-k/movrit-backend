package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.ParticipantRating;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 참여자 평가 저장소입니다(키: 신청 ID). */
public interface ParticipantRatingRepository extends JpaRepository<ParticipantRating, Long> {
  List<ParticipantRating> findAllByMemberId(Long memberId);
}

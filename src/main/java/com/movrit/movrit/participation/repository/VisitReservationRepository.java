package com.movrit.movrit.participation.repository;

import com.movrit.movrit.participation.entity.VisitReservation;
import org.springframework.data.jpa.repository.JpaRepository;

/** 방문 예약 저장소입니다(키: 신청 ID). */
public interface VisitReservationRepository extends JpaRepository<VisitReservation, Long> {}

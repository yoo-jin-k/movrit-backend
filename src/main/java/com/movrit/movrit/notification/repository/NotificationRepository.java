package com.movrit.movrit.notification.repository;

import com.movrit.movrit.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원 알림을 조회합니다. 항상 회원 ID를 조건으로 넣어 본인 알림만 다룹니다. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
  /** 회원 본인의 알림을 최근 순으로 조회합니다. */
  List<Notification> findAllByMemberIdOrderByIdDesc(Long memberId);

  /** 알림 ID와 회원 ID가 모두 맞을 때만 찾습니다. 본인 알림만 읽음 처리하도록 권한 확인을 겸합니다. */
  Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

  /** 헤더 알림 배지에 표시할 읽지 않은 알림 수를 셉니다. */
  long countByMemberIdAndReadFalse(Long memberId);
}

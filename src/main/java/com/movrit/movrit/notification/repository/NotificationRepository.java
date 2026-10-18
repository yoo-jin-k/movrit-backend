package com.movrit.movrit.notification.repository;

import com.movrit.movrit.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findAllByMemberIdOrderByIdDesc(Long memberId);

  Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

  long countByMemberIdAndReadFalse(Long memberId);
}

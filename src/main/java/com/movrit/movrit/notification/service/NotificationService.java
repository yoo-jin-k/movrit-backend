package com.movrit.movrit.notification.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.dto.NotificationDtos;
import com.movrit.movrit.notification.entity.Notification;
import com.movrit.movrit.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
  private final NotificationRepository repository;
  private final MemberService memberService;

  /** 도메인 서비스가 상태 변화를 확정한 뒤 해당 사용자에게 내부 알림을 남깁니다. */
  @Transactional
  public void send(
      Long memberId, String type, String title, String message, String targetType, Long targetId) {
    Member member = memberService.getMember(memberId);
    repository.save(
        Notification.builder()
            .member(member)
            .type(type)
            .title(title)
            .message(message)
            .targetType(targetType)
            .targetId(targetId)
            .read(false)
            .createdAt(LocalDateTime.now())
            .build());
  }

  public List<NotificationDtos.Response> findMine(Long memberId) {
    return repository.findAllByMemberIdOrderByIdDesc(memberId).stream()
        .map(NotificationDtos.Response::from)
        .toList();
  }

  public NotificationDtos.UnreadCount unreadCount(Long memberId) {
    return new NotificationDtos.UnreadCount(repository.countByMemberIdAndReadFalse(memberId));
  }

  @Transactional
  public NotificationDtos.Response markRead(Long memberId, Long notificationId) {
    Notification notification =
        repository
            .findByIdAndMemberId(notificationId, memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
    notification.markRead();
    return NotificationDtos.Response.from(notification);
  }

  @Transactional
  public void markAllRead(Long memberId) {
    repository.findAllByMemberIdOrderByIdDesc(memberId).forEach(Notification::markRead);
  }
}

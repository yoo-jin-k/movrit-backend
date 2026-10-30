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

/** 회원 알림을 만들고, 본인 알림 목록·읽지 않은 수 조회와 읽음 처리를 담당합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
  private final NotificationRepository repository;
  private final MemberService memberService;

  /** 다른 기능(신청 선정, 콘텐츠 검토 등)이 상태를 바꾼 뒤 해당 회원에게 알림을 남깁니다. 존재하지 않는 회원이면 거절됩니다. */
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

  /** 로그인한 회원 본인의 알림을 최신순으로 반환합니다. */
  public List<NotificationDtos.Response> findMine(Long memberId) {
    return repository.findAllByMemberIdOrderByIdDesc(memberId).stream()
        .map(NotificationDtos.Response::from)
        .toList();
  }

  /** 헤더 알림 배지에 쓸 읽지 않은 알림 수를 반환합니다. */
  public NotificationDtos.UnreadCount unreadCount(Long memberId) {
    return new NotificationDtos.UnreadCount(repository.countByMemberIdAndReadFalse(memberId));
  }

  /** 알림 하나를 읽음 처리합니다. 다른 회원의 알림이거나 없는 알림이면 404로 거절합니다. */
  @Transactional
  public NotificationDtos.Response markRead(Long memberId, Long notificationId) {
    Notification notification =
        repository
            .findByIdAndMemberId(notificationId, memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
    notification.markRead();
    return NotificationDtos.Response.from(notification);
  }

  /** 본인 알림을 모두 읽음 처리합니다. */
  @Transactional
  public void markAllRead(Long memberId) {
    repository.findAllByMemberIdOrderByIdDesc(memberId).forEach(Notification::markRead);
  }
}

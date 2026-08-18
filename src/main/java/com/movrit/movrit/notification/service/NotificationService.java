package com.movrit.movrit.notification.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.notification.dto.NotificationDtos;
import com.movrit.movrit.notification.entity.Notification;
import com.movrit.movrit.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    public List<NotificationDtos.Response> findMine(Long memberId) {
        return notificationRepository.findAllByMemberIdOrderByIdDesc(memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NotificationDtos.UnreadCountResponse unreadCount(Long memberId) {
        return new NotificationDtos.UnreadCountResponse(
                notificationRepository.countByMemberIdAndReadFalse(memberId)
        );
    }

    @Transactional
    public NotificationDtos.Response markRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "알림을 찾을 수 없어."
                ));
        notification.markRead();
        return toResponse(notification);
    }

    @Transactional
    public void markAllRead(Long memberId) {
        notificationRepository.findAllByMemberIdOrderByIdDesc(memberId)
                .forEach(Notification::markRead);
    }

    @Transactional
    public NotificationDtos.Response send(
            Long memberId,
            String type,
            String title,
            String message,
            String targetType,
            Long targetId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회원을 찾을 수 없어."
                ));

        Notification saved = notificationRepository.save(
                Notification.builder()
                        .member(member)
                        .type(type)
                        .title(title)
                        .message(message)
                        .targetType(targetType)
                        .targetId(targetId)
                        .build()
        );
        return toResponse(saved);
    }

    private NotificationDtos.Response toResponse(Notification notification) {
        return new NotificationDtos.Response(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}

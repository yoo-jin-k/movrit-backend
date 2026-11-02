package com.movrit.movrit.chat.dto;

import com.movrit.movrit.chat.entity.ChatRole;
import com.movrit.movrit.chat.entity.ChatRoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** 채팅 화면과 주고받는 요청·응답 형식입니다. */
public final class ChatDtos {
  private ChatDtos() {}

  /**
   * 채팅 목록의 방 하나입니다.
   *
   * @param subtitle 상대 표시(예: '브랜드 · 무브릿', '참여자 · 닉네임', '운영자')
   * @param unreadCount 내가 아직 읽지 않은 다른 사람의 메시지 수
   */
  public record RoomSummary(
      Long id,
      ChatRoomType type,
      String title,
      String subtitle,
      Long applicationId,
      String lastMessage,
      LocalDateTime lastMessageAt,
      long unreadCount) {}

  /**
   * 메시지 하나입니다. mine은 내가 보낸 메시지인지입니다.
   *
   * @param attachment 첨부 파일 정보. 없으면 null
   */
  public record Message(
      Long id,
      ChatRole senderRole,
      String senderName,
      boolean mine,
      String body,
      LocalDateTime createdAt,
      Attachment attachment) {}

  /** 첨부 파일 정보입니다. 파일은 /api/chats/{roomId}/attachments/{messageId}로 받습니다(참여자만). */
  public record Attachment(String name, String contentType, long size, boolean image) {}

  /**
   * 방 상세입니다. 메시지는 오래된 순서입니다.
   *
   * @param myRole 이 방에서의 내 역할
   * @param members 방에 참여한 사람 표시(예: '참여자 · 닉네임')
   * @param othersLastReadId 다른 참여자가 읽은 가장 마지막 메시지 번호(내 메시지의 '읽음' 표시용)
   * @param hasMore 이 목록보다 이전 메시지가 더 있는지('이전 메시지 더 보기')
   */
  public record Room(
      Long id,
      ChatRoomType type,
      String title,
      Long applicationId,
      ChatRole myRole,
      List<String> members,
      List<Message> messages,
      long othersLastReadId,
      boolean hasMore) {}

  /** 메시지 보내기 요청입니다. */
  public record SendRequest(@NotBlank @Size(max = 2000) String body) {}

  /** 안 읽은 메시지 수입니다. */
  public record UnreadCount(long count) {}
}

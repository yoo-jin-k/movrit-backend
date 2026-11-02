package com.movrit.movrit.chat.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 하나입니다. 보낸 사람의 역할을 함께 남깁니다. 글은 2,000자까지입니다.
 *
 * <p>사진(JPG·PNG)이나 PDF를 첨부할 수 있습니다. 첨부만 보낼 때는 본문이 빈 문자열입니다. 첨부 파일은 대화방 참여자만 받을 수 있습니다.
 */
@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private ChatRoom room;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_member_id")
  private Member sender;

  @Enumerated(EnumType.STRING)
  @Column(name = "sender_role", nullable = false, length = 20)
  private ChatRole senderRole;

  @Column(nullable = false, length = 2000)
  private String body;

  /** 첨부 파일 저장 키입니다(공개 주소 없이 채팅 API로만 내려갑니다). */
  @Column(name = "attachment_key", length = 100)
  private String attachmentKey;

  /** 사용자가 올린 원래 파일 이름입니다(내려받을 때 이름으로 씁니다). */
  @Column(name = "attachment_name", length = 200)
  private String attachmentName;

  @Column(name = "attachment_type", length = 100)
  private String attachmentType;

  @Column(name = "attachment_size")
  private Long attachmentSize;

  /** 운영자가 신고 처리로 가린 시각입니다. 가린 메시지는 내용·첨부 대신 안내 문구로 보여줍니다. */
  @Column(name = "hidden_at")
  private java.time.LocalDateTime hiddenAt;

  public boolean hasAttachment() {
    return attachmentKey != null;
  }

  public boolean isHidden() {
    return hiddenAt != null;
  }

  /** 운영자가 메시지를 가립니다(원문은 신고 기록에 남습니다). */
  public void hideByAdmin() {
    if (hiddenAt == null) hiddenAt = java.time.LocalDateTime.now();
  }
}

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

/** 채팅방에 참여한 회원과 '마지막으로 읽은 메시지'입니다. 안 읽은 메시지 수를 셀 때 씁니다. */
@Entity
@Table(name = "chat_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatParticipant extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private ChatRoom room;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Enumerated(EnumType.STRING)
  @Column(name = "participant_role", nullable = false, length = 20)
  private ChatRole role;

  /** 마지막으로 읽은 메시지 번호입니다. 동시에 읽어도 뒤로 가지 않도록 저장소의 advanceRead(DB에서 비교 후 갱신)로만 바꿉니다. */
  @Column(name = "last_read_message_id", nullable = false)
  private long lastReadMessageId;
}

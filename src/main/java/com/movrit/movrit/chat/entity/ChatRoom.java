package com.movrit.movrit.chat.entity;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 하나입니다.
 *
 * <ul>
 *   <li>신청 대화방(APPLICATION): 신청(application)마다 하나. 참여자·브랜드가 참여하고 운영자도 들어올 수 있습니다.
 *   <li>운영자 문의방(SUPPORT): 회원(owner)마다 하나. 회원과 운영자가 대화합니다.
 * </ul>
 */
@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "room_type", nullable = false, length = 20)
  private ChatRoomType roomType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id")
  private CampaignApplication application;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_member_id")
  private Member owner;

  /** 목록에 보여줄 방 제목입니다(예: 캠페인 이름, '운영자 문의'). */
  @Column(nullable = false, length = 200)
  private String title;

  /** 마지막 메시지 시각입니다. 목록을 최근 대화 순으로 정렬할 때 씁니다. */
  @Column(name = "last_message_at")
  private LocalDateTime lastMessageAt;

  /** 새 메시지가 오면 마지막 대화 시각을 갱신합니다. */
  public void touch(LocalDateTime at) {
    this.lastMessageAt = at;
  }
}

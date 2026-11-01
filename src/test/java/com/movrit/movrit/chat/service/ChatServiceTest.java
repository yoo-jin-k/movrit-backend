package com.movrit.movrit.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.chat.entity.ChatParticipant;
import com.movrit.movrit.chat.entity.ChatRole;
import com.movrit.movrit.chat.entity.ChatRoom;
import com.movrit.movrit.chat.entity.ChatRoomType;
import com.movrit.movrit.chat.repository.ChatMessageRepository;
import com.movrit.movrit.chat.repository.ChatParticipantRepository;
import com.movrit.movrit.chat.repository.ChatRoomRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.service.MemberService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 채팅방 접근 권한과 메시지 입력 규칙을 확인합니다. */
class ChatServiceTest {
  private final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
  private final ChatParticipantRepository participants = mock(ChatParticipantRepository.class);
  private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
  private final CampaignApplicationRepository applications =
      mock(CampaignApplicationRepository.class);
  private final MemberService members = mock(MemberService.class);
  private final ChatService service =
      new ChatService(rooms, participants, messages, applications, members);

  private final Member stranger = Member.builder().id(3L).role(MemberRole.USER).build();
  private final Member owner = Member.builder().id(1L).role(MemberRole.USER).build();
  private final ChatRoom support =
      ChatRoom.builder()
          .id(10L)
          .roomType(ChatRoomType.SUPPORT)
          .owner(owner)
          .title("운영자 문의")
          .build();

  @Test
  void strangerCannotOpenOthersRoom() {
    when(members.getMember(3L)).thenReturn(stranger);
    when(rooms.findById(10L)).thenReturn(Optional.of(support));
    when(participants.findByRoomIdAndMemberId(10L, 3L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.messages(3L, 10L, null))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void blankOrTooLongMessageIsRejected() {
    when(members.getMember(1L)).thenReturn(owner);
    when(rooms.findById(10L)).thenReturn(Optional.of(support));
    when(participants.findByRoomIdAndMemberId(10L, 1L))
        .thenReturn(
            Optional.of(
                ChatParticipant.builder()
                    .room(support)
                    .member(owner)
                    .role(ChatRole.MEMBER)
                    .build()));

    assertThatThrownBy(() -> service.send(1L, 10L, "   "))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.send(1L, 10L, "가".repeat(2001)))
        .isInstanceOf(ResponseStatusException.class);
    verify(messages, never()).save(any());
  }

  @Test
  void adminCannotOpenOwnSupportRoom() {
    when(members.getMember(9L)).thenReturn(Member.builder().id(9L).role(MemberRole.ADMIN).build());

    assertThatThrownBy(() -> service.openSupportRoom(9L))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }
}

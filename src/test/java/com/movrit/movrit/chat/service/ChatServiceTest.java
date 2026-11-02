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
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
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
      new ChatService(
          rooms,
          participants,
          messages,
          applications,
          members,
          mock(MemberRepository.class),
          mock(NotificationService.class),
          new ChatEventHub(),
          mock(ChatAttachmentStorage.class));

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

  @Test
  void attachmentNameIsSanitizedAndPdfIsDetected() {
    assertThat(ChatAttachmentStorage.safeName("../../etc/pass:wd.pdf"))
        .isEqualTo("....etcpasswd.pdf");
    assertThat(ChatAttachmentStorage.safeName("  ")).isEqualTo("첨부파일");
    assertThat(ChatAttachmentStorage.isPdf("%PDF-1.7 ...".getBytes())).isTrue();
    assertThat(ChatAttachmentStorage.isPdf("<html>".getBytes())).isFalse();
    // 영상 형식은 파일 앞부분 표식으로 구분하고, 같은 구조인 HEIC 사진은 영상으로 보지 않습니다.
    assertThat(ChatAttachmentStorage.videoType(box("isom"))).isEqualTo("video/mp4");
    assertThat(ChatAttachmentStorage.videoType(box("qt  "))).isEqualTo("video/quicktime");
    assertThat(ChatAttachmentStorage.videoType(box("heic"))).isNull();
    assertThat(
            ChatAttachmentStorage.videoType(
                new byte[] {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0, 0, 0, 0}))
        .isEqualTo("video/webm");
    assertThat(ChatAttachmentStorage.videoType("<html>hello</html>".getBytes())).isNull();
    assertThat(ChatAttachmentStorage.isJpegOrPng("GIF89a....".getBytes())).isFalse();
    assertThat(
            ChatAttachmentStorage.isJpegOrPng(
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0}))
        .isTrue();
  }

  /** 'ftyp' 상자 머리(크기 4바이트 + 'ftyp' + 브랜드 4바이트)를 만듭니다. */
  private static byte[] box(String brand) {
    byte[] bytes = new byte[16];
    bytes[3] = 0x18;
    System.arraycopy("ftyp".getBytes(), 0, bytes, 4, 4);
    System.arraycopy(brand.getBytes(), 0, bytes, 8, 4);
    return bytes;
  }
}

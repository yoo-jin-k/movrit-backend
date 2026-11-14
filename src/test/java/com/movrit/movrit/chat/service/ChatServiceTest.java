package com.movrit.movrit.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.chat.dto.ChatDtos;
import com.movrit.movrit.chat.entity.ChatMessage;
import com.movrit.movrit.chat.entity.ChatParticipant;
import com.movrit.movrit.chat.entity.ChatRole;
import com.movrit.movrit.chat.entity.ChatRoom;
import com.movrit.movrit.chat.entity.ChatRoomType;
import com.movrit.movrit.chat.repository.ChatMessageRepository;
import com.movrit.movrit.chat.repository.ChatParticipantRepository;
import com.movrit.movrit.chat.repository.ChatRoomRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
  private final ChatEventHub hub = mock(ChatEventHub.class);
  private final ChatService service =
      new ChatService(
          rooms,
          participants,
          messages,
          applications,
          members,
          mock(MemberRepository.class),
          mock(NotificationService.class),
          hub,
          mock(ChatAttachmentStorage.class));

  private final Member stranger = Member.builder().id(3L).role(MemberRole.USER).build();
  private final Member owner =
      Member.builder().id(1L).role(MemberRole.USER).status(MemberStatus.ACTIVE).build();
  private final Member admin =
      Member.builder().id(9L).role(MemberRole.ADMIN).status(MemberStatus.ACTIVE).build();
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

  @Test
  void hiddenLastMessageIsNotPreviewedInRoomList() {
    when(members.getMember(1L)).thenReturn(owner);
    when(participants.findAllByMemberId(1L))
        .thenReturn(List.of(participant(support, owner, ChatRole.MEMBER, 0)));
    when(participants.findByRoomIdAndMemberId(10L, 1L))
        .thenReturn(Optional.of(participant(support, owner, ChatRole.MEMBER, 0)));
    ChatMessage hidden =
        ChatMessage.builder()
            .id(5L)
            .room(support)
            .senderRole(ChatRole.ADMIN)
            .body("가려야 할 원문")
            .attachmentKey("a.jpg")
            .attachmentName("secret.jpg")
            .hiddenAt(java.time.LocalDateTime.now())
            .build();
    when(messages.findTopByRoomIdOrderByIdDesc(10L)).thenReturn(Optional.of(hidden));

    List<ChatDtos.RoomSummary> rooms = service.myRooms(1L);

    assertThat(rooms).hasSize(1);
    assertThat(rooms.get(0).lastMessage()).isEqualTo(ChatService.HIDDEN_TEXT);
  }

  @Test
  void adminCanReadWithoutJoiningAndLeavesNoReadPointer() {
    when(members.getMember(9L)).thenReturn(admin);
    when(rooms.findById(10L)).thenReturn(Optional.of(support));
    when(participants.findByRoomIdAndMemberId(10L, 9L)).thenReturn(Optional.empty());
    when(messages.findTop51ByRoomIdOrderByIdDesc(10L)).thenReturn(List.of(message(7L, owner)));

    ChatDtos.Room room = service.messages(9L, 10L, null);

    assertThat(room.myRole()).isEqualTo(ChatRole.ADMIN);
    verify(participants, never()).insertIfAbsent(anyLong(), anyLong(), anyString());
    verify(participants, never()).save(any());
    verify(participants, never()).advanceRead(anyLong(), anyLong());
  }

  @Test
  void adminSearchAndTypingDoNotJoin() {
    when(members.getMember(9L)).thenReturn(admin);
    when(rooms.findById(10L)).thenReturn(Optional.of(support));
    when(participants.findByRoomIdAndMemberId(10L, 9L)).thenReturn(Optional.empty());

    service.search(9L, 10L, "배송");
    service.typing(9L, 10L);

    verify(participants, never()).insertIfAbsent(anyLong(), anyLong(), anyString());
    verify(participants, never()).save(any());
  }

  @Test
  void adminReadDoesNotCountAsReadInApplicationRoom() {
    ChatRoom room = applicationRoom();
    Member creator =
        Member.builder().id(1L).role(MemberRole.USER).status(MemberStatus.ACTIVE).build();
    Member brand =
        Member.builder().id(2L).role(MemberRole.USER).status(MemberStatus.ACTIVE).build();
    when(members.getMember(1L)).thenReturn(creator);
    when(rooms.findById(20L)).thenReturn(Optional.of(room));
    ChatParticipant mine = participant(room, creator, ChatRole.CREATOR, 3);
    when(participants.findByRoomIdAndMemberId(20L, 1L)).thenReturn(Optional.of(mine));
    when(participants.findAllByRoomId(20L))
        .thenReturn(
            List.of(
                mine,
                participant(room, brand, ChatRole.BRAND, 4),
                participant(room, admin, ChatRole.ADMIN, 99)));

    ChatDtos.Room view = service.messages(1L, 20L, 3L);

    // 운영자는 99까지 읽었지만 상대(브랜드)가 읽은 4만 '읽음'으로 칩니다.
    assertThat(view.othersLastReadId()).isEqualTo(4L);
  }

  @Test
  void adminReadCountsInSupportRoom() {
    when(members.getMember(1L)).thenReturn(owner);
    when(rooms.findById(10L)).thenReturn(Optional.of(support));
    ChatParticipant mine = participant(support, owner, ChatRole.MEMBER, 0);
    when(participants.findByRoomIdAndMemberId(10L, 1L)).thenReturn(Optional.of(mine));
    when(participants.findAllByRoomId(10L))
        .thenReturn(List.of(mine, participant(support, admin, ChatRole.ADMIN, 12)));

    assertThat(service.messages(1L, 10L, 50L).othersLastReadId()).isEqualTo(12L);
  }

  @Test
  void sendingDoesNotMoveMyReadPointer() {
    when(members.getMember(1L)).thenReturn(owner);
    when(rooms.findById(10L)).thenReturn(Optional.of(support));
    ChatParticipant mine = participant(support, owner, ChatRole.MEMBER, 0);
    when(participants.findByRoomIdAndMemberId(10L, 1L)).thenReturn(Optional.of(mine));
    when(participants.findAllByRoomId(10L)).thenReturn(List.of(mine));
    when(messages.save(any())).thenReturn(message(30L, owner));

    service.send(1L, 10L, "안녕하세요");

    verify(participants, never()).advanceRead(anyLong(), anyLong());
    assertThat(mine.getLastReadMessageId()).isZero();
  }

  @Test
  void readingPublishesReadEventToMeAndOthers() {
    when(members.getMember(1L)).thenReturn(owner);
    when(rooms.findById(10L)).thenReturn(Optional.of(support));
    ChatParticipant mine = participant(support, owner, ChatRole.MEMBER, 0);
    when(participants.findByRoomIdAndMemberId(10L, 1L)).thenReturn(Optional.of(mine));
    when(participants.findAllByRoomId(10L))
        .thenReturn(List.of(mine, participant(support, admin, ChatRole.ADMIN, 0)));
    when(messages.findTop51ByRoomIdOrderByIdDesc(10L)).thenReturn(List.of(message(7L, admin)));
    when(participants.advanceRead(mine.getId(), 7L)).thenReturn(1);

    service.messages(1L, 10L, null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<Long>> to = ArgumentCaptor.forClass(Collection.class);
    ArgumentCaptor<Object> data = ArgumentCaptor.forClass(Object.class);
    verify(hub).publish(to.capture(), eq(false), eq("read"), data.capture());
    assertThat(to.getValue()).containsExactlyInAnyOrder(1L, 9L);
    assertThat(data.getValue()).isEqualTo(Map.of("roomId", 10L, "memberId", 1L));
  }

  @Test
  void suspendedMemberCannotChat() {
    Member suspended =
        Member.builder().id(1L).role(MemberRole.USER).status(MemberStatus.SUSPENDED).build();
    when(members.getMember(1L)).thenReturn(suspended);
    when(rooms.findById(10L)).thenReturn(Optional.of(support));
    when(participants.findByRoomIdAndMemberId(10L, 1L))
        .thenReturn(Optional.of(participant(support, suspended, ChatRole.MEMBER, 0)));

    for (Runnable call :
        List.<Runnable>of(
            () -> service.send(1L, 10L, "안녕하세요"),
            () -> service.typing(1L, 10L),
            () -> service.openSupportRoom(1L),
            () -> service.streamAccess(1L))) {
      assertThatThrownBy(call::run)
          .isInstanceOfSatisfying(
              ResponseStatusException.class,
              e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
    verify(messages, never()).save(any());
  }

  @Test
  void concurrentFirstOpenReadsExistingRowInsteadOfFailing() {
    when(members.getMember(1L)).thenReturn(owner);
    // 처음 확인할 때는 없었지만, 그 사이 다른 요청이 만든 방을 다시 읽어 씁니다.
    when(rooms.findByRoomTypeAndOwnerId(ChatRoomType.SUPPORT, 1L))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(support));
    when(rooms.findById(10L)).thenReturn(Optional.of(support));
    ChatParticipant mine = participant(support, owner, ChatRole.MEMBER, 0);
    when(participants.findByRoomIdAndMemberId(10L, 1L))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(mine));

    ChatDtos.Room room = service.openSupportRoom(1L);

    assertThat(room.id()).isEqualTo(10L);
    verify(rooms).insertSupportRoomIfAbsent(1L, "운영자 문의");
    verify(participants).insertIfAbsent(10L, 1L, "MEMBER");
    verify(rooms, never()).save(any());
    verify(participants, never()).save(any());
  }

  @Test
  void ftypBrandMustBeKnownVideoBrand() {
    assertThat(ChatAttachmentStorage.videoType(box("mp42"))).isEqualTo("video/mp4");
    assertThat(ChatAttachmentStorage.videoType(box("avif"))).isNull();
    assertThat(ChatAttachmentStorage.videoType(box("avis"))).isNull();
    assertThat(ChatAttachmentStorage.videoType(box("heix"))).isNull();
    assertThat(ChatAttachmentStorage.videoType(box("mif1"))).isNull();
    assertThat(ChatAttachmentStorage.videoType(box("msf1"))).isNull();
  }

  @Test
  void oversizedImageIsRejectedFromHeaderBeforeDecoding() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", out);
    byte[] small = out.toByteArray();
    ChatAttachmentStorage.requireImageSizeWithinLimit(small);

    // IHDR의 가로·세로(16~23번째 바이트)를 10,000 × 10,000으로 바꿉니다(1억 화소).
    byte[] huge = small.clone();
    int size = 10_000;
    for (int offset : new int[] {16, 20}) {
      huge[offset] = (byte) (size >>> 24);
      huge[offset + 1] = (byte) (size >>> 16);
      huge[offset + 2] = (byte) (size >>> 8);
      huge[offset + 3] = (byte) size;
    }
    assertThatThrownBy(() -> ChatAttachmentStorage.requireImageSizeWithinLimit(huge))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void disconnectClosesOnlyThatMembersStreams() {
    ChatEventHub realHub = new ChatEventHub();
    realHub.subscribe(1L, false);
    realHub.subscribe(1L, false);
    realHub.subscribe(2L, false);

    realHub.disconnect(1L);

    assertThat(realHub.connectionCount()).isEqualTo(1);
  }

  private ChatRoom applicationRoom() {
    CampaignApplication application = mock(CampaignApplication.class, RETURNS_DEEP_STUBS);
    when(application.getId()).thenReturn(100L);
    when(application.getCampaign().getBrand().getName()).thenReturn("브랜드");
    when(application.getCreator().getNickname()).thenReturn("참여자");
    return ChatRoom.builder()
        .id(20L)
        .roomType(ChatRoomType.APPLICATION)
        .application(application)
        .title("캠페인")
        .build();
  }

  private static ChatParticipant participant(
      ChatRoom room, Member member, ChatRole role, long lastRead) {
    return ChatParticipant.builder()
        .id(member.getId() * 100)
        .room(room)
        .member(member)
        .role(role)
        .lastReadMessageId(lastRead)
        .build();
  }

  private ChatMessage message(Long id, Member sender) {
    return ChatMessage.builder()
        .id(id)
        .room(support)
        .sender(sender)
        .senderRole(sender.isAdmin() ? ChatRole.ADMIN : ChatRole.MEMBER)
        .body("메시지")
        .build();
  }
}

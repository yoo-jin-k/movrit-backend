package com.movrit.movrit.chat.service;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 브랜드·참여자·운영자 채팅을 처리합니다.
 *
 * <ul>
 *   <li>신청 대화방: 신청 한 건마다 하나. 그 신청의 참여자와 캠페인 브랜드, 그리고 운영자만 열 수 있습니다.
 *   <li>운영자 문의방: 회원마다 하나. 본인과 운영자만 열 수 있습니다.
 *   <li>운영자는 모든 방을 열람·참여할 수 있고, 처음 열 때 참여자로 추가됩니다.
 *   <li>새 메시지·읽음 변화는 실시간 이벤트(SSE, {@link ChatEventHub})로 알리고, 화면은 이벤트를 받으면 '마지막으로 받은 메시지 다음'을 다시
 *       요청합니다. 연결이 끊겨도 화면의 주기적 확인(폴링)으로 이어집니다. 방을 열면 읽음 위치를 최신으로 옮깁니다.
 *   <li>상대가 읽은 위치(othersLastReadId)를 함께 내려 내 메시지에 '읽음'을 표시합니다.
 *   <li>상대가 안 읽은 상태에서 첫 새 메시지가 오면 알림함에도 '새 메시지' 알림을 넣습니다(메시지마다 쌓이지 않게).
 *   <li>사진(JPG·PNG)·PDF를 첨부할 수 있고, 첨부 파일은 그 방에 들어갈 수 있는 사람만 받을 수 있습니다.
 * </ul>
 *
 * <p>권한이 없는 방은 존재를 드러내지 않도록 404로 응답합니다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {
  private static final String SUPPORT_TITLE = "운영자 문의";
  private static final int PAGE_SIZE = 50;

  private final ChatRoomRepository roomRepository;
  private final ChatParticipantRepository participantRepository;
  private final ChatMessageRepository messageRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final MemberService memberService;
  private final MemberRepository memberRepository;
  private final NotificationService notificationService;
  private final ChatEventHub eventHub;
  private final ChatAttachmentStorage attachmentStorage;

  // ───────── 목록 ─────────

  /** 내 채팅 목록입니다. 운영자는 모든 방을, 그 외 회원은 참여한 방만 최근 대화 순으로 봅니다. */
  @Transactional(readOnly = true)
  public List<ChatDtos.RoomSummary> myRooms(Long memberId) {
    Member me = memberService.getMember(memberId);
    List<ChatRoom> rooms =
        me.isAdmin()
            ? roomRepository.findAllForAdmin()
            : participantRepository.findAllByMemberId(memberId).stream()
                .map(ChatParticipant::getRoom)
                .sorted(
                    Comparator.comparing(
                            ChatRoom::getLastMessageAt,
                            Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()))
                        .thenComparing(ChatRoom::getId, Comparator.reverseOrder()))
                .toList();
    return rooms.stream().map(room -> summary(room, me)).toList();
  }

  /** 안 읽은 메시지 총수입니다(헤더 배지용). 운영자는 자신이 참여한 방만 셉니다. */
  @Transactional(readOnly = true)
  public ChatDtos.UnreadCount unreadCount(Long memberId) {
    long total =
        participantRepository.findAllByMemberId(memberId).stream()
            .mapToLong(
                p ->
                    messageRepository.countUnread(
                        p.getRoom().getId(), p.getLastReadMessageId(), memberId))
            .sum();
    return new ChatDtos.UnreadCount(total);
  }

  // ───────── 방 열기 ─────────

  /**
   * 신청 대화방을 엽니다. 없으면 만들고 참여자·브랜드를 함께 넣습니다.
   *
   * @throws ResponseStatusException 신청이 없거나, 그 신청의 참여자·브랜드·운영자가 아니면 404
   */
  @Transactional
  public ChatDtos.Room openApplicationRoom(Long memberId, Long applicationId) {
    Member me = memberService.getMember(memberId);
    CampaignApplication application =
        applicationRepository.findById(applicationId).orElseThrow(ChatService::notFound);
    Member creator = application.getCreator().getMember();
    Member brand = application.getCampaign().getBrand().getSeller().getMember();
    if (!me.isAdmin() && !memberId.equals(creator.getId()) && !memberId.equals(brand.getId())) {
      throw notFound();
    }
    ChatRoom room =
        roomRepository
            .findByApplicationId(applicationId)
            .orElseGet(
                () ->
                    roomRepository.save(
                        ChatRoom.builder()
                            .roomType(ChatRoomType.APPLICATION)
                            .application(application)
                            .title(application.getCampaign().getTitle())
                            .build()));
    join(room, creator, ChatRole.CREATOR);
    join(room, brand, ChatRole.BRAND);
    if (me.isAdmin()) join(room, me, ChatRole.ADMIN);
    return messages(memberId, room.getId(), null);
  }

  /** 내 운영자 문의방을 엽니다. 없으면 만듭니다. 운영자 본인은 문의방을 만들 수 없습니다(400). */
  @Transactional
  public ChatDtos.Room openSupportRoom(Long memberId) {
    Member me = memberService.getMember(memberId);
    if (me.isAdmin()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "운영자는 채팅 목록에서 회원의 문의방을 열어 답변해 주세요.");
    }
    ChatRoom room =
        roomRepository
            .findByRoomTypeAndOwnerId(ChatRoomType.SUPPORT, memberId)
            .orElseGet(
                () ->
                    roomRepository.save(
                        ChatRoom.builder()
                            .roomType(ChatRoomType.SUPPORT)
                            .owner(me)
                            .title(SUPPORT_TITLE)
                            .build()));
    join(room, me, ChatRole.MEMBER);
    return messages(memberId, room.getId(), null);
  }

  // ───────── 메시지 ─────────

  /**
   * 방의 메시지를 가져옵니다.
   *
   * <ul>
   *   <li>afterId가 있으면 그 다음 메시지만(새 메시지 확인용) 줍니다.
   *   <li>beforeId가 있으면 그보다 이전 메시지 50개를 줍니다('이전 메시지 더 보기').
   *   <li>둘 다 없으면 최근 50개를 줍니다.
   * </ul>
   *
   * <p>최신 쪽을 받은 경우 그 위치까지 읽음 처리하고, 읽음 위치가 앞으로 움직였으면 다른 참여자에게 '읽음' 이벤트를 보냅니다.
   *
   * @throws ResponseStatusException 방이 없거나 열 권한이 없으면 404
   */
  @Transactional
  public ChatDtos.Room messages(Long memberId, Long roomId, Long afterId, Long beforeId) {
    Member me = memberService.getMember(memberId);
    ChatRoom room = accessibleRoom(me, roomId);
    List<ChatMessage> messages;
    boolean hasMore = false;
    boolean older = beforeId != null && beforeId > 0;
    if (older || afterId == null || afterId <= 0) {
      // 최신순으로 51개를 받아 50개만 쓰고, 남는 1개로 '이전 메시지가 더 있는지' 판단한 뒤 오래된 순으로 뒤집습니다.
      List<ChatMessage> page =
          new ArrayList<>(
              older
                  ? messageRepository.findTop51ByRoomIdAndIdLessThanOrderByIdDesc(roomId, beforeId)
                  : messageRepository.findTop51ByRoomIdOrderByIdDesc(roomId));
      hasMore = page.size() > PAGE_SIZE;
      messages = new ArrayList<>(page.subList(0, Math.min(page.size(), PAGE_SIZE)));
      Collections.reverse(messages);
    } else {
      messages = messageRepository.findTop200ByRoomIdAndIdGreaterThanOrderByIdAsc(roomId, afterId);
    }
    ChatParticipant mine = participant(room, me);
    if (!older && !messages.isEmpty()) {
      long before = mine.getLastReadMessageId();
      mine.readUpTo(messages.get(messages.size() - 1).getId());
      if (mine.getLastReadMessageId() > before) publishRead(room, me);
    }
    return new ChatDtos.Room(
        room.getId(),
        room.getRoomType(),
        room.getTitle(),
        room.getApplication() == null ? null : room.getApplication().getId(),
        mine.getRole(),
        memberLabels(room),
        messages.stream().map(message -> view(message, me)).toList(),
        othersLastRead(room, me),
        hasMore);
  }

  /** 새 메시지 확인·처음 열기용(이전 메시지 없이)입니다. */
  @Transactional
  public ChatDtos.Room messages(Long memberId, Long roomId, Long afterId) {
    return messages(memberId, roomId, afterId, null);
  }

  /**
   * 메시지를 보냅니다. 앞뒤 공백을 지운 1~2,000자만 받습니다.
   *
   * @throws ResponseStatusException 방이 없거나 권한이 없으면 404, 내용이 비었거나 너무 길면 400
   */
  @Transactional
  public ChatDtos.Message send(Long memberId, Long roomId, String body) {
    Member me = memberService.getMember(memberId);
    ChatRoom room = accessibleRoom(me, roomId);
    String text = body == null ? "" : body.strip();
    if (text.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지를 입력해 주세요.");
    requireLength(text);
    return post(me, room, text, null);
  }

  /**
   * 사진(JPG·PNG)이나 PDF를 첨부해 보냅니다. 함께 쓴 글(선택)도 2,000자까지 받습니다.
   *
   * @throws ResponseStatusException 방이 없거나 권한이 없으면 404, 형식·크기가 맞지 않으면 400
   */
  @Transactional
  public ChatDtos.Message sendAttachment(
      Long memberId, Long roomId, MultipartFile file, String body) {
    Member me = memberService.getMember(memberId);
    ChatRoom room = accessibleRoom(me, roomId);
    String text = body == null ? "" : body.strip();
    requireLength(text);
    ChatAttachmentStorage.Stored stored = attachmentStorage.store(file);
    try {
      return post(me, room, text, stored);
    } catch (RuntimeException exception) {
      attachmentStorage.delete(stored.key());
      throw exception;
    }
  }

  /** 검색 결과 최대 개수입니다. */
  private static final int SEARCH_LIMIT = 30;

  /**
   * 방 안에서 메시지를 검색합니다. 글 내용과 첨부 파일 이름에서 찾고, 최신순으로 30개까지 돌려줍니다.
   *
   * <p>검색어는 앞뒤 공백을 지운 1~50자만 받습니다. 검색만으로는 읽음 위치를 바꾸지 않습니다.
   *
   * @throws ResponseStatusException 방이 없거나 권한이 없으면 404, 검색어가 비었거나 너무 길면 400
   */
  @Transactional(readOnly = true)
  public List<ChatDtos.Message> search(Long memberId, Long roomId, String query) {
    Member me = memberService.getMember(memberId);
    accessibleRoom(me, roomId);
    String keyword = query == null ? "" : query.strip();
    if (keyword.isEmpty() || keyword.length() > 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 1~50자로 입력해 주세요.");
    }
    // %, _, \ 를 글자 그대로 찾도록 감쌉니다.
    String escaped =
        keyword
            .toLowerCase(java.util.Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return messageRepository
        .search(
            roomId,
            "%" + escaped + "%",
            org.springframework.data.domain.PageRequest.of(0, SEARCH_LIMIT))
        .stream()
        .map(message -> view(message, me))
        .toList();
  }

  /**
   * '입력 중' 신호를 방의 다른 참여자 화면에 보냅니다. 저장하지 않는 일회성 신호입니다.
   *
   * <p>화면은 글자를 입력하는 동안 3초에 한 번 정도만 보내고, 받는 쪽은 5초 동안 신호가 없으면 표시를 지웁니다.
   *
   * @throws ResponseStatusException 방이 없거나 권한이 없으면 404
   */
  @Transactional(readOnly = true)
  public void typing(Long memberId, Long roomId) {
    Member me = memberService.getMember(memberId);
    ChatRoom room = accessibleRoom(me, roomId);
    // 입력 신호만으로 운영자를 방 참여자로 추가하지 않도록, 참여 기록이 없으면 운영자로 표시합니다.
    ChatRole role =
        participantRepository
            .findByRoomIdAndMemberId(room.getId(), me.getId())
            .map(ChatParticipant::getRole)
            .orElse(ChatRole.ADMIN);
    eventHub.publish(
        participantRepository.findAllByRoomId(room.getId()).stream()
            .map(p -> p.getMember().getId())
            .filter(id -> !id.equals(me.getId()))
            .toList(),
        false,
        "typing",
        Map.of("roomId", room.getId(), "memberId", me.getId(), "name", senderName(room, role, me)));
  }

  /** 첨부 파일 정보입니다(내려받기 응답용). */
  public record AttachmentFile(java.nio.file.Path path, String name, String contentType) {}

  /** 첨부 파일을 돌려줍니다. 그 방에 들어갈 수 있는 사람만 받을 수 있습니다(아니면 404). */
  @Transactional
  public AttachmentFile attachment(Long memberId, Long roomId, Long messageId) {
    Member me = memberService.getMember(memberId);
    accessibleRoom(me, roomId);
    ChatMessage message =
        messageRepository
            .findById(messageId)
            .filter(value -> value.getRoom().getId().equals(roomId) && value.hasAttachment())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부 파일을 찾을 수 없습니다."));
    return new AttachmentFile(
        attachmentStorage.load(message.getAttachmentKey()),
        message.getAttachmentName(),
        message.getAttachmentType());
  }

  /** 메시지를 저장하고 방 시각·내 읽음 위치를 갱신한 뒤 알림과 실시간 이벤트를 보냅니다. */
  private ChatDtos.Message post(
      Member me, ChatRoom room, String text, ChatAttachmentStorage.Stored attachment) {
    ChatParticipant mine = participant(room, me);
    ChatMessage message =
        messageRepository.save(
            ChatMessage.builder()
                .room(room)
                .sender(me)
                .senderRole(mine.getRole())
                .body(text)
                .attachmentKey(attachment == null ? null : attachment.key())
                .attachmentName(attachment == null ? null : attachment.name())
                .attachmentType(attachment == null ? null : attachment.contentType())
                .attachmentSize(attachment == null ? null : attachment.size())
                .build());
    room.touch(message.getCreatedAt() == null ? LocalDateTime.now() : message.getCreatedAt());
    mine.readUpTo(message.getId());
    notifyRecipients(room, me, message);
    return view(message, me);
  }

  /**
   * 받는 사람에게 알림과 실시간 이벤트를 보냅니다.
   *
   * <ul>
   *   <li>알림함: 받는 사람이 이 방에서 안 읽은 메시지가 이번 1개뿐일 때만 넣습니다(대화마다 알림이 쌓이지 않게).
   *   <li>운영자 문의방에 아직 운영자가 들어오지 않았다면, 첫 메시지일 때 모든 운영자에게 알립니다.
   *   <li>실시간 이벤트: 방 참여자와 연결된 운영자 화면에 보냅니다(목록 갱신용).
   * </ul>
   */
  private void notifyRecipients(ChatRoom room, Member me, ChatMessage message) {
    List<ChatParticipant> participants = participantRepository.findAllByRoomId(room.getId());
    String preview =
        message.hasAttachment() && message.getBody().isBlank()
            ? "(첨부) " + message.getAttachmentName()
            : preview(message.getBody());
    for (ChatParticipant participant : participants) {
      Long memberId = participant.getMember().getId();
      if (memberId.equals(me.getId())) continue;
      long unread =
          messageRepository.countUnread(room.getId(), participant.getLastReadMessageId(), memberId);
      if (unread == 1) notifyMessage(memberId, room, preview);
    }
    boolean adminJoined = participants.stream().anyMatch(p -> p.getRole() == ChatRole.ADMIN);
    if (room.getRoomType() == ChatRoomType.SUPPORT
        && !adminJoined
        && messageRepository.countByRoomId(room.getId()) == 1) {
      memberRepository
          .findAllByRoleAndStatus(MemberRole.ADMIN, MemberStatus.ACTIVE)
          .forEach(admin -> notifyMessage(admin.getId(), room, preview));
    }
    eventHub.publish(
        participants.stream().map(p -> p.getMember().getId()).toList(),
        true,
        "message",
        Map.of("roomId", room.getId(), "messageId", message.getId()));
  }

  private void notifyMessage(Long memberId, ChatRoom room, String preview) {
    notificationService.send(
        memberId,
        "CHAT",
        "새 메시지가 도착했습니다.",
        room.getTitle() + " · " + preview,
        "CHAT_ROOM",
        room.getId());
  }

  /** 내가 읽은 위치가 바뀌었음을 다른 참여자 화면에 알립니다('읽음' 표시 갱신). */
  private void publishRead(ChatRoom room, Member me) {
    eventHub.publish(
        participantRepository.findAllByRoomId(room.getId()).stream()
            .map(p -> p.getMember().getId())
            .filter(id -> !id.equals(me.getId()))
            .toList(),
        false,
        "read",
        Map.of("roomId", room.getId()));
  }

  /** 나를 뺀 참여자들이 읽은 가장 마지막 메시지 번호입니다. */
  private long othersLastRead(ChatRoom room, Member me) {
    return participantRepository.findAllByRoomId(room.getId()).stream()
        .filter(p -> !p.getMember().getId().equals(me.getId()))
        .mapToLong(ChatParticipant::getLastReadMessageId)
        .max()
        .orElse(0L);
  }

  private static void requireLength(String text) {
    if (text.length() > 2000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지는 2,000자까지 보낼 수 있습니다.");
    }
  }

  // ───────── 내부 규칙 ─────────

  /** 참여자이거나 운영자일 때만 방을 열 수 있습니다. 운영자는 처음 열 때 참여자로 추가됩니다. */
  private ChatRoom accessibleRoom(Member me, Long roomId) {
    ChatRoom room = roomRepository.findById(roomId).orElseThrow(ChatService::notFound);
    boolean joined = participantRepository.findByRoomIdAndMemberId(roomId, me.getId()).isPresent();
    if (!joined && !me.isAdmin()) throw notFound();
    if (!joined) join(room, me, ChatRole.ADMIN);
    return room;
  }

  private ChatParticipant participant(ChatRoom room, Member me) {
    return participantRepository
        .findByRoomIdAndMemberId(room.getId(), me.getId())
        .orElseGet(() -> join(room, me, me.isAdmin() ? ChatRole.ADMIN : ChatRole.MEMBER));
  }

  /** 아직 참여하지 않았다면 참여자로 추가합니다. 이미 있으면 그대로 돌려줍니다. */
  private ChatParticipant join(ChatRoom room, Member member, ChatRole role) {
    return participantRepository
        .findByRoomIdAndMemberId(room.getId(), member.getId())
        .orElseGet(
            () ->
                participantRepository.save(
                    ChatParticipant.builder()
                        .room(room)
                        .member(member)
                        .role(role)
                        .lastReadMessageId(0L)
                        .build()));
  }

  private ChatDtos.RoomSummary summary(ChatRoom room, Member me) {
    ChatMessage last = messageRepository.findTopByRoomIdOrderByIdDesc(room.getId()).orElse(null);
    long unread =
        participantRepository
            .findByRoomIdAndMemberId(room.getId(), me.getId())
            .map(
                p ->
                    messageRepository.countUnread(
                        room.getId(), p.getLastReadMessageId(), me.getId()))
            .orElseGet(() -> messageRepository.countUnread(room.getId(), 0L, me.getId()));
    return new ChatDtos.RoomSummary(
        room.getId(),
        room.getRoomType(),
        room.getTitle(),
        counterpart(room, me),
        room.getApplication() == null ? null : room.getApplication().getId(),
        last == null
            ? null
            : last.hasAttachment() && last.getBody().isBlank()
                ? "(첨부) " + last.getAttachmentName()
                : preview(last.getBody()),
        room.getLastMessageAt(),
        unread);
  }

  /** 목록에 보여줄 상대 표시입니다. 참여자에게는 브랜드, 브랜드에게는 참여자, 운영자에게는 양쪽을 보여줍니다. */
  private String counterpart(ChatRoom room, Member me) {
    if (room.getRoomType() == ChatRoomType.SUPPORT) {
      return me.isAdmin() ? "회원 · " + displayName(room.getOwner()) : "운영자";
    }
    CampaignApplication application = room.getApplication();
    String brand = "브랜드 · " + application.getCampaign().getBrand().getName();
    String creator = "참여자 · " + application.getCreator().getNickname();
    Long creatorMemberId = application.getCreator().getMember().getId();
    if (me.isAdmin()) return creator + " / " + brand;
    return me.getId().equals(creatorMemberId) ? brand : creator;
  }

  private List<String> memberLabels(ChatRoom room) {
    return participantRepository.findAllByRoomId(room.getId()).stream()
        .map(
            p ->
                p.getRole() == ChatRole.ADMIN
                    ? "운영자"
                    : roleLabel(p.getRole()) + " " + senderName(room, p.getRole(), p.getMember()))
        .distinct()
        .toList();
  }

  private ChatDtos.Message view(ChatMessage message, Member me) {
    Member sender = message.getSender();
    return new ChatDtos.Message(
        message.getId(),
        message.getSenderRole(),
        senderName(message.getRoom(), message.getSenderRole(), sender),
        sender != null && Objects.equals(sender.getId(), me.getId()),
        message.getBody(),
        message.getCreatedAt(),
        message.hasAttachment()
            ? new ChatDtos.Attachment(
                message.getAttachmentName(),
                message.getAttachmentType(),
                message.getAttachmentSize() == null ? 0 : message.getAttachmentSize(),
                message.getAttachmentType() != null
                    && message.getAttachmentType().startsWith("image/"))
            : null);
  }

  /** 보낸 사람 이름입니다. 브랜드는 브랜드 이름, 참여자는 활동 닉네임, 운영자는 '운영자'로 보여줍니다. */
  private static String senderName(ChatRoom room, ChatRole role, Member sender) {
    return switch (role) {
      case ADMIN -> "운영자";
      case BRAND ->
          room.getApplication() != null
              ? room.getApplication().getCampaign().getBrand().getName()
              : displayName(sender);
      case CREATOR ->
          room.getApplication() != null
              ? room.getApplication().getCreator().getNickname()
              : displayName(sender);
      case MEMBER -> displayName(sender);
    };
  }

  private static String roleLabel(ChatRole role) {
    return switch (role) {
      case CREATOR -> "참여자";
      case BRAND -> "브랜드";
      case ADMIN -> "운영자";
      case MEMBER -> "회원";
    };
  }

  private static String displayName(Member member) {
    if (member == null) return "탈퇴한 회원";
    if (member.getNickname() != null && !member.getNickname().isBlank())
      return member.getNickname();
    return member.getName() == null ? "회원" : member.getName();
  }

  private static String preview(String body) {
    return body.length() > 60 ? body.substring(0, 60) + "…" : body;
  }

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다.");
  }
}

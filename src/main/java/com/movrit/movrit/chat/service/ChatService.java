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
import com.movrit.movrit.member.service.MemberService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 브랜드·참여자·운영자 채팅을 처리합니다.
 *
 * <ul>
 *   <li>신청 대화방: 신청 한 건마다 하나. 그 신청의 참여자와 캠페인 브랜드, 그리고 운영자만 열 수 있습니다.
 *   <li>운영자 문의방: 회원마다 하나. 본인과 운영자만 열 수 있습니다.
 *   <li>운영자는 모든 방을 열람·참여할 수 있고, 처음 열 때 참여자로 추가됩니다.
 *   <li>화면은 몇 초마다 '마지막으로 받은 메시지 다음'을 다시 요청해 새 메시지를 받습니다(폴링). 방을 열면 읽음 위치를 최신으로 옮깁니다.
 * </ul>
 *
 * <p>권한이 없는 방은 존재를 드러내지 않도록 404로 응답합니다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {
  private static final String SUPPORT_TITLE = "운영자 문의";

  private final ChatRoomRepository roomRepository;
  private final ChatParticipantRepository participantRepository;
  private final ChatMessageRepository messageRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final MemberService memberService;

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
   * 방의 메시지를 가져옵니다. afterId가 있으면 그 다음 메시지만(새 메시지 확인용), 없으면 최근 200개를 줍니다. 받은 곳까지 읽음 처리합니다.
   *
   * @throws ResponseStatusException 방이 없거나 열 권한이 없으면 404
   */
  @Transactional
  public ChatDtos.Room messages(Long memberId, Long roomId, Long afterId) {
    Member me = memberService.getMember(memberId);
    ChatRoom room = accessibleRoom(me, roomId);
    List<ChatMessage> messages;
    if (afterId == null || afterId <= 0) {
      // 최근 200개를 최신순으로 받아 화면 표시용(오래된 순)으로 뒤집습니다.
      messages = new ArrayList<>(messageRepository.findTop200ByRoomIdOrderByIdDesc(roomId));
      Collections.reverse(messages);
    } else {
      messages = messageRepository.findTop200ByRoomIdAndIdGreaterThanOrderByIdAsc(roomId, afterId);
    }
    ChatParticipant mine = participant(room, me);
    if (!messages.isEmpty()) mine.readUpTo(messages.get(messages.size() - 1).getId());
    return new ChatDtos.Room(
        room.getId(),
        room.getRoomType(),
        room.getTitle(),
        room.getApplication() == null ? null : room.getApplication().getId(),
        mine.getRole(),
        memberLabels(room),
        messages.stream().map(message -> view(message, me)).toList());
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
    if (text.length() > 2000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지는 2,000자까지 보낼 수 있습니다.");
    }
    ChatParticipant mine = participant(room, me);
    ChatMessage message =
        messageRepository.save(
            ChatMessage.builder()
                .room(room)
                .sender(me)
                .senderRole(mine.getRole())
                .body(text)
                .build());
    room.touch(message.getCreatedAt() == null ? LocalDateTime.now() : message.getCreatedAt());
    mine.readUpTo(message.getId());
    return view(message, me);
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
        last == null ? null : preview(last.getBody()),
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
        .map(p -> roleLabel(p.getRole()) + " · " + senderName(room, p.getRole(), p.getMember()))
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
        message.getCreatedAt());
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

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
 *   <li>운영자는 모든 방을 열람할 수 있고, 열람만으로는 참여자가 되지 않습니다. 메시지를 보낼 때 참여자로 추가됩니다.
 *   <li>이용 정지 등으로 ACTIVE가 아닌 회원은 방 열기·메시지 보내기·입력 중 신호가 막힙니다(403).
 *   <li>새 메시지·읽음 변화는 실시간 이벤트(SSE, {@link ChatEventHub})로 알리고, 화면은 이벤트를 받으면 '마지막으로 받은 메시지 다음'을 다시
 *       요청합니다. 연결이 끊겨도 화면의 주기적 확인(폴링)으로 이어집니다. 방을 열면 읽음 위치를 최신으로 옮깁니다.
 *   <li>상대가 읽은 위치(othersLastReadId)를 함께 내려 내 메시지에 '읽음'을 표시합니다.
 *   <li>상대가 안 읽은 상태에서 첫 새 메시지가 오면 알림함에도 '새 메시지' 알림을 넣습니다(메시지마다 쌓이지 않게).
 *   <li>사진(JPG·PNG)·PDF를 첨부할 수 있고, 첨부 파일은 그 방에 들어갈 수 있는 사람만 받을 수 있습니다.
 * </ul>
 *
 * <p>권한이 없는 방은 존재를 드러내지 않도록 404로 응답합니다.
 *
 * <p>변경 이력:
 *
 * <ul>
 *   <li>목록 미리보기에 운영자가 가린 메시지 원문이 보이던 것 → 안내 문구로 보여줌.
 *   <li>운영자가 열람·검색·입력 신호·첨부 받기만 해도 참여자로 추가되던 것 → 메시지를 보낼 때만 추가. 신청 대화방에서는 운영자의 읽음을 상대 '읽음' 표시에 쓰지
 *       않음.
 *   <li>보내기만 해도 내 읽음 위치가 내 메시지까지 넘어가 동시에 온 상대 메시지를 놓칠 수 있던 것 → 보내기는 읽음 위치를 옮기지 않음.
 *   <li>읽음 위치를 메모리 값으로 덮어써 동시 요청에서 뒤로 갈 수 있던 것 → DB에서 비교해 앞으로만 갱신.
 *   <li>동시에 처음 열면 방·참여자 중복 저장으로 500이 나던 것 → 없을 때만 넣고 다시 읽음.
 *   <li>이용 정지 회원이 토큰 만료 전까지 채팅할 수 있던 것 → 방 열기·보내기·입력 신호·실시간 연결을 403으로 막음.
 *   <li>'읽음' 이벤트가 나를 빼고 보내져 같은 계정 다른 창의 안 읽은 수가 늦게 바뀌던 것 → 나에게도 보내고 memberId로 구분.
 * </ul>
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
    requireActive(me);
    // 두 사람이 동시에 처음 열어도 500이 나지 않도록, 없을 때만 넣고(충돌하면 건너뜀) 다시 읽습니다.
    ChatRoom room =
        roomRepository
            .findByApplicationId(applicationId)
            .orElseGet(
                () -> {
                  roomRepository.insertApplicationRoomIfAbsent(
                      applicationId, application.getCampaign().getTitle());
                  return roomRepository
                      .findByApplicationId(applicationId)
                      .orElseThrow(ChatService::notFound);
                });
    join(room, creator, ChatRole.CREATOR);
    join(room, brand, ChatRole.BRAND);
    // 운영자는 열람만으로 참여자가 되지 않습니다. 메시지를 보낼 때 참여자로 추가됩니다.
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
    requireActive(me);
    ChatRoom room =
        roomRepository
            .findByRoomTypeAndOwnerId(ChatRoomType.SUPPORT, memberId)
            .orElseGet(
                () -> {
                  roomRepository.insertSupportRoomIfAbsent(memberId, SUPPORT_TITLE);
                  return roomRepository
                      .findByRoomTypeAndOwnerId(ChatRoomType.SUPPORT, memberId)
                      .orElseThrow(ChatService::notFound);
                });
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
    // 열람만 한 운영자는 참여 기록이 없으므로 읽음 위치를 남기지 않습니다.
    ChatParticipant mine =
        participantRepository.findByRoomIdAndMemberId(room.getId(), me.getId()).orElse(null);
    if (mine != null && !older && !messages.isEmpty()) {
      int moved =
          participantRepository.advanceRead(
              mine.getId(), messages.get(messages.size() - 1).getId());
      if (moved > 0) publishRead(room, me, mine.getRole());
    }
    return new ChatDtos.Room(
        room.getId(),
        room.getRoomType(),
        room.getTitle(),
        room.getApplication() == null ? null : room.getApplication().getId(),
        mine == null ? ChatRole.ADMIN : mine.getRole(),
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
    requireActive(me);
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
    requireActive(me);
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
    requireActive(me);
    // 입력 신호만으로 운영자를 방 참여자로 추가하지 않습니다. 참여 기록이 없으면 운영자로 표시합니다.
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

  /** 신고 대상 확인 결과입니다(작성자 회원 ID와 신고 당시 내용). */
  public record ReportTarget(Long ownerMemberId, String snapshot) {}

  /**
   * 신고할 수 있는 메시지인지 확인하고 작성자와 내용을 돌려줍니다. 그 방 참여자만 신고할 수 있고, 내 메시지는 신고할 수 없습니다.
   *
   * @throws ResponseStatusException 볼 수 없는 메시지면 404, 내 메시지면 400
   */
  @Transactional(readOnly = true)
  public ReportTarget reportTarget(Long memberId, Long messageId) {
    ChatMessage message =
        messageRepository
            .findById(messageId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."));
    boolean joined =
        participantRepository
            .findByRoomIdAndMemberId(message.getRoom().getId(), memberId)
            .isPresent();
    if (!joined) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다.");
    Long owner = message.getSender() == null ? null : message.getSender().getId();
    if (memberId.equals(owner)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내 메시지는 신고할 수 없습니다.");
    }
    String text =
        message.getBody()
            + (message.hasAttachment() ? " [첨부: " + message.getAttachmentName() + "]" : "");
    return new ReportTarget(owner, text.length() > 1900 ? text.substring(0, 1900) : text);
  }

  /** 운영자가 신고 처리로 메시지를 가리고, 방 참여자 화면이 바로 바뀌도록 이벤트를 보냅니다. */
  @Transactional
  public void hideMessage(Long messageId) {
    ChatMessage message =
        messageRepository
            .findById(messageId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."));
    message.hideByAdmin();
    eventHub.publish(
        participantRepository.findAllByRoomId(message.getRoom().getId()).stream()
            .map(p -> p.getMember().getId())
            .toList(),
        false,
        "hidden",
        Map.of("roomId", message.getRoom().getId(), "messageId", message.getId()));
  }

  /** 첨부 파일 정보입니다(내려받기 응답용). */
  public record AttachmentFile(java.nio.file.Path path, String name, String contentType) {}

  /** 첨부 파일을 돌려줍니다. 그 방에 들어갈 수 있는 사람만 받을 수 있습니다(아니면 404). */
  @Transactional(readOnly = true)
  public AttachmentFile attachment(Long memberId, Long roomId, Long messageId) {
    Member me = memberService.getMember(memberId);
    accessibleRoom(me, roomId);
    ChatMessage message =
        messageRepository
            .findById(messageId)
            .filter(
                value ->
                    value.getRoom().getId().equals(roomId)
                        && value.hasAttachment()
                        && !value.isHidden())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부 파일을 찾을 수 없습니다."));
    return new AttachmentFile(
        attachmentStorage.load(message.getAttachmentKey()),
        message.getAttachmentName(),
        message.getAttachmentType());
  }

  /**
   * 메시지를 저장하고 방 시각을 갱신한 뒤 알림과 실시간 이벤트를 보냅니다.
   *
   * <p>보낸 사람의 읽음 위치는 옮기지 않습니다. 동시에 저장된 상대 메시지(번호가 더 작은데 늦게 커밋된 것)를 받기 전에 읽음으로 넘기지 않도록, 읽음은 메시지를 실제로
   * 받아 간 범위(메시지 조회)에서만 옮깁니다. 내 메시지는 안 읽은 수에 들어가지 않습니다.
   */
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

  /**
   * 내가 읽은 위치가 바뀌었음을 알립니다({roomId, memberId}).
   *
   * <ul>
   *   <li>다른 참여자: '읽음' 표시 갱신용. 신청 대화방에서 운영자가 읽은 것은 상대의 '읽음' 표시에 쓰지 않으므로 보내지 않습니다.
   *   <li>나 자신: 같은 계정의 다른 창이 안 읽은 수를 바로 갱신하도록 함께 보냅니다(memberId로 내 읽음인지 구분).
   * </ul>
   */
  private void publishRead(ChatRoom room, Member me, ChatRole myRole) {
    List<Long> recipients =
        countsAsRead(room, myRole)
            ? participantRepository.findAllByRoomId(room.getId()).stream()
                .map(p -> p.getMember().getId())
                .toList()
            : List.of(me.getId());
    eventHub.publish(
        recipients, false, "read", Map.of("roomId", room.getId(), "memberId", me.getId()));
  }

  /**
   * 나를 뺀 참여자들이 읽은 가장 마지막 메시지 번호입니다.
   *
   * <p>신청 대화방에서는 운영자가 읽은 것을 '읽음'으로 치지 않습니다(운영자 열람이 참여자·브랜드 사이의 읽음으로 보이지 않게). 운영자 문의방은 운영자가 대화 상대이므로
   * 그대로 셉니다.
   */
  private long othersLastRead(ChatRoom room, Member me) {
    return participantRepository.findAllByRoomId(room.getId()).stream()
        .filter(p -> !p.getMember().getId().equals(me.getId()))
        .filter(p -> countsAsRead(room, p.getRole()))
        .mapToLong(ChatParticipant::getLastReadMessageId)
        .max()
        .orElse(0L);
  }

  /** 이 역할의 읽음 위치를 상대의 '읽음' 표시에 쓰는지입니다. 신청 대화방의 운영자만 제외합니다. */
  private static boolean countsAsRead(ChatRoom room, ChatRole role) {
    return !(room.getRoomType() == ChatRoomType.APPLICATION && role == ChatRole.ADMIN);
  }

  /**
   * 메시지를 보내거나 방을 열 수 있는 정상 계정인지 확인합니다. 이용 정지 등으로 ACTIVE가 아니면 로그인 토큰이 남아 있어도 막습니다.
   *
   * @throws ResponseStatusException ACTIVE가 아니면 403
   */
  private static void requireActive(Member me) {
    if (me.getStatus() != null && me.getStatus() != MemberStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이용이 제한된 계정이라 채팅을 이용할 수 없습니다.");
    }
  }

  /**
   * 실시간 이벤트 연결을 열 수 있는 회원인지 확인하고 운영자 여부를 돌려줍니다.
   *
   * @throws ResponseStatusException 회원이 없으면 404, ACTIVE가 아니면 403
   */
  @Transactional(readOnly = true)
  public boolean streamAccess(Long memberId) {
    Member me = memberService.getMember(memberId);
    requireActive(me);
    return me.isAdmin();
  }

  private static void requireLength(String text) {
    if (text.length() > 2000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지는 2,000자까지 보낼 수 있습니다.");
    }
  }

  // ───────── 내부 규칙 ─────────

  /** 참여자이거나 운영자일 때만 방을 열 수 있습니다. 운영자는 열람·검색·첨부 받기만으로는 참여자가 되지 않고, 메시지를 보낼 때 참여자로 추가됩니다. */
  private ChatRoom accessibleRoom(Member me, Long roomId) {
    ChatRoom room = roomRepository.findById(roomId).orElseThrow(ChatService::notFound);
    if (me.isAdmin()) return room;
    boolean joined = participantRepository.findByRoomIdAndMemberId(roomId, me.getId()).isPresent();
    if (!joined) throw notFound();
    return room;
  }

  private ChatParticipant participant(ChatRoom room, Member me) {
    return participantRepository
        .findByRoomIdAndMemberId(room.getId(), me.getId())
        .orElseGet(() -> join(room, me, me.isAdmin() ? ChatRole.ADMIN : ChatRole.MEMBER));
  }

  /**
   * 아직 참여하지 않았다면 참여자로 추가합니다. 이미 있으면 그대로 돌려줍니다.
   *
   * <p>같은 방을 동시에 처음 열어도 500이 나지 않도록, 없을 때만 넣고(충돌하면 건너뜀) 다시 읽습니다.
   */
  private ChatParticipant join(ChatRoom room, Member member, ChatRole role) {
    return participantRepository
        .findByRoomIdAndMemberId(room.getId(), member.getId())
        .orElseGet(
            () -> {
              participantRepository.insertIfAbsent(room.getId(), member.getId(), role.name());
              return participantRepository
                  .findByRoomIdAndMemberId(room.getId(), member.getId())
                  .orElseThrow(ChatService::notFound);
            });
  }

  /**
   * 목록 한 줄입니다. 운영자가 가린 마지막 메시지는 안내 문구로 보여줍니다.
   *
   * <p>안 읽은 수: 참여한 방은 내 읽음 위치 이후 개수입니다. 운영자가 참여하지 않은(열람만 한) 방은 읽음 위치가 없어 0으로 둡니다(헤더 배지와 같은 기준). 다만
   * 아직 어느 운영자도 답하지 않은 운영자 문의방은 답변 대기 표시로 전체 개수를 보여줍니다.
   */
  private ChatDtos.RoomSummary summary(ChatRoom room, Member me) {
    ChatMessage last = messageRepository.findTopByRoomIdOrderByIdDesc(room.getId()).orElse(null);
    long unread =
        participantRepository
            .findByRoomIdAndMemberId(room.getId(), me.getId())
            .map(
                p ->
                    messageRepository.countUnread(
                        room.getId(), p.getLastReadMessageId(), me.getId()))
            .orElseGet(
                () ->
                    awaitingAdmin(room)
                        ? messageRepository.countUnread(room.getId(), 0L, me.getId())
                        : 0L);
    return new ChatDtos.RoomSummary(
        room.getId(),
        room.getRoomType(),
        room.getTitle(),
        counterpart(room, me),
        room.getApplication() == null ? null : room.getApplication().getId(),
        last == null
            ? null
            : last.isHidden()
                ? HIDDEN_TEXT
                : last.hasAttachment() && last.getBody().isBlank()
                    ? "(첨부) " + last.getAttachmentName()
                    : preview(last.getBody()),
        room.getLastMessageAt(),
        unread);
  }

  /** 운영자가 한 명도 참여하지 않은 운영자 문의방인지(답변 대기)입니다. */
  private boolean awaitingAdmin(ChatRoom room) {
    return room.getRoomType() == ChatRoomType.SUPPORT
        && participantRepository.findAllByRoomId(room.getId()).stream()
            .noneMatch(p -> p.getRole() == ChatRole.ADMIN);
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

  /** 운영자가 가린 메시지에 보여줄 문구입니다. */
  static final String HIDDEN_TEXT = "운영 정책에 따라 운영자가 가린 메시지입니다.";

  private ChatDtos.Message view(ChatMessage message, Member me) {
    Member sender = message.getSender();
    if (message.isHidden()) {
      return new ChatDtos.Message(
          message.getId(),
          message.getSenderRole(),
          senderName(message.getRoom(), message.getSenderRole(), sender),
          sender != null && Objects.equals(sender.getId(), me.getId()),
          HIDDEN_TEXT,
          message.getCreatedAt(),
          null);
    }
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

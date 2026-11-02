package com.movrit.movrit.chat.controller;

import com.movrit.movrit.chat.dto.ChatDtos;
import com.movrit.movrit.chat.service.ChatEventHub;
import com.movrit.movrit.chat.service.ChatService;
import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.member.service.MemberService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 채팅 API입니다. 참여자·브랜드·운영자가 같은 주소를 쓰고, 볼 수 있는 방은 서비스가 로그인 회원 기준으로 정합니다.
 *
 * <ul>
 *   <li>GET /api/chats: 내 채팅 목록(운영자는 전체)
 *   <li>GET /api/chats/unread-count: 안 읽은 메시지 수
 *   <li>POST /api/chats/applications/{applicationId}: 신청 대화방 열기(없으면 만들기)
 *   <li>POST /api/chats/support: 운영자 문의방 열기(없으면 만들기)
 *   <li>GET /api/chats/{roomId}/messages?after=: 메시지 받기(after 다음 것만 받으면 새 메시지 확인)
 *   <li>POST /api/chats/{roomId}/messages: 메시지 보내기
 *   <li>GET /api/chats/{roomId}/messages?before=: 이전 메시지 50개
 *   <li>POST /api/chats/{roomId}/attachments: 사진·PDF·영상 첨부 보내기(multipart: file, body 선택)
 *   <li>GET /api/chats/{roomId}/search?q=: 방 안 메시지 검색(최신순 30개)
 *   <li>POST /api/chats/{roomId}/typing: '입력 중' 신호 보내기(저장하지 않음, 204)
 *   <li>GET /api/chats/{roomId}/attachments/{messageId}: 첨부 파일 받기(그 방에 들어갈 수 있는 사람만)
 *   <li>GET /api/chats/stream: 실시간 이벤트(SSE) 연결 — message·read·typing 이벤트로 새 메시지·읽음·입력 중을 알립니다
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {
  private final ChatService service;
  private final ChatEventHub eventHub;
  private final MemberService memberService;

  @GetMapping
  public List<ChatDtos.RoomSummary> rooms(@AuthenticationPrincipal Jwt jwt) {
    return service.myRooms(JwtMemberId.from(jwt));
  }

  @GetMapping("/unread-count")
  public ChatDtos.UnreadCount unreadCount(@AuthenticationPrincipal Jwt jwt) {
    return service.unreadCount(JwtMemberId.from(jwt));
  }

  @PostMapping("/applications/{applicationId}")
  public ChatDtos.Room openApplicationRoom(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.openApplicationRoom(JwtMemberId.from(jwt), applicationId);
  }

  @PostMapping("/support")
  public ChatDtos.Room openSupportRoom(@AuthenticationPrincipal Jwt jwt) {
    return service.openSupportRoom(JwtMemberId.from(jwt));
  }

  @GetMapping("/{roomId}/messages")
  public ChatDtos.Room messages(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long roomId,
      @RequestParam(name = "after", required = false) Long after,
      @RequestParam(name = "before", required = false) Long before) {
    return service.messages(JwtMemberId.from(jwt), roomId, after, before);
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@AuthenticationPrincipal Jwt jwt) {
    Long memberId = JwtMemberId.from(jwt);
    return eventHub.subscribe(memberId, memberService.getMember(memberId).isAdmin());
  }

  @PostMapping(value = "/{roomId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ChatDtos.Message sendAttachment(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long roomId,
      @RequestPart("file") MultipartFile file,
      @RequestParam(name = "body", required = false) String body) {
    return service.sendAttachment(JwtMemberId.from(jwt), roomId, file, body);
  }

  @GetMapping("/{roomId}/attachments/{messageId}")
  public ResponseEntity<Resource> attachment(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long messageId) {
    ChatService.AttachmentFile file = service.attachment(JwtMemberId.from(jwt), roomId, messageId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline()
                .filename(file.name(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(new FileSystemResource(file.path()));
  }

  @GetMapping("/{roomId}/search")
  public List<ChatDtos.Message> search(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long roomId,
      @RequestParam(name = "q", required = false) String query) {
    return service.search(JwtMemberId.from(jwt), roomId, query);
  }

  @PostMapping("/{roomId}/typing")
  public ResponseEntity<Void> typing(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
    service.typing(JwtMemberId.from(jwt), roomId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{roomId}/messages")
  public ChatDtos.Message send(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long roomId,
      @Valid @RequestBody ChatDtos.SendRequest request) {
    return service.send(JwtMemberId.from(jwt), roomId, request.body());
  }
}

package com.movrit.movrit.chat.controller;

import com.movrit.movrit.chat.dto.ChatDtos;
import com.movrit.movrit.chat.service.ChatService;
import com.movrit.movrit.common.security.JwtMemberId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {
  private final ChatService service;

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
      @RequestParam(name = "after", required = false) Long after) {
    return service.messages(JwtMemberId.from(jwt), roomId, after);
  }

  @PostMapping("/{roomId}/messages")
  public ChatDtos.Message send(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long roomId,
      @Valid @RequestBody ChatDtos.SendRequest request) {
    return service.send(JwtMemberId.from(jwt), roomId, request.body());
  }
}

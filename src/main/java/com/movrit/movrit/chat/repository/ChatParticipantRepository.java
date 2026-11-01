package com.movrit.movrit.chat.repository;

import com.movrit.movrit.chat.entity.ChatParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 채팅방 참여자와 읽음 위치를 조회합니다. */
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

  Optional<ChatParticipant> findByRoomIdAndMemberId(Long roomId, Long memberId);

  /** 내가 참여한 방 목록입니다(방 정보를 함께 읽습니다). */
  @EntityGraph(attributePaths = "room")
  List<ChatParticipant> findAllByMemberId(Long memberId);

  @EntityGraph(attributePaths = "member")
  List<ChatParticipant> findAllByRoomId(Long roomId);
}

package com.movrit.movrit.chat.repository;

import com.movrit.movrit.chat.entity.ChatParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 채팅방 참여자와 읽음 위치를 조회합니다. */
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

  Optional<ChatParticipant> findByRoomIdAndMemberId(Long roomId, Long memberId);

  /** 내가 참여한 방 목록입니다(방 정보를 함께 읽습니다). */
  @EntityGraph(attributePaths = "room")
  List<ChatParticipant> findAllByMemberId(Long memberId);

  @EntityGraph(attributePaths = "member")
  List<ChatParticipant> findAllByRoomId(Long roomId);

  /**
   * 참여자로 추가합니다. 같은 방·회원 기록이 이미 있으면(동시에 처음 연 경우 포함) 아무것도 하지 않습니다.
   *
   * @return 새로 추가했으면 1, 이미 있었으면 0입니다.
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          "insert into chat_participant"
              + " (room_id, member_id, participant_role, last_read_message_id, created_at, updated_at)"
              + " values (:roomId, :memberId, :role, 0, now(), now())"
              + " on conflict (room_id, member_id) do nothing",
      nativeQuery = true)
  int insertIfAbsent(
      @Param("roomId") Long roomId, @Param("memberId") Long memberId, @Param("role") String role);

  /**
   * 읽음 위치를 앞으로만 옮깁니다. DB에서 비교해 바꾸므로 동시에 읽어도 뒤로 돌아가지 않습니다.
   *
   * @return 위치가 앞으로 움직였으면 1, 그대로면 0입니다.
   */
  @Modifying
  @Query(
      value =
          "update chat_participant set last_read_message_id = :messageId, updated_at = now()"
              + " where id = :participantId and last_read_message_id < :messageId",
      nativeQuery = true)
  int advanceRead(@Param("participantId") Long participantId, @Param("messageId") long messageId);
}

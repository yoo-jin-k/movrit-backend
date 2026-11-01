package com.movrit.movrit.chat.repository;

import com.movrit.movrit.chat.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 채팅 메시지를 조회합니다. */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  /** 기준 번호 다음 메시지를 오래된 순으로 최대 200개 가져옵니다(처음 열 때는 0부터). */
  @EntityGraph(attributePaths = "sender")
  List<ChatMessage> findTop200ByRoomIdAndIdGreaterThanOrderByIdAsc(Long roomId, Long afterId);

  /** 처음 열 때 보여줄 최근 메시지 200개입니다(최신순으로 가져와 화면에서 뒤집습니다). */
  @EntityGraph(attributePaths = "sender")
  List<ChatMessage> findTop200ByRoomIdOrderByIdDesc(Long roomId);

  Optional<ChatMessage> findTopByRoomIdOrderByIdDesc(Long roomId);

  /** 내가 읽은 위치 이후에 다른 사람이 보낸 메시지 수입니다. */
  @Query(
      "select count(m) from ChatMessage m where m.room.id = :roomId and m.id > :afterId"
          + " and (m.sender is null or m.sender.id <> :memberId)")
  long countUnread(
      @Param("roomId") Long roomId,
      @Param("afterId") long afterId,
      @Param("memberId") Long memberId);
}

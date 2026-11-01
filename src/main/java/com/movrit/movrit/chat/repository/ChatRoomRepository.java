package com.movrit.movrit.chat.repository;

import com.movrit.movrit.chat.entity.ChatRoom;
import com.movrit.movrit.chat.entity.ChatRoomType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 채팅방을 조회합니다. */
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  Optional<ChatRoom> findByApplicationId(Long applicationId);

  Optional<ChatRoom> findByRoomTypeAndOwnerId(ChatRoomType roomType, Long ownerId);

  /** 운영자용 전체 목록입니다. 최근 대화가 있는 방부터, 대화가 없는 방은 뒤에 둡니다. */
  @Query("select r from ChatRoom r order by r.lastMessageAt desc nulls last, r.id desc")
  List<ChatRoom> findAllForAdmin();
}

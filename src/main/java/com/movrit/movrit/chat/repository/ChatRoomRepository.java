package com.movrit.movrit.chat.repository;

import com.movrit.movrit.chat.entity.ChatRoom;
import com.movrit.movrit.chat.entity.ChatRoomType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 채팅방을 조회합니다. */
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  Optional<ChatRoom> findByApplicationId(Long applicationId);

  Optional<ChatRoom> findByRoomTypeAndOwnerId(ChatRoomType roomType, Long ownerId);

  /** 운영자용 전체 목록입니다. 최근 대화가 있는 방부터, 대화가 없는 방은 뒤에 둡니다. */
  @Query("select r from ChatRoom r order by r.lastMessageAt desc nulls last, r.id desc")
  List<ChatRoom> findAllForAdmin();

  /**
   * 신청 대화방을 만듭니다. 같은 신청의 방이 이미 있으면(동시에 처음 연 경우 포함) 아무것도 하지 않습니다.
   *
   * @return 새로 만들었으면 1, 이미 있었으면 0입니다.
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          "insert into chat_room (room_type, application_id, title, created_at, updated_at)"
              + " values ('APPLICATION', :applicationId, :title, now(), now())"
              + " on conflict do nothing",
      nativeQuery = true)
  int insertApplicationRoomIfAbsent(
      @Param("applicationId") Long applicationId, @Param("title") String title);

  /**
   * 회원의 운영자 문의방을 만듭니다. 이미 있으면(동시에 처음 연 경우 포함) 아무것도 하지 않습니다.
   *
   * @return 새로 만들었으면 1, 이미 있었으면 0입니다.
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          "insert into chat_room (room_type, owner_member_id, title, created_at, updated_at)"
              + " values ('SUPPORT', :ownerId, :title, now(), now())"
              + " on conflict do nothing",
      nativeQuery = true)
  int insertSupportRoomIfAbsent(@Param("ownerId") Long ownerId, @Param("title") String title);
}

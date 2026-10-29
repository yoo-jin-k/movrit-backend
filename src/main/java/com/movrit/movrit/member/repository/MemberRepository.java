package com.movrit.movrit.member.repository;

import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

  boolean existsByEmail(String email);

  Optional<Member> findByEmail(String email);

  boolean existsByNickname(String nickname);

  boolean existsByNicknameAndIdNot(String nickname, Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select member from Member member where member.id = :id")
  Optional<Member> findByIdForUpdate(@Param("id") Long id);
}

package com.movrit.movrit.member.repository;

import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 회원 계정을 조회합니다. 이메일·닉네임 중복 확인과 동시 수정 방지용 잠금 조회를 제공합니다. */
public interface MemberRepository extends JpaRepository<Member, Long> {

  /** 같은 이메일로 가입한 회원이 있는지 확인합니다. */
  boolean existsByEmail(String email);

  /** 이메일로 회원을 찾습니다. 로그인과 소셜 계정 연결에 사용합니다. */
  Optional<Member> findByEmail(String email);

  /** 같은 닉네임을 쓰는 회원이 있는지 확인합니다. */
  boolean existsByNickname(String nickname);

  /** 닉네임을 바꿀 때 본인을 제외하고 같은 닉네임이 있는지 확인합니다. */
  boolean existsByNicknameAndIdNot(String nickname, Long id);

  /** 회원 행을 쓰기 잠금으로 조회합니다. 비밀번호·사진처럼 동시에 바뀌면 안 되는 정보를 수정할 때 사용합니다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select member from Member member where member.id = :id")
  Optional<Member> findByIdForUpdate(@Param("id") Long id);
}

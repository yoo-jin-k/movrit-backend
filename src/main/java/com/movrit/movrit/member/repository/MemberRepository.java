package com.movrit.movrit.member.repository;

import com.movrit.movrit.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}

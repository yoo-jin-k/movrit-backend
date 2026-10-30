package com.movrit.movrit.memberaddress.repository;

import com.movrit.movrit.memberaddress.entity.MemberAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원 배송지 주소록을 조회합니다. 항상 회원 ID를 조건으로 넣어 본인 주소만 다룹니다. */
public interface MemberAddressRepository extends JpaRepository<MemberAddress, Long> {
  /** 기본 배송지를 맨 앞에, 나머지는 최근 등록순으로 정렬해 조회합니다. */
  List<MemberAddress> findAllByMemberIdOrderByDefaultAddressDescIdDesc(Long memberId);

  /** 주소 ID와 회원 ID가 모두 맞을 때만 찾습니다. 본인 주소만 다루도록 권한 확인을 겸합니다. */
  Optional<MemberAddress> findByIdAndMemberId(Long id, Long memberId);

  /** 회원의 기본 배송지를 찾습니다. */
  Optional<MemberAddress> findFirstByMemberIdAndDefaultAddressTrue(Long memberId);

  /** 기본 배송지를 삭제했을 때 대신 기본으로 지정할 가장 최근 주소를 찾습니다. */
  Optional<MemberAddress> findFirstByMemberIdOrderByIdDesc(Long memberId);

  /** 회원이 등록한 배송지 수를 셉니다. 첫 주소를 자동으로 기본 배송지로 정할 때 사용합니다. */
  long countByMemberId(Long memberId);
}

package com.movrit.movrit.memberaddress.repository;

import com.movrit.movrit.memberaddress.entity.MemberAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberAddressRepository extends JpaRepository<MemberAddress, Long> {
  List<MemberAddress> findAllByMemberIdOrderByDefaultAddressDescIdDesc(Long memberId);

  Optional<MemberAddress> findByIdAndMemberId(Long id, Long memberId);

  Optional<MemberAddress> findFirstByMemberIdAndDefaultAddressTrue(Long memberId);

  Optional<MemberAddress> findFirstByMemberIdOrderByIdDesc(Long memberId);

  long countByMemberId(Long memberId);
}

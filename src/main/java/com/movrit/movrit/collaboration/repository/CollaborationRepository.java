package com.movrit.movrit.collaboration.repository;

import com.movrit.movrit.collaboration.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {
  boolean existsByPreorderIdAndCreatorId(Long p, Long c);

  Optional<Collaboration> findByReferralCode(String code);

  List<Collaboration> findAllByCreatorMemberIdOrderByIdDesc(Long id);

  List<Collaboration> findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(Long id);
}

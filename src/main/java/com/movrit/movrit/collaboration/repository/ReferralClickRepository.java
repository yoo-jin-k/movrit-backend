package com.movrit.movrit.collaboration.repository;import com.movrit.movrit.collaboration.entity.ReferralClick;import org.springframework.data.jpa.repository.JpaRepository;
public interface ReferralClickRepository extends JpaRepository<ReferralClick,Long>{long countByCollaborationId(Long id);}

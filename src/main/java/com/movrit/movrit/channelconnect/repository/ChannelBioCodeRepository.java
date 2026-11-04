package com.movrit.movrit.channelconnect.repository;

import com.movrit.movrit.channelconnect.entity.ChannelBioCode;
import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 소개글 인증 코드 저장소입니다. */
public interface ChannelBioCodeRepository extends JpaRepository<ChannelBioCode, Long> {

  /** 회원·서비스의 가장 최근 코드입니다. */
  Optional<ChannelBioCode> findFirstByMemberIdAndProviderOrderByCreatedAtDescIdDesc(
      Long memberId, ChannelProvider provider);

  /** 하루 발급 횟수 제한을 확인할 때 씁니다. */
  long countByMemberIdAndProviderAndCreatedAtAfter(
      Long memberId, ChannelProvider provider, LocalDateTime since);

  boolean existsByCode(String code);
}

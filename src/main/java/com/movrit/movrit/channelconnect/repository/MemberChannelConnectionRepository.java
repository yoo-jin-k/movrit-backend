package com.movrit.movrit.channelconnect.repository;

import com.movrit.movrit.channelconnect.entity.ChannelConnectionMethod;
import com.movrit.movrit.channelconnect.entity.ChannelProvider;
import com.movrit.movrit.channelconnect.entity.MemberChannelConnection;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원의 SNS 채널 연결 기록을 조회·저장합니다. */
public interface MemberChannelConnectionRepository
    extends JpaRepository<MemberChannelConnection, Long> {

  List<MemberChannelConnection> findAllByMemberId(Long memberId);

  Optional<MemberChannelConnection> findByMemberIdAndProvider(
      Long memberId, ChannelProvider provider);

  /** 운영자 '채널 확인' 목록입니다. 확인 요청이 오래된 순서로 보여줍니다. */
  @EntityGraph(attributePaths = "member")
  List<MemberChannelConnection> findAllByConnectionMethodOrderByConnectedAtAsc(
      ChannelConnectionMethod method);

  /** 운영자 '채널 확인' 목록(직접 등록·소개글 코드)입니다. 확인 요청이 오래된 순서로 보여줍니다. */
  @EntityGraph(attributePaths = "member")
  List<MemberChannelConnection> findAllByConnectionMethodInOrderByConnectedAtAsc(
      Collection<ChannelConnectionMethod> methods);

  /** 같은 SNS 계정이 이미 다른 회원에게 연결되어 있는지 확인할 때 씁니다. */
  Optional<MemberChannelConnection> findByProviderAndExternalId(
      ChannelProvider provider, String externalId);
}

package com.movrit.movrit.auth.service;

import com.movrit.movrit.auth.dto.SocialAccountResponse;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원에게 연결된 소셜 로그인 계정 목록을 조회합니다. 설정 화면의 "로그인 연결" 영역에서 사용합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAccountService {

  private final MemberSocialAccountRepository socialAccountRepository;

  /** 로그인한 회원에게 현재 연결된 Google·카카오 계정 목록을 반환합니다. 회원 ID는 컨트롤러가 인증 토큰에서 꺼내 전달합니다. */
  public List<SocialAccountResponse> findByMemberId(Long memberId) {
    return socialAccountRepository.findAllByMemberId(memberId).stream()
        .map(SocialAccountResponse::from)
        .toList();
  }
}

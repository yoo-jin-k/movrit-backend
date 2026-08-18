package com.movrit.movrit.auth.service;

import com.movrit.movrit.auth.dto.SocialAccountResponse;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAccountService {

    private final MemberSocialAccountRepository socialAccountRepository;

    /** 로그인한 회원에게 현재 연결된 카카오/구글 계정을 보여준다. */
    public List<SocialAccountResponse> findByMemberId(Long memberId) {
        return socialAccountRepository.findAllByMemberId(memberId).stream()
                .map(SocialAccountResponse::from)
                .toList();
    }
}

package com.movrit.movrit.member.service;

import com.movrit.movrit.member.dto.MemberSignupRequest;
import com.movrit.movrit.member.dto.MemberSignupResponse;
import com.movrit.movrit.member.dto.MemberUpdateRequest;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberSignupResponse signup(MemberSignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (memberRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일이야.");
        }

        Member member = Member.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .phone(normalizePhone(request.phone()))
                .status(MemberStatus.ACTIVE)
                .build();

        return MemberSignupResponse.from(memberRepository.save(member));
    }

    public List<MemberSignupResponse> findAll() {
        return memberRepository.findAll().stream().map(MemberSignupResponse::from).toList();
    }

    public MemberSignupResponse findById(Long id) {
        return MemberSignupResponse.from(getMember(id));
    }

    @Transactional
    public MemberSignupResponse update(Long id, MemberUpdateRequest request) {
        Member member = getMember(id);
        member.updateProfile(request.name().trim(), normalizePhone(request.phone()));
        return MemberSignupResponse.from(member);
    }

    @Transactional
    public void delete(Long id) {
        getMember(id).withdraw();
    }

    public Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없어."));
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        return phone.trim();
    }
}

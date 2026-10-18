package com.movrit.movrit.member.service;

import com.movrit.movrit.member.dto.MemberSignupRequest;
import com.movrit.movrit.member.dto.MemberSignupResponse;
import com.movrit.movrit.member.dto.MemberUpdateRequest;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
    }

    Member member =
        Member.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .name(request.name().trim())
            .phone(normalizePhone(request.phone()))
            // 회원가입 요청에서 관리자 권한을 받을 수 없도록 서버가 USER로 고정합니다.
            .role(MemberRole.USER)
            .status(MemberStatus.ACTIVE)
            .build();

    return MemberSignupResponse.from(memberRepository.save(member));
  }

  public List<MemberSignupResponse> findAll() {
    return memberRepository.findAll().stream()
        .filter(member -> member.getDeletedAt() == null)
        .map(MemberSignupResponse::from)
        .toList();
  }

  public List<MemberSignupResponse> findAllForAdmin(Long adminId) {
    getAdmin(adminId);
    return findAll();
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
  public MemberSignupResponse updateStatus(Long adminId, Long id, MemberStatus status) {
    getAdmin(adminId);
    Member m = getMember(id);
    try {
      m.changeStatus(status);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
    return MemberSignupResponse.from(m);
  }

  @Transactional
  public void delete(Long id) {
    getMember(id).withdraw();
  }

  public Member getMember(Long id) {
    Member member =
        memberRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    if (member.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
    }
    return member;
  }

  public Member getAdmin(Long memberId) {
    Member member = getMember(memberId);
    if (!member.isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
    }
    return member;
  }

  private String normalizePhone(String phone) {
    if (phone == null || phone.isBlank()) return null;
    return phone.trim();
  }
}

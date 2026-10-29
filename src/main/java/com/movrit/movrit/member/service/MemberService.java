package com.movrit.movrit.member.service;

import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.member.dto.MemberPasswordCapabilityResponse;
import com.movrit.movrit.member.dto.MemberPasswordChangeRequest;
import com.movrit.movrit.member.dto.MemberSignupRequest;
import com.movrit.movrit.member.dto.MemberSignupResponse;
import com.movrit.movrit.member.dto.MemberUpdateRequest;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import java.nio.charset.StandardCharsets;
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
  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional
  public MemberSignupResponse signup(MemberSignupRequest request) {
    String email = request.email().trim().toLowerCase();
    if (memberRepository.existsByEmail(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
    }
    String nickname = MemberNicknamePolicy.normalize(request.nickname());
    if (memberRepository.existsByNickname(nickname)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
    }

    Member member =
        Member.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .name(request.name().trim())
            .nickname(nickname)
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
    String nickname = null;
    if (request.nickname() != null) {
      nickname = MemberNicknamePolicy.normalize(request.nickname());
      if (memberRepository.existsByNicknameAndIdNot(nickname, id)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
      }
    }
    member.updateProfile(request.name().trim(), normalizePhone(request.phone()), nickname);
    return MemberSignupResponse.from(member);
  }

  /** 현재 비밀번호를 다시 확인하고 모든 재발급 세션을 폐기한 뒤 비밀번호를 변경합니다. */
  @Transactional
  public void changePassword(Long memberId, MemberPasswordChangeRequest request) {
    Member member = getMemberForUpdate(memberId);
    if (member.getPasswordHash() == null || member.getPasswordHash().isBlank()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "PASSWORD_NOT_CONFIGURED");
    }
    if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_TOO_LONG_IN_BYTES");
    }
    if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG_IN_BYTES");
    }
    if (!passwordEncoder.matches(request.currentPassword(), member.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_MISMATCH");
    }
    if (passwordEncoder.matches(request.newPassword(), member.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "PASSWORD_REUSE");
    }
    member.changePassword(passwordEncoder.encode(request.newPassword()));
    refreshTokenRepository.findByMemberId(memberId).ifPresent(token -> token.revoke());
  }

  public MemberPasswordCapabilityResponse passwordCapability(Long memberId) {
    Member member = getMember(memberId);
    boolean hasPassword = member.getPasswordHash() != null && !member.getPasswordHash().isBlank();
    return new MemberPasswordCapabilityResponse(
        hasPassword, hasPassword, hasPassword ? null : "SOCIAL_ONLY_ACCOUNT");
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

  /** 기본 주소 변경 같은 회원 단위 직렬화가 필요한 작업에서 회원 행을 잠급니다. */
  public Member getMemberForUpdate(Long id) {
    Member member =
        memberRepository
            .findByIdForUpdate(id)
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

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

/** 회원 가입·정보 수정·비밀번호 변경·상태 관리를 담당하고, 다른 기능이 쓰는 회원 조회와 관리자 권한 확인을 제공합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * 이메일로 회원가입합니다. 이메일은 소문자로 맞춰 저장하고, 이미 가입된 이메일이나 사용 중인 닉네임이면 409입니다. 권한은 항상 일반 회원(USER)으로 고정합니다.
   */
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

  /** 전체 회원 목록을 반환하는 내부 조회입니다. 화면에서는 관리자 확인을 거치는 findAllForAdmin을 사용합니다. */
  public List<MemberSignupResponse> findAll() {
    return memberRepository.findAll().stream()
        .filter(member -> member.getDeletedAt() == null)
        .map(MemberSignupResponse::from)
        .toList();
  }

  /** 운영자가 전체 회원 목록을 조회합니다. 관리자가 아니면 거절됩니다. */
  public List<MemberSignupResponse> findAllForAdmin(Long adminId) {
    getAdmin(adminId);
    return findAll();
  }

  /** 회원 정보를 ID로 조회합니다. 없거나 탈퇴한 회원이면 404입니다. */
  public MemberSignupResponse findById(Long id) {
    return MemberSignupResponse.from(getMember(id));
  }

  /** 회원 이름·연락처·닉네임을 수정합니다. 닉네임은 규칙 검사 후 다른 회원과 겹치면 409입니다. */
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

  /**
   * 현재 비밀번호를 다시 확인한 뒤 새 비밀번호로 바꿉니다. 바꾼 뒤에는 로그인 유지 토큰을 폐기해 다른 기기도 다시 로그인하게 합니다. 소셜 전용 계정(비밀번호 없음)이나
   * 같은 비밀번호 재사용은 409, 현재 비밀번호가 틀리거나 72바이트를 넘으면 400입니다. 오류 사유는 화면이 문구를 고를 수 있게 코드값으로 돌려줍니다.
   */
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

  /** 비밀번호 변경 화면을 보여줄 수 있는지 알려줍니다. 비밀번호 없이 소셜로만 가입한 계정이면 SOCIAL_ONLY_ACCOUNT로 표시합니다. */
  public MemberPasswordCapabilityResponse passwordCapability(Long memberId) {
    Member member = getMember(memberId);
    boolean hasPassword = member.getPasswordHash() != null && !member.getPasswordHash().isBlank();
    return new MemberPasswordCapabilityResponse(
        hasPassword, hasPassword, hasPassword ? null : "SOCIAL_ONLY_ACCOUNT");
  }

  /** 운영자가 회원 상태(정상·정지·탈퇴 등)를 바꿉니다. 관리자만 할 수 있고, 관리자 계정을 정지하려 하면 409입니다. */
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

  /** 회원을 탈퇴 처리합니다(실제 데이터는 지우지 않고 상태와 탈퇴 시각을 기록). */
  @Transactional
  public void delete(Long id) {
    getMember(id).withdraw();
  }

  /** 다른 기능에서 쓰는 회원 조회입니다. 없거나 탈퇴한 회원이면 404를 냅니다. */
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

  /** 요청자가 운영자(ADMIN)인지 DB 기준으로 확인하고 회원을 반환합니다. 관리자가 아니면 403입니다. 운영자 기능은 모두 이 확인을 거칩니다. */
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

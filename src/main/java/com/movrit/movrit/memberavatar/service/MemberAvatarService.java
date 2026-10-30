package com.movrit.movrit.memberavatar.service;

import com.movrit.movrit.member.dto.MemberSignupResponse;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 회원 프로필 사진의 교체·삭제·공개 조회를 담당합니다. DB 변경이 확정(커밋)된 경우에만 실제 파일을 지워, 실패 시 기존 사진이 그대로 남게 합니다. */
@Service
@RequiredArgsConstructor
public class MemberAvatarService {
  private final MemberRepository memberRepository;
  private final MemberAvatarStorage storage;

  /**
   * 로그인한 회원의 프로필 사진을 새 사진으로 바꿉니다. 새 표시용 파일을 먼저 만든 뒤 회원 행을 잠그고 교체하며, 저장이 확정되면 이전 파일을 지우고 실패하면 새 파일을
   * 지웁니다.
   */
  @Transactional
  public MemberSignupResponse replace(Long memberId, MultipartFile file) {
    MemberAvatarStorage.StoredAvatar prepared = storage.store(file);
    Member member;
    try {
      member = getForUpdate(memberId);
    } catch (RuntimeException exception) {
      storage.delete(prepared.key());
      throw exception;
    }
    String previousKey = member.getAvatarKey();
    registerFileCompletion(prepared.key(), previousKey);
    member.changeAvatar(prepared.key(), prepared.contentType());
    memberRepository.flush();
    return MemberSignupResponse.from(member);
  }

  /** 로그인한 회원의 프로필 사진을 삭제합니다. DB 저장이 확정된 뒤에만 실제 파일을 지워 중간 실패 시 기존 사진을 유지합니다. */
  @Transactional
  public void delete(Long memberId) {
    Member member = getForUpdate(memberId);
    String previousKey = member.getAvatarKey();
    if (previousKey == null) return;
    registerFileCompletion(null, previousKey);
    member.removeAvatar();
    memberRepository.flush();
  }

  /**
   * 다른 사람에게 보여줄 프로필 사진 파일을 찾습니다. 탈퇴·비활성 회원이거나 요청한 버전이 현재 사진과 다르면 404입니다(교체·삭제 전 사진 주소는 더 이상 열리지
   * 않음).
   */
  @Transactional(readOnly = true)
  public AvatarFile findPublic(Long memberId, long version) {
    Member member =
        memberRepository
            .findById(memberId)
            .filter(candidate -> candidate.getDeletedAt() == null)
            .filter(candidate -> candidate.getStatus() == MemberStatus.ACTIVE)
            .orElseThrow(this::missing);
    if (member.getAvatarKey() == null || member.getAvatarVersion() != version) throw missing();
    Path path = storage.load(member.getAvatarKey());
    return new AvatarFile(path, member.getAvatarContentType(), member.getAvatarVersion());
  }

  private Member getForUpdate(Long memberId) {
    return memberRepository
        .findByIdForUpdate(memberId)
        .filter(member -> member.getDeletedAt() == null)
        .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
  }

  private void registerFileCompletion(String newKey, String previousKey) {
    // 트랜잭션 밖에서 호출되면 파일 정리 시점을 보장할 수 없으므로 새 파일을 지우고 중단합니다.
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      storage.delete(newKey);
      throw new IllegalStateException("프로필 사진 변경 트랜잭션이 필요합니다.");
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_COMMITTED) {
              if (previousKey != null && !previousKey.equals(newKey)) storage.delete(previousKey);
            } else {
              storage.delete(newKey);
            }
          }
        });
  }

  private ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "프로필 사진을 찾을 수 없습니다.");
  }

  /** 공개 조회 응답에 쓰는 사진 파일 경로·형식·버전 묶음입니다. */
  public record AvatarFile(Path path, String contentType, long version) {}
}

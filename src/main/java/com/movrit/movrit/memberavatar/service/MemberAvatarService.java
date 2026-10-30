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

@Service
@RequiredArgsConstructor
public class MemberAvatarService {
  private final MemberRepository memberRepository;
  private final MemberAvatarStorage storage;

  /** 새 표시본을 준비한 뒤 회원 행을 잠가 교체하고, 커밋된 경우에만 이전 파일을 삭제합니다. */
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

  /** 사진 삭제는 DB 커밋 후에만 실제 파일을 지워 롤백 시 기존 사진을 유지합니다. */
  @Transactional
  public void delete(Long memberId) {
    Member member = getForUpdate(memberId);
    String previousKey = member.getAvatarKey();
    if (previousKey == null) return;
    registerFileCompletion(null, previousKey);
    member.removeAvatar();
    memberRepository.flush();
  }

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

  public record AvatarFile(Path path, String contentType, long version) {}
}

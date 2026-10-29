package com.movrit.movrit.memberavatar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.memberavatar.service.MemberAvatarService;
import com.movrit.movrit.memberavatar.service.MemberAvatarStorage;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class MemberAvatarServiceTest {

  @AfterEach
  void clearSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void committedReplacementDeletesOnlyPreviousFile() {
    MemberRepository members = mock(MemberRepository.class);
    MemberAvatarStorage storage = mock(MemberAvatarStorage.class);
    Member member = activeMember("old.png", 3);
    when(storage.store(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new MemberAvatarStorage.StoredAvatar("new.png", "image/png"));
    when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
    TransactionSynchronizationManager.initSynchronization();

    var response =
        new MemberAvatarService(members, storage)
            .replace(1L, new MockMultipartFile("file", new byte[] {1}));
    complete(TransactionSynchronization.STATUS_COMMITTED);

    assertThat(response.avatarUrl()).isEqualTo("/api/members/1/avatar?v=4");
    verify(storage).delete("old.png");
    verify(storage, never()).delete("new.png");
  }

  @Test
  void rollbackDeletesNewFileAndPreservesPreviousFile() {
    MemberRepository members = mock(MemberRepository.class);
    MemberAvatarStorage storage = mock(MemberAvatarStorage.class);
    Member member = activeMember("old.png", 3);
    when(storage.store(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new MemberAvatarStorage.StoredAvatar("new.png", "image/png"));
    when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
    org.mockito.Mockito.doThrow(new IllegalStateException("db failure")).when(members).flush();
    TransactionSynchronizationManager.initSynchronization();

    assertThatThrownBy(
            () ->
                new MemberAvatarService(members, storage)
                    .replace(1L, new MockMultipartFile("file", new byte[] {1})))
        .isInstanceOf(IllegalStateException.class);
    complete(TransactionSynchronization.STATUS_ROLLED_BACK);

    verify(storage).delete("new.png");
    verify(storage, never()).delete("old.png");
  }

  @Test
  void inactiveMemberCannotKeepPreparedFile() {
    MemberRepository members = mock(MemberRepository.class);
    MemberAvatarStorage storage = mock(MemberAvatarStorage.class);
    Member suspended = Member.builder().id(1L).status(MemberStatus.SUSPENDED).build();
    when(storage.store(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new MemberAvatarStorage.StoredAvatar("new.png", "image/png"));
    when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(suspended));

    assertThatThrownBy(
            () ->
                new MemberAvatarService(members, storage)
                    .replace(1L, new MockMultipartFile("file", new byte[] {1})))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);

    verify(storage).delete("new.png");
    verify(members, never()).flush();
  }

  private Member activeMember(String key, long version) {
    return Member.builder()
        .id(1L)
        .status(MemberStatus.ACTIVE)
        .avatarKey(key)
        .avatarContentType("image/png")
        .avatarVersion(version)
        .build();
  }

  private void complete(int status) {
    var synchronizations = TransactionSynchronizationManager.getSynchronizations();
    synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
    TransactionSynchronizationManager.clearSynchronization();
  }
}

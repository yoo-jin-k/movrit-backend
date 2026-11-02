package com.movrit.movrit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.auth.repository.RefreshTokenRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.entity.MemberStatus;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 회원 탈퇴의 확인 문구·진행 중 캠페인 차단·개인정보 정리를 확인합니다. */
class MemberWithdrawalServiceTest {
  private final MemberRepository members = mock(MemberRepository.class);
  private final MemberSocialAccountRepository socials = mock(MemberSocialAccountRepository.class);
  private final CampaignApplicationRepository applications =
      mock(CampaignApplicationRepository.class);
  private final CampaignRepository campaigns = mock(CampaignRepository.class);
  private final MemberWithdrawalService service =
      new MemberWithdrawalService(
          members,
          socials,
          mock(RefreshTokenRepository.class),
          applications,
          campaigns,
          mock(AuditLogService.class));

  private Member member() {
    return Member.builder()
        .id(7L)
        .email("me@movrit.local")
        .nickname("나")
        .name("김무브")
        .phone("010")
        .passwordHash("hash")
        .role(MemberRole.USER)
        .status(MemberStatus.ACTIVE)
        .build();
  }

  @Test
  void wrongConfirmTextIsRejected() {
    assertThatThrownBy(() -> service.withdraw(7L, "탈퇴"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void inProgressCampaignBlocksWithdrawal() {
    when(members.findByIdForUpdate(7L)).thenReturn(Optional.of(member()));
    when(applications.countByCreatorMemberIdAndStatusIn(any(), anyList())).thenReturn(1L);

    assertThatThrownBy(() -> service.withdraw(7L, "탈퇴합니다"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void withdrawalClearsPersonalInformation() {
    Member member = member();
    when(members.findByIdForUpdate(7L)).thenReturn(Optional.of(member));
    when(socials.findAllByMemberId(7L)).thenReturn(List.of());

    service.withdraw(7L, " 탈퇴합니다 ");

    assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    assertThat(member.getEmail()).isEqualTo("withdrawn-7@movrit.invalid");
    assertThat(member.getPhone()).isNull();
    assertThat(member.getPasswordHash()).isNull();
    assertThat(member.getDeletedAt()).isNotNull();
  }
}

package com.movrit.movrit.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 회원 탈퇴 요청·확인 응답입니다. */
public final class MemberWithdrawalDtos {
  private MemberWithdrawalDtos() {}

  /** 탈퇴 요청입니다. 확인 문구 '탈퇴합니다'를 그대로 입력해야 합니다. */
  public record WithdrawRequest(@NotBlank @Size(max = 20) String confirmText) {}

  /** 지금 탈퇴할 수 있는지와 막는 이유들입니다. */
  public record Eligibility(boolean allowed, List<String> reasons) {}
}

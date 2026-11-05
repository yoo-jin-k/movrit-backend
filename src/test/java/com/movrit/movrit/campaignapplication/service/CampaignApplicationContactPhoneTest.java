package com.movrit.movrit.campaignapplication.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 신청서 배송 연락처를 하이픈 모양으로 맞추는 규칙을 확인합니다. */
class CampaignApplicationContactPhoneTest {

  @Test
  void formatsKoreanNumbersAndKeepsOtherInputAsIs() {
    assertThat(CampaignApplicationService.contactPhone("01012340000")).isEqualTo("010-1234-0000");
    assertThat(CampaignApplicationService.contactPhone(" 010 1234 0000 "))
        .isEqualTo("010-1234-0000");
    assertThat(CampaignApplicationService.contactPhone("0311234567")).isEqualTo("031-123-4567");
    assertThat(CampaignApplicationService.contactPhone("0212345678")).isEqualTo("02-1234-5678");
    assertThat(CampaignApplicationService.contactPhone("021234567")).isEqualTo("02-123-4567");
    // 자릿수가 맞지 않거나 숫자가 아닌 글자가 있으면 입력한 그대로 둡니다.
    assertThat(CampaignApplicationService.contactPhone("12345")).isEqualTo("12345");
    assertThat(CampaignApplicationService.contactPhone("+82 10-1234-0000"))
        .isEqualTo("+82 10-1234-0000");
    assertThat(CampaignApplicationService.contactPhone(null)).isNull();
  }
}

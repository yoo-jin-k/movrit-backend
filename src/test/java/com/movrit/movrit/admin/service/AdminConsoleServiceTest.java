package com.movrit.movrit.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 운영자 화면에 개인정보가 그대로 노출되지 않는지 확인합니다. */
class AdminConsoleServiceTest {

  @Test
  void phoneNumberIsMaskedInTheMiddle() {
    assertThat(AdminConsoleService.maskPhone("010-1234-5678")).isEqualTo("010-****-5678");
    assertThat(AdminConsoleService.maskPhone("01012345678")).isEqualTo("010-****-5678");
    // 너무 짧은 값은 전부 가리고, 비어 있으면 표시하지 않습니다.
    assertThat(AdminConsoleService.maskPhone("1234")).isEqualTo("****");
    assertThat(AdminConsoleService.maskPhone(" ")).isNull();
  }
}

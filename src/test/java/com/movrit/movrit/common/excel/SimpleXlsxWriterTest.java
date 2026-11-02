package com.movrit.movrit.common.excel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 엑셀 숫자 판단과 시트 이름 규칙을 확인합니다. */
class SimpleXlsxWriterTest {

  @Test
  void onlyPlainNumbersBecomeNumberCells() {
    assertThat(SimpleXlsxWriter.isNumber("0")).isTrue();
    assertThat(SimpleXlsxWriter.isNumber("15000")).isTrue();
    assertThat(SimpleXlsxWriter.isNumber("-3.5")).isTrue();
    // 앞자리 0(전화번호·우편번호), 지수 표기, 날짜는 글자로 남깁니다.
    assertThat(SimpleXlsxWriter.isNumber("010")).isFalse();
    assertThat(SimpleXlsxWriter.isNumber("1e5")).isFalse();
    assertThat(SimpleXlsxWriter.isNumber("2026-11-01")).isFalse();
    assertThat(SimpleXlsxWriter.isNumber("")).isFalse();
  }

  @Test
  void sheetNameFollowsExcelRules() {
    assertThat(SimpleXlsxWriter.sheetName("요약/상세")).isEqualTo("요약 상세");
    assertThat(SimpleXlsxWriter.sheetName("가".repeat(40))).hasSize(31);
    assertThat(SimpleXlsxWriter.sheetName(" ")).isEqualTo("Sheet");
  }
}

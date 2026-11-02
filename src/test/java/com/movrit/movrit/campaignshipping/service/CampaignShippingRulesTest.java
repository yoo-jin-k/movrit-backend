package com.movrit.movrit.campaignshipping.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/** 엑셀 파일 구조와 송장 값 검사를 확인합니다. */
class CampaignShippingRulesTest {

  @Test
  void xlsxHasSheetWithTextCells() throws Exception {
    byte[] file =
        SimpleXlsxWriter.write(
            "배송지",
            List.of("신청번호", "연락처"),
            List.of(List.of("12", "010-0000-0000"), List.of("13", "=HYPERLINK(\"x\")")));
    String sheet = null;
    int entries = 0;
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(file))) {
      for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
        entries += 1;
        if (entry.getName().equals("xl/worksheets/sheet1.xml")) {
          sheet = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
        }
      }
    }
    assertThat(entries).isEqualTo(6);
    assertThat(sheet).contains("010-0000-0000").contains("t=\"inlineStr\"");
    // 수식처럼 보이는 값도 글자로 저장되고 따옴표는 이스케이프됩니다.
    assertThat(sheet).contains("=HYPERLINK(&quot;x&quot;)").doesNotContain("<f>");
    assertThat(SimpleXlsxWriter.column(0)).isEqualTo("A");
    assertThat(SimpleXlsxWriter.column(26)).isEqualTo("AA");
  }

  @Test
  void trackingRules() {
    assertThat(CampaignShippingService.problem(input("CJ대한통운", "1234-5678-9012"), null))
        .contains("배송형 참여자가 아닙니다");
    assertThat(CampaignShippingService.COURIERS).contains("CJ대한통운", "기타");
  }

  private static com.movrit.movrit.campaignshipping.dto.CampaignShippingDtos.ShipmentInput input(
      String courier, String tracking) {
    return new com.movrit.movrit.campaignshipping.dto.CampaignShippingDtos.ShipmentInput(
        1L, courier, tracking);
  }
}

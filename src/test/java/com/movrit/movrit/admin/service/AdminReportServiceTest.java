package com.movrit.movrit.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.movrit.movrit.admin.dto.AdminConsoleDtos;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** 운영 리포트의 기간 규칙과 엑셀 구성을 확인합니다. */
class AdminReportServiceTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

  @Test
  void periodFillsMissingSideAndRejectsWrongRange() {
    assertThat(AdminReportService.period(null, null, TODAY).all()).isTrue();

    var onlyFrom = AdminReportService.period(LocalDate.of(2026, 10, 1), null, TODAY);
    assertThat(onlyFrom.to()).isEqualTo(TODAY);

    var onlyTo = AdminReportService.period(null, LocalDate.of(2026, 10, 30), TODAY);
    assertThat(onlyTo.from()).isEqualTo(LocalDate.of(2026, 10, 1));

    assertThatThrownBy(
            () ->
                AdminReportService.period(
                    LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1), TODAY))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                AdminReportService.period(
                    LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2), TODAY))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void periodIncludesBothEndDays() {
    var period =
        AdminReportService.period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), TODAY);
    assertThat(period.contains(LocalDateTime.of(2026, 11, 1, 0, 0))).isTrue();
    assertThat(period.contains(LocalDateTime.of(2026, 11, 2, 23, 59))).isTrue();
    assertThat(period.contains(LocalDateTime.of(2026, 11, 3, 0, 0))).isFalse();
    assertThat(period.contains(null)).isFalse();
  }

  @Test
  void dailyHasEveryDayEvenWithoutData() {
    var period =
        AdminReportService.period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), TODAY);
    var rows = AdminReportService.daily(period, List.of(), List.of(), List.of(), List.of());
    assertThat(rows)
        .extracting(AdminConsoleDtos.DailyRow::date)
        .containsExactly(
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3));
    assertThat(rows).allMatch(row -> row.signups() == 0 && row.completed() == 0);
  }

  @Test
  void excelHasThreeSheetsWithNumbers() throws Exception {
    Map<String, Long> applications = new HashMap<>();
    applications.put("COMPLETED", 2L);
    var report =
        new AdminConsoleDtos.Report(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 1),
            3,
            1,
            4,
            2,
            Map.of("ACTIVE", 3L),
            Map.of("PRODUCT", 1L),
            5,
            applications,
            Map.of("APPROVED", 2L),
            50.0,
            Map.of(),
            new BigDecimal("15000.00"),
            BigDecimal.ZERO,
            Map.of(),
            List.of(new AdminConsoleDtos.DailyRow(LocalDate.of(2026, 11, 1), 3, 4, 2, 2)),
            LocalDateTime.of(2026, 11, 3, 1, 0));
    byte[] file = AdminReportService.excel(report);
    Map<String, String> parts = new HashMap<>();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(file))) {
      for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
        parts.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
      }
    }
    assertThat(parts.get("xl/workbook.xml")).contains("요약", "날짜별", "상태별");
    // 금액은 지수 표기 없이 숫자 칸으로, 상태 이름은 한글로 들어갑니다.
    assertThat(parts.get("xl/worksheets/sheet1.xml")).contains("<v>15000</v>");
    assertThat(parts.get("xl/worksheets/sheet2.xml")).contains("2026-11-01").contains("<v>4</v>");
    assertThat(parts.get("xl/worksheets/sheet3.xml")).contains("완료").contains("최종 승인");
  }
}

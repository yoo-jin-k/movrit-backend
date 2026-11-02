package com.movrit.movrit.admin.service;

import com.movrit.movrit.admin.dto.AdminConsoleDtos;
import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.entity.CampaignContentSubmission;
import com.movrit.movrit.campaigncontent.entity.ContentSubmissionStatus;
import com.movrit.movrit.campaigncontent.repository.CampaignContentSubmissionRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.common.excel.SimpleXlsxWriter;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.settlement.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 운영 리포트를 기간으로 잘라 집계하고 엑셀로 내보냅니다.
 *
 * <ul>
 *   <li>기간을 고르면 그 기간에 생긴 회원·캠페인·신청·콘텐츠·주문·결제·정산만 셉니다. 시작일·종료일 모두 포함입니다.
 *   <li>기간을 고르지 않으면 전체 기간입니다. 날짜별 추이는 기간을 골랐을 때만 만듭니다.
 *   <li>기간은 최대 1년(366일)입니다. 시작일이 종료일보다 늦으면 400입니다.
 *   <li>엑셀은 '요약'·'날짜별'·'상태별' 세 시트이고, 숫자 칸은 숫자로 저장합니다. 내려받으면 감사 기록을 남깁니다.
 * </ul>
 *
 * <p>시연 규모(수백 건)에서는 전체를 읽어 메모리에서 거르는 것으로 충분합니다. 데이터가 커지면 기간 조건을 쿼리로 옮깁니다.
 */
@Service
@RequiredArgsConstructor
public class AdminReportService {
  static final int MAX_DAYS = 366;

  /** 완료율 분모: 선정 이후 단계로 보는 상태입니다. */
  private static final List<CampaignApplicationStatus> SELECTED_OR_LATER =
      List.of(
          CampaignApplicationStatus.SELECTED,
          CampaignApplicationStatus.ACTIVE,
          CampaignApplicationStatus.CONTENT_SUBMITTED,
          CampaignApplicationStatus.REVISION_REQUESTED,
          CampaignApplicationStatus.COMPLETED);

  private static final Map<String, String> APPLICATION_LABELS =
      Map.of(
          "SUBMITTED", "접수",
          "SELECTED", "선정",
          "REJECTED", "미선정",
          "ACTIVE", "진행 중",
          "CONTENT_SUBMITTED", "콘텐츠 제출",
          "REVISION_REQUESTED", "수정 요청",
          "COMPLETED", "완료",
          "CANCELLED", "취소");
  private static final Map<String, String> CONTENT_LABELS =
      Map.of(
          "SUBMITTED", "검토 대기",
          "REVISION_REQUESTED", "수정 요청",
          "BRAND_APPROVED", "업체 검토 완료",
          "APPROVED", "최종 승인");
  private static final Map<String, String> CAMPAIGN_TYPE_LABELS =
      Map.of("PRODUCT", "제품", "LOCAL", "지역", "REPORTER", "기자단", "PURCHASE_REVIEW", "구매평");

  private final MemberService memberService;
  private final MemberRepository members;
  private final CampaignRepository campaigns;
  private final CampaignApplicationRepository applications;
  private final CampaignContentSubmissionRepository contents;
  private final OrderRepository orders;
  private final PaymentRepository payments;
  private final SettlementRepository settlements;
  private final AuditLogService auditLogService;

  /** 시작일·종료일(포함)입니다. 둘 다 null이면 전체 기간입니다. */
  record Period(LocalDate from, LocalDate to) {
    boolean all() {
      return from == null;
    }

    boolean contains(LocalDateTime at) {
      if (all()) return true;
      if (at == null) return false;
      LocalDate day = at.toLocalDate();
      return !day.isBefore(from) && !day.isAfter(to);
    }
  }

  /**
   * 기간 값을 확인합니다. 한쪽만 있으면 나머지를 채웁니다(시작일만: 오늘까지, 종료일만: 30일 전부터).
   *
   * @throws ResponseStatusException 시작일이 종료일보다 늦거나 1년을 넘으면 400
   */
  static Period period(LocalDate from, LocalDate to, LocalDate today) {
    if (from == null && to == null) return new Period(null, null);
    LocalDate end = to == null ? today : to;
    LocalDate start = from == null ? end.minusDays(29) : from;
    if (start.isAfter(end)) throw badRequest("시작일이 종료일보다 늦습니다. 기간을 다시 골라 주세요.");
    if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
      throw badRequest("기간은 1년(366일) 안으로 골라 주세요.");
    }
    return new Period(start, end);
  }

  /** 기간 리포트입니다. */
  @Transactional(readOnly = true)
  public AdminConsoleDtos.Report report(Long adminId, LocalDate from, LocalDate to) {
    memberService.getAdmin(adminId);
    return build(period(from, to, LocalDate.now()));
  }

  /** 기간 리포트를 엑셀로 만들고 내려받기 기록을 남깁니다. */
  @Transactional
  public byte[] export(Long adminId, LocalDate from, LocalDate to) {
    memberService.getAdmin(adminId);
    Period period = period(from, to, LocalDate.now());
    AdminConsoleDtos.Report report = build(period);
    auditLogService.record(
        adminId,
        "ADMIN_REPORT_EXPORTED",
        "ADMIN_REPORT",
        null,
        null,
        "{\"from\":\"" + report.from() + "\",\"to\":\"" + report.to() + "\"}",
        null);
    return excel(report);
  }

  // ───────── 집계 ─────────

  AdminConsoleDtos.Report build(Period period) {
    List<Member> newMembers =
        members.findAll().stream()
            .filter(m -> m.getDeletedAt() == null && period.contains(m.getCreatedAt()))
            .toList();
    List<Campaign> allCampaigns = campaigns.findAll();
    List<Campaign> newCampaigns =
        allCampaigns.stream().filter(c -> period.contains(c.getCreatedAt())).toList();
    List<CampaignApplication> newApplications =
        applications.findAll().stream().filter(a -> period.contains(appliedAt(a))).toList();
    List<CampaignContentSubmission> allContents = contents.findAll();
    List<CampaignContentSubmission> newContents =
        allContents.stream().filter(c -> period.contains(submittedAt(c))).toList();
    List<CampaignContentSubmission> approved =
        allContents.stream()
            .filter(c -> c.getStatus() == ContentSubmissionStatus.APPROVED)
            .filter(c -> period.contains(c.getReviewedAt()))
            .toList();
    List<Payment> periodPayments =
        payments.findAll().stream().filter(p -> period.contains(paidAt(p))).toList();
    long selected =
        newApplications.stream().filter(a -> SELECTED_OR_LATER.contains(a.getStatus())).count();
    long completed =
        newApplications.stream()
            .filter(a -> a.getStatus() == CampaignApplicationStatus.COMPLETED)
            .count();
    return new AdminConsoleDtos.Report(
        period.from(),
        period.to(),
        newMembers.size(),
        newCampaigns.size(),
        newApplications.size(),
        approved.size(),
        count(newMembers.stream(), Member::getStatus),
        count(newCampaigns.stream(), Campaign::getCampaignType),
        allCampaigns.stream().filter(Campaign::isOpen).count(),
        count(newApplications.stream(), CampaignApplication::getStatus),
        count(newContents.stream(), CampaignContentSubmission::getStatus),
        selected == 0 ? 0 : Math.round(completed * 1000.0 / selected) / 10.0,
        count(
            orders.findAll().stream().filter(o -> period.contains(o.getCreatedAt())),
            Order::getStatus),
        sum(periodPayments, PaymentStatus.PAID),
        sum(periodPayments, PaymentStatus.REFUNDED),
        count(
            settlements.findAll().stream().filter(s -> period.contains(s.getCreatedAt())),
            settlement -> settlement.getStatus()),
        period.all()
            ? List.of()
            : daily(period, newMembers, newApplications, newContents, approved),
        LocalDateTime.now());
  }

  /** 기간의 날마다 가입·신청·콘텐츠 제출·완료 수를 셉니다(0인 날도 넣습니다). */
  static List<AdminConsoleDtos.DailyRow> daily(
      Period period,
      List<Member> newMembers,
      List<CampaignApplication> newApplications,
      List<CampaignContentSubmission> newContents,
      List<CampaignContentSubmission> approved) {
    Map<LocalDate, Long> signups = byDay(newMembers.stream().map(Member::getCreatedAt));
    Map<LocalDate, Long> applied =
        byDay(newApplications.stream().map(AdminReportService::appliedAt));
    Map<LocalDate, Long> submitted =
        byDay(newContents.stream().map(AdminReportService::submittedAt));
    Map<LocalDate, Long> done =
        byDay(approved.stream().map(CampaignContentSubmission::getReviewedAt));
    List<AdminConsoleDtos.DailyRow> rows = new ArrayList<>();
    for (LocalDate day = period.from(); !day.isAfter(period.to()); day = day.plusDays(1)) {
      rows.add(
          new AdminConsoleDtos.DailyRow(
              day,
              signups.getOrDefault(day, 0L),
              applied.getOrDefault(day, 0L),
              submitted.getOrDefault(day, 0L),
              done.getOrDefault(day, 0L)));
    }
    return rows;
  }

  // ───────── 엑셀 ─────────

  static byte[] excel(AdminConsoleDtos.Report report) {
    String range = report.from() == null ? "전체 기간" : report.from() + " ~ " + report.to();
    List<List<String>> summary =
        List.of(
            List.of("기간", range),
            List.of("새 회원", String.valueOf(report.newMembers())),
            List.of("새 캠페인", String.valueOf(report.newCampaigns())),
            List.of("새 신청", String.valueOf(report.newApplications())),
            List.of("완료(최종 승인)", String.valueOf(report.completedApplications())),
            List.of("선정 후 완료율(%)", String.valueOf(report.completionRate())),
            List.of("지금 모집 중 캠페인", String.valueOf(report.openCampaigns())),
            List.of("결제 완료 금액(원)", plain(report.paidAmount())),
            List.of("환불 금액(원)", plain(report.refundedAmount())),
            List.of("집계 시각", report.generatedAt().withNano(0).toString().replace('T', ' ')));
    List<List<String>> daily =
        report.daily().stream()
            .map(
                row ->
                    List.of(
                        row.date().toString(),
                        String.valueOf(row.signups()),
                        String.valueOf(row.applications()),
                        String.valueOf(row.contents()),
                        String.valueOf(row.completed())))
            .toList();
    List<List<String>> states = new ArrayList<>();
    addCounts(states, "신청", report.applicationsByStatus(), APPLICATION_LABELS);
    addCounts(states, "콘텐츠", report.contentsByStatus(), CONTENT_LABELS);
    addCounts(states, "캠페인 종류", report.campaignsByType(), CAMPAIGN_TYPE_LABELS);
    addCounts(states, "회원", report.membersByStatus(), Map.of());
    return SimpleXlsxWriter.write(
        List.of(
            new SimpleXlsxWriter.Sheet("요약", List.of("항목", "값"), summary, true),
            new SimpleXlsxWriter.Sheet(
                "날짜별", List.of("날짜", "가입", "신청", "콘텐츠 제출", "완료"), daily, true),
            new SimpleXlsxWriter.Sheet("상태별", List.of("구분", "상태", "건수"), states, true)));
  }

  private static void addCounts(
      List<List<String>> rows, String group, Map<String, Long> counts, Map<String, String> labels) {
    counts.forEach(
        (code, value) ->
            rows.add(List.of(group, labels.getOrDefault(code, code), String.valueOf(value))));
  }

  // ───────── 도우미 ─────────

  private static LocalDateTime appliedAt(CampaignApplication application) {
    return application.getSubmittedAt() != null
        ? application.getSubmittedAt()
        : application.getCreatedAt();
  }

  private static LocalDateTime submittedAt(CampaignContentSubmission content) {
    return content.getSubmittedAt() != null ? content.getSubmittedAt() : content.getCreatedAt();
  }

  private static LocalDateTime paidAt(Payment payment) {
    return payment.getApprovedAt() != null ? payment.getApprovedAt() : payment.getCreatedAt();
  }

  private static Map<LocalDate, Long> byDay(Stream<LocalDateTime> times) {
    return times
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));
  }

  private static BigDecimal sum(List<Payment> list, PaymentStatus status) {
    return list.stream()
        .filter(payment -> payment.getStatus() == status)
        .map(Payment::getApprovedAmount)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** 금액을 지수 표기 없이 씁니다(엑셀 숫자 칸용). */
  private static String plain(BigDecimal value) {
    return value == null ? "0" : value.stripTrailingZeros().toPlainString();
  }

  /** 값(주로 상태 enum)별 건수를 이름 순으로 셉니다. null은 '미지정'입니다. */
  private static <T> Map<String, Long> count(Stream<T> items, Function<T, ?> key) {
    return items.collect(
        Collectors.groupingBy(
            item -> {
              Object value = key.apply(item);
              return value == null ? "미지정" : value.toString();
            },
            TreeMap::new,
            Collectors.counting()));
  }

  private static ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}

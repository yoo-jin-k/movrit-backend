package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.entity.ContentChannel;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignType;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자 캠페인 목록·검색을 서버에서 거르고 나눠 줍니다.
 *
 * <ul>
 *   <li>대상: 모집이 시작된 공개 캠페인(마감된 캠페인도 '모집 마감'으로 함께 보여 줍니다).
 *   <li>조건: 검색어(제목·소개·제공 내역·브랜드), 종류, 카테고리, 지역, 콘텐츠 채널, 프리미어, 오늘 오픈. DB 쿼리에서 거릅니다.
 *   <li>정렬: 모집 중인 캠페인을 먼저, 마감된 캠페인을 뒤에 두고 그 안에서 latest(최신)·closing(마감 임박)·popular(신청 많은
 *       순)·chance(경쟁률 낮은 순)로 정렬합니다. 인기·경쟁률은 신청 수가 필요해 한 번에 집계한 뒤 정렬합니다.
 *   <li>페이지: 0부터, 기본 20개·최대 60개입니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CampaignSearchService {
  static final int DEFAULT_SIZE = 20;
  static final int MAX_SIZE = 60;
  static final Set<String> SORTS = Set.of("latest", "closing", "popular", "chance");

  /** '오늘 오픈' 조건이 없을 때 쓰는 시작 시각 하한입니다(모든 캠페인이 해당). */
  static final LocalDateTime ANY_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

  private final CampaignRepository campaigns;
  private final CampaignApplicationRepository applications;

  /** 검색 조건입니다. 빈 문자열은 조건 없음으로 봅니다. */
  public record Query(
      String q,
      String type,
      String category,
      String region,
      String channel,
      boolean premier,
      boolean openedToday,
      String sort,
      int page,
      int size) {}

  /** 정렬에 필요한 값만 묶은 한 줄입니다. */
  record Row(Campaign campaign, long applications, boolean ended) {}

  @Transactional(readOnly = true)
  public CollaborationDtos.CampaignPage search(Query query) {
    return search(query, LocalDateTime.now());
  }

  CollaborationDtos.CampaignPage search(Query query, LocalDateTime now) {
    String sort = query.sort() == null || query.sort().isBlank() ? "latest" : query.sort();
    if (!SORTS.contains(sort)) throw badRequest("정렬 방식을 확인해 주세요.");
    int size = query.size() <= 0 ? DEFAULT_SIZE : Math.min(query.size(), MAX_SIZE);
    int page = Math.max(query.page(), 0);
    List<Campaign> found =
        campaigns.searchVisible(
            now,
            keyword(query.q()),
            enumValue(CampaignType.class, query.type(), "캠페인 종류"),
            blankToNull(query.category()),
            blankToNull(query.region()),
            enumValue(ContentChannel.class, query.channel(), "콘텐츠 채널"),
            query.premier(),
            query.openedToday() ? LocalDate.from(now).atStartOfDay() : ANY_TIME);
    Map<Long, Long> counts = new HashMap<>();
    if (!found.isEmpty()) {
      applications
          .countActiveApplicationsByCampaignIds(found.stream().map(Campaign::getId).toList())
          .forEach(row -> counts.put(row.getCampaignId(), row.getApplicationCount()));
    }
    List<Row> rows =
        found.stream()
            .map(c -> new Row(c, counts.getOrDefault(c.getId(), 0L), !now.isBefore(c.getEndAt())))
            .sorted(order(sort))
            .toList();
    int total = rows.size();
    int totalPages = total == 0 ? 0 : (total + size - 1) / size;
    List<CollaborationDtos.CampaignResponse> items =
        rows.stream()
            .skip((long) page * size)
            .limit(size)
            .map(row -> CollaborationDtos.CampaignResponse.from(row.campaign(), row.applications()))
            .toList();
    return new CollaborationDtos.CampaignPage(items, page, size, totalPages, total);
  }

  /** 모집 중 → 마감 순서를 먼저 보고, 같은 그룹 안에서 고른 정렬, 마지막으로 최신순을 씁니다. */
  static Comparator<Row> order(String sort) {
    Comparator<Row> group = Comparator.comparing(Row::ended);
    Comparator<Row> latest =
        Comparator.comparing((Row row) -> row.campaign().getId(), Comparator.reverseOrder());
    Comparator<Row> chosen =
        switch (sort) {
          case "closing" -> Comparator.comparing((Row row) -> row.campaign().getEndAt());
          case "popular" -> Comparator.comparing(Row::applications, Comparator.reverseOrder());
          case "chance" -> Comparator.comparingDouble(CampaignSearchService::competition);
          default -> (a, b) -> 0;
        };
    return group.thenComparing(chosen).thenComparing(latest);
  }

  /** 모집 인원 대비 신청 수입니다. 모집 인원을 모르면 맨 뒤로 보냅니다. */
  static double competition(Row row) {
    Integer capacity = row.campaign().getRecruitmentCount();
    if (capacity == null || capacity <= 0) return Double.MAX_VALUE;
    return (double) row.applications() / capacity;
  }

  /** 검색어를 LIKE 패턴으로 바꿉니다. %·_·\는 글자 그대로 찾습니다. */
  static String keyword(String q) {
    if (q == null || q.isBlank()) return null;
    String text = q.trim().toLowerCase(Locale.ROOT);
    if (text.length() > 50) throw badRequest("검색어는 50자 이하로 입력해 주세요.");
    String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escaped + "%";
  }

  private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String label) {
    if (value == null || value.isBlank()) return null;
    try {
      return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw badRequest(label + " 값을 확인해 주세요.");
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}

package com.movrit.movrit.participation.service;

/**
 * 활동 등급 기준입니다. 완료한 캠페인 수로 정합니다.
 *
 * <table>
 *   <tr><th>등급</th><th>완료 수</th></tr>
 *   <tr><td>새싹</td><td>0</td></tr>
 *   <tr><td>브론즈</td><td>1~2</td></tr>
 *   <tr><td>실버</td><td>3~5</td></tr>
 *   <tr><td>골드</td><td>6~9</td></tr>
 *   <tr><td>다이아</td><td>10 이상</td></tr>
 * </table>
 */
public final class ActivityGrades {
  private ActivityGrades() {}

  /** 등급 계산 결과입니다. */
  public record Grade(String code, String label, String nextLabel, int remainingToNext) {}

  private static final int[] THRESHOLDS = {0, 1, 3, 6, 10};
  private static final String[] CODES = {"SPROUT", "BRONZE", "SILVER", "GOLD", "DIAMOND"};
  private static final String[] LABELS = {"새싹", "브론즈", "실버", "골드", "다이아"};

  public static Grade of(long completed) {
    int index = 0;
    for (int i = 0; i < THRESHOLDS.length; i += 1) if (completed >= THRESHOLDS[i]) index = i;
    boolean top = index == THRESHOLDS.length - 1;
    return new Grade(
        CODES[index],
        LABELS[index],
        top ? null : LABELS[index + 1],
        top ? 0 : (int) (THRESHOLDS[index + 1] - completed));
  }
}

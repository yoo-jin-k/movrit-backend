package com.movrit.movrit.admin.dto;

import java.math.BigDecimal;
import java.util.List;

/** 운영 현황의 요청 및 응답 계약을 모읍니다. 입력 제약과 응답 필드의 변경은 호출 화면 및 API 계약과 함께 검토합니다. */
public final class AdminDtos {
  private AdminDtos() {}

  /**
   * 운영 현황의 대시보드 요약을 전달합니다.
   *
   * @param members 회원 수입니다.
   * @param sellers 업체 수입니다.
   * @param creators 기존 채널 프로필 수입니다.
   * @param preorders 프리오더 집계 또는 목록입니다.
   * @param orders 주문 집계 또는 목록입니다.
   * @param payments 결제 집계 또는 목록입니다.
   * @param collaborations 협업 집계 또는 목록입니다.
   * @param pendingSettlements 대기 중인 정산 건수입니다.
   * @param paidGmv 결제 기준 집계 거래 금액입니다.
   */
  public record Summary(
      long members,
      long sellers,
      long creators,
      long preorders,
      long orders,
      long payments,
      long collaborations,
      long pendingSettlements,
      BigDecimal paidGmv) {}

  /**
   * 운영 현황의 목록 항목을 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param type 데이터 또는 질문 유형입니다.
   * @param title 제목입니다.
   * @param status 현재 업무 상태입니다.
   * @param detail 목록에 표시할 상세 안내입니다.
   */
  public record Item(Long id, String type, String title, String status, String detail) {}

  /**
   * 운영 현황의 운영 대시보드 응답을 전달합니다.
   *
   * @param summary 대시보드 요약 집계입니다.
   * @param preorders 프리오더 집계 또는 목록입니다.
   * @param orders 주문 집계 또는 목록입니다.
   * @param payments 결제 집계 또는 목록입니다.
   * @param collaborations 협업 집계 또는 목록입니다.
   * @param settlements 정산 목록입니다.
   */
  public record Dashboard(
      Summary summary,
      List<Item> preorders,
      List<Item> orders,
      List<Item> payments,
      List<Item> collaborations,
      List<Item> settlements) {}
}

package com.movrit.movrit.seller.dto;

import com.movrit.movrit.preorder.entity.PreorderStatus;
import java.math.BigDecimal;
import java.util.List;

/** 업체 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
public final class SellerDashboardDtos {
  private SellerDashboardDtos() {}

  /**
   * 업체의 대시보드 요약을 전달합니다.
   *
   * @param projects 프로젝트 집계 또는 목록입니다.
   * @param activeProjects 진행 중인 프로젝트 수입니다.
   * @param orders 주문 집계 또는 목록입니다.
   * @param paidGmv 결제 기준 집계 거래 금액입니다.
   * @param refundAmount 환불 금액입니다.
   * @param estimatedSettlement 예상 정산 금액입니다.
   */
  public record Summary(
      long projects,
      long activeProjects,
      long orders,
      BigDecimal paidGmv,
      BigDecimal refundAmount,
      BigDecimal estimatedSettlement) {}

  /**
   * 업체의 프로젝트별 집계를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param title 제목입니다.
   * @param status 현재 업무 상태입니다.
   * @param orderedQuantity 누적 주문 수량입니다.
   * @param targetQuantity 목표 수량입니다.
   * @param achievementRate 목표 대비 달성률입니다.
   * @param paidGmv 결제 기준 집계 거래 금액입니다.
   * @param refundAmount 환불 금액입니다.
   */
  public record Project(
      Long id,
      String title,
      PreorderStatus status,
      long orderedQuantity,
      long targetQuantity,
      double achievementRate,
      BigDecimal paidGmv,
      BigDecimal refundAmount) {}

  /**
   * 업체의 조회 및 처리 결과를 전달합니다.
   *
   * @param summary 대시보드 요약 집계입니다.
   * @param projects 프로젝트 집계 또는 목록입니다.
   */
  public record Response(Summary summary, List<Project> projects) {}
}

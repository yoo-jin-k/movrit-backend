package com.movrit.movrit.seller.service;

import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import com.movrit.movrit.seller.dto.SellerDashboardDtos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 업체 대시보드(기존 프리오더 판매 현황)의 매출·환불·수수료·주문 수를 집계합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDashboardService {
  private final SellerProfileService sellerProfileService;
  private final PreorderRepository preorderRepository;
  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;

  @Value("${movrit.settlement.default-fee-rate:10}")
  private BigDecimal defaultFeeRate;

  /**
   * 로그인한 업체 본인의 프리오더별 판매 현황과 합계를 계산해 반환합니다. 업체 프로필이 없으면 거절됩니다. 수수료는 유효 매출(결제−환불)에 기본 수수료율을 곱해
   * 계산합니다.
   */
  public SellerDashboardDtos.Response get(Long memberId) {
    sellerProfileService.findMine(memberId);
    List<Preorder> preorders =
        preorderRepository.findAll().stream()
            .filter(p -> p.getDeletedAt() == null)
            .filter(p -> p.getProduct().getBrand().getSeller().getMember().getId().equals(memberId))
            .toList();
    List<SellerDashboardDtos.Project> projects = preorders.stream().map(this::project).toList();
    BigDecimal paid =
        projects.stream()
            .map(SellerDashboardDtos.Project::paidGmv)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refunds =
        projects.stream()
            .map(SellerDashboardDtos.Project::refundAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal validSales = paid.subtract(refunds);
    BigDecimal fee =
        validSales
            .multiply(defaultFeeRate)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    Set<PreorderStatus> active =
        Set.of(
            PreorderStatus.SCHEDULED,
            PreorderStatus.OPEN,
            PreorderStatus.SUCCESS,
            PreorderStatus.PRODUCTION,
            PreorderStatus.SHIPPING);
    long orderCount =
        orderRepository.findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId).size();
    return new SellerDashboardDtos.Response(
        new SellerDashboardDtos.Summary(
            preorders.size(),
            preorders.stream().filter(p -> active.contains(p.getStatus())).count(),
            orderCount,
            paid,
            refunds,
            validSales.subtract(fee)),
        projects);
  }

  private SellerDashboardDtos.Project project(Preorder preorder) {
    var payments = paymentRepository.findAllByOrderPreorderId(preorder.getId());
    BigDecimal paid =
        payments.stream()
            .filter(
                p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.REFUNDED)
            .map(p -> p.getApprovedAmount() == null ? BigDecimal.ZERO : p.getApprovedAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refunds =
        payments.stream()
            .filter(p -> p.getStatus() == PaymentStatus.REFUNDED)
            .map(p -> p.getApprovedAmount() == null ? BigDecimal.ZERO : p.getApprovedAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    double rate =
        Math.min(100.0, preorder.getOrderedQuantity() * 100.0 / preorder.getTargetQuantity());
    return new SellerDashboardDtos.Project(
        preorder.getId(),
        preorder.getTitle(),
        preorder.getStatus(),
        preorder.getOrderedQuantity(),
        preorder.getTargetQuantity(),
        rate,
        paid,
        refunds);
  }
}

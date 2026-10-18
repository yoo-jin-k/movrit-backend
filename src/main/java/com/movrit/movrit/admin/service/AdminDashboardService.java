package com.movrit.movrit.admin.service;

import com.movrit.movrit.admin.dto.AdminDtos;
import com.movrit.movrit.collaboration.repository.CollaborationRepository;
import com.movrit.movrit.creator.repository.CreatorRepository;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import com.movrit.movrit.seller.repository.SellerProfileRepository;
import com.movrit.movrit.settlement.entity.SettlementStatus;
import com.movrit.movrit.settlement.repository.SettlementRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {
  private final MemberService auth;
  private final MemberRepository members;
  private final SellerProfileRepository sellers;
  private final CreatorRepository creators;
  private final PreorderRepository preorders;
  private final OrderRepository orders;
  private final PaymentRepository payments;
  private final CollaborationRepository collaborations;
  private final SettlementRepository settlements;

  public AdminDtos.Dashboard get(Long adminId) {
    auth.getAdmin(adminId);
    var ps = preorders.findAll();
    var os = orders.findAll();
    var pays = payments.findAll();
    var cs = collaborations.findAll();
    var ss = settlements.findAll();
    BigDecimal gmv =
        pays.stream()
            .filter(p -> p.getStatus() == PaymentStatus.PAID)
            .map(p -> p.getApprovedAmount() == null ? BigDecimal.ZERO : p.getApprovedAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    var summary =
        new AdminDtos.Summary(
            members.count(),
            sellers.count(),
            creators.count(),
            ps.size(),
            os.size(),
            pays.size(),
            cs.size(),
            ss.stream()
                .filter(
                    s ->
                        s.getStatus() != SettlementStatus.PAID
                            && s.getStatus() != SettlementStatus.CANCELLED)
                .count(),
            gmv);
    return new AdminDtos.Dashboard(
        summary,
        ps.stream()
            .map(
                p ->
                    new AdminDtos.Item(
                        p.getId(),
                        "PREORDER",
                        p.getTitle(),
                        p.getStatus().name(),
                        p.getProduct().getName()))
            .toList(),
        os.stream()
            .map(
                o ->
                    new AdminDtos.Item(
                        o.getId(),
                        "ORDER",
                        o.getOrderNumber(),
                        o.getStatus().name(),
                        o.getMember().getName() + " / " + o.getTotalPaymentAmount()))
            .toList(),
        pays.stream()
            .map(
                p ->
                    new AdminDtos.Item(
                        p.getId(),
                        "PAYMENT",
                        "주문 " + p.getOrder().getOrderNumber(),
                        p.getStatus().name(),
                        String.valueOf(p.getRequestedAmount())))
            .toList(),
        cs.stream()
            .map(
                c ->
                    new AdminDtos.Item(
                        c.getId(),
                        "COLLABORATION",
                        c.getPreorder().getTitle(),
                        c.getStatus().name(),
                        c.getCreator().getNickname()))
            .toList(),
        ss.stream()
            .map(
                s ->
                    new AdminDtos.Item(
                        s.getId(),
                        "SETTLEMENT",
                        s.getSettlementNumber(),
                        s.getStatus().name(),
                        String.valueOf(s.getNetAmount())))
            .toList());
  }
}

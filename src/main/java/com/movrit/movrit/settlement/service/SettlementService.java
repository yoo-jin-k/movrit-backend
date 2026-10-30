package com.movrit.movrit.settlement.service;

import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.preorder.entity.*;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.settlement.dto.SettlementDtos;
import com.movrit.movrit.settlement.entity.*;
import com.movrit.movrit.settlement.repository.SettlementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {
  private final SettlementRepository settlementRepository;
  private final PreorderService preorderService;
  private final MemberService memberService;
  private final PaymentRepository paymentRepository;

  @Transactional
  public SettlementDtos.Response create(Long memberId, SettlementDtos.CreateRequest request) {
    memberService.getAdmin(memberId);
    Preorder preorder = preorderService.getPreorder(request.preorderId());
    if (preorder.getStatus() != PreorderStatus.COMPLETED)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "구매 확정이 끝난 프리오더만 정산할 수 있습니다.");
    if (settlementRepository.findByPreorderId(preorder.getId()).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 정산이 생성된 프리오더야.");
    var payments = paymentRepository.findAllByOrderPreorderId(preorder.getId());
    BigDecimal gross =
        payments.stream()
            .filter(
                payment ->
                    payment.getStatus() == PaymentStatus.PAID
                        || payment.getStatus() == PaymentStatus.REFUNDED)
            .map(
                payment ->
                    payment.getApprovedAmount() == null
                        ? BigDecimal.ZERO
                        : payment.getApprovedAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refund =
        payments.stream()
            .filter(payment -> payment.getStatus() == PaymentStatus.REFUNDED)
            .map(
                payment ->
                    payment.getApprovedAmount() == null
                        ? BigDecimal.ZERO
                        : payment.getApprovedAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal validSales = gross.subtract(refund);
    BigDecimal fee =
        validSales
            .multiply(request.platformFeeRate())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    Settlement s =
        Settlement.builder()
            .preorder(preorder)
            .settlementNumber(
                "ST-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")))
            .status(SettlementStatus.PENDING)
            .grossAmount(gross)
            .refundAmount(refund)
            .platformFeeAmount(fee)
            .netAmount(validSales.subtract(fee))
            .currency(preorder.getCurrency())
            .calculatedAt(LocalDateTime.now())
            .build();
    return SettlementDtos.Response.from(settlementRepository.save(s));
  }

  public List<SettlementDtos.Response> findMine(Long memberId) {
    return settlementRepository
        .findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
        .stream()
        .map(SettlementDtos.Response::from)
        .toList();
  }

  public List<SettlementDtos.Response> findAll(Long memberId) {
    memberService.getAdmin(memberId);
    return settlementRepository.findAll().stream().map(SettlementDtos.Response::from).toList();
  }

  @Transactional
  public SettlementDtos.Response confirm(Long memberId, Long id) {
    memberService.getAdmin(memberId);
    Settlement s = get(id);
    transit(s::confirm);
    return SettlementDtos.Response.from(s);
  }

  @Transactional
  public SettlementDtos.Response pay(Long memberId, Long id) {
    memberService.getAdmin(memberId);
    Settlement s = get(id);
    transit(s::pay);
    return SettlementDtos.Response.from(s);
  }

  @Transactional
  public SettlementDtos.Response cancel(Long memberId, Long id) {
    memberService.getAdmin(memberId);
    Settlement s = get(id);
    transit(s::cancel);
    return SettlementDtos.Response.from(s);
  }

  private Settlement get(Long id) {
    return settlementRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "정산 정보를 찾을 수 없습니다."));
  }

  private void transit(Runnable action) {
    try {
      action.run();
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }
}

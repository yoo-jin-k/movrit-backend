package com.movrit.movrit.settlement.service;

import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.settlement.dto.SettlementDtos;
import com.movrit.movrit.settlement.entity.Settlement;
import com.movrit.movrit.settlement.entity.SettlementStatus;
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

/** 운영자가 프리오더 정산을 만들고 확정·지급·취소합니다. 업체는 자신의 정산 내역을 조회합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {
  private final SettlementRepository settlementRepository;
  private final PreorderService preorderService;
  private final MemberService memberService;
  private final PaymentRepository paymentRepository;

  /**
   * 구매 확정이 끝난 프리오더의 정산을 만듭니다.
   *
   * <p>승인·환불된 결제의 승인 금액을 합쳐 총액을 구하고, 환불 금액을 뺀 유효 매출에 수수료율(%)을 곱해 수수료를 계산한 뒤 업체 지급액을 정합니다. 프리오더마다 한
   * 번만 만들 수 있습니다.
   */
  @Transactional
  public SettlementDtos.Response create(Long memberId, SettlementDtos.CreateRequest request) {
    memberService.getAdmin(memberId);
    Preorder preorder = preorderService.getPreorder(request.preorderId());
    if (preorder.getStatus() != PreorderStatus.COMPLETED)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "구매 확정이 끝난 프리오더만 정산할 수 있습니다.");
    if (settlementRepository.findByPreorderId(preorder.getId()).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 정산이 생성된 프리오더입니다.");
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

  /** 업체가 소유한 브랜드의 정산 내역을 조회합니다. */
  public List<SettlementDtos.Response> findMine(Long memberId) {
    return settlementRepository
        .findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
        .stream()
        .map(SettlementDtos.Response::from)
        .toList();
  }

  /** 운영자가 전체 정산 내역을 조회합니다. */
  public List<SettlementDtos.Response> findAll(Long memberId) {
    memberService.getAdmin(memberId);
    return settlementRepository.findAll().stream().map(SettlementDtos.Response::from).toList();
  }

  /** 운영자가 정산을 확정합니다. */
  @Transactional
  public SettlementDtos.Response confirm(Long memberId, Long id) {
    memberService.getAdmin(memberId);
    Settlement s = get(id);
    transit(s::confirm);
    return SettlementDtos.Response.from(s);
  }

  /** 운영자가 정산 지급 완료를 기록합니다. */
  @Transactional
  public SettlementDtos.Response pay(Long memberId, Long id) {
    memberService.getAdmin(memberId);
    Settlement s = get(id);
    transit(s::pay);
    return SettlementDtos.Response.from(s);
  }

  /** 운영자가 정산을 취소합니다. 지급 완료된 정산은 취소할 수 없습니다. */
  @Transactional
  public SettlementDtos.Response cancel(Long memberId, Long id) {
    memberService.getAdmin(memberId);
    Settlement s = get(id);
    transit(s::cancel);
    return SettlementDtos.Response.from(s);
  }

  /** 정산을 찾습니다. 없으면 404입니다. */
  private Settlement get(Long id) {
    return settlementRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "정산 정보를 찾을 수 없습니다."));
  }

  /** 상태 규칙 위반을 409 응답으로 바꿔 사유를 전달합니다. */
  private void transit(Runnable action) {
    try {
      action.run();
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }
}

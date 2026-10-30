package com.movrit.movrit.payment.service;

import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderItem;
import com.movrit.movrit.order.entity.OrderStatus;
import com.movrit.movrit.order.service.OrderService;
import com.movrit.movrit.payment.dto.PaymentDtos;
import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.product.service.ProductService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 주문 결제를 만들고 승인·실패·환불을 처리합니다.
 *
 * <p>결제 승인 시 예약했던 재고를 판매로 확정하고 주문을 결제 완료로 바꿉니다. 환불 시에는 판매 수량과 프리오더 주문 수량을 되돌립니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
  private final PaymentRepository paymentRepository;
  private final OrderService orderService;
  private final ProductService productService;
  private final PreorderService preorderService;

  /** 결제 대기 주문의 결제를 만듭니다. 같은 요청 키로 이미 만든 결제가 있으면 그대로 돌려주고, 주문에 결제가 이미 있으면 409입니다. */
  @Transactional
  public PaymentDtos.Response create(Long memberId, PaymentDtos.CreateRequest request) {
    Payment duplicate =
        paymentRepository.findByIdempotencyKey(request.idempotencyKey().trim()).orElse(null);
    if (duplicate != null) return owned(memberId, duplicate);
    Order order = ownedOrder(memberId, request.orderId());
    paymentRepository
        .findByOrderId(order.getId())
        .ifPresent(
            value -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 결제가 생성된 주문입니다.");
            });
    if (order.getStatus() != OrderStatus.PENDING_PAYMENT)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 대기 주문만 결제할 수 있습니다.");
    Payment payment =
        paymentRepository.save(
            Payment.builder()
                .order(order)
                .status(PaymentStatus.READY)
                .requestedAmount(order.getTotalPaymentAmount())
                .currency(order.getCurrency())
                .idempotencyKey(request.idempotencyKey().trim())
                .requestedAt(LocalDateTime.now())
                .build());
    return PaymentDtos.Response.from(payment);
  }

  /** 구매자 본인의 결제 내역을 조회합니다. */
  public List<PaymentDtos.Response> findMine(Long memberId) {
    return paymentRepository.findAllByOrderMemberIdOrderByIdDesc(memberId).stream()
        .map(PaymentDtos.Response::from)
        .toList();
  }

  /**
   * 결제를 승인합니다.
   *
   * <p>같은 승인 키로 다시 요청하면 결과만 돌려주고, 다른 결제에 이미 쓴 승인 키면 409입니다. 승인 후 재고를 판매로 확정하고 주문을 결제 완료로 바꿉니다.
   */
  @Transactional
  public PaymentDtos.Response approve(Long memberId, Long id, PaymentDtos.ApproveRequest request) {
    Payment payment = getOwned(memberId, id);
    String paymentKey = request.paymentKey().trim();
    if (payment.getStatus() == PaymentStatus.PAID && paymentKey.equals(payment.getPaymentKey())) {
      return PaymentDtos.Response.from(payment);
    }
    paymentRepository
        .findByPaymentKey(paymentKey)
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(
            existing -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 결제 승인 키입니다.");
            });
    try {
      payment.approve(paymentKey, request.pgProvider().trim(), request.method().trim());
      for (OrderItem item : orderService.getItems(payment.getOrder().getId()))
        productService.getVariant(item.getProductVariantId()).confirmSale(item.getQuantity());
      payment.getOrder().markPaid();
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
    return PaymentDtos.Response.from(payment);
  }

  /** 결제를 실패로 기록합니다. 결제 대기 상태에서만 가능합니다. */
  @Transactional
  public PaymentDtos.Response fail(Long memberId, Long id, PaymentDtos.FailRequest request) {
    Payment payment = getOwned(memberId, id);
    try {
      payment.fail(request.failureCode().trim(), request.failureMessage().trim());
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
    return PaymentDtos.Response.from(payment);
  }

  /** 구매자 요청으로 승인된 결제를 환불하고, 주문·판매 수량·프리오더 주문 수량을 되돌립니다. */
  @Transactional
  public PaymentDtos.Response refund(Long memberId, Long id) {
    Payment payment = getOwned(memberId, id);
    try {
      payment.refund();
      payment.getOrder().refund();
      for (OrderItem item : orderService.getItems(payment.getOrder().getId()))
        productService.getVariant(item.getProductVariantId()).refundSale(item.getQuantity());
      preorderService.removeOrderQuantity(
          payment.getOrder().getPreorder(),
          orderService.getItems(payment.getOrder().getId()).stream()
              .mapToLong(OrderItem::getQuantity)
              .sum());
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
    return PaymentDtos.Response.from(payment);
  }

  /** 목표 수량을 채우지 못한 프리오더의 결제를 자동 환불합니다. 승인 완료 상태가 아니면 아무것도 하지 않습니다. */
  @Transactional
  public void refundForFailedPreorder(Payment payment) {
    if (payment.getStatus() != PaymentStatus.PAID) return;
    try {
      payment.refund();
      payment.getOrder().refund();
      for (OrderItem item : orderService.getItems(payment.getOrder().getId()))
        productService.getVariant(item.getProductVariantId()).refundSale(item.getQuantity());
      preorderService.removeOrderQuantity(
          payment.getOrder().getPreorder(),
          orderService.getItems(payment.getOrder().getId()).stream()
              .mapToLong(OrderItem::getQuantity)
              .sum());
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  /** 결제를 찾고 구매자 본인 결제인지 확인합니다. */
  private Payment getOwned(Long memberId, Long id) {
    return ownedPayment(
        memberId,
        paymentRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다.")));
  }

  /** 본인 결제인지 확인한 뒤 응답으로 바꿉니다. */
  private PaymentDtos.Response owned(Long memberId, Payment payment) {
    return PaymentDtos.Response.from(ownedPayment(memberId, payment));
  }

  /** 결제가 속한 주문이 본인 주문인지 확인합니다. */
  private Payment ownedPayment(Long memberId, Payment payment) {
    ownedOrder(memberId, payment.getOrder().getId());
    return payment;
  }

  /** 본인 주문인지 확인합니다. 아니면 403입니다. */
  private Order ownedOrder(Long memberId, Long orderId) {
    Order order = orderService.getOrder(orderId);
    if (!order.getMember().getId().equals(memberId))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 주문의 결제만 처리할 수 있습니다.");
    return order;
  }
}

package com.movrit.movrit.payment.service;

import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderStatus;
import com.movrit.movrit.order.service.OrderService;
import com.movrit.movrit.payment.dto.PaymentDtos;
import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    @Transactional
    public PaymentDtos.Response create(Long memberId, PaymentDtos.CreateRequest request) {
        String idempotencyKey = request.idempotencyKey().trim();
        Payment duplicate = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (duplicate != null) {
            return PaymentDtos.Response.from(requireOwned(memberId, duplicate));
        }

        Order order = requireOwnedOrder(memberId, request.orderId());
        paymentRepository.findByOrderId(order.getId()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 결제가 생성된 주문이야.");
        });
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 대기 주문만 결제할 수 있어.");
        }

        Payment payment = Payment.builder()
                .order(order)
                .status(PaymentStatus.READY)
                .requestedAmount(order.getTotalPaymentAmount())
                .currency(order.getCurrency())
                .idempotencyKey(idempotencyKey)
                .requestedAt(LocalDateTime.now())
                .build();

        return PaymentDtos.Response.from(paymentRepository.save(payment));
    }

    public List<PaymentDtos.Response> findMine(Long memberId) {
        return paymentRepository.findAllByOrderMemberIdOrderByIdDesc(memberId).stream()
                .map(PaymentDtos.Response::from)
                .toList();
    }

    @Transactional
    public PaymentDtos.Response approve(Long memberId, Long id, PaymentDtos.ApproveRequest request) {
        Payment payment = requireOwned(memberId, getPayment(id));
        try {
            payment.approve(request.paymentKey().trim(), request.pgProvider().trim(), request.method().trim());
            orderService.markPaid(payment.getOrder().getId());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return PaymentDtos.Response.from(payment);
    }

    @Transactional
    public PaymentDtos.Response fail(Long memberId, Long id, PaymentDtos.FailRequest request) {
        Payment payment = requireOwned(memberId, getPayment(id));
        try {
            payment.fail(request.failureCode().trim(), request.failureMessage().trim());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return PaymentDtos.Response.from(payment);
    }

    @Transactional
    public PaymentDtos.Response refund(Long memberId, Long id) {
        Payment payment = requireOwned(memberId, getPayment(id));
        try {
            payment.refund();
            orderService.markRefunded(payment.getOrder().getId());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return PaymentDtos.Response.from(payment);
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없어."));
    }

    private Payment requireOwned(Long memberId, Payment payment) {
        requireOwnedOrder(memberId, payment.getOrder().getId());
        return payment;
    }

    private Order requireOwnedOrder(Long memberId, Long orderId) {
        Order order = orderService.getOrder(orderId);
        if (!order.getMember().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 주문의 결제만 처리할 수 있어.");
        }
        return order;
    }
}

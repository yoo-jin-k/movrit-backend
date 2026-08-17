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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    @Transactional
    public PaymentDtos.Response create(PaymentDtos.CreateRequest request) {
        Order order = orderService.getOrder(request.orderId());
        validateAmount(order, request.requestedAmount());
        if (request.status() == PaymentStatus.PAID) {
            validateApprovedAmount(order, request.approvedAmount());
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentKey(request.paymentKey())
                .pgProvider(request.pgProvider())
                .method(request.method())
                .status(request.status())
                .requestedAmount(request.requestedAmount())
                .approvedAmount(request.approvedAmount())
                .currency(request.currency().trim().toUpperCase())
                .idempotencyKey(request.idempotencyKey())
                .requestedAt(request.requestedAt())
                .approvedAt(request.approvedAt())
                .failedAt(request.failedAt())
                .failureCode(request.failureCode())
                .failureMessage(request.failureMessage())
                .build();
        paymentRepository.save(payment);

        applyOrderState(order, null, request.status(), request.approvedAmount());
        return PaymentDtos.Response.from(payment);
    }

    public List<PaymentDtos.Response> findAll() {
        return paymentRepository.findAll().stream().map(PaymentDtos.Response::from).toList();
    }

    public PaymentDtos.Response findById(Long id) {
        return PaymentDtos.Response.from(getPayment(id));
    }

    @Transactional
    public PaymentDtos.Response update(Long id, PaymentDtos.UpdateRequest request) {
        Payment payment = getPayment(id);
        PaymentStatus previous = payment.getStatus();
        if (request.status() == PaymentStatus.PAID) {
            validateApprovedAmount(payment.getOrder(), request.approvedAmount());
        }
        payment.update(request.paymentKey(), request.pgProvider(), request.method(), request.status(),
                request.approvedAmount(), request.requestedAt(), request.approvedAt(), request.failedAt(),
                request.failureCode(), request.failureMessage());
        applyOrderState(payment.getOrder(), previous, request.status(), request.approvedAmount());
        return PaymentDtos.Response.from(payment);
    }

    @Transactional
    public void delete(Long id) {
        paymentRepository.delete(getPayment(id));
    }

    private void applyOrderState(Order order, PaymentStatus previous, PaymentStatus current, BigDecimal approvedAmount) {
        if (current == PaymentStatus.PAID && previous != PaymentStatus.PAID) {
            validateApprovedAmount(order, approvedAmount);
            orderService.markPaid(order.getId());
        } else if ((current == PaymentStatus.CANCELLED || current == PaymentStatus.REFUNDED)
                && previous != current) {
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                orderService.cancel(order.getId());
            } else if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PREPARING) {
                orderService.markRefunded(order.getId());
            }
        }
    }

    private void validateAmount(Order order, BigDecimal amount) {
        if (amount.compareTo(order.getTotalPaymentAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문 결제금액과 요청금액이 일치하지 않아.");
        }
    }

    private void validateApprovedAmount(Order order, BigDecimal approvedAmount) {
        if (approvedAmount == null || approvedAmount.compareTo(order.getTotalPaymentAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인금액이 주문 결제금액과 일치하지 않아.");
        }
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없어."));
    }
}

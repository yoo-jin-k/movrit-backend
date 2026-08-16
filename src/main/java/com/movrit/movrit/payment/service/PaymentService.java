package com.movrit.movrit.payment.service;

import com.movrit.movrit.order.service.OrderService;
import com.movrit.movrit.payment.dto.PaymentDtos;
import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    @Transactional
    public PaymentDtos.Response create(PaymentDtos.CreateRequest request) {
        Payment payment = Payment.builder()
                .order(orderService.getOrder(request.orderId()))
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
        return PaymentDtos.Response.from(paymentRepository.save(payment));
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
        payment.update(request.paymentKey(), request.pgProvider(), request.method(), request.status(),
                request.approvedAmount(), request.requestedAt(), request.approvedAt(), request.failedAt(),
                request.failureCode(), request.failureMessage());
        return PaymentDtos.Response.from(payment);
    }

    @Transactional
    public void delete(Long id) {
        paymentRepository.delete(getPayment(id));
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없어."));
    }
}

package com.movrit.movrit.payment.service;

import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderItem;
import com.movrit.movrit.order.entity.OrderStatus;
import com.movrit.movrit.order.service.OrderService;
import com.movrit.movrit.payment.dto.PaymentDtos;
import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.product.service.ProductService;
import com.movrit.movrit.preorder.service.PreorderService;
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
    private final ProductService productService;
    private final PreorderService preorderService;

    @Transactional
    public PaymentDtos.Response create(Long memberId, PaymentDtos.CreateRequest request) {
        Payment duplicate = paymentRepository.findByIdempotencyKey(request.idempotencyKey().trim()).orElse(null);
        if (duplicate != null) return owned(memberId, duplicate);
        Order order = ownedOrder(memberId, request.orderId());
        paymentRepository.findByOrderId(order.getId()).ifPresent(value -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 결제가 생성된 주문이야."); });
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 대기 주문만 결제할 수 있어.");
        Payment payment = paymentRepository.save(Payment.builder().order(order).status(PaymentStatus.READY)
                .requestedAmount(order.getTotalPaymentAmount()).currency(order.getCurrency())
                .idempotencyKey(request.idempotencyKey().trim()).requestedAt(LocalDateTime.now()).build());
        return PaymentDtos.Response.from(payment);
    }

    public List<PaymentDtos.Response> findMine(Long memberId) { return paymentRepository.findAllByOrderMemberIdOrderByIdDesc(memberId).stream().map(PaymentDtos.Response::from).toList(); }

    @Transactional
    public PaymentDtos.Response approve(Long memberId, Long id, PaymentDtos.ApproveRequest request) {
        Payment payment = getOwned(memberId, id);
        String paymentKey = request.paymentKey().trim();
        if (payment.getStatus() == PaymentStatus.PAID && paymentKey.equals(payment.getPaymentKey())) {
            return PaymentDtos.Response.from(payment);
        }
        paymentRepository.findByPaymentKey(paymentKey).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 결제 승인 키야.");
        });
        try {
            payment.approve(paymentKey, request.pgProvider().trim(), request.method().trim());
            for (OrderItem item : orderService.getItems(payment.getOrder().getId()))
                productService.getVariant(item.getProductVariantId()).confirmSale(item.getQuantity());
            payment.getOrder().markPaid();
        } catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        return PaymentDtos.Response.from(payment);
    }

    @Transactional
    public PaymentDtos.Response fail(Long memberId, Long id, PaymentDtos.FailRequest request) {
        Payment payment = getOwned(memberId, id);
        try { payment.fail(request.failureCode().trim(), request.failureMessage().trim()); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        return PaymentDtos.Response.from(payment);
    }

    @Transactional
    public PaymentDtos.Response refund(Long memberId, Long id) {
        Payment payment = getOwned(memberId, id);
        try {
            payment.refund();
            payment.getOrder().refund();
            for (OrderItem item : orderService.getItems(payment.getOrder().getId()))
                productService.getVariant(item.getProductVariantId()).refundSale(item.getQuantity());
            preorderService.removeOrderQuantity(payment.getOrder().getPreorder(), orderService.getItems(payment.getOrder().getId()).stream().mapToLong(OrderItem::getQuantity).sum());
        } catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        return PaymentDtos.Response.from(payment);
    }

    @Transactional
    public void refundForFailedPreorder(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) return;
        try {
            payment.refund();
            payment.getOrder().refund();
            for (OrderItem item : orderService.getItems(payment.getOrder().getId()))
                productService.getVariant(item.getProductVariantId()).refundSale(item.getQuantity());
            preorderService.removeOrderQuantity(payment.getOrder().getPreorder(), orderService.getItems(payment.getOrder().getId()).stream().mapToLong(OrderItem::getQuantity).sum());
        } catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
    }

    private Payment getOwned(Long memberId, Long id) { return ownedPayment(memberId, paymentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제를 찾을 수 없어."))); }
    private PaymentDtos.Response owned(Long memberId, Payment payment) { return PaymentDtos.Response.from(ownedPayment(memberId, payment)); }
    private Payment ownedPayment(Long memberId, Payment payment) { ownedOrder(memberId, payment.getOrder().getId()); return payment; }
    private Order ownedOrder(Long memberId, Long orderId) { Order order = orderService.getOrder(orderId); if (!order.getMember().getId().equals(memberId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 주문의 결제만 처리할 수 있어."); return order; }
}

package com.movrit.movrit.order.service;

import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.order.dto.OrderDtos;
import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderItem;
import com.movrit.movrit.order.entity.OrderStatus;
import com.movrit.movrit.order.repository.OrderItemRepository;
import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderVariant;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.preorder.service.PreorderVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberService memberService;
    private final PreorderService preorderService;
    private final PreorderVariantService preorderVariantService;

    @Transactional
    public OrderDtos.Response create(Long memberId, OrderDtos.CreateRequest request) {
        String orderNumber = request.orderNumber().trim();
        String requestKey = request.requestKey().trim();
        if (orderRepository.existsByOrderNumber(orderNumber) || orderRepository.existsByRequestKey(requestKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 주문 요청이야.");
        }

        Preorder preorder = preorderService.getPreorder(request.preorderId());
        BigDecimal productAmount = BigDecimal.ZERO;
        for (OrderDtos.ItemRequest item : request.items()) {
            PreorderVariant variant = preorderVariantService.reserve(item.preorderVariantId(), item.quantity());
            if (!variant.getPreorder().getId().equals(preorder.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 공동구매의 옵션이 포함되어 있어.");
            }
            productAmount = productAmount.add(variant.getSalePrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        if (request.discountAmount().compareTo(productAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "할인금액이 상품금액보다 클 수 없어.");
        }
        BigDecimal paymentAmount = productAmount.subtract(request.discountAmount()).add(request.shippingAmount());

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .requestKey(requestKey)
                .member(memberService.getMember(memberId))
                .preorder(preorder)
                .attributionId(request.attributionId())
                .acquisitionSource(request.acquisitionSource().trim())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalProductAmount(productAmount)
                .discountAmount(request.discountAmount())
                .shippingAmount(request.shippingAmount())
                .totalPaymentAmount(paymentAmount)
                .currency(request.currency().trim().toUpperCase())
                .receiverName(request.receiverName().trim())
                .receiverPhone(request.receiverPhone().trim())
                .postalCode(request.postalCode())
                .address1(request.address1().trim())
                .address2(request.address2())
                .build();
        orderRepository.save(order);

        for (OrderDtos.ItemRequest item : request.items()) {
            PreorderVariant variant = preorderVariantService.get(item.preorderVariantId());
            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .product(variant.getPreorder().getProduct())
                    .productVariantId(variant.getProductSku().getId())
                    .preorderVariantId(variant.getId())
                    .productName(variant.getPreorder().getProduct().getName())
                    .optionName(variant.getProductSku().getOptionName())
                    .unitPrice(variant.getSalePrice())
                    .quantity(item.quantity())
                    .totalPrice(variant.getSalePrice().multiply(BigDecimal.valueOf(item.quantity())))
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return OrderDtos.Response.from(order);
    }

    public List<OrderDtos.Response> findAll() {
        return orderRepository.findAll().stream().map(OrderDtos.Response::from).toList();
    }

    public List<OrderDtos.Response> findMine(Long memberId) {
        return orderRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(OrderDtos.Response::from).toList();
    }

    public List<OrderDtos.Response> findBySellerId(Long sellerId) {
        return orderRepository.findAllByPreorderSellerId(sellerId).stream().map(OrderDtos.Response::from).toList();
    }

    public OrderDtos.Response findById(Long id) {
        return OrderDtos.Response.from(getOrder(id));
    }

    public OrderDtos.Response findMineById(Long memberId, Long id) {
        return OrderDtos.Response.from(getMine(memberId, id));
    }

    @Transactional
    public OrderDtos.Response update(Long id, OrderDtos.UpdateRequest request) {
        Order order = getOrder(id);
        if (request.status() == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            cancel(id);
            return OrderDtos.Response.from(order);
        }
        order.update(request.status(), request.receiverName().trim(), request.receiverPhone().trim(),
                request.postalCode(), request.address1().trim(), request.address2(), request.paidAt(), request.cancelledAt());
        return OrderDtos.Response.from(order);
    }

    @Transactional
    public void cancelMine(Long memberId, Long id) {
        getMine(memberId, id);
        cancel(id);
    }

    @Transactional
    public void cancel(Long id) {
        Order order = getOrder(id);
        OrderStatus previous = order.getStatus();
        try {
            order.cancel();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        if (previous != OrderStatus.CANCELLED) {
            orderItemRepository.findAllByOrderId(id).forEach(item ->
                    preorderVariantService.release(item.getPreorderVariantId(), item.getQuantity()));
        }
    }

    @Transactional
    public void markPaid(Long id) {
        Order order = getOrder(id);
        try {
            order.markPaid();
            orderItemRepository.findAllByOrderId(id).forEach(item ->
                    preorderVariantService.commit(item.getPreorderVariantId(), item.getQuantity()));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @Transactional
    public void markPreparing(Long id) {
        try { getOrder(id).markPreparing(); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
    }

    @Transactional
    public void markShipped(Long id) {
        try { getOrder(id).markShipped(); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
    }

    @Transactional
    public void markDelivered(Long id) {
        try { getOrder(id).markDelivered(); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
    }

    @Transactional
    public void markRefunded(Long id) {
        getOrder(id).markRefunded();
    }

    @Transactional
    public void delete(Long id) {
        Order order = getOrder(id);
        if (order.getStatus() != OrderStatus.CANCELLED) cancel(id);
        orderRepository.delete(order);
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없어."));
    }

    private Order getMine(Long memberId, Long id) {
        Order order = getOrder(id);
        if (!order.getMember().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 주문만 조회할 수 있어.");
        }
        return order;
    }
}

package com.movrit.movrit.order.service;

import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.order.dto.OrderDtos;
import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.preorder.service.PreorderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final PreorderService preorderService;

    @Transactional
    public OrderDtos.Response create(OrderDtos.CreateRequest request) {
        Order order = Order.builder()
                .orderNumber(request.orderNumber().trim())
                .requestKey(request.requestKey().trim())
                .member(memberService.getMember(request.memberId()))
                .preorder(preorderService.getPreorder(request.preorderId()))
                .attributionId(request.attributionId())
                .acquisitionSource(request.acquisitionSource().trim())
                .status(request.status())
                .totalProductAmount(request.totalProductAmount())
                .discountAmount(request.discountAmount())
                .shippingAmount(request.shippingAmount())
                .totalPaymentAmount(request.totalPaymentAmount())
                .currency(request.currency().trim().toUpperCase())
                .receiverName(request.receiverName().trim())
                .receiverPhone(request.receiverPhone().trim())
                .postalCode(request.postalCode())
                .address1(request.address1().trim())
                .address2(request.address2())
                .build();
        return OrderDtos.Response.from(orderRepository.save(order));
    }

    public List<OrderDtos.Response> findAll() {
        return orderRepository.findAll().stream().map(OrderDtos.Response::from).toList();
    }

    public OrderDtos.Response findById(Long id) {
        return OrderDtos.Response.from(getOrder(id));
    }

    @Transactional
    public OrderDtos.Response update(Long id, OrderDtos.UpdateRequest request) {
        Order order = getOrder(id);
        order.update(request.status(), request.receiverName().trim(), request.receiverPhone().trim(),
                request.postalCode(), request.address1().trim(), request.address2(), request.paidAt(), request.cancelledAt());
        return OrderDtos.Response.from(order);
    }

    @Transactional
    public void delete(Long id) {
        orderRepository.delete(getOrder(id));
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없어."));
    }
}

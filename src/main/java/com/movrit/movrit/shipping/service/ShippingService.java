package com.movrit.movrit.shipping.service;

import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderStatus;
import com.movrit.movrit.order.service.OrderService;
import com.movrit.movrit.shipping.dto.ShippingDtos;
import com.movrit.movrit.shipping.entity.Shipping;
import com.movrit.movrit.shipping.entity.ShippingStatus;
import com.movrit.movrit.shipping.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingService {
    private final ShippingRepository shippingRepository;
    private final OrderService orderService;

    @Transactional
    public ShippingDtos.Response create(ShippingDtos.CreateRequest request) {
        Order order = orderService.getOrder(request.orderId());
        Shipping shipping = Shipping.builder()
                .order(order)
                .courierCode(request.courierCode())
                .trackingNumber(request.trackingNumber())
                .status(request.status())
                .shippedAt(request.shippedAt())
                .deliveredAt(request.deliveredAt())
                .build();
        shippingRepository.save(shipping);
        applyOrderState(order, null, request.status());
        return ShippingDtos.Response.from(shipping);
    }

    public List<ShippingDtos.Response> findAll() {
        return shippingRepository.findAll().stream().map(ShippingDtos.Response::from).toList();
    }

    public ShippingDtos.Response findById(Long id) {
        return ShippingDtos.Response.from(getShipping(id));
    }

    @Transactional
    public ShippingDtos.Response update(Long id, ShippingDtos.UpdateRequest request) {
        Shipping shipping = getShipping(id);
        ShippingStatus previous = shipping.getStatus();
        shipping.update(request.courierCode(), request.trackingNumber(), request.status(), request.shippedAt(), request.deliveredAt());
        applyOrderState(shipping.getOrder(), previous, request.status());
        return ShippingDtos.Response.from(shipping);
    }

    @Transactional
    public void delete(Long id) {
        shippingRepository.delete(getShipping(id));
    }

    private void applyOrderState(Order order, ShippingStatus previous, ShippingStatus current) {
        if (current == previous) return;
        if (current == ShippingStatus.PREPARING && order.getStatus() == OrderStatus.PAID) {
            orderService.markPreparing(order.getId());
        } else if (current == ShippingStatus.SHIPPED) {
            if (order.getStatus() == OrderStatus.PAID) orderService.markPreparing(order.getId());
            orderService.markShipped(order.getId());
        } else if (current == ShippingStatus.DELIVERED) {
            if (order.getStatus() == OrderStatus.PAID) orderService.markPreparing(order.getId());
            if (order.getStatus() == OrderStatus.PREPARING) orderService.markShipped(order.getId());
            orderService.markDelivered(order.getId());
        }
    }

    private Shipping getShipping(Long id) {
        return shippingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없어."));
    }
}

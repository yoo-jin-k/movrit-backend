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

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class ShippingService {
    private final ShippingRepository shippingRepository;
    private final OrderService orderService;

    @Transactional
    public ShippingDtos.Response create(Long memberId, Long orderId) {
        Order order = sellerOrder(memberId, orderId);
        if (order.getStatus() != OrderStatus.PAID) throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 완료 주문만 배송을 준비할 수 있어.");
        Shipping existing = shippingRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) return ShippingDtos.Response.from(existing);
        order.markPreparing();
        return ShippingDtos.Response.from(shippingRepository.save(Shipping.builder().order(order).status(ShippingStatus.READY).build()));
    }

    public List<ShippingDtos.Response> findMine(Long memberId) { return shippingRepository.findAllByOrderPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId).stream().map(ShippingDtos.Response::from).toList(); }
    public List<ShippingDtos.Response> findBuyerShipments(Long memberId) { return shippingRepository.findAllByOrderMemberIdOrderByIdDesc(memberId).stream().map(ShippingDtos.Response::from).toList(); }

    @Transactional public ShippingDtos.Response prepare(Long memberId, Long id) { Shipping s = owned(memberId, id); transit(s::prepare); return ShippingDtos.Response.from(s); }
    @Transactional public ShippingDtos.Response ship(Long memberId, Long id, ShippingDtos.ShipRequest request) {
        Shipping s = owned(memberId, id);
        transit(() -> { s.ship(request.courierCode(), request.trackingNumber()); s.getOrder().markShipped(); });
        return ShippingDtos.Response.from(s);
    }
    @Transactional public ShippingDtos.Response deliver(Long memberId, Long id) { Shipping s = owned(memberId, id); transit(() -> { s.deliver(); s.getOrder().markDelivered(); }); return ShippingDtos.Response.from(s); }

    private Shipping owned(Long memberId, Long id) { Shipping s = shippingRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없어.")); sellerOrder(memberId, s.getOrder().getId()); return s; }
    private Order sellerOrder(Long memberId, Long id) { Order o = orderService.getOrder(id); if (!o.getPreorder().getProduct().getBrand().getSeller().getMember().getId().equals(memberId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 브랜드 주문의 배송만 관리할 수 있어."); return o; }
    private void transit(Runnable action) { try { action.run(); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); } catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); } }
}

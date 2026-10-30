package com.movrit.movrit.shipping.service;

import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderStatus;
import com.movrit.movrit.order.service.OrderService;
import com.movrit.movrit.shipping.dto.ShippingDtos;
import com.movrit.movrit.shipping.entity.Shipping;
import com.movrit.movrit.shipping.entity.ShippingStatus;
import com.movrit.movrit.shipping.repository.ShippingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 업체가 주문 배송을 준비·발송·완료 처리합니다. 배송 상태가 바뀌면 주문 상태도 함께 바꿉니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingService {
  private final ShippingRepository shippingRepository;
  private final OrderService orderService;

  /** 결제 완료 주문의 배송 정보를 만들고 주문을 배송 준비로 바꿉니다. 이미 있으면 기존 배송 정보를 돌려줍니다. */
  @Transactional
  public ShippingDtos.Response create(Long memberId, Long orderId) {
    Order order = sellerOrder(memberId, orderId);
    if (order.getStatus() != OrderStatus.PAID)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 완료 주문만 배송을 준비할 수 있습니다.");
    Shipping existing = shippingRepository.findByOrderId(orderId).orElse(null);
    if (existing != null) return ShippingDtos.Response.from(existing);
    order.markPreparing();
    return ShippingDtos.Response.from(
        shippingRepository.save(
            Shipping.builder().order(order).status(ShippingStatus.READY).build()));
  }

  /** 업체가 소유한 브랜드 주문의 배송 목록을 조회합니다. */
  public List<ShippingDtos.Response> findMine(Long memberId) {
    return shippingRepository
        .findAllByOrderPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
        .stream()
        .map(ShippingDtos.Response::from)
        .toList();
  }

  /** 구매자 본인 주문의 배송 목록을 조회합니다. */
  public List<ShippingDtos.Response> findBuyerShipments(Long memberId) {
    return shippingRepository.findAllByOrderMemberIdOrderByIdDesc(memberId).stream()
        .map(ShippingDtos.Response::from)
        .toList();
  }

  /** 배송 준비를 시작합니다. */
  @Transactional
  public ShippingDtos.Response prepare(Long memberId, Long id) {
    Shipping s = owned(memberId, id);
    transit(s::prepare);
    return ShippingDtos.Response.from(s);
  }

  /** 택배사·운송장 번호를 입력해 발송 처리하고 주문을 발송 상태로 바꿉니다. */
  @Transactional
  public ShippingDtos.Response ship(Long memberId, Long id, ShippingDtos.ShipRequest request) {
    Shipping s = owned(memberId, id);
    transit(
        () -> {
          s.ship(request.courierCode(), request.trackingNumber());
          s.getOrder().markShipped();
        });
    return ShippingDtos.Response.from(s);
  }

  /** 배송 완료로 처리하고 주문을 배송 완료로 바꿉니다. */
  @Transactional
  public ShippingDtos.Response deliver(Long memberId, Long id) {
    Shipping s = owned(memberId, id);
    transit(
        () -> {
          s.deliver();
          s.getOrder().markDelivered();
        });
    return ShippingDtos.Response.from(s);
  }

  /** 배송 정보를 찾고, 요청한 업체의 브랜드 주문인지 확인합니다. */
  private Shipping owned(Long memberId, Long id) {
    Shipping s =
        shippingRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없습니다."));
    sellerOrder(memberId, s.getOrder().getId());
    return s;
  }

  /** 요청한 업체가 주문 상품의 브랜드 소유자인지 확인합니다. 아니면 403입니다. */
  private Order sellerOrder(Long memberId, Long id) {
    Order o = orderService.getOrder(id);
    if (!o.getPreorder().getProduct().getBrand().getSeller().getMember().getId().equals(memberId))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 브랜드 주문의 배송만 관리할 수 있습니다.");
    return o;
  }

  /** 상태 변경 중 입력 오류는 400, 상태 규칙 위반은 409로 바꿔 사유를 전달합니다. */
  private void transit(Runnable action) {
    try {
      action.run();
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }
}

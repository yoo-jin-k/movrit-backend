package com.movrit.movrit.order.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.order.dto.OrderDtos;
import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderItem;
import com.movrit.movrit.order.entity.OrderStatus;
import com.movrit.movrit.order.repository.OrderItemRepository;
import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.product.entity.ProductVariant;
import com.movrit.movrit.product.service.ProductService;
import com.movrit.movrit.collaboration.entity.Collaboration;
import com.movrit.movrit.collaboration.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberService memberService;
    private final PreorderService preorderService;
    private final ProductService productService;
    private final CollaborationService collaborationService;

    @Transactional
    public OrderDtos.Response create(Long memberId, OrderDtos.CreateRequest request) {
        Order existing = orderRepository.findByRequestKey(request.requestKey().trim()).orElse(null);
        if (existing != null) {
            if (!existing.getMember().getId().equals(memberId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용된 요청 키야.");
            return response(existing);
        }

        Member member = memberService.getMember(memberId);
        // 프리오더를 먼저 잠그고 SKU를 잠가 모든 주문이 같은 순서로 재고를 확인한다.
        Preorder preorder = preorderService.getPreorderForOrder(request.preorderId());
        ProductVariant variant = productService.getVariantForOrder(request.productVariantId());
        if (!variant.getProduct().getId().equals(preorder.getProduct().getId())
                || !preorder.canSellVariant(variant.getId())
                || !variant.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프리오더에서 판매 중인 SKU가 아니야.");
        }
        long alreadyOrdered = orderRepository.findAllByMemberIdOrderByIdDesc(memberId).stream()
                .filter(order -> order.getPreorder().getId().equals(preorder.getId()))
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.REFUNDED)
                .flatMap(order -> orderItemRepository.findAllByOrderId(order.getId()).stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();
        if (preorder.getMaxOrderQuantityPerMember() != null
                && alreadyOrdered + request.quantity() > preorder.getMaxOrderQuantityPerMember()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원별 최대 주문 수량을 초과해.");
        }

        try {
            variant.reserve(request.quantity());
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        preorderService.addOrderQuantity(preorder, request.quantity());

        BigDecimal unitPrice = preorder.getPreorderPrice().add(variant.getAdditionalPrice());
        BigDecimal productAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        Collaboration attribution = request.referralCode() == null || request.referralCode().isBlank()
                ? null : collaborationService.findByReferral(request.referralCode().trim());
        if (attribution != null && !attribution.getPreorder().getId().equals(preorder.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 프리오더의 추천코드야.");
        Order order = orderRepository.save(Order.builder()
                .orderNumber("MV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")))
                .requestKey(request.requestKey().trim()).member(member).preorder(preorder)
                .attributionId(attribution == null ? null : attribution.getId())
                .acquisitionSource(attribution == null ? blankToDefault(request.acquisitionSource(), "DIRECT") : "INFLUENCER")
                .status(OrderStatus.PENDING_PAYMENT).totalProductAmount(productAmount)
                .discountAmount(BigDecimal.ZERO).shippingAmount(BigDecimal.ZERO)
                .totalPaymentAmount(productAmount).currency(preorder.getCurrency())
                .receiverName(request.receiverName().trim()).receiverPhone(request.receiverPhone().trim())
                .postalCode(request.postalCode()).address1(request.address1().trim()).address2(request.address2())
                .build());

        String optionName = optionLabel(variant);
        OrderItem item = orderItemRepository.save(OrderItem.builder().order(order).product(variant.getProduct())
                .productVariantId(variant.getId()).productName(variant.getProduct().getName())
                .optionName(optionName).unitPrice(unitPrice).quantity(request.quantity())
                .totalPrice(productAmount).createdAt(LocalDateTime.now()).build());
        return OrderDtos.Response.from(order, List.of(item));
    }

    public List<OrderDtos.Response> findMine(Long memberId) {
        return orderRepository.findAllByMemberIdOrderByIdDesc(memberId).stream().map(this::response).toList();
    }

    public List<OrderDtos.Response> findSellerOrders(Long memberId) {
        return orderRepository.findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId).stream().map(this::response).toList();
    }

    public OrderDtos.Response findById(Long memberId, Long id) { return response(getAccessibleOrder(memberId, id)); }

    @Transactional
    public OrderDtos.Response updateReceiver(Long memberId, Long id, OrderDtos.ReceiverUpdateRequest request) {
        Order order = getMemberOrder(memberId, id);
        try {
            order.updateReceiver(request.receiverName().trim(), request.receiverPhone().trim(), request.postalCode(),
                    request.address1().trim(), request.address2());
        } catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        return response(order);
    }

    @Transactional
    public OrderDtos.Response cancel(Long memberId, Long id) {
        Order order = getMemberOrder(memberId, id);
        List<OrderItem> items = orderItemRepository.findAllByOrderId(id);
        try {
            order.cancel();
            for (OrderItem item : items) productService.getVariant(item.getProductVariantId()).releaseReservation(item.getQuantity());
            preorderService.removeOrderQuantity(order.getPreorder(), items.stream().mapToLong(OrderItem::getQuantity).sum());
        } catch (IllegalArgumentException | IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        return OrderDtos.Response.from(order, items);
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없어."));
    }

    public List<OrderItem> getItems(Long orderId) { return orderItemRepository.findAllByOrderId(orderId); }

    private Order getMemberOrder(Long memberId, Long id) {
        Order order = getOrder(id);
        if (!order.getMember().getId().equals(memberId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 주문만 관리할 수 있어.");
        return order;
    }

    private Order getAccessibleOrder(Long memberId, Long id) {
        Order order = getOrder(id);
        boolean buyer = order.getMember().getId().equals(memberId);
        boolean seller = order.getPreorder().getProduct().getBrand().getSeller().getMember().getId().equals(memberId);
        if (!buyer && !seller) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 주문을 조회할 권한이 없어.");
        return order;
    }

    private OrderDtos.Response response(Order order) { return OrderDtos.Response.from(order, getItems(order.getId())); }
    private String blankToDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private String optionLabel(ProductVariant variant) {
        String first = variant.getOptionValue1() == null ? "" : variant.getOptionValue1();
        String second = variant.getOptionValue2() == null ? "" : " / " + variant.getOptionValue2();
        return (first + second).isBlank() ? "기본" : first + second;
    }
}

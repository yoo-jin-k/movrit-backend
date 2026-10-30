package com.movrit.movrit.order.dto;

import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderItem;
import com.movrit.movrit.order.entity.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 주문 API에서 주고받는 요청·응답 형식을 모아 둔 클래스입니다. */
public final class OrderDtos {
  private OrderDtos() {}

  /**
   * 주문의 생성 요청을 전달합니다.
   *
   * @param requestKey 주문 중복 생성을 방지하기 위한 요청 식별값입니다. 빈 문자열을 허용하지 않습니다.
   * @param preorderId 연결된 프리오더 식별자입니다. null을 허용하지 않습니다.
   * @param productVariantId 선택한 상품 옵션 식별자입니다. null을 허용하지 않습니다.
   * @param quantity 수량입니다. null을 허용하지 않습니다. 최솟값은 1입니다.
   * @param referralCode 추천 추적 코드입니다.
   * @param acquisitionSource 유입 경로입니다.
   * @param receiverName 주문 수령인 이름입니다. 빈 문자열을 허용하지 않습니다.
   * @param receiverPhone 주문 수령인 연락처입니다. 빈 문자열을 허용하지 않습니다.
   * @param postalCode 우편번호입니다.
   * @param address1 기본 주소입니다. 빈 문자열을 허용하지 않습니다.
   * @param address2 상세 주소입니다.
   */
  public record CreateRequest(
      @NotBlank String requestKey,
      @NotNull Long preorderId,
      @NotNull Long productVariantId,
      @NotNull @Min(1) Long quantity,
      String referralCode,
      String acquisitionSource,
      @NotBlank String receiverName,
      @NotBlank String receiverPhone,
      String postalCode,
      @NotBlank String address1,
      String address2) {}

  /**
   * 주문의 주문 배송지 수정 요청을 전달합니다.
   *
   * @param receiverName 주문 수령인 이름입니다. 빈 문자열을 허용하지 않습니다.
   * @param receiverPhone 주문 수령인 연락처입니다. 빈 문자열을 허용하지 않습니다.
   * @param postalCode 우편번호입니다.
   * @param address1 기본 주소입니다. 빈 문자열을 허용하지 않습니다.
   * @param address2 상세 주소입니다.
   */
  public record ReceiverUpdateRequest(
      @NotBlank String receiverName,
      @NotBlank String receiverPhone,
      String postalCode,
      @NotBlank String address1,
      String address2) {}

  /**
   * 주문의 주문 항목 응답을 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param productId 상품 식별자입니다.
   * @param productVariantId 선택한 상품 옵션 식별자입니다.
   * @param productName 상품명입니다.
   * @param optionName 주문 시점의 옵션 표시 이름입니다.
   * @param unitPrice 주문 항목 단가입니다.
   * @param quantity 수량입니다.
   * @param totalPrice 주문 항목 합계 금액입니다.
   */
  public record ItemResponse(
      Long id,
      Long productId,
      Long productVariantId,
      String productName,
      String optionName,
      BigDecimal unitPrice,
      Long quantity,
      BigDecimal totalPrice) {
    /** 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
    public static ItemResponse from(OrderItem item) {
      return new ItemResponse(
          item.getId(),
          item.getProduct().getId(),
          item.getProductVariantId(),
          item.getProductName(),
          item.getOptionName(),
          item.getUnitPrice(),
          item.getQuantity(),
          item.getTotalPrice());
    }
  }

  /**
   * 주문의 조회 및 처리 결과를 전달합니다.
   *
   * @param id 대상 데이터의 식별자입니다.
   * @param orderNumber 주문 표시 번호입니다.
   * @param memberId 회원 식별자입니다.
   * @param preorderId 연결된 프리오더 식별자입니다.
   * @param preorderTitle 프리오더 제목입니다.
   * @param status 현재 업무 상태입니다.
   * @param totalProductAmount 상품 금액 합계입니다.
   * @param discountAmount 할인 금액입니다.
   * @param shippingAmount 배송비입니다.
   * @param totalPaymentAmount 최종 결제 대상 금액입니다.
   * @param currency 금액의 통화 코드입니다.
   * @param receiverName 주문 수령인 이름입니다.
   * @param receiverPhone 주문 수령인 연락처입니다.
   * @param postalCode 우편번호입니다.
   * @param address1 기본 주소입니다.
   * @param address2 상세 주소입니다.
   * @param paidAt 결제 완료 시각입니다.
   * @param cancelledAt 취소 시각입니다.
   * @param items 주문 항목 목록입니다.
   */
  public record Response(
      Long id,
      String orderNumber,
      Long memberId,
      Long preorderId,
      String preorderTitle,
      OrderStatus status,
      BigDecimal totalProductAmount,
      BigDecimal discountAmount,
      BigDecimal shippingAmount,
      BigDecimal totalPaymentAmount,
      String currency,
      String receiverName,
      String receiverPhone,
      String postalCode,
      String address1,
      String address2,
      LocalDateTime paidAt,
      LocalDateTime cancelledAt,
      List<ItemResponse> items) {
    /** 엔티티를 화면에 내려줄 응답 형식으로 바꿉니다. 권한 확인은 이 메서드를 호출하는 서비스가 맡습니다. */
    public static Response from(Order order, List<OrderItem> items) {
      return new Response(
          order.getId(),
          order.getOrderNumber(),
          order.getMember().getId(),
          order.getPreorder().getId(),
          order.getPreorder().getTitle(),
          order.getStatus(),
          order.getTotalProductAmount(),
          order.getDiscountAmount(),
          order.getShippingAmount(),
          order.getTotalPaymentAmount(),
          order.getCurrency(),
          order.getReceiverName(),
          order.getReceiverPhone(),
          order.getPostalCode(),
          order.getAddress1(),
          order.getAddress2(),
          order.getPaidAt(),
          order.getCancelledAt(),
          items.stream().map(ItemResponse::from).toList());
    }
  }
}

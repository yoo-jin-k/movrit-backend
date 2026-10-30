package com.movrit.movrit.order.entity;

import com.movrit.movrit.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 주문에 담긴 상품 한 줄(옵션·단가·수량·합계)입니다. 상품명·옵션명은 주문 시점 값을 복사해 보관하므로 나중에 상품을 고쳐도 바뀌지 않습니다. */
@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "product_variant_id")
  private Long productVariantId;

  @Column(name = "preorder_variant_id")
  private Long preorderVariantId;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(name = "option_name")
  private String optionName;

  @Column(name = "unit_price", nullable = false)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private Long quantity;

  @Column(name = "total_price", nullable = false)
  private BigDecimal totalPrice;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}

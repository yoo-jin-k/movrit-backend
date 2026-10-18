package com.movrit.movrit.order.entity;

import com.movrit.movrit.product.entity.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

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

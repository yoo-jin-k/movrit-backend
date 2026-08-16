package com.movrit.movrit.preorder.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "preorder")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Preorder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "preorder_price", nullable = false)
    private BigDecimal preorderPrice;

    @Column(name = "target_quantity", nullable = false)
    private Long targetQuantity;

    @Column(name = "max_quantity")
    private Long maxQuantity;

    @Column(name = "ordered_quantity", nullable = false)
    private Long orderedQuantity;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "production_expected_at")
    private LocalDateTime productionExpectedAt;

    @Column(name = "shipping_expected_at")
    private LocalDateTime shippingExpectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreorderStatus status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

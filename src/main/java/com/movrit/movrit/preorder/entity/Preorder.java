package com.movrit.movrit.preorder.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

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

    public void update(String title, String description, BigDecimal preorderPrice, String currency,
                       Long targetQuantity, Long maxQuantity, LocalDateTime startAt, LocalDateTime endAt,
                       LocalDateTime productionExpectedAt, LocalDateTime shippingExpectedAt, PreorderStatus status,
                       String cancellationReason) {
        this.title = title;
        this.description = description;
        this.preorderPrice = preorderPrice;
        this.currency = currency;
        this.targetQuantity = targetQuantity;
        this.maxQuantity = maxQuantity;
        this.startAt = startAt;
        this.endAt = endAt;
        this.productionExpectedAt = productionExpectedAt;
        this.shippingExpectedAt = shippingExpectedAt;
        this.status = status;
        this.cancellationReason = cancellationReason;
    }

    public void increaseOrderedQuantity(long quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("주문 수량은 1개 이상이어야 해.");
        long next = orderedQuantity + quantity;
        if (maxQuantity != null && next > maxQuantity) {
            throw new IllegalStateException("공동구매 최대 판매수량을 초과했어.");
        }
        orderedQuantity = next;
    }

    public void decreaseOrderedQuantity(long quantity) {
        if (quantity <= 0) return;
        orderedQuantity = Math.max(0L, orderedQuantity - quantity);
    }

    public boolean targetReached() {
        return orderedQuantity >= targetQuantity;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}

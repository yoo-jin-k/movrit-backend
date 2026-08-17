package com.movrit.movrit.preorder.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.product.entity.ProductSku;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "preorder_variant", uniqueConstraints = @UniqueConstraint(name = "uk_preorder_variant", columnNames = {"preorder_id", "product_sku_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PreorderVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preorder_id", nullable = false)
    private Preorder preorder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_sku_id", nullable = false)
    private ProductSku productSku;

    @Column(name = "sale_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "max_quantity")
    private Long maxQuantity;

    @Column(name = "ordered_quantity", nullable = false)
    private Long orderedQuantity;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private Long version;

    public long remainingQuantity() {
        return maxQuantity == null ? Long.MAX_VALUE : Math.max(0L, maxQuantity - orderedQuantity);
    }

    public void reserve(long quantity) {
        if (!active) throw new IllegalStateException("판매 중지된 옵션이야.");
        if (quantity <= 0) throw new IllegalArgumentException("주문 수량은 1개 이상이어야 해.");
        if (remainingQuantity() < quantity) throw new IllegalStateException("공동구매 옵션 판매수량을 초과했어.");
        orderedQuantity += quantity;
    }

    public void release(long quantity) {
        if (quantity <= 0) return;
        orderedQuantity = Math.max(0L, orderedQuantity - quantity);
    }
}

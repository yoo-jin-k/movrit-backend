package com.movrit.movrit.inventory.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.product.entity.ProductSku;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_sku_id", nullable = false, unique = true)
    private ProductSku productSku;

    @Column(name = "on_hand_quantity", nullable = false)
    private Long onHandQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Long reservedQuantity;

    @Column(name = "safety_stock_quantity", nullable = false)
    private Long safetyStockQuantity;

    @Version
    @Column(nullable = false)
    private Long version;

    public long availableQuantity() {
        return Math.max(0L, onHandQuantity - reservedQuantity - safetyStockQuantity);
    }

    public void receive(long quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("입고 수량은 1개 이상이어야 해.");
        this.onHandQuantity += quantity;
    }

    public void reserve(long quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("예약 수량은 1개 이상이어야 해.");
        if (availableQuantity() < quantity) throw new IllegalStateException("판매 가능한 재고가 부족해.");
        this.reservedQuantity += quantity;
    }

    public void release(long quantity) {
        if (quantity <= 0) return;
        this.reservedQuantity = Math.max(0L, this.reservedQuantity - quantity);
    }

    public void commit(long quantity) {
        if (quantity <= 0) return;
        if (reservedQuantity < quantity || onHandQuantity < quantity) {
            throw new IllegalStateException("확정할 예약 재고가 부족해.");
        }
        this.reservedQuantity -= quantity;
        this.onHandQuantity -= quantity;
    }

    public void changeSafetyStock(long quantity) {
        if (quantity < 0) throw new IllegalArgumentException("안전재고는 0개 이상이어야 해.");
        this.safetyStockQuantity = quantity;
    }
}

package com.movrit.movrit.preorder.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // 목표 수량에 도달하면 프리오더 성공 판단에 사용한다.
    @Column(name = "target_quantity", nullable = false)
    private Long targetQuantity;

    // 전체 판매 상한. null이면 별도 상한 없이 SKU 재고를 기준으로 판매한다.
    @Column(name = "max_quantity")
    private Long maxQuantity;

    @Column(name = "ordered_quantity", nullable = false)
    private Long orderedQuantity;

    @Column(name = "min_order_quantity", nullable = false)
    private Long minOrderQuantity;

    @Column(name = "max_order_quantity_per_member")
    private Long maxOrderQuantityPerMember;

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
                       Long targetQuantity, Long maxQuantity, Long minOrderQuantity,
                       Long maxOrderQuantityPerMember, LocalDateTime startAt, LocalDateTime endAt,
                       LocalDateTime productionExpectedAt, LocalDateTime shippingExpectedAt) {
        if (status == PreorderStatus.OPEN || status == PreorderStatus.SUCCESS || status == PreorderStatus.FAILED) {
            throw new IllegalStateException("판매가 시작되거나 종료된 프리오더의 핵심 조건은 수정할 수 없어.");
        }
        this.title = title;
        this.description = description;
        this.preorderPrice = preorderPrice;
        this.currency = currency;
        this.targetQuantity = targetQuantity;
        this.maxQuantity = maxQuantity;
        this.minOrderQuantity = minOrderQuantity;
        this.maxOrderQuantityPerMember = maxOrderQuantityPerMember;
        this.startAt = startAt;
        this.endAt = endAt;
        this.productionExpectedAt = productionExpectedAt;
        this.shippingExpectedAt = shippingExpectedAt;
    }

    public void schedule() {
        if (status != PreorderStatus.DRAFT) throw new IllegalStateException("초안 상태에서만 판매 예약할 수 있어.");
        this.status = PreorderStatus.SCHEDULED;
    }

    public void open(LocalDateTime now) {
        if (status != PreorderStatus.SCHEDULED && status != PreorderStatus.DRAFT) {
            throw new IllegalStateException("예약 또는 초안 상태에서만 판매를 시작할 수 있어.");
        }
        if (now.isBefore(startAt)) throw new IllegalStateException("프리오더 시작 시간이 아직 안 됐어.");
        if (!now.isBefore(endAt)) throw new IllegalStateException("이미 종료 시간이 지난 프리오더야.");
        this.status = PreorderStatus.OPEN;
    }

    public void close() {
        if (status != PreorderStatus.OPEN) throw new IllegalStateException("판매 중인 프리오더만 마감할 수 있어.");
        this.status = PreorderStatus.CLOSED;
    }

    /** 마감 후 목표 수량 달성 여부를 확정한다. */
    public void finalizeResult() {
        if (status != PreorderStatus.CLOSED) throw new IllegalStateException("마감된 프리오더만 결과를 확정할 수 있어.");
        this.status = orderedQuantity >= targetQuantity ? PreorderStatus.SUCCESS : PreorderStatus.FAILED;
    }

    public void cancel(String reason) {
        if (status == PreorderStatus.SUCCESS || status == PreorderStatus.FAILED) {
            throw new IllegalStateException("결과가 확정된 프리오더는 취소할 수 없어.");
        }
        this.status = PreorderStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    public void increaseOrderedQuantity(long quantity) {
        if (status != PreorderStatus.OPEN) throw new IllegalStateException("판매 중인 프리오더가 아니야.");
        if (quantity < minOrderQuantity) throw new IllegalStateException("최소 주문 수량보다 적어.");
        if (maxQuantity != null && orderedQuantity + quantity > maxQuantity) {
            throw new IllegalStateException("프리오더 최대 판매 수량을 초과해.");
        }
        this.orderedQuantity += quantity;
    }

    public void decreaseOrderedQuantity(long quantity) {
        if (quantity <= 0 || orderedQuantity < quantity) throw new IllegalArgumentException("취소 수량이 올바르지 않아.");
        this.orderedQuantity -= quantity;
    }

    public void softDelete() {
        if (status == PreorderStatus.OPEN) throw new IllegalStateException("판매 중인 프리오더는 삭제할 수 없어.");
        this.deletedAt = LocalDateTime.now();
    }
}

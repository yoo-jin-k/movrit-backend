package com.movrit.movrit.preorder.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "preorder_variant",
            joinColumns = @JoinColumn(name = "preorder_id"),
            inverseJoinColumns = @JoinColumn(name = "product_variant_id")
    )
    @Builder.Default
    private Set<ProductVariant> saleVariants = new LinkedHashSet<>();

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
        if (status == PreorderStatus.REVIEW || status == PreorderStatus.CANCEL_REQUESTED
                || status == PreorderStatus.OPEN || status == PreorderStatus.CLOSED
                || status == PreorderStatus.SUCCESS || status == PreorderStatus.FAILED
                || status == PreorderStatus.PRODUCTION || status == PreorderStatus.SHIPPING
                || status == PreorderStatus.COMPLETED || status == PreorderStatus.CANCELLED) {
            throw new IllegalStateException("검토 중이거나 판매가 시작된 프리오더의 핵심 조건은 수정할 수 없어.");
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

    public void replaceSaleVariants(Collection<ProductVariant> variants) {
        if (status != PreorderStatus.DRAFT && status != PreorderStatus.SCHEDULED) {
            throw new IllegalStateException("초안 또는 승인된 예약 상태에서만 SKU 구성을 수정할 수 있어.");
        }
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("판매할 SKU를 하나 이상 선택해줘.");
        }
        this.saleVariants.clear();
        this.saleVariants.addAll(variants);
    }

    public boolean canSellVariant(Long variantId) {
        return saleVariants.stream().anyMatch(variant -> variant.getId().equals(variantId));
    }

    /** 판매자가 관리자 검토를 요청한다. */
    public void requestReview() {
        if (status != PreorderStatus.DRAFT) {
            throw new IllegalStateException("초안 상태에서만 검토를 요청할 수 있어.");
        }
        if (saleVariants.isEmpty()) {
            throw new IllegalStateException("판매할 SKU를 하나 이상 연결해줘.");
        }
        status = PreorderStatus.REVIEW;
    }

    /** 관리자가 검토를 승인해 판매 예약 상태로 전환한다. */
    public void approveReview() {
        if (status != PreorderStatus.REVIEW) {
            throw new IllegalStateException("검토 중인 프리오더만 승인할 수 있어.");
        }
        status = PreorderStatus.SCHEDULED;
    }

    /** 관리자가 수정을 요청하면 판매자가 다시 편집할 수 있도록 초안으로 돌린다. */
    public void requestChanges() {
        if (status != PreorderStatus.REVIEW) {
            throw new IllegalStateException("검토 중인 프리오더만 수정 요청할 수 있어.");
        }
        status = PreorderStatus.DRAFT;
    }

    public void open(LocalDateTime now) {
        if (status != PreorderStatus.SCHEDULED) {
            throw new IllegalStateException("관리자 검토가 승인된 예약 상태에서만 판매를 시작할 수 있어.");
        }
        if (saleVariants.isEmpty()) throw new IllegalStateException("판매할 SKU를 하나 이상 연결해줘.");
        if (saleVariants.stream().noneMatch(variant -> variant.isActive() && variant.getAvailableQuantity() > 0)) {
            throw new IllegalStateException("판매 가능한 재고가 있는 SKU가 없어.");
        }
        if (now.isBefore(startAt)) throw new IllegalStateException("프리오더 시작 시간이 아직 안 됐어.");
        if (!now.isBefore(endAt)) throw new IllegalStateException("이미 종료 시간이 지난 프리오더야.");
        this.status = PreorderStatus.OPEN;
    }

    public void close() {
        if (status != PreorderStatus.OPEN) throw new IllegalStateException("판매 중인 프리오더만 마감할 수 있어.");
        this.status = PreorderStatus.CLOSED;
    }

    public void finalizeResult() {
        if (status != PreorderStatus.CLOSED) throw new IllegalStateException("마감된 프리오더만 결과를 확정할 수 있어.");
        this.status = orderedQuantity >= targetQuantity ? PreorderStatus.SUCCESS : PreorderStatus.FAILED;
    }

    /**
     * 판매 전에는 즉시 취소하고, 판매 중에는 관리자 승인 대기로 전환한다.
     */
    public void cancel(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("취소 사유를 입력해줘.");
        }
        if (status == PreorderStatus.DRAFT || status == PreorderStatus.REVIEW || status == PreorderStatus.SCHEDULED) {
            status = PreorderStatus.CANCELLED;
            cancellationReason = reason;
            return;
        }
        if (status == PreorderStatus.OPEN) {
            status = PreorderStatus.CANCEL_REQUESTED;
            cancellationReason = reason;
            return;
        }
        throw new IllegalStateException("현재 상태에서는 프리오더 취소를 요청할 수 없어.");
    }

    public void approveCancellation() {
        if (status != PreorderStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("취소 승인 대기 프리오더가 아니야.");
        }
        status = PreorderStatus.CANCELLED;
    }

    public void rejectCancellation() {
        if (status != PreorderStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("취소 승인 대기 프리오더가 아니야.");
        }
        status = PreorderStatus.OPEN;
        cancellationReason = null;
    }

    public void startProduction() {
        if (status != PreorderStatus.SUCCESS) throw new IllegalStateException("성공 확정 프리오더만 생산을 시작할 수 있어.");
        status = PreorderStatus.PRODUCTION;
    }

    public void startShipping() {
        if (status != PreorderStatus.PRODUCTION) throw new IllegalStateException("생산 중 프리오더만 배송 단계로 변경할 수 있어.");
        status = PreorderStatus.SHIPPING;
    }

    public void complete() {
        if (status != PreorderStatus.SHIPPING) throw new IllegalStateException("배송 단계 프리오더만 완료할 수 있어.");
        status = PreorderStatus.COMPLETED;
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
        if (status == PreorderStatus.OPEN || status == PreorderStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("판매 중이거나 취소 심사 중인 프리오더는 삭제할 수 없어.");
        }
        this.deletedAt = LocalDateTime.now();
    }
}

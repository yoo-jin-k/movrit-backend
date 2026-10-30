package com.movrit.movrit.preorder.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.entity.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 상품을 미리 주문받는 프리오더 한 건입니다.
 *
 * <p>초안(DRAFT) → 판매 예약(SCHEDULED) → 판매 중(OPEN) → 마감(CLOSED) → 성공(SUCCESS) 또는 실패(FAILED)로 진행합니다.
 * 성공하면 생산(PRODUCTION) → 배송(SHIPPING) → 완료(COMPLETED)로 이어지고, 결과 확정 전에는 취소(CANCELLED)할 수 있습니다. 목표 수량을
 * 채우면 성공, 못 채우면 실패입니다.
 */
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

  /** 현재 주문 흐름에서 판매를 허용한 SKU 연결입니다. 가격·주문 참조를 가진 레거시 preorder_variant와 분리해 기존 이력을 보존합니다. */
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "preorder_sale_variant",
      joinColumns = @JoinColumn(name = "preorder_id"),
      inverseJoinColumns = @JoinColumn(name = "product_variant_id"))
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

  // 목표 수량에 도달하면 프리오더 성공 판단에 사용합니다.
  @Column(name = "target_quantity", nullable = false)
  private Long targetQuantity;

  // 전체 판매 상한. null이면 별도 상한 없이 SKU 재고를 기준으로 판매합니다.
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

  @Column(name = "cancellation_reason", length = 500)
  private String cancellationReason;

  @Version
  @Column(nullable = false)
  private Long version;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** 프리오더 조건(제목·가격·수량·기간 등)을 고칩니다. 판매가 시작되었거나 결과가 확정된 뒤에는 고칠 수 없습니다. */
  public void update(
      String title,
      String description,
      BigDecimal preorderPrice,
      String currency,
      Long targetQuantity,
      Long maxQuantity,
      Long minOrderQuantity,
      Long maxOrderQuantityPerMember,
      LocalDateTime startAt,
      LocalDateTime endAt,
      LocalDateTime productionExpectedAt,
      LocalDateTime shippingExpectedAt) {
    if (status == PreorderStatus.OPEN
        || status == PreorderStatus.SUCCESS
        || status == PreorderStatus.FAILED) {
      throw new IllegalStateException("판매가 시작되거나 종료된 프리오더의 핵심 조건은 수정할 수 없습니다.");
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

  /** 판매할 옵션(SKU) 구성을 바꿉니다. 판매가 시작된 뒤에는 바꿀 수 없고, 하나 이상 선택해야 합니다. */
  public void replaceSaleVariants(Collection<ProductVariant> variants) {
    if (status == PreorderStatus.OPEN
        || status == PreorderStatus.CLOSED
        || status == PreorderStatus.SUCCESS
        || status == PreorderStatus.FAILED) {
      throw new IllegalStateException("판매가 시작된 프리오더의 SKU 구성은 변경할 수 없습니다.");
    }
    if (variants == null || variants.isEmpty()) {
      throw new IllegalArgumentException("판매할 SKU를 하나 이상 선택해 주세요.");
    }
    this.saleVariants.clear();
    this.saleVariants.addAll(variants);
  }

  /** 이 프리오더에서 판매하는 옵션인지 확인합니다. */
  public boolean canSellVariant(Long variantId) {
    return saleVariants.stream().anyMatch(variant -> variant.getId().equals(variantId));
  }

  /** 초안을 판매 예약 상태로 바꿉니다. 판매할 옵션이 하나 이상 있어야 합니다. */
  public void schedule() {
    if (status != PreorderStatus.DRAFT) throw new IllegalStateException("초안 상태에서만 판매 예약할 수 있습니다.");
    if (saleVariants.isEmpty()) throw new IllegalStateException("판매할 SKU를 하나 이상 연결해 주세요.");
    this.status = PreorderStatus.SCHEDULED;
  }

  /** 판매를 시작합니다. 재고가 있는 활성 옵션이 있어야 하고, 시작 시각 이후·종료 시각 이전이어야 합니다. */
  public void open(LocalDateTime now) {
    if (status != PreorderStatus.SCHEDULED && status != PreorderStatus.DRAFT) {
      throw new IllegalStateException("예약 또는 초안 상태에서만 판매를 시작할 수 있습니다.");
    }
    if (saleVariants.isEmpty()) throw new IllegalStateException("판매할 SKU를 하나 이상 연결해 주세요.");
    if (saleVariants.stream()
        .noneMatch(variant -> variant.isActive() && variant.getAvailableQuantity() > 0)) {
      throw new IllegalStateException("판매 가능한 재고가 있는 SKU가 없습니다.");
    }
    if (now.isBefore(startAt)) throw new IllegalStateException("프리오더 시작 시간이 아직 되지 않았습니다.");
    if (!now.isBefore(endAt)) throw new IllegalStateException("이미 종료 시간이 지난 프리오더입니다.");
    this.status = PreorderStatus.OPEN;
  }

  /** 판매 중인 프리오더를 마감합니다. */
  public void close() {
    if (status != PreorderStatus.OPEN) throw new IllegalStateException("판매 중인 프리오더만 마감할 수 있습니다.");
    this.status = PreorderStatus.CLOSED;
  }

  /** 마감 후 목표 수량 달성 여부를 확정합니다. */
  public void finalizeResult() {
    if (status != PreorderStatus.CLOSED)
      throw new IllegalStateException("마감된 프리오더만 결과를 확정할 수 있습니다.");
    this.status =
        orderedQuantity >= targetQuantity ? PreorderStatus.SUCCESS : PreorderStatus.FAILED;
  }

  /** 프리오더를 취소하고 사유를 기록합니다. 성공·실패가 확정된 뒤에는 취소할 수 없습니다. */
  public void cancel(String reason) {
    if (status == PreorderStatus.SUCCESS || status == PreorderStatus.FAILED) {
      throw new IllegalStateException("결과가 확정된 프리오더는 취소할 수 없습니다.");
    }
    this.status = PreorderStatus.CANCELLED;
    this.cancellationReason = reason;
  }

  /** 성공한 프리오더의 생산을 시작합니다. */
  public void startProduction() {
    if (status != PreorderStatus.SUCCESS)
      throw new IllegalStateException("성공 확정 프리오더만 생산을 시작할 수 있습니다.");
    status = PreorderStatus.PRODUCTION;
  }

  /** 생산 중인 프리오더를 배송 단계로 넘깁니다. */
  public void startShipping() {
    if (status != PreorderStatus.PRODUCTION)
      throw new IllegalStateException("생산 중 프리오더만 배송 단계로 변경할 수 있습니다.");
    status = PreorderStatus.SHIPPING;
  }

  /** 배송 단계 프리오더를 완료합니다. */
  public void complete() {
    if (status != PreorderStatus.SHIPPING)
      throw new IllegalStateException("배송 단계 프리오더만 완료할 수 있습니다.");
    status = PreorderStatus.COMPLETED;
  }

  /** 주문이 들어오면 주문 수량을 늘립니다. 판매 중이어야 하고, 최소 주문 수량 이상·최대 판매 수량 이하여야 합니다. */
  public void increaseOrderedQuantity(long quantity) {
    if (status != PreorderStatus.OPEN) throw new IllegalStateException("판매 중인 프리오더가 아닙니다.");
    if (quantity < minOrderQuantity) throw new IllegalStateException("최소 주문 수량보다 적습니다.");
    if (maxQuantity != null && orderedQuantity + quantity > maxQuantity) {
      throw new IllegalStateException("프리오더 최대 판매 수량을 초과합니다.");
    }
    this.orderedQuantity += quantity;
  }

  /** 주문 취소·환불 시 주문 수량을 줄입니다. */
  public void decreaseOrderedQuantity(long quantity) {
    if (quantity <= 0 || orderedQuantity < quantity)
      throw new IllegalArgumentException("취소 수량이 올바르지 않습니다.");
    this.orderedQuantity -= quantity;
  }

  /** 프리오더를 삭제 표시합니다(데이터는 남깁니다). 판매 중에는 삭제할 수 없습니다. */
  public void softDelete() {
    if (status == PreorderStatus.OPEN) throw new IllegalStateException("판매 중인 프리오더는 삭제할 수 없습니다.");
    this.deletedAt = LocalDateTime.now();
  }
}

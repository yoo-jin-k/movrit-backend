package com.movrit.movrit.product.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_variant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  // 옵션이 없는 단일 상품도 variant 1개를 갖도록 하면 재고 로직을 한 곳에서 처리할 수 있습니다.
  @Column(name = "sku_code", nullable = false, unique = true, length = 80)
  private String skuCode;

  @Column(name = "option_name_1", length = 50)
  private String optionName1;

  @Column(name = "option_value_1", length = 100)
  private String optionValue1;

  @Column(name = "option_name_2", length = 50)
  private String optionName2;

  @Column(name = "option_value_2", length = 100)
  private String optionValue2;

  @Column(name = "additional_price", nullable = false)
  private BigDecimal additionalPrice;

  // 실제 창고에 존재하는 수량.
  @Column(name = "on_hand_quantity", nullable = false)
  private Long onHandQuantity;

  // 주문은 생성됐지만 아직 최종 출고되지 않아 임시로 잡혀 있는 수량.
  @Column(name = "reserved_quantity", nullable = false)
  private Long reservedQuantity;

  // 품절 직전 버퍼로 남겨두는 수량. 판매가능수량 계산에서 제외합니다.
  @Column(name = "safety_stock_quantity", nullable = false)
  private Long safetyStockQuantity;

  @Column(name = "sold_quantity", nullable = false)
  private Long soldQuantity;

  @Column(nullable = false)
  private boolean active;

  @Version private Long version;

  public long getAvailableQuantity() {
    return Math.max(0L, onHandQuantity - reservedQuantity - safetyStockQuantity);
  }

  public void updateOption(
      String optionName1,
      String optionValue1,
      String optionName2,
      String optionValue2,
      BigDecimal additionalPrice,
      boolean active) {
    this.optionName1 = optionName1;
    this.optionValue1 = optionValue1;
    this.optionName2 = optionName2;
    this.optionValue2 = optionValue2;
    this.additionalPrice = additionalPrice;
    this.active = active;
  }

  public void deactivate() {
    if (reservedQuantity > 0) throw new IllegalStateException("예약된 재고가 있는 SKU는 비활성화할 수 없습니다.");
    this.active = false;
  }

  public void restock(long quantity) {
    requirePositive(quantity);
    this.onHandQuantity += quantity;
  }

  public void adjustSafetyStock(long quantity) {
    if (quantity < 0) throw new IllegalArgumentException("안전재고는 0 이상이어야 합니다.");
    this.safetyStockQuantity = quantity;
  }

  public void reserve(long quantity) {
    requirePositive(quantity);
    if (getAvailableQuantity() < quantity) {
      throw new IllegalStateException("판매 가능한 재고가 부족합니다.");
    }
    this.reservedQuantity += quantity;
  }

  public void releaseReservation(long quantity) {
    requirePositive(quantity);
    if (reservedQuantity < quantity) {
      throw new IllegalStateException("해제할 예약 재고보다 요청 수량이 많아.");
    }
    this.reservedQuantity -= quantity;
  }

  /** 결제/주문 확정 시 예약재고를 실제 출고 수량으로 전환합니다. */
  public void confirmSale(long quantity) {
    requirePositive(quantity);
    if (reservedQuantity < quantity || onHandQuantity < quantity) {
      throw new IllegalStateException("확정할 재고 수량이 맞지 않습니다.");
    }
    this.reservedQuantity -= quantity;
    this.onHandQuantity -= quantity;
    this.soldQuantity += quantity;
  }

  public void refundSale(long quantity) {
    requirePositive(quantity);
    if (soldQuantity < quantity) throw new IllegalStateException("환불할 판매 수량이 부족합니다.");
    this.onHandQuantity += quantity;
    this.soldQuantity -= quantity;
  }

  private void requirePositive(long quantity) {
    if (quantity <= 0) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
  }
}

package com.movrit.movrit.product.entity;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 브랜드가 판매하는 상품입니다. 재고와 옵션은 상품 아래의 옵션(SKU) 단위로 관리합니다. */
@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private Brand brand;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  private String category;

  @Column(name = "normal_price", nullable = false)
  private BigDecimal normalPrice;

  @Column(name = "thumbnail_url", length = 500)
  private String thumbnailUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProductStatus status;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** 상품 이름·설명·카테고리·정상가·판매 상태를 고칩니다. */
  public void update(
      String name,
      String description,
      String category,
      BigDecimal normalPrice,
      ProductStatus status) {
    this.name = name;
    this.description = description;
    this.category = category;
    this.normalPrice = normalPrice;
    this.status = status;
  }

  /** 대표 이미지 주소를 바꿉니다. */
  public void updateThumbnail(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  /** 상품을 삭제 표시합니다(데이터는 남깁니다). */
  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }
}

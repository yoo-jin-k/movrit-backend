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

  public void updateThumbnail(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }
}

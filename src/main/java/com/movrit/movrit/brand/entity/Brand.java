package com.movrit.movrit.brand.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.seller.entity.SellerProfile;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "brand")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Brand extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 판매자 한 명이 여러 브랜드를 운영할 수 있도록 ManyToOne으로 둡니다.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_id", nullable = false)
  private SellerProfile seller;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  @Column(name = "cover_url", length = 500)
  private String coverUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BrandStatus status;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void update(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public void updateLogo(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  public void updateCover(String coverUrl) {
    this.coverUrl = coverUrl;
  }

  public void activate() {
    this.status = BrandStatus.ACTIVE;
  }

  public void deactivate() {
    this.status = BrandStatus.INACTIVE;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
    this.status = BrandStatus.INACTIVE;
  }
}

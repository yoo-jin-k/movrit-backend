package com.movrit.movrit.brand.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.seller.entity.SellerProfile;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 업체가 운영하는 브랜드입니다. 업체 한 곳이 여러 브랜드를 가질 수 있고, 캠페인은 브랜드 단위로 소유됩니다. 삭제는 실제 삭제 대신 삭제 시각을 기록합니다. */
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

  /** 브랜드 이름과 소개를 수정합니다. */
  public void update(String name, String description) {
    this.name = name;
    this.description = description;
  }

  /** 브랜드 로고 이미지 주소를 바꿉니다. */
  public void updateLogo(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  /** 브랜드 대표(커버) 이미지 주소를 바꿉니다. */
  public void updateCover(String coverUrl) {
    this.coverUrl = coverUrl;
  }

  /** 브랜드를 사용 중(ACTIVE)으로 바꿉니다. */
  public void activate() {
    this.status = BrandStatus.ACTIVE;
  }

  /** 브랜드를 사용 중지(INACTIVE)로 바꿉니다. */
  public void deactivate() {
    this.status = BrandStatus.INACTIVE;
  }

  /** 브랜드를 목록에서 숨기기 위해 삭제 시각을 기록하고 사용 중지로 바꿉니다. 기존 캠페인·주문 기록과의 연결은 그대로 남습니다. */
  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
    this.status = BrandStatus.INACTIVE;
  }
}

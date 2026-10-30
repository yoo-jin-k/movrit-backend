package com.movrit.movrit.collaboration.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "campaign_detail_image",
    uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "display_order"}))
/** 캠페인 상세 화면에 순서대로 보여줄 상세 이미지 한 장입니다. 같은 캠페인 안에서 표시 순서(display_order)는 겹칠 수 없습니다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CampaignDetailImage extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false)
  private Campaign campaign;

  @Column(name = "image_url", nullable = false, length = 1000)
  private String imageUrl;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @Column(name = "alt_text", length = 200)
  private String altText;

  /** 상세 이미지의 표시 순서를 바꿉니다. 순서 중복 검사는 호출하는 서비스가 맡습니다. */
  public void reorder(int order) {
    this.displayOrder = order;
  }

  /** 이미지를 볼 수 없는 사용자를 위한 대체 설명 문구를 바꿉니다. */
  public void updateAltText(String value) {
    this.altText = value;
  }
}

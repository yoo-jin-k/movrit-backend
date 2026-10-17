package com.movrit.movrit.collaboration.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "campaign_detail_image", uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "display_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CampaignDetailImage extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public void reorder(int order) { this.displayOrder = order; }
    public void updateAltText(String value) { this.altText = value; }
}

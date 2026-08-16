package com.movrit.movrit.settlement.entity;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preorder_id", nullable = false)
    private Preorder preorder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal platformFee;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal creatorAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal brandAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    private LocalDateTime settledAt;
}

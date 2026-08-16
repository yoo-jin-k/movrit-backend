package com.movrit.movrit.product.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(name = "normal_price", nullable = false)
    private BigDecimal normalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(String name, String description, String category, BigDecimal normalPrice, ProductStatus status) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.normalPrice = normalPrice;
        this.status = status;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}

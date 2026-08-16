package com.movrit.movrit.product.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal regularPrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;
}

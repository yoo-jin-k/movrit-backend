package com.movrit.movrit.product.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_sku", uniqueConstraints = @UniqueConstraint(name = "uk_product_sku_code", columnNames = "sku_code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductSku extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku_code", nullable = false, length = 100)
    private String skuCode;

    @Column(name = "option_name", nullable = false, length = 200)
    private String optionName;

    @Column(name = "additional_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal additionalPrice;

    @Column(nullable = false)
    private boolean active;

    public void update(String optionName, BigDecimal additionalPrice, boolean active) {
        this.optionName = optionName;
        this.additionalPrice = additionalPrice;
        this.active = active;
    }
}

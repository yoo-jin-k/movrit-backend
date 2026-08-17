package com.movrit.movrit.settlement.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement")
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

    @Column(name = "settlement_number", nullable = false)
    private String settlementNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "platform_fee_amount", nullable = false)
    private BigDecimal platformFeeAmount;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public void update(SettlementStatus status, BigDecimal grossAmount, BigDecimal refundAmount,
                       BigDecimal platformFeeAmount, BigDecimal netAmount, LocalDateTime calculatedAt,
                       LocalDateTime confirmedAt, LocalDateTime paidAt) {
        this.status = status;
        this.grossAmount = grossAmount;
        this.refundAmount = refundAmount;
        this.platformFeeAmount = platformFeeAmount;
        this.netAmount = netAmount;
        this.calculatedAt = calculatedAt;
        this.confirmedAt = confirmedAt;
        this.paidAt = paidAt;
    }
}

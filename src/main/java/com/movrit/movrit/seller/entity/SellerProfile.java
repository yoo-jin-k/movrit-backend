package com.movrit.movrit.seller.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seller_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SellerProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "business_number")
    private String businessNumber;

    @Column(name = "representative_name")
    private String representativeName;

    @Column(nullable = false)
    private String status;

    @Column(name = "settlement_bank_code")
    private String settlementBankCode;

    @Column(name = "settlement_account_masked")
    private String settlementAccountMasked;

    public void update(String businessName, String brandName, String businessNumber, String representativeName,
                       String status, String settlementBankCode, String settlementAccountMasked) {
        this.businessName = businessName;
        this.brandName = brandName;
        this.businessNumber = businessNumber;
        this.representativeName = representativeName;
        this.status = status;
        this.settlementBankCode = settlementBankCode;
        this.settlementAccountMasked = settlementAccountMasked;
    }
}

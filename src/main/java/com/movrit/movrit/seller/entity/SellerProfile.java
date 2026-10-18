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

  // 일반 회원이 판매자 등록을 신청하면 회원 1명당 하나의 판매자 프로필을 갖습니다.
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

  // 문자열 대신 enum을 사용해 잘못된 상태값이 DB에 저장되는 것을 막습니다.
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SellerStatus status;

  @Column(name = "settlement_bank_code")
  private String settlementBankCode;

  @Column(name = "settlement_account_masked")
  private String settlementAccountMasked;

  public void updateApplication(
      String businessName,
      String brandName,
      String businessNumber,
      String representativeName,
      String settlementBankCode,
      String settlementAccountMasked) {
    this.businessName = businessName;
    this.brandName = brandName;
    this.businessNumber = businessNumber;
    this.representativeName = representativeName;
    this.settlementBankCode = settlementBankCode;
    this.settlementAccountMasked = settlementAccountMasked;
  }

  public void approve() {
    this.status = SellerStatus.APPROVED;
  }

  public void reject() {
    this.status = SellerStatus.REJECTED;
  }

  public void suspend() {
    this.status = SellerStatus.SUSPENDED;
  }
}

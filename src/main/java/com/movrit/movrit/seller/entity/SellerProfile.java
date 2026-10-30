package com.movrit.movrit.seller.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

/** 업체(판매자) 등록 신청과 심사 결과를 담는 프로필입니다. 회원 한 명당 하나이며, 승인(APPROVED)되어야 업체 화면을 사용할 수 있습니다. */
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

  /** 업체 신청서 내용(상호·브랜드명·사업자번호·대표자·정산 계좌 정보)을 수정합니다. 계좌번호는 가린 값만 저장합니다. */
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

  /** 운영자 승인: 업체 기능을 사용할 수 있게 합니다. */
  public void approve() {
    this.status = SellerStatus.APPROVED;
  }

  /** 운영자 반려: 업체 기능을 사용할 수 없습니다. 내용을 고쳐 다시 신청할 수 있습니다. */
  public void reject() {
    this.status = SellerStatus.REJECTED;
  }

  /** 운영자 정지: 승인된 업체의 권한을 막습니다. */
  public void suspend() {
    this.status = SellerStatus.SUSPENDED;
  }
}

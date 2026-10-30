package com.movrit.movrit.memberaddress.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 배송지 주소록의 한 항목입니다. 기본 배송지는 회원당 하나만 지정됩니다. 캠페인 신청 때는 이 값이 신청서에 복사되므로, 나중에 주소록을 바꿔도 지난 신청 주소는 바뀌지
 * 않습니다.
 */
@Entity
@Table(name = "member_address")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberAddress extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "address_name", length = 30)
  private String label;

  @Column(name = "receiver_name", nullable = false, length = 50)
  private String recipientName;

  @Column(name = "receiver_phone", nullable = false, length = 20)
  private String recipientPhone;

  @Column(name = "postal_code", nullable = false, length = 10)
  private String postalCode;

  @Column(nullable = false, length = 300)
  private String address1;

  @Column(length = 300)
  private String address2;

  @Column(name = "is_default", nullable = false)
  private boolean defaultAddress;

  /** 배송지 이름·받는 사람·연락처·우편번호·주소를 한 번에 수정합니다. */
  public void update(
      String label,
      String recipientName,
      String recipientPhone,
      String postalCode,
      String address1,
      String address2) {
    this.label = label;
    this.recipientName = recipientName;
    this.recipientPhone = recipientPhone;
    this.postalCode = postalCode;
    this.address1 = address1;
    this.address2 = address2;
  }

  /** 이 주소를 기본 배송지로 표시합니다. 다른 주소의 기본 표시 해제는 서비스에서 함께 처리합니다. */
  public void markDefault() {
    defaultAddress = true;
  }

  /** 기본 배송지 표시를 해제합니다. */
  public void clearDefault() {
    defaultAddress = false;
  }
}

package com.movrit.movrit.memberaddress.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

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

  public void markDefault() {
    defaultAddress = true;
  }

  public void clearDefault() {
    defaultAddress = false;
  }
}

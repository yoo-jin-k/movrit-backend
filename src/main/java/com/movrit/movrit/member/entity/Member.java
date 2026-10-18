package com.movrit.movrit.member.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, length = 100)
  private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(length = 20)
  private String phone;

  /** 시스템 접근 권한만 관리합니다. 판매자 여부는 SellerProfile 승인 상태로 판단하므로 판매자가 되어도 role은 USER일 수 있다. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MemberRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MemberStatus status;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Column(name = "email_verified_at")
  private LocalDateTime emailVerifiedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void updateProfile(String name, String phone) {
    this.name = name;
    this.phone = phone;
  }

  public void changePassword(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void updateLastLoginAt() {
    this.lastLoginAt = LocalDateTime.now();
  }

  public void verifyEmail() {
    this.emailVerifiedAt = LocalDateTime.now();
  }

  /** 이메일이 변경되면 이전 주소의 소유 확인은 즉시 무효가 됩니다. */
  public void changeEmail(String email) {
    this.email = email;
    this.emailVerifiedAt = null;
  }

  public boolean isEmailVerified() {
    return emailVerifiedAt != null;
  }

  public boolean isAdmin() {
    return this.role == MemberRole.ADMIN;
  }

  public void withdraw() {
    this.status = MemberStatus.WITHDRAWN;
    this.deletedAt = LocalDateTime.now();
  }

  public void changeStatus(MemberStatus status) {
    if (this.role == MemberRole.ADMIN && status != MemberStatus.ACTIVE)
      throw new IllegalStateException("관리자 계정은 이 API로 정지할 수 없습니다.");
    this.status = status;
    if (status == MemberStatus.WITHDRAWN) this.deletedAt = LocalDateTime.now();
  }
}

package com.movrit.movrit.member.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MOVRIT 회원 계정입니다. 로그인 정보(이메일·비밀번호 해시), 표시 닉네임, 프로필 사진, 이메일 인증 여부, 계정 상태를 가집니다. 업체 여부는 이 엔티티가 아니라
 * 승인된 SellerProfile로 판단합니다.
 */
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

  /** 화면에 노출하는 회원 표시명이며 실명·업체명·채널 프로필 이름과 구분합니다. */
  @Column(nullable = false, unique = true, length = 20)
  private String nickname;

  @Column(length = 20)
  private String phone;

  /** 시스템 접근 권한만 관리합니다. 판매자 여부는 SellerProfile 승인 상태로 판단하므로 판매자가 되어도 role은 USER일 수 있습니다. */
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

  /** 휴대폰 번호 인증을 마친 시각입니다. 연락처를 다른 번호로 바꾸면 비웁니다. */
  @Column(name = "phone_verified_at")
  private LocalDateTime phoneVerifiedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** 재인코딩된 프로필 표시본의 서버 생성 저장 키입니다. 원본 파일명이나 경로는 저장하지 않습니다. */
  @Column(name = "avatar_key", length = 64)
  private String avatarKey;

  @Column(name = "avatar_content_type", length = 20)
  private String avatarContentType;

  @Column(name = "avatar_version", nullable = false)
  private long avatarVersion;

  /** 이름과 연락처를 바꿉니다. 닉네임은 값이 전달된 경우에만 바꿉니다. 연락처 숫자가 바뀌면 휴대폰 인증을 다시 받아야 합니다. */
  public void updateProfile(String name, String phone, String nickname) {
    this.name = name;
    if (!digits(phone).equals(digits(this.phone))) this.phoneVerifiedAt = null;
    this.phone = phone;
    if (nickname != null) this.nickname = nickname;
  }

  /** 휴대폰 번호 인증을 마쳤습니다. 인증한 번호(숫자만)를 연락처로 저장합니다. */
  public void verifyPhone(String phoneDigits) {
    this.phone = phoneDigits;
    this.phoneVerifiedAt = LocalDateTime.now();
  }

  /** 휴대폰 번호 인증을 마쳤는지 확인합니다. */
  public boolean isPhoneVerified() {
    return phoneVerifiedAt != null;
  }

  private static String digits(String value) {
    return value == null ? "" : value.replaceAll("\\D", "");
  }

  /** 새 비밀번호 해시로 교체합니다. 암호화는 서비스에서 끝낸 값을 받습니다. */
  public void changePassword(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /** 새 프로필 사진 저장 키와 형식을 기록하고 버전을 올립니다. 버전이 바뀌면 이전 사진 주소는 더 이상 열리지 않습니다. */
  public void changeAvatar(String key, String contentType) {
    avatarKey = key;
    avatarContentType = contentType;
    avatarVersion++;
  }

  /** 프로필 사진을 지우고 버전을 올립니다. */
  public void removeAvatar() {
    avatarKey = null;
    avatarContentType = null;
    avatarVersion++;
  }

  /** 마지막 로그인 시각을 현재 시각으로 기록합니다. */
  public void updateLastLoginAt() {
    this.lastLoginAt = LocalDateTime.now();
  }

  /** 이메일 소유 확인이 끝났음을 현재 시각으로 기록합니다. */
  public void verifyEmail() {
    this.emailVerifiedAt = LocalDateTime.now();
  }

  /** 이메일을 바꾸고 이메일 인증을 초기화합니다. 새 주소는 다시 인증해야 합니다. */
  public void changeEmail(String email) {
    this.email = email;
    this.emailVerifiedAt = null;
  }

  /** 이메일 인증을 마쳤는지 확인합니다. 캠페인 신청 전에 검사합니다. */
  public boolean isEmailVerified() {
    return emailVerifiedAt != null;
  }

  /** 운영자(ADMIN) 계정인지 확인합니다. */
  public boolean isAdmin() {
    return this.role == MemberRole.ADMIN;
  }

  /** 회원 탈퇴 처리: 상태를 WITHDRAWN으로 바꾸고 탈퇴 시각을 기록합니다. */
  public void withdraw() {
    this.status = MemberStatus.WITHDRAWN;
    this.deletedAt = LocalDateTime.now();
  }

  /**
   * 본인 탈퇴 시 개인정보를 지웁니다. 이메일·닉네임은 다시 가입할 수 있도록 겹치지 않는 값으로 바꾸고, 이름·연락처·비밀번호·프로필 사진은 비웁니다. 캠페인·주문 기록은
   * 법적 보관과 업체 정산을 위해 남지만 화면에는 '탈퇴한 회원'으로 보입니다.
   */
  public void anonymizeForWithdrawal() {
    withdraw();
    this.email = "withdrawn-" + id + "@movrit.invalid";
    this.nickname = "탈퇴회원" + id;
    this.name = "탈퇴한 회원";
    this.phone = null;
    this.phoneVerifiedAt = null;
    this.passwordHash = null;
    this.emailVerifiedAt = null;
    this.avatarKey = null;
    this.avatarContentType = null;
    this.avatarVersion++;
  }

  /** 운영자가 회원 상태를 바꿉니다. 관리자 계정은 정지·탈퇴 상태로 바꿀 수 없고, WITHDRAWN이면 탈퇴 시각도 기록합니다. */
  public void changeStatus(MemberStatus status) {
    if (this.role == MemberRole.ADMIN && status != MemberStatus.ACTIVE)
      throw new IllegalStateException("관리자 계정은 이 API로 정지할 수 없습니다.");
    this.status = status;
    if (status == MemberStatus.WITHDRAWN) this.deletedAt = LocalDateTime.now();
  }
}

package com.movrit.movrit.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 운영자 관리 화면(회원·업체·인플루언서·프리오더·주문·협업·리포트·설정)에서 쓰는 조회 응답입니다.
 *
 * <p>모두 읽기 전용이며, 연락처·계좌 같은 민감 정보는 가린 값만 담습니다. 상태를 바꾸는 요청은 각 기능의 기존 API를 사용합니다.
 */
public final class AdminConsoleDtos {
  private AdminConsoleDtos() {}

  /**
   * 회원 목록의 한 줄입니다.
   *
   * @param role 일반 회원(USER) 또는 운영자(ADMIN)입니다.
   * @param sellerStatus 업체 신청을 했다면 그 심사 상태, 아니면 null입니다.
   * @param creator 활동(인플루언서) 프로필이 있으면 true입니다.
   * @param socialProviders 연결된 소셜 로그인(GOOGLE·KAKAO) 목록입니다.
   * @param applicationCount 캠페인 신청 수(취소 포함)입니다.
   */
  public record MemberRow(
      Long id,
      String email,
      String name,
      String nickname,
      String role,
      String status,
      boolean emailVerified,
      String sellerStatus,
      boolean creator,
      List<String> socialProviders,
      long applicationCount,
      LocalDateTime createdAt,
      LocalDateTime lastLoginAt) {}

  /** 회원 상세입니다. 연락처는 가운데를 가린 값입니다. */
  public record MemberDetail(
      MemberRow member,
      String phoneMasked,
      List<String> brands,
      List<ApplicationRow> recentApplications) {}

  /** 캠페인 신청 한 건의 요약입니다. */
  public record ApplicationRow(
      Long id, Long campaignId, String campaignTitle, String status, LocalDateTime submittedAt) {}

  /** 업체 상세입니다. 정산 계좌는 이미 가린 값만 저장되어 있습니다. */
  public record SellerDetail(
      Long id,
      Long memberId,
      String memberEmail,
      String memberName,
      String businessName,
      String businessNumber,
      String representativeName,
      String status,
      String settlementBankCode,
      String settlementAccountMasked,
      LocalDateTime createdAt,
      List<BrandRow> brands,
      long campaignCount,
      long applicationCount) {}

  /** 업체가 운영하는 브랜드 한 개와 그 브랜드의 캠페인 수입니다. */
  public record BrandRow(Long id, String name, String status, long campaignCount) {}

  /** 인플루언서(크리에이터) 목록의 한 줄입니다. */
  public record CreatorRow(
      Long id,
      Long memberId,
      String nickname,
      String primaryCategory,
      String snsUrl,
      Long followerCount,
      String status,
      long applicationCount,
      long completedCount,
      LocalDateTime createdAt) {}

  /** 인플루언서 상세입니다. 채널 지표와 최근 신청을 함께 보여줍니다. */
  public record CreatorDetail(
      CreatorRow creator,
      String bio,
      String memberEmail,
      List<MetricRow> metrics,
      List<ApplicationRow> recentApplications) {}

  /** 회원이 등록한 채널 지표 한 건입니다. */
  public record MetricRow(
      String channelType,
      String metricType,
      Long value,
      String measurementPeriod,
      String verificationStatus,
      LocalDateTime measuredAt) {}

  /** 프리오더 목록의 한 줄입니다. */
  public record PreorderRow(
      Long id,
      String title,
      String brandName,
      String productName,
      String status,
      BigDecimal preorderPrice,
      String currency,
      Long targetQuantity,
      Long orderedQuantity,
      LocalDateTime startAt,
      LocalDateTime endAt) {}

  /** 프리오더 상세입니다. 주문·결제 집계를 함께 보여줍니다. */
  public record PreorderDetail(
      PreorderRow preorder,
      String description,
      Long maxQuantity,
      Long minOrderQuantity,
      Long maxOrderQuantityPerMember,
      LocalDateTime productionExpectedAt,
      LocalDateTime shippingExpectedAt,
      String cancellationReason,
      long orderCount,
      BigDecimal paidAmount,
      BigDecimal refundedAmount,
      String settlementStatus) {}

  /** 주문 목록의 한 줄입니다. 결제·배송 상태를 함께 보여줍니다. */
  public record OrderRow(
      Long id,
      String orderNumber,
      String buyerName,
      String preorderTitle,
      String status,
      BigDecimal totalPaymentAmount,
      String currency,
      String paymentStatus,
      String shippingStatus,
      LocalDateTime createdAt) {}

  /** 주문 상세입니다. 받는 사람 연락처는 가린 값입니다. */
  public record OrderDetail(
      OrderRow order,
      String receiverName,
      String receiverPhoneMasked,
      String address,
      List<OrderItemRow> items,
      String paymentMethod,
      LocalDateTime paidAt,
      LocalDateTime cancelledAt,
      String failureMessage,
      String courierCode,
      String trackingNumber,
      LocalDateTime shippedAt,
      LocalDateTime deliveredAt) {}

  /** 주문에 담긴 상품 한 줄입니다. */
  public record OrderItemRow(
      String productName,
      String optionName,
      BigDecimal unitPrice,
      Long quantity,
      BigDecimal totalPrice) {}

  /** 크리에이터 협업(추천 링크) 한 건입니다. */
  public record CollaborationRow(
      Long id,
      String preorderTitle,
      String brandName,
      String creatorNickname,
      String type,
      String status,
      String referralCode,
      long clickCount,
      long orderCount,
      LocalDateTime createdAt) {}

  /**
   * 운영 리포트입니다. 상태별 건수와 주요 비율·금액을 담습니다.
   *
   * @param completionRate 선정된 신청 중 완료된 비율(%)입니다. 선정된 신청이 없으면 0입니다.
   */
  public record Report(
      Map<String, Long> membersByStatus,
      Map<String, Long> campaignsByType,
      long openCampaigns,
      Map<String, Long> applicationsByStatus,
      Map<String, Long> contentsByStatus,
      double completionRate,
      Map<String, Long> ordersByStatus,
      BigDecimal paidAmount,
      BigDecimal refundedAmount,
      Map<String, Long> settlementsByStatus,
      LocalDateTime generatedAt) {}

  /**
   * 운영 설정 현황입니다. 비밀값 자체는 담지 않고 설정 여부만 알려 줍니다.
   *
   * @param activeProfiles 실행 중인 프로필(integration 등)입니다.
   * @param emailDelivery 이메일 인증 발송 방식(local-preview·smtp·unconfigured)입니다.
   * @param googleLoginConfigured Google 로그인 키가 설정되었는지입니다.
   * @param kakaoLoginConfigured Kakao 로그인 키가 설정되었는지입니다.
   * @param schemaVersion 적용된 DB 마이그레이션 최신 버전입니다.
   * @param preorderLifecycleDelayMs 프리오더 자동 진행 주기(밀리초)입니다.
   */
  public record Settings(
      List<String> activeProfiles,
      String emailDelivery,
      int verificationExpiresMinutes,
      int verificationHourlyLimit,
      boolean googleLoginConfigured,
      boolean kakaoLoginConfigured,
      String schemaVersion,
      long preorderLifecycleDelayMs,
      String uploadDirectory) {}
}

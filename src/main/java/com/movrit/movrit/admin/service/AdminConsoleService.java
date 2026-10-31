package com.movrit.movrit.admin.service;

import com.movrit.movrit.admin.dto.AdminConsoleDtos;
import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.brand.repository.BrandRepository;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.repository.CampaignContentSubmissionRepository;
import com.movrit.movrit.campaigneligibility.repository.MemberChannelMetricRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.Collaboration;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.repository.CollaborationRepository;
import com.movrit.movrit.collaboration.repository.ReferralClickRepository;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.repository.CreatorRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.member.repository.MemberSocialAccountRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.repository.OrderItemRepository;
import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.repository.SellerProfileRepository;
import com.movrit.movrit.settlement.repository.SettlementRepository;
import com.movrit.movrit.shipping.entity.Shipping;
import com.movrit.movrit.shipping.repository.ShippingRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 운영자 관리 화면에 보여줄 실제 데이터를 모아 읽습니다(읽기 전용).
 *
 * <p>모든 메서드는 먼저 운영자(ADMIN)인지 확인합니다. 포트폴리오·시연 규모(수백 건 이하)를 기준으로 목록을 한 번에 읽어 화면용으로 묶습니다. 데이터가 크게 늘면
 * 페이지 나누기와 집계 쿼리로 바꿔야 합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminConsoleService {
  /** 신청 완료율을 계산할 때 '선정 이후' 단계로 보는 상태입니다. */
  private static final List<CampaignApplicationStatus> SELECTED_OR_LATER =
      List.of(
          CampaignApplicationStatus.SELECTED,
          CampaignApplicationStatus.ACTIVE,
          CampaignApplicationStatus.CONTENT_SUBMITTED,
          CampaignApplicationStatus.REVISION_REQUESTED,
          CampaignApplicationStatus.COMPLETED);

  private final MemberService memberService;
  private final MemberRepository members;
  private final MemberSocialAccountRepository socialAccounts;
  private final SellerProfileRepository sellers;
  private final BrandRepository brands;
  private final CreatorRepository creators;
  private final MemberChannelMetricRepository metrics;
  private final CampaignRepository campaigns;
  private final CampaignApplicationRepository applications;
  private final CampaignContentSubmissionRepository contents;
  private final PreorderRepository preorders;
  private final OrderRepository orders;
  private final OrderItemRepository orderItems;
  private final PaymentRepository payments;
  private final ShippingRepository shippings;
  private final CollaborationRepository collaborations;
  private final ReferralClickRepository referralClicks;
  private final SettlementRepository settlements;
  private final JdbcTemplate jdbc;
  private final Environment environment;

  @Value("${movrit.email.delivery:unconfigured}")
  private String emailDelivery;

  @Value("${movrit.email.verification.expires-minutes:10}")
  private int verificationExpiresMinutes;

  @Value("${movrit.email.verification.hourly-limit:5}")
  private int verificationHourlyLimit;

  @Value("${google.client-id:}")
  private String googleClientId;

  @Value("${kakao.client-id:}")
  private String kakaoClientId;

  @Value("${app.preorder-lifecycle-delay-ms:60000}")
  private long preorderLifecycleDelayMs;

  @Value("${app.upload-dir:uploads}")
  private String uploadDirectory;

  // ───────────────────────── 회원 ─────────────────────────

  /** 탈퇴하지 않은 회원 전체를 최근 가입 순으로 돌려줍니다. */
  public List<AdminConsoleDtos.MemberRow> members(Long adminId) {
    memberService.getAdmin(adminId);
    MemberIndex index = memberIndex();
    return members.findAll().stream()
        .filter(member -> member.getDeletedAt() == null)
        .sorted(Comparator.comparing(Member::getId).reversed())
        .map(member -> memberRow(member, index))
        .toList();
  }

  /** 회원 상세: 기본 정보, 운영 브랜드, 최근 캠페인 신청 10건입니다. */
  public AdminConsoleDtos.MemberDetail member(Long adminId, Long memberId) {
    memberService.getAdmin(adminId);
    Member member = members.findById(memberId).orElseThrow(() -> notFound("회원"));
    List<String> brandNames =
        sellers
            .findByMemberId(memberId)
            .map(seller -> brands.findAllBySellerIdAndDeletedAtIsNull(seller.getId()))
            .orElse(List.of())
            .stream()
            .map(Brand::getName)
            .toList();
    return new AdminConsoleDtos.MemberDetail(
        memberRow(member, memberIndex()),
        maskPhone(member.getPhone()),
        brandNames,
        applications.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
            .limit(10)
            .map(this::applicationRow)
            .toList());
  }

  // ───────────────────────── 업체 ─────────────────────────

  /** 업체 상세: 업체 정보, 브랜드별 캠페인 수, 받은 신청 수입니다. */
  public AdminConsoleDtos.SellerDetail seller(Long adminId, Long sellerId) {
    memberService.getAdmin(adminId);
    SellerProfile seller = sellers.findById(sellerId).orElseThrow(() -> notFound("업체"));
    List<Campaign> sellerCampaigns =
        campaigns.findAllByBrandSellerMemberIdOrderByIdDesc(seller.getMember().getId());
    Map<Long, Long> campaignsByBrand =
        sellerCampaigns.stream()
            .collect(Collectors.groupingBy(c -> c.getBrand().getId(), Collectors.counting()));
    List<AdminConsoleDtos.BrandRow> brandRows =
        brands.findAllBySellerIdAndDeletedAtIsNull(sellerId).stream()
            .map(
                brand ->
                    new AdminConsoleDtos.BrandRow(
                        brand.getId(),
                        brand.getName(),
                        name(brand.getStatus()),
                        campaignsByBrand.getOrDefault(brand.getId(), 0L)))
            .toList();
    return new AdminConsoleDtos.SellerDetail(
        seller.getId(),
        seller.getMember().getId(),
        seller.getMember().getEmail(),
        seller.getMember().getName(),
        seller.getBusinessName(),
        seller.getBusinessNumber(),
        seller.getRepresentativeName(),
        name(seller.getStatus()),
        seller.getSettlementBankCode(),
        seller.getSettlementAccountMasked(),
        seller.getCreatedAt(),
        brandRows,
        sellerCampaigns.size(),
        applications
            .findAllByCampaignBrandSellerMemberIdOrderByIdDesc(seller.getMember().getId())
            .size());
  }

  // ───────────────────────── 인플루언서 ─────────────────────────

  /** 인플루언서(크리에이터) 전체를 최근 등록 순으로, 신청·완료 수와 함께 돌려줍니다. */
  public List<AdminConsoleDtos.CreatorRow> creators(Long adminId) {
    memberService.getAdmin(adminId);
    Map<Long, List<CampaignApplication>> byCreator = applicationsByCreator();
    return creators.findAll().stream()
        .sorted(Comparator.comparing(Creator::getId).reversed())
        .map(creator -> creatorRow(creator, byCreator))
        .toList();
  }

  /** 인플루언서 상세: 소개, 채널 지표, 최근 신청 10건입니다. */
  public AdminConsoleDtos.CreatorDetail creator(Long adminId, Long creatorId) {
    memberService.getAdmin(adminId);
    Creator creator = creators.findById(creatorId).orElseThrow(() -> notFound("인플루언서"));
    Long memberId = creator.getMember().getId();
    return new AdminConsoleDtos.CreatorDetail(
        creatorRow(creator, applicationsByCreator()),
        creator.getBio(),
        creator.getMember().getEmail(),
        metrics.findAllByMemberIdOrderById(memberId).stream()
            .map(
                metric ->
                    new AdminConsoleDtos.MetricRow(
                        metric.getChannelType(),
                        metric.getMetricType(),
                        metric.getValue(),
                        metric.getMeasurementPeriod(),
                        name(metric.getVerificationStatus()),
                        metric.getMeasuredAt()))
            .toList(),
        applications.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
            .limit(10)
            .map(this::applicationRow)
            .toList());
  }

  // ───────────────────────── 프리오더 ─────────────────────────

  /** 삭제하지 않은 프리오더 전체를 최근 등록 순으로 돌려줍니다. */
  public List<AdminConsoleDtos.PreorderRow> preorders(Long adminId) {
    memberService.getAdmin(adminId);
    return preorders.findAll().stream()
        .filter(preorder -> preorder.getDeletedAt() == null)
        .sorted(Comparator.comparing(Preorder::getId).reversed())
        .map(this::preorderRow)
        .toList();
  }

  /** 프리오더 상세: 판매 조건과 주문 수·결제 금액·환불 금액·정산 상태입니다. */
  public AdminConsoleDtos.PreorderDetail preorder(Long adminId, Long preorderId) {
    memberService.getAdmin(adminId);
    Preorder preorder = preorders.findById(preorderId).orElseThrow(() -> notFound("프리오더"));
    List<Payment> preorderPayments = payments.findAllByOrderPreorderId(preorderId);
    return new AdminConsoleDtos.PreorderDetail(
        preorderRow(preorder),
        preorder.getDescription(),
        preorder.getMaxQuantity(),
        preorder.getMinOrderQuantity(),
        preorder.getMaxOrderQuantityPerMember(),
        preorder.getProductionExpectedAt(),
        preorder.getShippingExpectedAt(),
        preorder.getCancellationReason(),
        orders.findAllByPreorderId(preorderId).size(),
        sumApproved(preorderPayments, PaymentStatus.PAID),
        sumApproved(preorderPayments, PaymentStatus.REFUNDED),
        settlements
            .findByPreorderId(preorderId)
            .map(settlement -> name(settlement.getStatus()))
            .orElse(null));
  }

  // ───────────────────────── 주문·환불 ─────────────────────────

  /** 주문 전체를 최근 순으로, 결제·배송 상태와 함께 돌려줍니다. */
  public List<AdminConsoleDtos.OrderRow> orders(Long adminId) {
    memberService.getAdmin(adminId);
    Map<Long, Payment> paymentByOrder =
        payments.findAll().stream()
            .collect(Collectors.toMap(p -> p.getOrder().getId(), Function.identity(), (a, b) -> a));
    Map<Long, Shipping> shippingByOrder =
        shippings.findAll().stream()
            .collect(Collectors.toMap(s -> s.getOrder().getId(), Function.identity(), (a, b) -> a));
    return orders.findAll().stream()
        .sorted(Comparator.comparing(Order::getId).reversed())
        .map(
            order ->
                orderRow(
                    order, paymentByOrder.get(order.getId()), shippingByOrder.get(order.getId())))
        .toList();
  }

  /** 주문 상세: 받는 사람(연락처 가림), 상품, 결제·환불, 배송 정보입니다. */
  public AdminConsoleDtos.OrderDetail order(Long adminId, Long orderId) {
    memberService.getAdmin(adminId);
    Order order = orders.findById(orderId).orElseThrow(() -> notFound("주문"));
    Payment payment = payments.findByOrderId(orderId).orElse(null);
    Shipping shipping = shippings.findByOrderId(orderId).orElse(null);
    return new AdminConsoleDtos.OrderDetail(
        orderRow(order, payment, shipping),
        order.getReceiverName(),
        maskPhone(order.getReceiverPhone()),
        joinAddress(order.getPostalCode(), order.getAddress1(), order.getAddress2()),
        orderItems.findAllByOrderId(orderId).stream()
            .map(
                item ->
                    new AdminConsoleDtos.OrderItemRow(
                        item.getProductName(),
                        item.getOptionName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getTotalPrice()))
            .toList(),
        payment == null ? null : payment.getMethod(),
        order.getPaidAt(),
        order.getCancelledAt(),
        payment == null ? null : payment.getFailureMessage(),
        shipping == null ? null : shipping.getCourierCode(),
        shipping == null ? null : shipping.getTrackingNumber(),
        shipping == null ? null : shipping.getShippedAt(),
        shipping == null ? null : shipping.getDeliveredAt());
  }

  // ───────────────────────── 협업 ─────────────────────────

  /** 크리에이터 협업(추천 링크) 전체를 최근 순으로, 클릭·주문 수와 함께 돌려줍니다. */
  public List<AdminConsoleDtos.CollaborationRow> collaborations(Long adminId) {
    memberService.getAdmin(adminId);
    return collaborations.findAll().stream()
        .sorted(Comparator.comparing(Collaboration::getId).reversed())
        .map(
            collaboration ->
                new AdminConsoleDtos.CollaborationRow(
                    collaboration.getId(),
                    collaboration.getPreorder().getTitle(),
                    collaboration.getPreorder().getProduct().getBrand().getName(),
                    collaboration.getCreator().getNickname(),
                    name(collaboration.getType()),
                    name(collaboration.getStatus()),
                    collaboration.getReferralCode(),
                    referralClicks.countByCollaborationId(collaboration.getId()),
                    orders.findAllByAttributionId(collaboration.getId()).size(),
                    collaboration.getCreatedAt()))
        .toList();
  }

  // ───────────────────────── 리포트·설정 ─────────────────────────

  /** 운영 리포트: 회원·캠페인·신청·콘텐츠·주문·정산의 상태별 건수와 완료율·결제 금액입니다. */
  public AdminConsoleDtos.Report report(Long adminId) {
    memberService.getAdmin(adminId);
    List<CampaignApplication> allApplications = applications.findAll();
    long selectedOrLater =
        allApplications.stream().filter(a -> SELECTED_OR_LATER.contains(a.getStatus())).count();
    long completed =
        allApplications.stream()
            .filter(a -> a.getStatus() == CampaignApplicationStatus.COMPLETED)
            .count();
    List<Payment> allPayments = payments.findAll();
    List<Campaign> allCampaigns = campaigns.findAll();
    return new AdminConsoleDtos.Report(
        count(members.findAll().stream().filter(m -> m.getDeletedAt() == null), Member::getStatus),
        count(allCampaigns.stream(), Campaign::getCampaignType),
        allCampaigns.stream().filter(Campaign::isOpen).count(),
        count(allApplications.stream(), CampaignApplication::getStatus),
        count(contents.findAll().stream(), submission -> submission.getStatus()),
        selectedOrLater == 0 ? 0 : Math.round(completed * 1000.0 / selectedOrLater) / 10.0,
        count(orders.findAll().stream(), Order::getStatus),
        sumApproved(allPayments, PaymentStatus.PAID),
        sumApproved(allPayments, PaymentStatus.REFUNDED),
        count(settlements.findAll().stream(), settlement -> settlement.getStatus()),
        LocalDateTime.now());
  }

  /** 운영 설정 현황: 실행 프로필, 이메일 발송 방식, 소셜 로그인 설정 여부, DB 버전 등입니다. 비밀값은 담지 않습니다. */
  public AdminConsoleDtos.Settings settings(Long adminId) {
    memberService.getAdmin(adminId);
    String schemaVersion =
        jdbc
            .query(
                "select version from movrit_schema_history where success order by installed_rank"
                    + " desc limit 1",
                (rs, row) -> rs.getString(1))
            .stream()
            .findFirst()
            .orElse(null);
    return new AdminConsoleDtos.Settings(
        Arrays.asList(environment.getActiveProfiles()),
        emailDelivery,
        verificationExpiresMinutes,
        verificationHourlyLimit,
        configured(googleClientId),
        configured(kakaoClientId),
        schemaVersion,
        preorderLifecycleDelayMs,
        uploadDirectory);
  }

  // ───────────────────────── 내부 도우미 ─────────────────────────

  /** 회원 목록 한 줄을 만들 때 필요한 업체·인플루언서·소셜·신청 정보를 회원 ID 기준으로 한 번에 모은 것입니다. */
  private record MemberIndex(
      Map<Long, String> sellerStatus,
      Map<Long, Boolean> creator,
      Map<Long, List<String>> providers,
      Map<Long, Long> applicationCount) {}

  private MemberIndex memberIndex() {
    return new MemberIndex(
        sellers.findAll().stream()
            .collect(
                Collectors.toMap(
                    s -> s.getMember().getId(), s -> name(s.getStatus()), (a, b) -> a)),
        creators.findAll().stream()
            .collect(Collectors.toMap(c -> c.getMember().getId(), c -> true, (a, b) -> a)),
        socialAccounts.findAll().stream()
            .collect(
                Collectors.groupingBy(
                    s -> s.getMember().getId(),
                    Collectors.mapping(s -> name(s.getProvider()), Collectors.toList()))),
        applications.findAll().stream()
            .collect(
                Collectors.groupingBy(
                    a -> a.getCreator().getMember().getId(), Collectors.counting())));
  }

  private AdminConsoleDtos.MemberRow memberRow(Member member, MemberIndex index) {
    return new AdminConsoleDtos.MemberRow(
        member.getId(),
        member.getEmail(),
        member.getName(),
        member.getNickname(),
        name(member.getRole()),
        name(member.getStatus()),
        member.getEmailVerifiedAt() != null,
        index.sellerStatus().get(member.getId()),
        index.creator().getOrDefault(member.getId(), false),
        index.providers().getOrDefault(member.getId(), List.of()),
        index.applicationCount().getOrDefault(member.getId(), 0L),
        member.getCreatedAt(),
        member.getLastLoginAt());
  }

  private Map<Long, List<CampaignApplication>> applicationsByCreator() {
    return applications.findAll().stream()
        .collect(Collectors.groupingBy(a -> a.getCreator().getId()));
  }

  private AdminConsoleDtos.CreatorRow creatorRow(
      Creator creator, Map<Long, List<CampaignApplication>> byCreator) {
    List<CampaignApplication> mine = byCreator.getOrDefault(creator.getId(), List.of());
    return new AdminConsoleDtos.CreatorRow(
        creator.getId(),
        creator.getMember().getId(),
        creator.getNickname(),
        creator.getPrimaryCategory(),
        creator.getSnsUrl(),
        creator.getFollowerCount(),
        creator.getStatus(),
        mine.size(),
        mine.stream().filter(a -> a.getStatus() == CampaignApplicationStatus.COMPLETED).count(),
        creator.getCreatedAt());
  }

  private AdminConsoleDtos.ApplicationRow applicationRow(CampaignApplication application) {
    return new AdminConsoleDtos.ApplicationRow(
        application.getId(),
        application.getCampaign().getId(),
        application.getCampaign().getTitle(),
        name(application.getStatus()),
        application.getSubmittedAt());
  }

  private AdminConsoleDtos.PreorderRow preorderRow(Preorder preorder) {
    return new AdminConsoleDtos.PreorderRow(
        preorder.getId(),
        preorder.getTitle(),
        preorder.getProduct().getBrand().getName(),
        preorder.getProduct().getName(),
        name(preorder.getStatus()),
        preorder.getPreorderPrice(),
        preorder.getCurrency(),
        preorder.getTargetQuantity(),
        preorder.getOrderedQuantity(),
        preorder.getStartAt(),
        preorder.getEndAt());
  }

  private AdminConsoleDtos.OrderRow orderRow(Order order, Payment payment, Shipping shipping) {
    return new AdminConsoleDtos.OrderRow(
        order.getId(),
        order.getOrderNumber(),
        order.getMember().getName(),
        order.getPreorder().getTitle(),
        name(order.getStatus()),
        order.getTotalPaymentAmount(),
        order.getCurrency(),
        payment == null ? null : name(payment.getStatus()),
        shipping == null ? null : name(shipping.getStatus()),
        order.getCreatedAt());
  }

  /** 지정한 상태 결제의 승인 금액 합계입니다. */
  private static BigDecimal sumApproved(List<Payment> list, PaymentStatus status) {
    return list.stream()
        .filter(payment -> payment.getStatus() == status)
        .map(Payment::getApprovedAmount)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** 값(주로 상태 enum)별 건수를 이름 순으로 셉니다. */
  private static <T> Map<String, Long> count(java.util.stream.Stream<T> items, Function<T, ?> key) {
    return items.collect(
        Collectors.groupingBy(item -> name(key.apply(item)), TreeMap::new, Collectors.counting()));
  }

  /** enum이나 값을 문자열로 바꿉니다. null이면 '미지정'입니다. */
  private static String name(Object value) {
    return value == null ? "미지정" : value.toString();
  }

  /** 휴대전화 번호 가운데를 가립니다(예: 010-****-1234). */
  static String maskPhone(String phone) {
    if (phone == null || phone.isBlank()) return null;
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() < 8) return "****";
    return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
  }

  private static String joinAddress(String postalCode, String address1, String address2) {
    List<String> parts = new ArrayList<>();
    if (postalCode != null && !postalCode.isBlank()) parts.add("(" + postalCode + ")");
    if (address1 != null) parts.add(address1);
    if (address2 != null && !address2.isBlank()) parts.add(address2);
    return String.join(" ", parts);
  }

  /** 소셜 로그인 키가 실제로 설정되었는지 확인합니다. 통합 실행 스크립트의 기본값(local-unconfigured)은 미설정으로 봅니다. */
  private static boolean configured(String clientId) {
    return clientId != null && !clientId.isBlank() && !clientId.equals("local-unconfigured");
  }

  private static ResponseStatusException notFound(String what) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, what + "을(를) 찾을 수 없습니다.");
  }
}

package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignSelectionMode;
import com.movrit.movrit.collaboration.entity.Collaboration;
import com.movrit.movrit.collaboration.entity.CollaborationStatus;
import com.movrit.movrit.collaboration.entity.CollaborationType;
import com.movrit.movrit.collaboration.entity.ReferralClick;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.repository.CollaborationRepository;
import com.movrit.movrit.collaboration.repository.ReferralClickRepository;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.service.CreatorService;
import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderItem;
import com.movrit.movrit.order.entity.OrderStatus;
import com.movrit.movrit.order.repository.OrderItemRepository;
import com.movrit.movrit.order.repository.OrderRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.product.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 캠페인 등록·공개 목록 조회와 기존 크리에이터 협업(신청·제안·승인·추천 링크 성과) 기능을 맡습니다. 업체 쪽 처리는 상품 소유 업체만, 크리에이터 쪽 처리는 협업 당사자만
 * 할 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollaborationService {
  private final CampaignRepository campaigns;
  private final CollaborationRepository collaborations;
  private final ReferralClickRepository clicks;
  private final OrderRepository orders;
  private final OrderItemRepository orderItems;
  private final CreatorService creators;
  private final PreorderService preorders;
  private final ProductService products;
  private final CampaignApplicationRepository campaignApplications;
  private final CampaignOwnershipService campaignOwnership;

  /**
   * 업체가 자기 브랜드의 캠페인(모집 공고)을 새로 등록합니다. 선정 방식을 고르지 않으면 브랜드 직접 선정으로 저장하고 바로 공개합니다.
   *
   * <p>운영 브랜드 정하기:
   *
   * <ul>
   *   <li>프리오더를 함께 고르면 그 프리오더 상품의 브랜드가 운영 브랜드가 됩니다. 프리오더 하나에는 캠페인 하나만 만들 수 있습니다.
   *   <li>프리오더 없이 만들면 brandId의 브랜드(본인 소유)를 쓰고, 생략하면 운영 중인 브랜드가 하나일 때 그 브랜드를 씁니다.
   *   <li>프리오더와 brandId를 함께 보냈는데 브랜드가 다르면 400으로 거절합니다.
   * </ul>
   *
   * @throws ResponseStatusException 본인 브랜드·프리오더가 아니거나, 이미 캠페인이 있는 프리오더이거나(409), 모집 종료일이 시작일보다 빠르면
   *     거절합니다
   */
  @Transactional
  public CollaborationDtos.CampaignResponse createCampaign(
      Long memberId, CollaborationDtos.CampaignRequest r) {
    Preorder p = null;
    Brand brand;
    if (r.preorderId() != null) {
      p = preorders.getPreorder(r.preorderId());
      brand = products.getOwnedProduct(memberId, p.getProduct().getId()).getBrand();
      if (r.brandId() != null && !r.brandId().equals(brand.getId()))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 프리오더는 선택한 브랜드의 상품이 아닙니다.");
      if (campaigns.findByPreorderId(p.getId()).isPresent())
        throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 모집공고가 있습니다.");
    } else {
      brand = campaignOwnership.resolveBrandForNewCampaign(memberId, r.brandId());
    }
    validatePeriod(r.startAt(), r.endAt());
    Campaign c =
        Campaign.builder()
            .brand(brand)
            .preorder(p)
            .title(r.title().trim())
            .description(r.description())
            .category(r.category())
            .minFollowerCount(r.minFollowerCount())
            .recruitmentCount(r.recruitmentCount())
            .selectionMode(
                r.selectionMode() == null ? CampaignSelectionMode.BRAND_DIRECT : r.selectionMode())
            .rewardDescription(r.rewardDescription())
            .startAt(r.startAt())
            .endAt(r.endAt())
            .open(true)
            .build();
    // 유형·지역·주요 채널 분류와 (보냈다면) 참여 안내·세부 일정을 적용합니다.
    // 목록에 없는 지역이나 순서가 맞지 않는 일정이면 400으로 안내합니다.
    try {
      c.classify(r.campaignType(), r.region(), r.contentChannel());
      if (r.details() != null) applyDetails(c, r.details());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
    return CollaborationDtos.CampaignResponse.from(campaigns.save(c));
  }

  /** 등록 요청의 참여 안내(빈 칸은 저장하지 않음)와 세부 일정을 새 캠페인에 채웁니다. */
  private void applyDetails(Campaign c, CollaborationDtos.CampaignDetails d) {
    c.describe(
        blankToNull(d.mission()),
        blankToNull(d.channelGuide()),
        blankToNull(d.scheduleGuide()),
        blankToNull(d.deliveryGuide()),
        blankToNull(d.visitGuide()),
        blankToNull(d.caution()),
        blankToNull(d.submissionRequirement()),
        d.selectionAnnouncedAt(),
        d.experienceStartAt(),
        d.experienceEndAt(),
        d.contentSubmissionStartAt(),
        d.contentSubmissionEndAt(),
        d.resultsAnnouncedAt());
  }

  /** 빈 문자열이면 null, 아니면 앞뒤 공백을 지운 값을 돌려줍니다. */
  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /**
   * 현재 모집 기간 안에 있는 공개 캠페인 목록을 반환합니다.
   *
   * <p>카드에 "신청 N / 모집 M명"을 보여줄 수 있도록 취소를 제외한 신청 수를 캠페인 ID 묶음으로 한 번에 집계해 붙입니다. 신청이 없는 캠페인은 0으로
   * 응답합니다.
   */
  public List<CollaborationDtos.CampaignResponse> openCampaigns() {
    LocalDateTime now = LocalDateTime.now();
    List<Campaign> visible =
        campaigns.findAllByOpenTrueOrderByIdDesc().stream()
            .filter(c -> !now.isBefore(c.getStartAt()) && now.isBefore(c.getEndAt()))
            .toList();
    if (visible.isEmpty()) return List.of();
    Map<Long, Long> counts = new HashMap<>();
    campaignApplications
        .countActiveApplicationsByCampaignIds(visible.stream().map(Campaign::getId).toList())
        .forEach(row -> counts.put(row.getCampaignId(), row.getApplicationCount()));
    return visible.stream()
        .map(c -> CollaborationDtos.CampaignResponse.from(c, counts.getOrDefault(c.getId(), 0L)))
        .toList();
  }

  /** 업체 본인이 소유한 캠페인을 최신순으로 조회합니다. 공개·마감 여부와 관계없이 모두 보여줍니다. */
  public List<CollaborationDtos.CampaignResponse> sellerCampaigns(Long id) {
    return campaigns.findAllByBrandSellerMemberIdOrderByIdDesc(id).stream()
        .map(CollaborationDtos.CampaignResponse::from)
        .toList();
  }

  /**
   * 크리에이터가 캠페인에 협업을 신청합니다(기존 협업 신청 방식).
   *
   * <p>모집 중인 공개 캠페인이어야 하고, 승인된 크리에이터·최소 팔로워 충족·활동 채널 등록·이메일 확인을 모두 만족해야 합니다. 같은 프리오더에 이미 신청·제안 이력이
   * 있으면 거절합니다.
   */
  @Transactional
  public CollaborationDtos.Response apply(
      Long memberId, Long campaignId, CollaborationDtos.ApplyRequest r) {
    Campaign c = getCampaign(campaignId);
    LocalDateTime now = LocalDateTime.now();
    if (!c.isOpen() || now.isBefore(c.getStartAt()) || !now.isBefore(c.getEndAt()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 신청할 수 없는 캠페인입니다.");
    Creator creator = creators.getByMember(memberId);
    if (!"ACTIVE".equals(creator.getStatus()) && !"APPROVED".equals(creator.getStatus()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인된 크리에이터만 캠페인에 신청할 수 있습니다.");
    if (creator.getFollowerCount() < c.getMinFollowerCount())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "캠페인의 최소 팔로워 조건을 충족하지 못했습니다.");
    if (creator.getSnsUrl() == null || creator.getSnsUrl().isBlank())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "활동 채널 정보를 먼저 등록해 주세요.");
    if (!creator.getMember().isEmailVerified())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이메일 소유 확인 후 캠페인에 신청할 수 있습니다.");
    // 기존 협업 지원은 프리오더 판매 협업이므로, 프리오더가 연결되지 않은 체험단 캠페인에는 사용할 수 없습니다.
    if (c.getPreorder() == null)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "프리오더가 연결된 캠페인만 판매 협업에 지원할 수 있습니다. 캠페인 신청을 이용해 주세요.");
    return save(c.getPreorder(), creator, CollaborationType.APPLICATION, r.message());
  }

  /** 업체가 특정 크리에이터에게 자기 상품의 협업을 먼저 제안합니다. 같은 크리에이터에게 중복 제안할 수 없습니다. */
  @Transactional
  public CollaborationDtos.Response propose(Long sellerId, CollaborationDtos.ProposeRequest r) {
    Preorder p = preorders.getPreorder(r.preorderId());
    products.getOwnedProduct(sellerId, p.getProduct().getId());
    return save(p, creators.get(r.creatorId()), CollaborationType.PROPOSAL, r.message());
  }

  /** 크리에이터 본인의 협업 목록을 최신순으로 조회합니다. */
  public List<CollaborationDtos.Response> mine(Long memberId) {
    return collaborations.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(CollaborationDtos.Response::from)
        .toList();
  }

  /** 업체가 소유한 상품의 협업 목록을 최신순으로 조회합니다. */
  public List<CollaborationDtos.Response> seller(Long memberId) {
    return collaborations
        .findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
        .stream()
        .map(CollaborationDtos.Response::from)
        .toList();
  }

  /** 업체가 대기 중인 협업 신청을 승인하거나 거절합니다. 승인하면 추천 코드가 새로 발급됩니다. 대기 상태가 아니면 409로 거절합니다. */
  @Transactional
  public CollaborationDtos.Response sellerDecision(Long memberId, Long id, boolean approve) {
    Collaboration c = ownedBySeller(memberId, id);
    transit(
        () -> {
          if (approve) c.approve(code());
          else c.reject();
        });
    return CollaborationDtos.Response.from(c);
  }

  /** 크리에이터가 업체의 협업 제안을 수락하거나 거절합니다. 본인 협업이 아니면 403으로 거절합니다. */
  @Transactional
  public CollaborationDtos.Response creatorDecision(Long memberId, Long id, boolean accept) {
    Collaboration c = ownedByCreator(memberId, id);
    transit(
        () -> {
          if (accept) c.acceptProposal(code());
          else c.reject();
        });
    return CollaborationDtos.Response.from(c);
  }

  /** 업체가 진행 중인 협업을 종료합니다. 종료 후에는 추천 링크가 더 이상 유효하지 않습니다. */
  @Transactional
  public CollaborationDtos.Response end(Long memberId, Long id) {
    Collaboration c = ownedBySeller(memberId, id);
    transit(c::end);
    return CollaborationDtos.Response.from(c);
  }

  /** 추천 코드로 협업을 찾습니다. 코드가 없거나 진행 중인 협업이 아니면 거절합니다. */
  public Collaboration findByReferral(String code) {
    Collaboration c =
        collaborations
            .findByReferralCode(code)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 추천코드입니다."));
    if (c.getStatus() != CollaborationStatus.ACTIVE)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "활성 협업의 추천코드가 아닙니다.");
    return c;
  }

  /** 추천 링크 클릭을 한 건 기록합니다. 로그인 없이 호출되며 진행 중 협업의 코드만 기록합니다. */
  @Transactional
  public void click(String code, CollaborationDtos.ClickRequest r) {
    Collaboration c = findByReferral(code);
    clicks.save(
        ReferralClick.builder()
            .collaboration(c)
            .visitorKey(r.visitorKey())
            .userAgent(r.userAgent())
            .build());
  }

  /** 크리에이터 본인 협업별 클릭 수·주문 수·판매 수량·매출·전환율을 조회합니다. */
  public List<CollaborationDtos.PerformanceResponse> creatorPerformance(Long memberId) {
    return collaborations.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(this::performance)
        .toList();
  }

  /** 업체 상품에 연결된 협업별 성과(클릭·주문·판매 수량·매출·전환율)를 조회합니다. */
  public List<CollaborationDtos.PerformanceResponse> sellerPerformance(Long memberId) {
    return collaborations
        .findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
        .stream()
        .map(this::performance)
        .toList();
  }

  /** 협업 한 건의 성과를 계산합니다. 결제 대기·취소·환불 주문은 성과에서 제외합니다. */
  private CollaborationDtos.PerformanceResponse performance(Collaboration c) {
    long clickCount = clicks.countByCollaborationId(c.getId());
    List<Order> paid =
        orders.findAllByAttributionId(c.getId()).stream()
            .filter(
                o ->
                    o.getStatus() != OrderStatus.PENDING_PAYMENT
                        && o.getStatus() != OrderStatus.CANCELLED
                        && o.getStatus() != OrderStatus.REFUNDED)
            .toList();
    long qty =
        paid.stream()
            .flatMap(o -> orderItems.findAllByOrderId(o.getId()).stream())
            .mapToLong(OrderItem::getQuantity)
            .sum();
    BigDecimal gmv =
        paid.stream().map(Order::getTotalPaymentAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    double conversion = clickCount == 0 ? 0.0 : (double) paid.size() * 100 / clickCount;
    return new CollaborationDtos.PerformanceResponse(
        c.getId(),
        c.getCreator().getNickname(),
        c.getPreorder().getTitle(),
        clickCount,
        paid.size(),
        qty,
        gmv,
        conversion);
  }

  /** 새 협업을 대기 상태로 저장합니다. 같은 프리오더에 같은 크리에이터의 이력이 있으면 거절합니다. */
  private CollaborationDtos.Response save(Preorder p, Creator c, CollaborationType t, String m) {
    if (collaborations.existsByPreorderIdAndCreatorId(p.getId(), c.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 지원 또는 제안 이력이 있습니다.");
    return CollaborationDtos.Response.from(
        collaborations.save(
            Collaboration.builder()
                .preorder(p)
                .creator(c)
                .type(t)
                .status(CollaborationStatus.PENDING)
                .message(m.trim())
                .build()));
  }

  private Campaign getCampaign(Long id) {
    return campaigns
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "모집공고를 찾을 수 없습니다."));
  }

  private Collaboration get(Long id) {
    return collaborations
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "협업을 찾을 수 없습니다."));
  }

  private Collaboration ownedBySeller(Long m, Long id) {
    Collaboration c = get(id);
    // 기존 프리오더 협업은 프리오더 상품의 주인 기준으로 확인합니다.
    products.getOwnedProduct(m, c.getPreorder().getProduct().getId());
    return c;
  }

  private Collaboration ownedByCreator(Long m, Long id) {
    Collaboration c = get(id);
    if (!c.getCreator().getMember().getId().equals(m))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 협업만 처리할 수 있습니다.");
    return c;
  }

  /** 추천 링크에 쓸 임의 추천 코드(INF_ + 12자리)를 만듭니다. */
  private String code() {
    return "INF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
  }

  private void validatePeriod(LocalDateTime s, LocalDateTime e) {
    if (!s.isBefore(e))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "모집 종료일은 시작일보다 뒤여야 합니다.");
  }

  /** 엔티티 상태 변경 중 잘못된 상태 전환 예외를 409 응답으로 바꿉니다. */
  private void transit(Runnable r) {
    try {
      r.run();
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }
}

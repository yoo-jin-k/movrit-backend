package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.entity.*;
import com.movrit.movrit.collaboration.entity.ReferralClick;
import com.movrit.movrit.collaboration.repository.*;
import com.movrit.movrit.collaboration.repository.ReferralClickRepository;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.service.CreatorService;
import com.movrit.movrit.order.entity.*;
import com.movrit.movrit.order.repository.*;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.product.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

  @Transactional
  public CollaborationDtos.CampaignResponse createCampaign(
      Long memberId, CollaborationDtos.CampaignRequest r) {
    Preorder p = preorders.getPreorder(r.preorderId());
    products.getOwnedProduct(memberId, p.getProduct().getId());
    if (campaigns.findByPreorderId(p.getId()).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 모집공고가 있어.");
    validatePeriod(r.startAt(), r.endAt());
    Campaign c =
        Campaign.builder()
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
    return CollaborationDtos.CampaignResponse.from(campaigns.save(c));
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

  public List<CollaborationDtos.CampaignResponse> sellerCampaigns(Long id) {
    return campaigns.findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(id).stream()
        .map(CollaborationDtos.CampaignResponse::from)
        .toList();
  }

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
    return save(c.getPreorder(), creator, CollaborationType.APPLICATION, r.message());
  }

  @Transactional
  public CollaborationDtos.Response propose(Long sellerId, CollaborationDtos.ProposeRequest r) {
    Preorder p = preorders.getPreorder(r.preorderId());
    products.getOwnedProduct(sellerId, p.getProduct().getId());
    return save(p, creators.get(r.creatorId()), CollaborationType.PROPOSAL, r.message());
  }

  public List<CollaborationDtos.Response> mine(Long memberId) {
    return collaborations.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(CollaborationDtos.Response::from)
        .toList();
  }

  public List<CollaborationDtos.Response> seller(Long memberId) {
    return collaborations
        .findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
        .stream()
        .map(CollaborationDtos.Response::from)
        .toList();
  }

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

  @Transactional
  public CollaborationDtos.Response end(Long memberId, Long id) {
    Collaboration c = ownedBySeller(memberId, id);
    transit(c::end);
    return CollaborationDtos.Response.from(c);
  }

  public Collaboration findByReferral(String code) {
    Collaboration c =
        collaborations
            .findByReferralCode(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 추천코드야."));
    if (c.getStatus() != CollaborationStatus.ACTIVE)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "활성 협업의 추천코드가 아니야.");
    return c;
  }

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

  public List<CollaborationDtos.PerformanceResponse> creatorPerformance(Long memberId) {
    return collaborations.findAllByCreatorMemberIdOrderByIdDesc(memberId).stream()
        .map(this::performance)
        .toList();
  }

  public List<CollaborationDtos.PerformanceResponse> sellerPerformance(Long memberId) {
    return collaborations
        .findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
        .stream()
        .map(this::performance)
        .toList();
  }

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

  private CollaborationDtos.Response save(Preorder p, Creator c, CollaborationType t, String m) {
    if (collaborations.existsByPreorderIdAndCreatorId(p.getId(), c.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 지원 또는 제안 이력이 있어.");
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
    products.getOwnedProduct(m, c.getPreorder().getProduct().getId());
    return c;
  }

  private Collaboration ownedByCreator(Long m, Long id) {
    Collaboration c = get(id);
    if (!c.getCreator().getMember().getId().equals(m))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 협업만 처리할 수 있어.");
    return c;
  }

  private String code() {
    return "INF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
  }

  private void validatePeriod(LocalDateTime s, LocalDateTime e) {
    if (!s.isBefore(e))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "모집 종료일은 시작일보다 뒤여야 해.");
  }

  private void transit(Runnable r) {
    try {
      r.run();
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }
}

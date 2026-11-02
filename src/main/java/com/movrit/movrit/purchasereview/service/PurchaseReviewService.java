package com.movrit.movrit.purchasereview.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignType;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.purchasereview.dto.PurchaseReviewDtos;
import com.movrit.movrit.purchasereview.entity.CampaignPurchaseProof;
import com.movrit.movrit.purchasereview.entity.CampaignPurchaseReview;
import com.movrit.movrit.purchasereview.repository.CampaignPurchaseProofRepository;
import com.movrit.movrit.purchasereview.repository.CampaignPurchaseReviewRepository;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 구매평 캠페인의 구매 안내·구매 인증·환급 기록을 처리합니다.
 *
 * <p>진행 순서: 신청 → 선정(참여 중) → <b>구매 인증 제출</b>(체험 완료 확인을 대신함) → 구매평(콘텐츠) 제출 → 검토·승인(완료) → <b>업체가 직접
 * 환급하고 '환급 완료' 기록</b>. MOVRIT은 결제·송금을 처리하지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class PurchaseReviewService {
  private final CampaignRepository campaignRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final CampaignOwnershipService campaignOwnership;
  private final CampaignPurchaseReviewRepository reviewRepository;
  private final CampaignPurchaseProofRepository proofRepository;
  private final PurchaseProofStorage storage;
  private final MemberService memberService;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  /** 구매 안내를 공개 조회합니다. 아직 등록하지 않았으면 404입니다. */
  @Transactional(readOnly = true)
  public PurchaseReviewDtos.Info info(Long campaignId) {
    return reviewRepository
        .findById(campaignId)
        .map(PurchaseReviewDtos.Info::from)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "구매 안내가 아직 등록되지 않았습니다."));
  }

  /**
   * 업체가 구매 안내를 저장합니다. 구매평 유형 캠페인에서만 쓸 수 있습니다.
   *
   * @throws ResponseStatusException 캠페인 주인이 아니면 403/404, 구매평 캠페인이 아니면 409, 값이 맞지 않으면 400
   */
  @Transactional
  public PurchaseReviewDtos.Info saveInfo(
      Long sellerMemberId, Long campaignId, PurchaseReviewDtos.InfoRequest request) {
    Campaign campaign =
        campaignRepository
            .findById(campaignId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    campaignOwnership.requireOwner(sellerMemberId, campaign);
    if (campaign.getCampaignType() != CampaignType.PURCHASE_REVIEW) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "구매평 캠페인에서만 구매 안내를 등록할 수 있습니다.");
    }
    String url = request.productUrl().strip();
    requireWebAddress(url);
    if (request.paybackAmount() > request.productPrice()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "환급액은 판매가보다 클 수 없습니다.");
    }
    CampaignPurchaseReview review =
        reviewRepository
            .findById(campaignId)
            .orElseGet(() -> new CampaignPurchaseReview(campaignId));
    review.update(
        request.storeName().strip(),
        url,
        request.productPrice(),
        request.paybackAmount(),
        blankToNull(request.purchaseGuide()));
    return PurchaseReviewDtos.Info.from(reviewRepository.save(review));
  }

  /**
   * 참여자가 구매 인증(주문번호·결제 금액·주문 내역 캡처)을 제출합니다.
   *
   * <p>처음 제출하면 '체험 완료 확인'과 같은 효과가 있어 구매평(콘텐츠)을 제출할 수 있게 됩니다. 환급 전까지는 다시 제출해 고칠 수 있습니다.
   *
   * @throws ResponseStatusException 본인 신청이 아니면 404, 구매평 신청이 아니거나 진행 단계가 아니면 409, 값이 맞지 않으면 400
   */
  @Transactional
  public PurchaseReviewDtos.Proof submitProof(
      Long memberId,
      Long applicationId,
      String orderNumber,
      Integer paidAmount,
      MultipartFile file) {
    CampaignApplication application =
        applicationRepository
            .findByIdAndCreatorMemberId(applicationId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    if (application.getFulfillmentType() != FulfillmentType.PURCHASE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "구매평 캠페인 신청에서만 구매 인증을 제출할 수 있습니다.");
    }
    CampaignApplicationStatus status = application.getStatus();
    if (status != CampaignApplicationStatus.ACTIVE
        && status != CampaignApplicationStatus.REVISION_REQUESTED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "참여 중인 캠페인에서만 구매 인증을 제출할 수 있습니다.");
    }
    String order = orderNumber == null ? "" : orderNumber.strip();
    if (order.isEmpty() || order.length() > 60) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문번호를 60자 이내로 입력해 주세요.");
    }
    if (paidAmount == null || paidAmount < 0 || paidAmount > 100_000_000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액을 확인해 주세요.");
    }
    CampaignPurchaseProof proof =
        proofRepository
            .findById(applicationId)
            .orElseGet(() -> new CampaignPurchaseProof(applicationId));
    if (proof.isPaid()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "환급이 끝난 구매 인증은 바꿀 수 없습니다.");
    }
    String key = storage.store(file);
    boolean first = proof.getSubmittedAt() == null;
    String previousKey;
    try {
      previousKey = proof.submit(order, paidAmount, key);
      proofRepository.save(proof);
      if (status == CampaignApplicationStatus.ACTIVE) application.confirmExperience();
    } catch (RuntimeException exception) {
      storage.delete(key);
      throw exception;
    }
    // 바뀐 캡처는 저장이 확정된 뒤에만 이전 파일을 지웁니다.
    afterCommit(() -> storage.delete(previousKey));
    notificationService.send(
        sellerMemberId(application),
        "CAMPAIGN_PURCHASE",
        first ? "구매 인증이 제출되었습니다." : "구매 인증이 수정되었습니다.",
        application.getCreator().getNickname() + "님 · " + application.getCampaign().getTitle(),
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        memberId,
        "CAMPAIGN_PURCHASE_PROOF_SUBMITTED",
        "CAMPAIGN_APPLICATION",
        application.getId(),
        null,
        "{\"paidAmount\":" + paidAmount + "}",
        null);
    return PurchaseReviewDtos.Proof.from(proof);
  }

  /**
   * 구매 인증·환급 상태를 조회합니다. 참여자 본인·캠페인 업체·운영자만 볼 수 있고, 아직 제출 전이면 비어 있습니다.
   *
   * @throws ResponseStatusException 볼 권한이 없으면 404
   */
  @Transactional(readOnly = true)
  public Optional<PurchaseReviewDtos.Proof> proof(Long memberId, Long applicationId) {
    readableApplication(memberId, applicationId);
    return proofRepository.findById(applicationId).map(PurchaseReviewDtos.Proof::from);
  }

  /** 주문 내역 캡처 파일입니다. 권한은 {@link #proof}와 같습니다. */
  @Transactional(readOnly = true)
  public Path evidence(Long memberId, Long applicationId) {
    readableApplication(memberId, applicationId);
    CampaignPurchaseProof proof =
        proofRepository
            .findById(applicationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "구매 인증이 아직 없습니다."));
    return storage.load(proof.getEvidenceKey());
  }

  /** 캡처 응답 형식입니다. */
  public static String evidenceType(Path path) {
    return PurchaseProofStorage.contentType(path.getFileName().toString());
  }

  /**
   * 업체가 환급 완료를 기록합니다. 구매평이 승인되어 '완료'된 신청에서만, 한 번만 할 수 있습니다.
   *
   * <p>실제 송금은 업체가 MOVRIT 밖에서 직접 합니다. 이 기록은 참여자에게 알림으로 전달됩니다.
   *
   * @throws ResponseStatusException 캠페인 주인이 아니면 403/404, 완료 전이거나 이미 기록했으면 409
   */
  @Transactional
  public PurchaseReviewDtos.Proof markPaid(
      Long sellerMemberId, Long applicationId, PurchaseReviewDtos.PaybackRequest request) {
    CampaignApplication application = getApplication(applicationId);
    campaignOwnership.requireOwner(sellerMemberId, application.getCampaign());
    if (application.getStatus() != CampaignApplicationStatus.COMPLETED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "구매평 검토가 끝나 완료된 참여자만 환급 완료로 기록할 수 있습니다.");
    }
    CampaignPurchaseProof proof =
        proofRepository
            .findById(applicationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.CONFLICT, "구매 인증이 없는 신청입니다."));
    try {
      proof.markPaid(blankToNull(request == null ? null : request.note()));
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
    }
    int payback =
        reviewRepository
            .findById(application.getCampaign().getId())
            .map(CampaignPurchaseReview::getPaybackAmount)
            .orElse(0);
    notificationService.send(
        application.getCreator().getMember().getId(),
        "CAMPAIGN_PAYBACK",
        "구매평 환급이 완료되었습니다.",
        application.getCampaign().getTitle()
            + (payback > 0 ? " · " + String.format("%,d", payback) + "원" : ""),
        "CAMPAIGN_APPLICATION",
        application.getId());
    auditLogService.record(
        sellerMemberId,
        "CAMPAIGN_PAYBACK_RECORDED",
        "CAMPAIGN_APPLICATION",
        application.getId(),
        "{\"paybackStatus\":\"PENDING\"}",
        "{\"paybackStatus\":\"PAID\"}",
        proof.getPaybackNote());
    return PurchaseReviewDtos.Proof.from(proof);
  }

  /** 업체의 구매평 캠페인 참여자별 구매 인증·환급 현황입니다(최신 신청 먼저). 접수·미선정·취소된 신청은 뺍니다. */
  @Transactional(readOnly = true)
  public java.util.List<PurchaseReviewDtos.PaybackRow> sellerPaybacks(Long sellerMemberId) {
    java.util.Set<CampaignApplicationStatus> skipped =
        java.util.Set.of(
            CampaignApplicationStatus.SUBMITTED,
            CampaignApplicationStatus.REJECTED,
            CampaignApplicationStatus.CANCELLED);
    java.util.Map<Long, Integer> paybackByCampaign = new java.util.HashMap<>();
    return applicationRepository
        .findAllByCampaignBrandSellerMemberIdOrderByIdDesc(sellerMemberId)
        .stream()
        .filter(application -> application.getFulfillmentType() == FulfillmentType.PURCHASE)
        .filter(application -> !skipped.contains(application.getStatus()))
        .map(
            application -> {
              Long campaignId = application.getCampaign().getId();
              Integer payback =
                  paybackByCampaign.computeIfAbsent(
                      campaignId,
                      id ->
                          reviewRepository
                              .findById(id)
                              .map(CampaignPurchaseReview::getPaybackAmount)
                              .orElse(null));
              CampaignPurchaseProof proof =
                  proofRepository.findById(application.getId()).orElse(null);
              String status = proof == null ? "NO_PROOF" : proof.getPaybackStatus();
              return new PurchaseReviewDtos.PaybackRow(
                  application.getId(),
                  campaignId,
                  application.getCampaign().getTitle(),
                  application.getCreator().getNickname(),
                  application.getStatus().name(),
                  proof == null ? null : proof.getOrderNumber(),
                  proof == null ? null : proof.getPaidAmount(),
                  proof == null ? null : proof.getSubmittedAt(),
                  payback,
                  status,
                  proof == null ? null : proof.getPaybackPaidAt(),
                  proof == null ? null : proof.getPaybackNote(),
                  proof != null
                      && !proof.isPaid()
                      && application.getStatus() == CampaignApplicationStatus.COMPLETED);
            })
        .toList();
  }

  /** 참여자 본인·캠페인 업체·운영자만 통과시킵니다. 그 밖에는 신청이 있는지도 알리지 않도록 404입니다. */
  private CampaignApplication readableApplication(Long memberId, Long applicationId) {
    CampaignApplication application = getApplication(applicationId);
    if (application.getCreator().getMember().getId().equals(memberId)) return application;
    if (memberService.getMember(memberId).isAdmin()) return application;
    if (sellerMemberId(application).equals(memberId)) return application;
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다.");
  }

  private CampaignApplication getApplication(Long applicationId) {
    return applicationRepository
        .findById(applicationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
  }

  private static Long sellerMemberId(CampaignApplication application) {
    return application.getCampaign().getBrand().getSeller().getMember().getId();
  }

  /** 상품 주소는 http·https 주소만 받습니다(javascript: 같은 주소로 화면이 열리지 않게). */
  static void requireWebAddress(String url) {
    try {
      URI uri = URI.create(url);
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
      if ((scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null) return;
    } catch (IllegalArgumentException ignored) {
      // 아래에서 안내합니다.
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "상품 페이지 주소를 http(s)://로 시작하게 입력해 주세요.");
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static void afterCommit(Runnable task) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              task.run();
            }
          });
    } else {
      task.run();
    }
  }
}

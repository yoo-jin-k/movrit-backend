package com.movrit.movrit.campaignshipping.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationStatus;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaignshipping.dto.CampaignShippingDtos;
import com.movrit.movrit.campaignshipping.entity.CampaignShipment;
import com.movrit.movrit.campaignshipping.repository.CampaignShipmentRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import com.movrit.movrit.common.excel.SimpleXlsxWriter;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 배송형 캠페인의 배송 관리를 처리합니다.
 *
 * <ul>
 *   <li>목록: 선정된(참여 중 이후) 배송형 참여자의 배송지와 발송 상태를 보여줍니다.
 *   <li>송장 저장: 여러 명을 한 번에 저장하고, 건별로 성공·실패 사유를 돌려줍니다. 처음 저장하면 '발송' 알림, 고치면 '송장 변경' 알림을 보냅니다. 참여자가
 *       수령을 확인한 뒤에는 고칠 수 없습니다.
 *   <li>엑셀 내려받기: 배송지와 송장 칸이 담긴 .xlsx 파일입니다. 개인정보를 담고 있어 내려받을 때마다 감사 기록을 남깁니다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CampaignShippingService {
  /** 고를 수 있는 택배사입니다. */
  public static final List<String> COURIERS =
      List.of("CJ대한통운", "우체국택배", "한진택배", "롯데택배", "로젠택배", "경동택배", "기타");

  private static final Pattern TRACKING = Pattern.compile("^[0-9A-Za-z-]{6,30}$");
  private static final Set<CampaignApplicationStatus> SHIPPABLE =
      Set.of(
          CampaignApplicationStatus.SELECTED,
          CampaignApplicationStatus.ACTIVE,
          CampaignApplicationStatus.CONTENT_SUBMITTED,
          CampaignApplicationStatus.REVISION_REQUESTED,
          CampaignApplicationStatus.COMPLETED);
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final CampaignRepository campaignRepository;
  private final CampaignApplicationRepository applicationRepository;
  private final CampaignShipmentRepository shipmentRepository;
  private final CampaignOwnershipService campaignOwnership;
  private final MemberService memberService;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  /** 캠페인의 배송 관리 목록입니다(신청 번호 순). */
  @Transactional(readOnly = true)
  public List<CampaignShippingDtos.Row> rows(Long sellerMemberId, Long campaignId) {
    ownedCampaign(sellerMemberId, campaignId);
    List<CampaignApplication> applications = shippable(campaignId);
    Map<Long, CampaignShipment> shipments =
        shipmentRepository
            .findAllByApplicationIdIn(
                applications.stream().map(CampaignApplication::getId).toList())
            .stream()
            .collect(Collectors.toMap(CampaignShipment::getApplicationId, Function.identity()));
    return applications.stream()
        .map(
            application -> {
              CampaignShipment shipment = shipments.get(application.getId());
              return new CampaignShippingDtos.Row(
                  application.getId(),
                  application.getCreator().getNickname(),
                  application.getStatus().name(),
                  application.getRecipientName(),
                  application.getRecipientPhone(),
                  application.getPostalCode(),
                  application.getAddress1(),
                  application.getAddress2(),
                  shipment == null ? null : shipment.getCourier(),
                  shipment == null ? null : shipment.getTrackingNumber(),
                  shipment == null ? null : shipment.getShippedAt(),
                  application.getExperienceConfirmedAt());
            })
        .toList();
  }

  /**
   * 여러 참여자의 송장을 저장합니다. 한 건이 실패해도 나머지는 저장합니다.
   *
   * @throws ResponseStatusException 캠페인 주인이 아니면 403/404
   */
  @Transactional
  public CampaignShippingDtos.SaveResponse save(
      Long sellerMemberId, Long campaignId, CampaignShippingDtos.SaveRequest request) {
    Campaign campaign = ownedCampaign(sellerMemberId, campaignId);
    Map<Long, CampaignApplication> applications =
        shippable(campaignId).stream()
            .collect(
                Collectors.toMap(
                    CampaignApplication::getId,
                    Function.identity(),
                    (a, b) -> a,
                    LinkedHashMap::new));
    List<CampaignShippingDtos.SaveResult> results = new ArrayList<>();
    for (CampaignShippingDtos.ShipmentInput input : request.shipments()) {
      String problem = problem(input, applications.get(input.applicationId()));
      if (problem != null) {
        results.add(new CampaignShippingDtos.SaveResult(input.applicationId(), false, problem));
        continue;
      }
      CampaignApplication application = applications.get(input.applicationId());
      String courier = input.courier().trim();
      String tracking = input.trackingNumber().trim();
      Optional<CampaignShipment> existing = shipmentRepository.findById(application.getId());
      boolean first = existing.isEmpty();
      boolean changed;
      if (first) {
        shipmentRepository.save(new CampaignShipment(application.getId(), courier, tracking));
        changed = true;
      } else {
        changed = existing.get().change(courier, tracking);
      }
      if (changed) {
        notificationService.send(
            application.getCreator().getMember().getId(),
            "CAMPAIGN_SHIPPING",
            first ? "체험 상품이 발송되었습니다." : "송장 정보가 바뀌었습니다.",
            campaign.getTitle() + " · " + courier + " " + tracking,
            "CAMPAIGN_APPLICATION",
            application.getId());
      }
      results.add(new CampaignShippingDtos.SaveResult(application.getId(), true, null));
    }
    int success = (int) results.stream().filter(CampaignShippingDtos.SaveResult::success).count();
    return new CampaignShippingDtos.SaveResponse(success, results.size() - success, results);
  }

  /** 엑셀(.xlsx) 내려받기 파일입니다. 내려받을 때마다 감사 기록을 남깁니다(그래서 쓰기 트랜잭션입니다). */
  @Transactional
  public byte[] export(Long sellerMemberId, Long campaignId) {
    List<CampaignShippingDtos.Row> rows = rows(sellerMemberId, campaignId);
    List<List<String>> data =
        rows.stream()
            .map(
                row ->
                    List.of(
                        String.valueOf(row.applicationId()),
                        text(row.creatorNickname()),
                        text(row.recipientName()),
                        text(row.recipientPhone()),
                        text(row.postalCode()),
                        text(row.address1()),
                        text(row.address2()),
                        text(row.courier()),
                        text(row.trackingNumber()),
                        row.shippedAt() == null ? "" : row.shippedAt().format(TIME),
                        row.receivedAt() == null ? "" : row.receivedAt().format(TIME)))
            .toList();
    auditLogService.record(
        sellerMemberId,
        "CAMPAIGN_SHIPPING_EXPORTED",
        "CAMPAIGN",
        campaignId,
        null,
        "{\"rows\":" + rows.size() + "}",
        null);
    return SimpleXlsxWriter.write(
        "배송지",
        List.of("신청번호", "닉네임", "수령인", "연락처", "우편번호", "주소", "상세주소", "택배사", "송장번호", "발송일", "수령 확인"),
        data);
  }

  /** 참여자 본인·캠페인 업체·운영자가 보는 발송 정보입니다. 아직 발송 전이면 비어 있습니다. */
  @Transactional(readOnly = true)
  public Optional<CampaignShippingDtos.Shipment> shipment(Long memberId, Long applicationId) {
    CampaignApplication application =
        applicationRepository
            .findById(applicationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
    boolean mine = application.getCreator().getMember().getId().equals(memberId);
    if (!mine && !memberService.getMember(memberId).isAdmin()) {
      campaignOwnership.requireOwner(memberId, application.getCampaign());
    }
    return shipmentRepository.findById(applicationId).map(CampaignShippingDtos.Shipment::from);
  }

  /** 송장 한 건의 문제를 찾습니다. 문제가 없으면 null입니다. */
  static String problem(CampaignShippingDtos.ShipmentInput input, CampaignApplication application) {
    if (application == null) return "이 캠페인의 선정된 배송형 참여자가 아닙니다.";
    if (application.getExperienceConfirmedAt() != null) return "참여자가 이미 수령을 확인해 고칠 수 없습니다.";
    if (input.courier() == null || !COURIERS.contains(input.courier().trim()))
      return "택배사를 골라 주세요.";
    if (input.trackingNumber() == null
        || !TRACKING.matcher(input.trackingNumber().trim()).matches()) {
      return "송장번호는 영문·숫자·하이픈 6~30자로 입력해 주세요.";
    }
    return null;
  }

  private List<CampaignApplication> shippable(Long campaignId) {
    return applicationRepository.findAllByCampaignId(campaignId).stream()
        .filter(application -> application.getFulfillmentType() == FulfillmentType.DELIVERY)
        .filter(application -> SHIPPABLE.contains(application.getStatus()))
        .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
        .toList();
  }

  private Campaign ownedCampaign(Long sellerMemberId, Long campaignId) {
    Campaign campaign =
        campaignRepository
            .findById(campaignId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    campaignOwnership.requireOwner(sellerMemberId, campaign);
    return campaign;
  }

  private static String text(String value) {
    return value == null ? "" : value;
  }
}

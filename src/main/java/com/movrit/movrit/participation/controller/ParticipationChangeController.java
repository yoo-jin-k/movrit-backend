package com.movrit.movrit.participation.controller;

import com.movrit.movrit.common.security.JwtMemberId;
import com.movrit.movrit.participation.dto.ParticipationChangeDtos;
import com.movrit.movrit.participation.service.ParticipationChangeService;
import com.movrit.movrit.participation.service.ParticipationJobService;
import com.movrit.movrit.participation.service.UnselectedRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 참여 조정 API입니다.
 *
 * <ul>
 *   <li>GET /api/campaigns/applications/{id}/changes: 지금 할 수 있는 일(철회·취소·연장)과 진행 중인 요청(참여자)
 *   <li>POST …/withdraw: 선정 발표 전 신청 철회 · POST …/self-cancel: 참여 취소(취소 횟수 1회)
 *   <li>POST …/cancel-requests: 협의 취소 요청(사유 + 증빙 이미지) · GET /api/cancel-requests/{id}/evidence
 *   <li>POST …/extension: 기간 연장 요청 · PATCH …/extension: 업체·운영자 승인·반려 · GET
 *       /api/campaigns/extension-requests
 *   <li>GET·PATCH /api/admin/cancel-requests: 운영자 협의 취소 확인
 *   <li>GET /api/sellers/me/unselected-campaigns, /api/admin/unselected-campaigns: 업체 미선정 기록
 *   <li>POST /api/admin/participation-jobs/run: 마감 미제출 자동 취소·미선정 기록을 바로 실행(점검용)
 * </ul>
 */
@Tag(name = "공통 · 참여 조정(철회·취소·연장)")
@RestController
@RequiredArgsConstructor
public class ParticipationChangeController {
  private final ParticipationChangeService service;
  private final UnselectedRecordService unselected;
  private final ParticipationJobService jobs;

  /** 참여자가 이 신청으로 지금 할 수 있는 일(철회·취소·연장)과 진행 중인 요청, 취소 횟수를 조회합니다. */
  @GetMapping("/api/campaigns/applications/{applicationId}/changes")
  public ParticipationChangeDtos.Status status(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.status(JwtMemberId.from(jwt), applicationId);
  }

  /** 참여자가 선정 발표 전에 신청을 철회합니다. 취소 횟수는 남지 않습니다. */
  @PostMapping("/api/campaigns/applications/{applicationId}/withdraw")
  public ParticipationChangeDtos.Status withdraw(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody(required = false) ParticipationChangeDtos.WithdrawRequest request) {
    return service.withdraw(JwtMemberId.from(jwt), applicationId, request);
  }

  /** 참여자가 선정된 참여를 바로 취소합니다(단순 취소). 취소 횟수 1회가 남습니다. */
  @PostMapping("/api/campaigns/applications/{applicationId}/self-cancel")
  public ParticipationChangeDtos.Status selfCancel(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody ParticipationChangeDtos.SelfCancelRequest request) {
    return service.selfCancel(JwtMemberId.from(jwt), applicationId, request);
  }

  /** 참여자가 사유와 증빙 이미지(JPG·PNG, 5MB)를 올려 협의 취소를 요청합니다. 운영자가 확인합니다. */
  @PostMapping(
      value = "/api/campaigns/applications/{applicationId}/cancel-requests",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ParticipationChangeDtos.Status requestAgreedCancel(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @RequestPart("reason") String reason,
      @RequestPart("evidence") MultipartFile evidence) {
    return service.requestAgreedCancel(JwtMemberId.from(jwt), applicationId, reason, evidence);
  }

  /** 요청한 참여자·캠페인 업체·운영자만 협의 취소 증빙 이미지를 내려받습니다. */
  @GetMapping("/api/cancel-requests/{requestId}/evidence")
  public ResponseEntity<Resource> cancelEvidence(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId) {
    ParticipationChangeService.Evidence evidence =
        service.cancelEvidence(JwtMemberId.from(jwt), requestId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(evidence.contentType()))
        .body(new FileSystemResource(evidence.path()));
  }

  /** 참여자가 기간 연장(1~30일)을 요청합니다. 신청마다 한 번만 요청할 수 있습니다. */
  @PostMapping("/api/campaigns/applications/{applicationId}/extension")
  public ParticipationChangeDtos.Status requestExtension(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody ParticipationChangeDtos.ExtensionRequest request) {
    return service.requestExtension(JwtMemberId.from(jwt), applicationId, request);
  }

  /** 캠페인 업체(또는 운영자)가 기간 연장을 승인하거나 반려합니다. 승인하면 이 신청의 제출 마감이 늦춰집니다. */
  @PatchMapping("/api/campaigns/applications/{applicationId}/extension")
  public ParticipationChangeDtos.Extension decideExtension(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody ParticipationChangeDtos.Decision decision) {
    return service.decideExtension(JwtMemberId.from(jwt), applicationId, decision);
  }

  /** 업체가 운영하는 캠페인의 기간 연장 요청 목록을 조회합니다(확인 중인 요청 먼저). */
  @GetMapping("/api/campaigns/extension-requests")
  public List<ParticipationChangeDtos.Extension> sellerExtensions(
      @AuthenticationPrincipal Jwt jwt) {
    return service.sellerExtensions(JwtMemberId.from(jwt));
  }

  /** 캠페인 업체(또는 운영자)가 참여자 한 명의 체험 기간·콘텐츠 마감을 직접 늘립니다. 횟수 제한은 없고 모두 기록됩니다. */
  @PostMapping("/api/campaigns/applications/{applicationId}/extensions")
  public ParticipationChangeDtos.ApplicationExtensions directExtend(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long applicationId,
      @Valid @RequestBody ParticipationChangeDtos.DirectExtensionRequest request) {
    return service.directExtend(JwtMemberId.from(jwt), applicationId, request);
  }

  /** 신청 하나의 연장 현황과 기록, 참여자의 연장 횟수를 조회합니다(캠페인 업체·운영자). */
  @GetMapping("/api/campaigns/applications/{applicationId}/extensions")
  public ParticipationChangeDtos.ApplicationExtensions applicationExtensions(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long applicationId) {
    return service.applicationExtensions(JwtMemberId.from(jwt), applicationId);
  }

  /** 운영자가 전체 기간 연장 요청을 조회합니다(확인 중인 요청 먼저, 최근 200건). */
  @GetMapping("/api/admin/extension-requests")
  public List<ParticipationChangeDtos.Extension> adminExtensions(@AuthenticationPrincipal Jwt jwt) {
    return service.adminExtensions(JwtMemberId.from(jwt));
  }

  /** 운영자가 회원 한 명의 기간 연장 횟수와 이력을 조회합니다. */
  @GetMapping("/api/admin/members/{memberId}/extensions")
  public ParticipationChangeDtos.MemberExtensionHistory memberExtensions(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long memberId) {
    return service.memberExtensions(JwtMemberId.from(jwt), memberId);
  }

  /** 운영자가 협의 취소 요청 목록을 조회합니다(확인 중인 요청 먼저, 최근 200건). */
  @GetMapping("/api/admin/cancel-requests")
  public List<ParticipationChangeDtos.CancelRequest> adminCancelRequests(
      @AuthenticationPrincipal Jwt jwt) {
    return service.adminCancelRequests(JwtMemberId.from(jwt));
  }

  /** 운영자가 협의 취소를 승인(취소 횟수 없이 취소)하거나 반려합니다. */
  @PatchMapping("/api/admin/cancel-requests/{requestId}")
  public ParticipationChangeDtos.CancelRequest decideCancelRequest(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long requestId,
      @Valid @RequestBody ParticipationChangeDtos.Decision decision) {
    return service.decideCancelRequest(JwtMemberId.from(jwt), requestId, decision);
  }

  /** 업체가 자기 미선정 기록(선정 마감까지 아무도 선정하지 않은 캠페인)과 횟수를 조회합니다. */
  @GetMapping("/api/sellers/me/unselected-campaigns")
  public ParticipationChangeDtos.UnselectedSummary myUnselected(@AuthenticationPrincipal Jwt jwt) {
    return unselected.mine(JwtMemberId.from(jwt));
  }

  /** 운영자가 전체 업체 미선정 기록을 조회합니다(최근 200건). */
  @GetMapping("/api/admin/unselected-campaigns")
  public ParticipationChangeDtos.UnselectedSummary allUnselected(@AuthenticationPrincipal Jwt jwt) {
    return unselected.all(JwtMemberId.from(jwt));
  }

  /** 운영자가 마감 미제출 자동 취소와 업체 미선정 기록을 지금 바로 한 번 실행합니다(점검용). */
  @PostMapping("/api/admin/participation-jobs/run")
  public ParticipationChangeDtos.JobResult runJobs(@AuthenticationPrincipal Jwt jwt) {
    return jobs.runNow(JwtMemberId.from(jwt));
  }
}

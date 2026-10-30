package com.movrit.movrit.inquiry.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.inquiry.dto.InquiryDtos;
import com.movrit.movrit.inquiry.entity.Inquiry;
import com.movrit.movrit.inquiry.entity.InquiryStatus;
import com.movrit.movrit.inquiry.repository.InquiryRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.service.PreorderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 1:1 문의 등록·조회와 운영자 답변을 처리합니다. 답변이 등록되면 작성자에게 알림을 보내고 감사 기록을 남깁니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {
  private final InquiryRepository repository;
  private final MemberService memberService;
  private final PreorderService preorderService;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  /** 로그인한 회원이 문의를 등록합니다. 프리오더 ID가 있으면 해당 프리오더와 연결합니다. */
  @Transactional
  public InquiryDtos.Response create(Long memberId, InquiryDtos.CreateRequest request) {
    Member member = memberService.getMember(memberId);
    Preorder preorder =
        request.preorderId() == null ? null : preorderService.getPreorder(request.preorderId());
    Inquiry inquiry =
        repository.save(
            Inquiry.builder()
                .member(member)
                .preorder(preorder)
                .category(request.category().trim())
                .title(request.title().trim())
                .content(request.content().trim())
                .status(InquiryStatus.OPEN)
                .build());
    auditLogService.record(
        memberId,
        "INQUIRY_CREATED",
        "INQUIRY",
        inquiry.getId(),
        null,
        "{\"status\":\"OPEN\"}",
        null);
    return InquiryDtos.Response.from(inquiry);
  }

  /** 로그인한 회원 본인의 문의를 최신순으로 반환합니다. */
  public List<InquiryDtos.Response> findMine(Long memberId) {
    return repository.findAllByMemberIdOrderByIdDesc(memberId).stream()
        .map(InquiryDtos.Response::from)
        .toList();
  }

  /** 운영자가 전체 문의를 최신순으로 조회합니다. 관리자가 아니면 거절됩니다. */
  public List<InquiryDtos.Response> findAll(Long adminId) {
    memberService.getAdmin(adminId);
    return repository.findAllByOrderByIdDesc().stream().map(InquiryDtos.Response::from).toList();
  }

  /** 운영자가 문의에 답변합니다. 종료된 문의면 409로 거절하고, 성공하면 작성자에게 알림을 보냅니다. */
  @Transactional
  public InquiryDtos.Response answer(
      Long adminId, Long inquiryId, InquiryDtos.AnswerRequest request) {
    Member admin = memberService.getAdmin(adminId);
    Inquiry inquiry = get(inquiryId);
    try {
      inquiry.answer(admin, request.answer().trim());
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
    }
    notificationService.send(
        inquiry.getMember().getId(),
        "INQUIRY",
        "문의에 답변이 등록되었습니다.",
        inquiry.getTitle(),
        "INQUIRY",
        inquiry.getId());
    auditLogService.record(
        adminId,
        "INQUIRY_ANSWERED",
        "INQUIRY",
        inquiry.getId(),
        "{\"status\":\"OPEN\"}",
        "{\"status\":\"ANSWERED\"}",
        null);
    return InquiryDtos.Response.from(inquiry);
  }

  /** 작성자 본인이 문의를 종료합니다. 본인 문의가 아니면 찾을 수 없음으로 처리합니다. */
  @Transactional
  public InquiryDtos.Response close(Long memberId, Long inquiryId) {
    Inquiry inquiry =
        repository
            .findByIdAndMemberId(inquiryId, memberId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의 내역을 찾을 수 없습니다."));
    InquiryStatus previousStatus = inquiry.getStatus();
    inquiry.close();
    auditLogService.record(
        memberId,
        "INQUIRY_CLOSED",
        "INQUIRY",
        inquiry.getId(),
        "{\"status\":\"" + previousStatus + "\"}",
        "{\"status\":\"CLOSED\"}",
        null);
    return InquiryDtos.Response.from(inquiry);
  }

  private Inquiry get(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의 내역을 찾을 수 없습니다."));
  }
}

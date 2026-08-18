package com.movrit.movrit.admin.service;

import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.payment.service.PaymentService;
import com.movrit.movrit.preorder.dto.PreorderDtos;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPreorderService {

    private final MemberService memberService;
    private final PreorderRepository preorderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public List<PreorderDtos.Response> reviewQueue(Long adminId) {
        memberService.getAdmin(adminId);
        return byStatus(PreorderStatus.REVIEW);
    }

    public List<PreorderDtos.Response> cancellationQueue(Long adminId) {
        memberService.getAdmin(adminId);
        return byStatus(PreorderStatus.CANCEL_REQUESTED);
    }

    @Transactional
    public PreorderDtos.Response approveReview(Long adminId, Long preorderId) {
        memberService.getAdmin(adminId);
        Preorder preorder = get(preorderId);
        transit(preorder::approveReview);
        notifySeller(preorder, "PREORDER_REVIEW_APPROVED", "프리오더 검토 승인", preorder.getTitle() + " 검토가 승인됐어.");
        return PreorderDtos.Response.from(preorder);
    }

    @Transactional
    public PreorderDtos.Response requestChanges(Long adminId, Long preorderId) {
        memberService.getAdmin(adminId);
        Preorder preorder = get(preorderId);
        transit(preorder::requestChanges);
        notifySeller(preorder, "PREORDER_REVIEW_CHANGES", "프리오더 수정 요청", preorder.getTitle() + " 검토에서 수정 요청이 왔어.");
        return PreorderDtos.Response.from(preorder);
    }

    @Transactional
    public PreorderDtos.Response approveCancellation(Long adminId, Long preorderId) {
        memberService.getAdmin(adminId);
        Preorder preorder = get(preorderId);
        transit(preorder::approveCancellation);

        paymentRepository.findAllByOrderPreorderId(preorderId).forEach(payment -> {
            Long buyerId = payment.getOrder().getMember().getId();
            paymentService.refundForFailedPreorder(payment);
            notificationService.send(
                    buyerId,
                    "PREORDER_CANCEL_REFUND",
                    "프리오더 취소·환불 안내",
                    preorder.getTitle() + " 프리오더 취소가 승인되어 결제가 환불 처리됐어.",
                    "PREORDER",
                    preorderId
            );
        });
        notifySeller(preorder, "PREORDER_CANCEL_APPROVED", "프리오더 취소 승인", preorder.getTitle() + " 취소 요청이 승인됐어.");
        return PreorderDtos.Response.from(preorder);
    }

    @Transactional
    public PreorderDtos.Response rejectCancellation(Long adminId, Long preorderId) {
        memberService.getAdmin(adminId);
        Preorder preorder = get(preorderId);
        transit(preorder::rejectCancellation);
        notifySeller(preorder, "PREORDER_CANCEL_REJECTED", "프리오더 취소 반려", preorder.getTitle() + " 취소 요청이 반려되어 판매가 계속돼.");
        return PreorderDtos.Response.from(preorder);
    }

    private List<PreorderDtos.Response> byStatus(PreorderStatus status) {
        return preorderRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null && p.getStatus() == status)
                .map(PreorderDtos.Response::from)
                .toList();
    }

    private Preorder get(Long id) {
        return preorderRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없어."));
    }

    private void notifySeller(Preorder preorder, String type, String title, String message) {
        notificationService.send(
                preorder.getProduct().getBrand().getSeller().getMember().getId(),
                type,
                title,
                message,
                "PREORDER",
                preorder.getId()
        );
    }

    private void transit(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}

package com.movrit.movrit.campaigncontent.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.entity.CampaignApplication;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.campaigncontent.dto.CampaignContentDtos;
import com.movrit.movrit.campaigncontent.entity.CampaignContentSubmission;
import com.movrit.movrit.campaigncontent.entity.ContentSubmissionStatus;
import com.movrit.movrit.campaigncontent.repository.CampaignContentSubmissionRepository;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignContentService {
    private final CampaignApplicationRepository applicationRepository;
    private final CampaignContentSubmissionRepository submissionRepository;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final MemberService memberService;

    @Transactional
    public CampaignContentDtos.Response submit(
            Long memberId,
            Long applicationId,
            CampaignContentDtos.SubmitRequest request
    ) {
        CampaignApplication application = applicationRepository
                .findByIdAndCreatorMemberId(applicationId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
        CampaignContentSubmission submission = submissionRepository.findByApplicationId(applicationId).orElse(null);

        if (submission == null) {
            transitionApplication(application, CampaignApplication::markContentSubmitted);
            submission = submissionRepository.save(CampaignContentSubmission.builder()
                    .application(application)
                    .contentUrl(request.contentUrl().trim())
                    .note(normalize(request.note()))
                    .status(ContentSubmissionStatus.SUBMITTED)
                    .revisionCount(0)
                    .submittedAt(LocalDateTime.now())
                    .build());
        } else {
            requireVersion(submission, request.expectedVersion());
            transitionSubmission(submission, value -> value.resubmit(request.contentUrl().trim(), normalize(request.note())));
            transitionApplication(application, CampaignApplication::markContentSubmitted);
        }

        Long sellerMemberId = sellerMemberId(application);
        notificationService.send(sellerMemberId, "CAMPAIGN_CONTENT", "캠페인 콘텐츠가 제출되었습니다.",
                application.getCreator().getNickname() + "님이 콘텐츠를 제출하셨습니다.", "CAMPAIGN_CONTENT", submission.getId());
        auditLogService.record(memberId, "CAMPAIGN_CONTENT_SUBMITTED", "CAMPAIGN_CONTENT", submission.getId(),
                null, "{\"status\":\"SUBMITTED\"}", null);
        return response(submission);
    }

    public List<CampaignContentDtos.Response> findMine(Long memberId) {
        return submissionRepository.findAllByApplicationCreatorMemberIdOrderByIdDesc(memberId)
                .stream().map(this::response).toList();
    }

    public List<CampaignContentDtos.Response> findSellerContents(Long memberId) {
        return submissionRepository
                .findAllByApplicationCampaignPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId)
                .stream().map(this::response).toList();
    }

    @Transactional
    public CampaignContentDtos.Response requestRevision(
            Long memberId,
            Long submissionId,
            CampaignContentDtos.ReviewRequest request
    ) {
        CampaignContentSubmission submission = getOwnedSubmission(memberId, submissionId);
        requireVersion(submission, request.expectedVersion());
        transitionSubmission(submission, value -> value.requestRevision(request.comment().trim()));
        transitionApplication(submission.getApplication(), CampaignApplication::requestContentRevision);
        notifyCreatorAndAudit(memberId, submission, "콘텐츠 수정 요청이 도착했습니다.",
                "CAMPAIGN_CONTENT_REVISION_REQUESTED", "REVISION_REQUESTED", request.comment().trim());
        return response(submission);
    }

    @Transactional
    public CampaignContentDtos.Response approve(
            Long memberId,
            Long submissionId,
            CampaignContentDtos.ApproveRequest request
    ) {
        CampaignContentSubmission submission = getOwnedSubmission(memberId, submissionId);
        requireVersion(submission, request.expectedVersion());
        transitionSubmission(submission, value -> value.approveByBrand(normalize(request.comment())));
        notifyCreatorAndAudit(memberId, submission, "브랜드 콘텐츠 검토가 완료되었습니다.",
                "CAMPAIGN_CONTENT_BRAND_APPROVED", "BRAND_APPROVED", normalize(request.comment()));
        return response(submission);
    }

    public List<CampaignContentDtos.Response> findOperatorContents(Long memberId) {
        memberService.getAdmin(memberId);
        return submissionRepository.findAllByOrderByIdDesc().stream().map(this::response).toList();
    }

    @Transactional
    public CampaignContentDtos.Response requestRevisionByOperator(
            Long memberId,
            Long submissionId,
            CampaignContentDtos.ReviewRequest request
    ) {
        memberService.getAdmin(memberId);
        CampaignContentSubmission submission = getSubmission(submissionId);
        requireVersion(submission, request.expectedVersion());
        transitionSubmission(submission, value -> value.requestRevisionByOperator(request.comment().trim()));
        transitionApplication(submission.getApplication(), CampaignApplication::requestContentRevision);
        notifyCreatorAndAudit(memberId, submission, "운영자 콘텐츠 수정 요청이 도착했습니다.",
                "CAMPAIGN_CONTENT_OPERATOR_REVISION_REQUESTED", "REVISION_REQUESTED", request.comment().trim());
        return response(submission);
    }

    @Transactional
    public CampaignContentDtos.Response approveByOperator(
            Long memberId,
            Long submissionId,
            CampaignContentDtos.ApproveRequest request
    ) {
        memberService.getAdmin(memberId);
        CampaignContentSubmission submission = getSubmission(submissionId);
        requireVersion(submission, request.expectedVersion());
        transitionSubmission(submission, value -> value.approveByOperator(normalize(request.comment())));
        transitionApplication(submission.getApplication(), CampaignApplication::completeContentReview);
        notifyCreatorAndAudit(memberId, submission, "콘텐츠 최종 검토가 완료되었습니다.",
                "CAMPAIGN_CONTENT_OPERATOR_APPROVED", "APPROVED", normalize(request.comment()));
        return response(submission);
    }

    private CampaignContentSubmission getOwnedSubmission(Long memberId, Long submissionId) {
        CampaignContentSubmission submission = getSubmission(submissionId);
        productService.getOwnedProduct(memberId,
                submission.getApplication().getCampaign().getPreorder().getProduct().getId());
        return submission;
    }

    private CampaignContentSubmission getSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "콘텐츠 제출 내역을 찾을 수 없습니다."));
    }

    private void notifyCreatorAndAudit(Long actorMemberId, CampaignContentSubmission submission, String title,
                                       String action, String status, String reason) {
        Long creatorMemberId = submission.getApplication().getCreator().getMember().getId();
        notificationService.send(creatorMemberId, "CAMPAIGN_CONTENT", title,
                submission.getApplication().getCampaign().getTitle(), "CAMPAIGN_CONTENT", submission.getId());
        auditLogService.record(actorMemberId, action, "CAMPAIGN_CONTENT", submission.getId(),
                "{\"status\":\"SUBMITTED\"}", "{\"status\":\"" + status + "\"}", reason);
    }

    private void transitionApplication(CampaignApplication application, Consumer<CampaignApplication> action) {
        try {
            action.accept(application);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    private void transitionSubmission(
            CampaignContentSubmission submission,
            Consumer<CampaignContentSubmission> action
    ) {
        try {
            action.accept(submission);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    private CampaignContentDtos.Response response(CampaignContentSubmission submission) {
        return new CampaignContentDtos.Response(
                submission.getId(),
                submission.getApplication().getId(),
                submission.getApplication().getCreator().getNickname(),
                submission.getApplication().getCampaign().getTitle(),
                submission.getContentUrl(),
                submission.getNote(),
                submission.getStatus(),
                submission.getRevisionCount(),
                submission.getVersion(),
                submission.getReviewComment(),
                submission.getSubmittedAt(),
                submission.getReviewedAt()
        );
    }

    private Long sellerMemberId(CampaignApplication application) {
        return application.getCampaign().getPreorder().getProduct().getBrand().getSeller().getMember().getId();
    }

    private void requireVersion(CampaignContentSubmission submission, Long expectedVersion) {
        if (!java.util.Objects.equals(submission.getVersion(), expectedVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "콘텐츠 제출 상태가 변경되었습니다. 다시 조회해 주세요.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

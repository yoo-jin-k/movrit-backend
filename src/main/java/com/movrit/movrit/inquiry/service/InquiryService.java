package com.movrit.movrit.inquiry.service;

import com.movrit.movrit.inquiry.dto.InquiryDtos;
import com.movrit.movrit.inquiry.entity.Inquiry;
import com.movrit.movrit.inquiry.entity.InquiryStatus;
import com.movrit.movrit.inquiry.repository.InquiryRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.preorder.entity.Preorder;
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
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;
    private final PreorderRepository preorderRepository;
    private final NotificationService notificationService;

    @Transactional
    public InquiryDtos.Response create(Long memberId, InquiryDtos.CreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없어."));

        Preorder preorder = null;
        if (request.preorderId() != null) {
            preorder = preorderRepository.findById(request.preorderId())
                    .filter(item -> item.getDeletedAt() == null)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없어."));
        }

        Inquiry saved = inquiryRepository.save(
                Inquiry.builder()
                        .member(member)
                        .preorder(preorder)
                        .category(request.category().trim())
                        .title(request.title().trim())
                        .content(request.content().trim())
                        .status(InquiryStatus.OPEN)
                        .build()
        );
        return toResponse(saved);
    }

    public List<InquiryDtos.Response> findMine(Long memberId) {
        return inquiryRepository.findAllByMemberIdOrderByIdDesc(memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public InquiryDtos.Response findMineById(Long memberId, Long inquiryId) {
        return inquiryRepository.findByIdAndMemberId(inquiryId, memberId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없어."));
    }

    @Transactional
    public InquiryDtos.Response close(Long memberId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findByIdAndMemberId(inquiryId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없어."));
        inquiry.close();
        return toResponse(inquiry);
    }

    public List<InquiryDtos.Response> findForAdmin(Long adminMemberId, InquiryStatus status) {
        requireAdmin(adminMemberId);
        List<Inquiry> list = status == null
                ? inquiryRepository.findAllByOrderByIdDesc()
                : inquiryRepository.findAllByStatusOrderByIdDesc(status);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public InquiryDtos.Response answer(Long adminMemberId, Long inquiryId, InquiryDtos.AnswerRequest request) {
        Member admin = requireAdmin(adminMemberId);
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없어."));

        inquiry.answer(admin, request.answer().trim());
        notificationService.send(
                inquiry.getMember().getId(),
                "INQUIRY_ANSWERED",
                "문의 답변이 등록됐어",
                inquiry.getTitle() + " 문의에 답변이 등록됐어.",
                "INQUIRY",
                inquiry.getId()
        );
        return toResponse(inquiry);
    }

    private Member requireAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없어."));
        if (!member.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 사용할 수 있어.");
        }
        return member;
    }

    private InquiryDtos.Response toResponse(Inquiry inquiry) {
        return new InquiryDtos.Response(
                inquiry.getId(),
                inquiry.getMember().getId(),
                inquiry.getMember().getName(),
                inquiry.getPreorder() == null ? null : inquiry.getPreorder().getId(),
                inquiry.getPreorder() == null ? null : inquiry.getPreorder().getTitle(),
                inquiry.getCategory(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getAnswer(),
                inquiry.getAnsweredByMember() == null ? null : inquiry.getAnsweredByMember().getId(),
                inquiry.getAnsweredAt(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }
}

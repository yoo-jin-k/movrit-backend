package com.movrit.movrit.creator.service;

import com.movrit.movrit.creator.dto.CreatorDtos;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.entity.CreatorCampaign;
import com.movrit.movrit.creator.entity.CreatorCampaignStatus;
import com.movrit.movrit.creator.entity.CreatorStatus;
import com.movrit.movrit.creator.repository.CreatorCampaignRepository;
import com.movrit.movrit.creator.repository.CreatorRepository;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatorService {
    private final CreatorRepository creatorRepository;
    private final CreatorCampaignRepository campaignRepository;
    private final MemberService memberService;
    private final PreorderRepository preorderRepository;

    @Transactional
    public CreatorDtos.Response apply(Long memberId, CreatorDtos.ApplyRequest request) {
        if (creatorRepository.existsByMemberId(memberId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 인플루언서 신청 내역이 있어.");
        }

        Creator creator = Creator.builder()
                .member(memberService.getMember(memberId))
                .nickname(request.nickname().trim())
                .bio(trimToNull(request.bio()))
                .primaryCategory(trimToNull(request.primaryCategory()))
                .channelPlatform(request.channelPlatform().trim())
                .channelHandle(request.channelHandle().trim())
                .channelUrl(request.channelUrl().trim())
                .followerCount(request.followerCount())
                .status(CreatorStatus.PENDING)
                .build();

        return CreatorDtos.Response.from(creatorRepository.save(creator));
    }

    public CreatorDtos.Response findMine(Long memberId) {
        return CreatorDtos.Response.from(getByMemberId(memberId));
    }

    public List<CreatorDtos.Response> findAll(CreatorStatus status) {
        List<Creator> creators = status == null
                ? creatorRepository.findAll()
                : creatorRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return creators.stream().map(CreatorDtos.Response::from).toList();
    }

    public CreatorDtos.Response findById(Long id) {
        return CreatorDtos.Response.from(getCreator(id));
    }

    @Transactional
    public CreatorDtos.Response updateMine(Long memberId, CreatorDtos.UpdateProfileRequest request) {
        Creator creator = getByMemberId(memberId);
        creator.updateProfile(
                request.nickname().trim(),
                trimToNull(request.bio()),
                trimToNull(request.primaryCategory()),
                request.channelPlatform().trim(),
                request.channelHandle().trim(),
                request.channelUrl().trim(),
                request.followerCount()
        );
        return CreatorDtos.Response.from(creator);
    }

    @Transactional
    public CreatorDtos.Response approve(Long creatorId) {
        Creator creator = getCreator(creatorId);
        creator.approve();
        return CreatorDtos.Response.from(creator);
    }

    @Transactional
    public CreatorDtos.Response reject(Long creatorId, CreatorDtos.RejectRequest request) {
        Creator creator = getCreator(creatorId);
        creator.reject(request.reason().trim());
        return CreatorDtos.Response.from(creator);
    }

    @Transactional
    public CreatorDtos.CampaignResponse assignCampaign(Long creatorId, CreatorDtos.AssignCampaignRequest request) {
        Creator creator = getCreator(creatorId);
        if (creator.getStatus() != CreatorStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인된 인플루언서만 캠페인에 연결할 수 있어.");
        }
        if (campaignRepository.existsByCreatorIdAndPreorderId(creatorId, request.preorderId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 연결된 공동구매 캠페인이야.");
        }

        Preorder preorder = preorderRepository.findById(request.preorderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공동구매를 찾을 수 없어."));

        CreatorCampaign campaign = CreatorCampaign.builder()
                .creator(creator)
                .preorder(preorder)
                .status(CreatorCampaignStatus.PROPOSED)
                .commissionRate(request.commissionRate())
                .build();

        return CreatorDtos.CampaignResponse.from(campaignRepository.save(campaign));
    }

    @Transactional
    public CreatorDtos.CampaignResponse updateCampaignStatus(
            Long memberId,
            Long campaignId,
            CreatorDtos.CampaignStatusRequest request
    ) {
        Creator creator = getByMemberId(memberId);
        CreatorCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없어."));

        if (!campaign.getCreator().getId().equals(creator.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 캠페인만 변경할 수 있어.");
        }

        CreatorCampaignStatus next = request.status();
        if (next != CreatorCampaignStatus.ACCEPTED && next != CreatorCampaignStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인플루언서는 제안을 수락하거나 거절할 수 있어.");
        }

        campaign.changeStatus(next);
        return CreatorDtos.CampaignResponse.from(campaign);
    }

    public CreatorDtos.DashboardResponse dashboard(Long memberId) {
        Creator creator = getByMemberId(memberId);
        List<CreatorDtos.CampaignResponse> campaigns = campaignRepository
                .findAllByCreatorIdOrderByCreatedAtDesc(creator.getId())
                .stream()
                .map(CreatorDtos.CampaignResponse::from)
                .toList();

        long proposed = campaigns.stream().filter(it -> it.status() == CreatorCampaignStatus.PROPOSED).count();
        long active = campaigns.stream().filter(it ->
                it.status() == CreatorCampaignStatus.ACCEPTED || it.status() == CreatorCampaignStatus.ACTIVE
        ).count();
        long completed = campaigns.stream().filter(it -> it.status() == CreatorCampaignStatus.COMPLETED).count();
        long totalOrders = campaigns.stream().mapToLong(CreatorDtos.CampaignResponse::orderCount).sum();
        BigDecimal grossSales = campaigns.stream()
                .map(CreatorDtos.CampaignResponse::grossSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal estimatedEarnings = campaigns.stream()
                .map(CreatorDtos.CampaignResponse::estimatedEarnings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CreatorDtos.DashboardResponse(
                CreatorDtos.Response.from(creator),
                proposed,
                active,
                completed,
                totalOrders,
                grossSales,
                estimatedEarnings,
                campaigns
        );
    }

    private Creator getCreator(Long id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인플루언서를 찾을 수 없어."));
    }

    private Creator getByMemberId(Long memberId) {
        return creatorRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인플루언서 신청 내역을 찾을 수 없어."));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}

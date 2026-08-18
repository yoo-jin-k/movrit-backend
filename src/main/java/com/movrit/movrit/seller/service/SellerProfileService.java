package com.movrit.movrit.seller.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.entity.SellerStatus;
import com.movrit.movrit.seller.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final MemberService memberService;

    @Transactional
    public SellerDtos.Response create(Long memberId, SellerDtos.ApplyRequest request) {
        if (sellerProfileRepository.findByMemberId(memberId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 판매자 신청 내역이 있어.");
        }

        Member member = memberService.getMember(memberId);
        SellerProfile seller = SellerProfile.builder()
                .member(member)
                .businessName(request.businessName().trim())
                .brandName(request.brandName().trim())
                .businessNumber(request.businessNumber().trim())
                .representativeName(request.representativeName().trim())
                .status(SellerStatus.PENDING)
                .settlementBankCode(request.settlementBankCode())
                .settlementAccountMasked(request.settlementAccountMasked())
                .build();

        return SellerDtos.Response.from(sellerProfileRepository.save(seller));
    }

    public SellerDtos.Response findMine(Long memberId) {
        return SellerDtos.Response.from(sellerProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매자 신청 내역이 없어.")));
    }

    public List<SellerDtos.Response> findAllForAdmin(Long adminMemberId) {
        requireAdmin(adminMemberId);
        return sellerProfileRepository.findAll().stream().map(SellerDtos.Response::from).toList();
    }

    public List<SellerDtos.Response> findPendingForAdmin(Long adminMemberId) {
        requireAdmin(adminMemberId);
        return sellerProfileRepository.findAll().stream()
                .filter(seller -> seller.getStatus() == SellerStatus.PENDING)
                .map(SellerDtos.Response::from)
                .toList();
    }

    @Transactional
    public SellerDtos.Response approve(Long adminMemberId, Long sellerId) {
        requireAdmin(adminMemberId);
        SellerProfile seller = getSeller(sellerId);
        seller.approve();
        seller.getMember().changeRole(MemberRole.BRAND);
        return SellerDtos.Response.from(seller);
    }

    @Transactional
    public SellerDtos.Response reject(Long adminMemberId, Long sellerId) {
        requireAdmin(adminMemberId);
        SellerProfile seller = getSeller(sellerId);
        seller.reject();
        if (seller.getMember().getRole() == MemberRole.BRAND) {
            seller.getMember().changeRole(MemberRole.BUYER);
        }
        return SellerDtos.Response.from(seller);
    }

    public SellerProfile getSeller(Long id) {
        return sellerProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매자 정보를 찾을 수 없어."));
    }

    private void requireAdmin(Long memberId) {
        Member member = memberService.getMember(memberId);
        if (member.getRole() != MemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 처리할 수 있어.");
        }
    }
}

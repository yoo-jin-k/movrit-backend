package com.movrit.movrit.seller.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.entity.MemberRole;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.entity.SellerProfile;
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
    public SellerDtos.Response create(Long memberId, SellerDtos.CreateRequest request) {
        if (sellerProfileRepository.findByMemberId(memberId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 판매자 신청 내역이 있어.");
        }
        Member member = memberService.getMember(memberId);
        SellerProfile seller = SellerProfile.builder()
                .member(member)
                .businessName(request.businessName().trim())
                .brandName(request.brandName())
                .businessNumber(request.businessNumber())
                .representativeName(request.representativeName())
                .status("PENDING")
                .settlementBankCode(request.settlementBankCode())
                .settlementAccountMasked(request.settlementAccountMasked())
                .build();
        return SellerDtos.Response.from(sellerProfileRepository.save(seller));
    }

    public SellerDtos.Response findMine(Long memberId) {
        SellerProfile seller = sellerProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매자 신청 내역이 없어."));
        return SellerDtos.Response.from(seller);
    }

    public List<SellerDtos.Response> findAll() {
        return sellerProfileRepository.findAll().stream().map(SellerDtos.Response::from).toList();
    }

    public SellerDtos.Response findById(Long id) {
        return SellerDtos.Response.from(getSeller(id));
    }

    @Transactional
    public SellerDtos.Response update(Long id, SellerDtos.UpdateRequest request) {
        SellerProfile seller = getSeller(id);
        String status = request.status().trim().toUpperCase();
        if (!List.of("PENDING", "APPROVED", "REJECTED", "SUSPENDED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 판매자 상태야.");
        }
        seller.update(request.businessName().trim(), request.brandName(), request.businessNumber(),
                request.representativeName(), status, request.settlementBankCode(),
                request.settlementAccountMasked());
        if ("APPROVED".equals(status)) {
            seller.getMember().changeRole(MemberRole.BRAND);
        } else if (seller.getMember().getRole() == MemberRole.BRAND && ("REJECTED".equals(status) || "SUSPENDED".equals(status))) {
            seller.getMember().changeRole(MemberRole.BUYER);
        }
        return SellerDtos.Response.from(seller);
    }

    @Transactional
    public void delete(Long id) {
        SellerProfile seller = getSeller(id);
        if (seller.getMember().getRole() == MemberRole.BRAND) seller.getMember().changeRole(MemberRole.BUYER);
        sellerProfileRepository.delete(seller);
    }

    public SellerProfile getSeller(Long id) {
        return sellerProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매자 정보를 찾을 수 없어."));
    }
}

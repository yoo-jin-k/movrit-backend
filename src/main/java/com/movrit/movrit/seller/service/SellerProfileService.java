package com.movrit.movrit.seller.service;

import com.movrit.movrit.member.entity.Member;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final MemberService memberService;

    /**
     * 로그인한 일반 회원을 판매자 등록 심사 대상으로 만든다.
     * 신청 직후에는 상품을 등록할 수 없고 관리자 승인 후 APPROVED 상태가 되어야 한다.
     */
    @Transactional
    public SellerDtos.Response apply(Long memberId, SellerDtos.ApplyRequest request) {
        if (sellerProfileRepository.existsByMemberId(memberId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 판매자 등록 신청 정보가 있어.");
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

    /** 로그인한 회원 본인의 판매자 신청 상태를 조회한다. */
    public SellerDtos.Response findMine(Long memberId) {
        SellerProfile seller = sellerProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "판매자 등록 신청 정보가 없어."
                ));

        return SellerDtos.Response.from(seller);
    }

    /** 이후 상품 등록 서비스에서도 승인 여부를 검사할 때 사용할 수 있는 공통 조회 메서드다. */
    public SellerProfile getApprovedSeller(Long memberId) {
        SellerProfile seller = sellerProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "판매자 등록이 필요해."));

        if (seller.getStatus() != SellerStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인된 판매자만 이용할 수 있어.");
        }

        return seller;
    }
}

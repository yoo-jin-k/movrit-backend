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

import java.util.List;

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
        return SellerDtos.Response.from(getSellerByMemberId(memberId));
    }

    /** 관리자 화면에서 전체 신청 처리 이력을 확인할 때 사용한다. */
    public List<SellerDtos.Response> findAllForAdmin(Long adminMemberId) {
        memberService.getAdmin(adminMemberId);

        return sellerProfileRepository.findAll().stream()
                .map(SellerDtos.Response::from)
                .toList();
    }

    /** 관리자 화면에서 심사 대기 신청만 확인할 때 사용한다. */
    public List<SellerDtos.Response> findPendingForAdmin(Long adminMemberId) {
        memberService.getAdmin(adminMemberId);

        return sellerProfileRepository.findAll().stream()
                .filter(seller -> seller.getStatus() == SellerStatus.PENDING)
                .map(SellerDtos.Response::from)
                .toList();
    }

    /**
     * 화면에서 관리자 버튼을 숨기는 것만으로는 보안이 되지 않으므로,
     * 실제 승인/반려 시점에도 DB의 관리자 권한을 다시 확인한다.
     */
    @Transactional
    public SellerDtos.Response approve(Long adminMemberId, Long sellerId) {
        memberService.getAdmin(adminMemberId);
        SellerProfile seller = getSeller(sellerId);

        if (seller.getStatus() != SellerStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "심사 대기 중인 신청만 승인할 수 있어.");
        }

        seller.approve();
        return SellerDtos.Response.from(seller);
    }

    @Transactional
    public SellerDtos.Response reject(Long adminMemberId, Long sellerId) {
        memberService.getAdmin(adminMemberId);
        SellerProfile seller = getSeller(sellerId);

        if (seller.getStatus() != SellerStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "심사 대기 중인 신청만 반려할 수 있어.");
        }

        seller.reject();
        return SellerDtos.Response.from(seller);
    }

    public SellerProfile getSeller(Long sellerId) {
        return sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매자 정보를 찾을 수 없어."));
    }

    public SellerProfile getSellerByMemberId(Long memberId) {
        return sellerProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매자 등록 신청 정보를 찾을 수 없습니다."));
    }

    /** 이후 상품 등록 서비스에서 승인 여부를 검사할 때 사용할 공통 조회 메서드다. */
    public SellerProfile getApprovedSeller(Long memberId) {
        SellerProfile seller = getSellerByMemberId(memberId);

        if (seller.getStatus() != SellerStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인된 판매자만 이용할 수 있어.");
        }

        return seller;
    }
}

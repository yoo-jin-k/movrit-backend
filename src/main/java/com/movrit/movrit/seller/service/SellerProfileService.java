package com.movrit.movrit.seller.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.seller.dto.SellerDtos;
import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.entity.SellerStatus;
import com.movrit.movrit.seller.repository.SellerProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 업체(판매자) 등록 신청과 운영자 심사(승인·반려)를 처리하고, 다른 기능이 쓰는 "승인된 업체인지" 확인을 제공합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProfileService {

  private final SellerProfileRepository sellerProfileRepository;
  private final MemberService memberService;

  /** 로그인한 일반 회원을 판매자 등록 심사 대상으로 만듭니다. 신청 직후에는 상품을 등록할 수 없고 관리자 승인 후 APPROVED 상태가 되어야 합니다. */
  @Transactional
  public SellerDtos.Response apply(Long memberId, SellerDtos.ApplyRequest request) {
    if (sellerProfileRepository.existsByMemberId(memberId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 판매자 등록 신청 정보가 있습니다.");
    }

    Member member = memberService.getMember(memberId);

    SellerProfile seller =
        SellerProfile.builder()
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

  /** 로그인한 회원 본인의 판매자 신청 상태를 조회합니다. */
  public SellerDtos.Response findMine(Long memberId) {
    return SellerDtos.Response.from(getSellerByMemberId(memberId));
  }

  /** 관리자 화면에서 전체 신청 처리 이력을 확인할 때 사용합니다. */
  public List<SellerDtos.Response> findAllForAdmin(Long adminMemberId) {
    memberService.getAdmin(adminMemberId);

    return sellerProfileRepository.findAll().stream().map(SellerDtos.Response::from).toList();
  }

  /** 관리자 화면에서 심사 대기 신청만 확인할 때 사용합니다. */
  public List<SellerDtos.Response> findPendingForAdmin(Long adminMemberId) {
    memberService.getAdmin(adminMemberId);

    return sellerProfileRepository.findAll().stream()
        .filter(seller -> seller.getStatus() == SellerStatus.PENDING)
        .map(SellerDtos.Response::from)
        .toList();
  }

  /** 화면에서 관리자 버튼을 숨기는 것만으로는 보안이 되지 않으므로, 실제 승인/반려 시점에도 DB의 관리자 권한을 다시 확인합니다. */
  @Transactional
  public SellerDtos.Response approve(Long adminMemberId, Long sellerId) {
    memberService.getAdmin(adminMemberId);
    SellerProfile seller = getSeller(sellerId);

    if (seller.getStatus() != SellerStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "심사 대기 중인 신청만 승인할 수 있습니다.");
    }

    seller.approve();
    return SellerDtos.Response.from(seller);
  }

  /** 운영자가 심사 대기 중인 업체 신청을 반려합니다. 관리자 권한을 실제 DB 기준으로 다시 확인하며, 대기 상태가 아니면 409입니다. */
  @Transactional
  public SellerDtos.Response reject(Long adminMemberId, Long sellerId) {
    memberService.getAdmin(adminMemberId);
    SellerProfile seller = getSeller(sellerId);

    if (seller.getStatus() != SellerStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "심사 대기 중인 신청만 반려할 수 있습니다.");
    }

    seller.reject();
    return SellerDtos.Response.from(seller);
  }

  /** 업체 프로필을 ID로 찾습니다. 없으면 404입니다. */
  public SellerProfile getSeller(Long sellerId) {
    return sellerProfileRepository
        .findById(sellerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매자 정보를 찾을 수 없습니다."));
  }

  /** 회원 ID로 업체 프로필을 찾습니다. 없으면 404입니다. */
  public SellerProfile getSellerByMemberId(Long memberId) {
    return sellerProfileRepository
        .findByMemberId(memberId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매자 등록 신청 정보를 찾을 수 없습니다."));
  }

  /** 승인된 업체만 통과시키는 공통 확인입니다. 브랜드·캠페인 등 업체 기능은 이 메서드로 권한을 검사하며, 승인 상태가 아니면 403입니다. */
  public SellerProfile getApprovedSeller(Long memberId) {
    SellerProfile seller = getSellerByMemberId(memberId);

    if (seller.getStatus() != SellerStatus.APPROVED) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인된 판매자만 이용할 수 있습니다.");
    }

    return seller;
  }
}

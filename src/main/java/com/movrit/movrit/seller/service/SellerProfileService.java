package com.movrit.movrit.seller.service;

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
    public SellerDtos.Response create(SellerDtos.CreateRequest request) {
        SellerProfile seller = SellerProfile.builder()
                .member(memberService.getMember(request.memberId()))
                .businessName(request.businessName().trim())
                .brandName(request.brandName())
                .businessNumber(request.businessNumber())
                .representativeName(request.representativeName())
                .status(request.status().trim())
                .settlementBankCode(request.settlementBankCode())
                .settlementAccountMasked(request.settlementAccountMasked())
                .build();
        return SellerDtos.Response.from(sellerProfileRepository.save(seller));
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
        seller.update(request.businessName().trim(), request.brandName(), request.businessNumber(),
                request.representativeName(), request.status().trim(), request.settlementBankCode(),
                request.settlementAccountMasked());
        return SellerDtos.Response.from(seller);
    }

    @Transactional
    public void delete(Long id) {
        sellerProfileRepository.delete(getSeller(id));
    }

    public SellerProfile getSeller(Long id) {
        return sellerProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매자 정보를 찾을 수 없어."));
    }
}

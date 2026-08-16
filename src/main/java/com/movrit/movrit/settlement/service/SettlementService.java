package com.movrit.movrit.settlement.service;

import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.settlement.dto.SettlementDtos;
import com.movrit.movrit.settlement.entity.Settlement;
import com.movrit.movrit.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final PreorderService preorderService;

    @Transactional
    public SettlementDtos.Response create(SettlementDtos.CreateRequest request) {
        Settlement settlement = Settlement.builder()
                .preorder(preorderService.getPreorder(request.preorderId()))
                .settlementNumber(request.settlementNumber().trim())
                .status(request.status())
                .grossAmount(request.grossAmount())
                .refundAmount(request.refundAmount())
                .platformFeeAmount(request.platformFeeAmount())
                .netAmount(request.netAmount())
                .currency(request.currency().trim().toUpperCase())
                .calculatedAt(request.calculatedAt())
                .confirmedAt(request.confirmedAt())
                .paidAt(request.paidAt())
                .build();
        return SettlementDtos.Response.from(settlementRepository.save(settlement));
    }

    public List<SettlementDtos.Response> findAll() {
        return settlementRepository.findAll().stream().map(SettlementDtos.Response::from).toList();
    }

    public SettlementDtos.Response findById(Long id) {
        return SettlementDtos.Response.from(getSettlement(id));
    }

    @Transactional
    public SettlementDtos.Response update(Long id, SettlementDtos.UpdateRequest request) {
        Settlement settlement = getSettlement(id);
        settlement.update(request.status(), request.grossAmount(), request.refundAmount(), request.platformFeeAmount(),
                request.netAmount(), request.calculatedAt(), request.confirmedAt(), request.paidAt());
        return SettlementDtos.Response.from(settlement);
    }

    @Transactional
    public void delete(Long id) {
        settlementRepository.delete(getSettlement(id));
    }

    private Settlement getSettlement(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "정산 정보를 찾을 수 없어."));
    }
}

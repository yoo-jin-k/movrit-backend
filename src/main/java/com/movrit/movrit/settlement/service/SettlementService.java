package com.movrit.movrit.settlement.service;
import com.movrit.movrit.preorder.entity.*;
import com.movrit.movrit.preorder.service.PreorderService;
import com.movrit.movrit.product.service.ProductService;
import com.movrit.movrit.settlement.dto.SettlementDtos;
import com.movrit.movrit.settlement.entity.*;
import com.movrit.movrit.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final PreorderService preorderService;
    private final ProductService productService;

    @Transactional
    public SettlementDtos.Response create(Long memberId, SettlementDtos.CreateRequest request) {
        Preorder preorder = preorderService.getPreorder(request.preorderId());
        productService.getOwnedProduct(memberId, preorder.getProduct().getId());
        if (preorder.getStatus() != PreorderStatus.SUCCESS) throw new ResponseStatusException(HttpStatus.CONFLICT, "성공 확정된 프리오더만 정산할 수 있어.");
        if (settlementRepository.findByPreorderId(preorder.getId()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 정산이 생성된 프리오더야.");
        if (request.grossAmount().subtract(request.refundAmount()).subtract(request.platformFeeAmount()).compareTo(request.netAmount()) != 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "순정산액 계산이 맞지 않아.");
        Settlement s = Settlement.builder().preorder(preorder).settlementNumber(request.settlementNumber().trim())
                .status(SettlementStatus.PENDING).grossAmount(request.grossAmount()).refundAmount(request.refundAmount())
                .platformFeeAmount(request.platformFeeAmount()).netAmount(request.netAmount())
                .currency(request.currency().trim().toUpperCase()).calculatedAt(LocalDateTime.now()).build();
        return SettlementDtos.Response.from(settlementRepository.save(s));
    }

    public List<SettlementDtos.Response> findMine(Long memberId) { return settlementRepository.findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(memberId).stream().map(SettlementDtos.Response::from).toList(); }
    @Transactional public SettlementDtos.Response confirm(Long memberId, Long id) { Settlement s=owned(memberId,id); transit(s::confirm); return SettlementDtos.Response.from(s); }
    @Transactional public SettlementDtos.Response pay(Long memberId, Long id) { Settlement s=owned(memberId,id); transit(s::pay); return SettlementDtos.Response.from(s); }
    @Transactional public SettlementDtos.Response cancel(Long memberId, Long id) { Settlement s=owned(memberId,id); transit(s::cancel); return SettlementDtos.Response.from(s); }
    private Settlement owned(Long memberId, Long id) { Settlement s=settlementRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"정산 정보를 찾을 수 없어.")); productService.getOwnedProduct(memberId,s.getPreorder().getProduct().getId()); return s; }
    private void transit(Runnable action) { try { action.run(); } catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT,e.getMessage()); } }
}

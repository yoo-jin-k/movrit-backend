package com.movrit.movrit.preorder.service;

import com.movrit.movrit.preorder.dto.PreorderDtos;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreorderService {
    private final PreorderRepository preorderRepository;
    private final ProductService productService;

    @Transactional
    public PreorderDtos.Response create(Long memberId, PreorderDtos.CreateRequest request) {
        Product product = productService.getOwnedProduct(memberId, request.productId());
        validatePeriod(request.startAt(), request.endAt());
        validateQuantityRules(request.targetQuantity(), request.maxQuantity(),
                request.minOrderQuantity(), request.maxOrderQuantityPerMember());

        Preorder preorder = Preorder.builder()
                .product(product)
                .title(request.title().trim())
                .description(request.description())
                .preorderPrice(request.preorderPrice())
                .currency(request.currency().trim().toUpperCase())
                .targetQuantity(request.targetQuantity())
                .maxQuantity(request.maxQuantity())
                .orderedQuantity(0L)
                .minOrderQuantity(request.minOrderQuantity())
                .maxOrderQuantityPerMember(request.maxOrderQuantityPerMember())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .productionExpectedAt(request.productionExpectedAt())
                .shippingExpectedAt(request.shippingExpectedAt())
                .status(PreorderStatus.DRAFT)
                .build();

        return PreorderDtos.Response.from(preorderRepository.save(preorder));
    }

    public List<PreorderDtos.Response> findMine(Long memberId) {
        return preorderRepository.findAll().stream()
                .filter(preorder -> preorder.getDeletedAt() == null)
                .filter(preorder -> preorder.getProduct().getBrand().getSeller().getMember().getId().equals(memberId))
                .map(PreorderDtos.Response::from)
                .toList();
    }

    public List<PreorderDtos.Response> findOpen() {
        return preorderRepository.findAll().stream()
                .filter(preorder -> preorder.getDeletedAt() == null)
                .filter(preorder -> preorder.getStatus() == PreorderStatus.OPEN)
                .map(PreorderDtos.Response::from)
                .toList();
    }

    public PreorderDtos.Response findById(Long id) {
        return PreorderDtos.Response.from(getPreorder(id));
    }

    @Transactional
    public PreorderDtos.Response update(Long memberId, Long id, PreorderDtos.UpdateRequest request) {
        Preorder preorder = getOwnedPreorder(memberId, id);
        validatePeriod(request.startAt(), request.endAt());
        validateQuantityRules(request.targetQuantity(), request.maxQuantity(),
                request.minOrderQuantity(), request.maxOrderQuantityPerMember());
        try {
            preorder.update(request.title().trim(), request.description(), request.preorderPrice(),
                    request.currency().trim().toUpperCase(), request.targetQuantity(), request.maxQuantity(),
                    request.minOrderQuantity(), request.maxOrderQuantityPerMember(), request.startAt(), request.endAt(),
                    request.productionExpectedAt(), request.shippingExpectedAt());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return PreorderDtos.Response.from(preorder);
    }

    @Transactional
    public PreorderDtos.Response schedule(Long memberId, Long id) {
        return applyTransition(getOwnedPreorder(memberId, id), Preorder::schedule);
    }

    @Transactional
    public PreorderDtos.Response open(Long memberId, Long id) {
        Preorder preorder = getOwnedPreorder(memberId, id);
        return applyTransition(preorder, value -> value.open(LocalDateTime.now()));
    }

    @Transactional
    public PreorderDtos.Response close(Long memberId, Long id) {
        return applyTransition(getOwnedPreorder(memberId, id), Preorder::close);
    }

    @Transactional
    public PreorderDtos.Response finalizeResult(Long memberId, Long id) {
        return applyTransition(getOwnedPreorder(memberId, id), Preorder::finalizeResult);
    }

    @Transactional
    public PreorderDtos.Response cancel(Long memberId, Long id, String reason) {
        Preorder preorder = getOwnedPreorder(memberId, id);
        return applyTransition(preorder, value -> value.cancel(reason.trim()));
    }

    @Transactional
    public void delete(Long memberId, Long id) {
        Preorder preorder = getOwnedPreorder(memberId, id);
        try {
            preorder.softDelete();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    public Preorder getPreorder(Long id) {
        Preorder preorder = preorderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없어."));
        if (preorder.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없어.");
        }
        return preorder;
    }

    @Transactional
    public void addOrderQuantity(Preorder preorder, long quantity) {
        try {
            preorder.increaseOrderedQuantity(quantity);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @Transactional
    public void removeOrderQuantity(Preorder preorder, long quantity) {
        preorder.decreaseOrderedQuantity(quantity);
    }

    private Preorder getOwnedPreorder(Long memberId, Long id) {
        Preorder preorder = getPreorder(id);
        productService.getOwnedProduct(memberId, preorder.getProduct().getId());
        return preorder;
    }

    private PreorderDtos.Response applyTransition(Preorder preorder, java.util.function.Consumer<Preorder> transition) {
        try {
            transition.accept(preorder);
            return PreorderDtos.Response.from(preorder);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (!startAt.isBefore(endAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간보다 뒤여야 해.");
        }
    }

    private void validateQuantityRules(Long target, Long max, Long minOrder, Long maxPerMember) {
        if (max != null && max < target) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최대 판매 수량은 목표 수량보다 작을 수 없어.");
        }
        if (maxPerMember != null && maxPerMember < minOrder) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원별 최대 주문 수량은 최소 주문 수량보다 작을 수 없어.");
        }
    }
}

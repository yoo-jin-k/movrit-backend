package com.movrit.movrit.preorder.service;

import com.movrit.movrit.preorder.dto.PreorderDtos;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import com.movrit.movrit.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreorderService {
    private final PreorderRepository preorderRepository;
    private final ProductService productService;

    @Transactional
    public PreorderDtos.Response create(PreorderDtos.CreateRequest request) {
        Preorder preorder = Preorder.builder()
                .sellerId(request.sellerId())
                .product(productService.getProduct(request.productId()))
                .title(request.title().trim())
                .description(request.description())
                .preorderPrice(request.preorderPrice())
                .currency(request.currency().trim().toUpperCase())
                .targetQuantity(request.targetQuantity())
                .maxQuantity(request.maxQuantity())
                .orderedQuantity(0L)
                .startAt(request.startAt())
                .endAt(request.endAt())
                .productionExpectedAt(request.productionExpectedAt())
                .shippingExpectedAt(request.shippingExpectedAt())
                .status(request.status())
                .build();
        return PreorderDtos.Response.from(preorderRepository.save(preorder));
    }

    public List<PreorderDtos.Response> findAll() {
        return preorderRepository.findAll().stream()
                .filter(preorder -> preorder.getDeletedAt() == null)
                .map(PreorderDtos.Response::from)
                .toList();
    }

    public PreorderDtos.Response findById(Long id) {
        return PreorderDtos.Response.from(getPreorder(id));
    }

    @Transactional
    public PreorderDtos.Response update(Long id, PreorderDtos.UpdateRequest request) {
        Preorder preorder = getPreorder(id);
        preorder.update(request.title().trim(), request.description(), request.preorderPrice(),
                request.currency().trim().toUpperCase(), request.targetQuantity(), request.maxQuantity(),
                request.startAt(), request.endAt(), request.productionExpectedAt(), request.shippingExpectedAt(),
                request.status(), request.cancellationReason());
        return PreorderDtos.Response.from(preorder);
    }

    @Transactional
    public void delete(Long id) {
        getPreorder(id).softDelete();
    }

    public Preorder getPreorder(Long id) {
        Preorder preorder = preorderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없어."));
        if (preorder.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없어.");
        }
        return preorder;
    }
}

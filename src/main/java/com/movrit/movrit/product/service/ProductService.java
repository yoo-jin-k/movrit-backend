package com.movrit.movrit.product.service;

import com.movrit.movrit.product.dto.ProductDtos;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.repository.ProductRepository;
import com.movrit.movrit.seller.service.SellerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final SellerProfileService sellerProfileService;

    @Transactional
    public ProductDtos.Response create(ProductDtos.CreateRequest request) {
        sellerProfileService.getSeller(request.sellerId());

        Product product = Product.builder()
                .sellerId(request.sellerId())
                .name(request.name().trim())
                .description(request.description())
                .category(request.category())
                .normalPrice(request.normalPrice())
                .status(request.status())
                .build();
        return ProductDtos.Response.from(productRepository.save(product));
    }

    public List<ProductDtos.Response> findAll() {
        return productRepository.findAll().stream()
                .filter(product -> product.getDeletedAt() == null)
                .map(ProductDtos.Response::from)
                .toList();
    }

    public ProductDtos.Response findById(Long id) {
        return ProductDtos.Response.from(getProduct(id));
    }

    @Transactional
    public ProductDtos.Response update(Long id, ProductDtos.UpdateRequest request) {
        Product product = getProduct(id);
        product.update(request.name().trim(), request.description(), request.category(), request.normalPrice(), request.status());
        return ProductDtos.Response.from(product);
    }

    @Transactional
    public void delete(Long id) {
        getProduct(id).softDelete();
    }

    public Product getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없어."));
        if (product.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없어.");
        }
        return product;
    }
}

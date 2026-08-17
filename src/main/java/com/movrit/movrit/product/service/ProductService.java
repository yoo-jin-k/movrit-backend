package com.movrit.movrit.product.service;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.brand.entity.BrandStatus;
import com.movrit.movrit.brand.service.BrandService;
import com.movrit.movrit.common.file.LocalFileStorageService;
import com.movrit.movrit.product.dto.ProductDtos;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.entity.ProductVariant;
import com.movrit.movrit.product.repository.ProductRepository;
import com.movrit.movrit.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BrandService brandService;
    private final LocalFileStorageService fileStorageService;

    @Transactional
    public ProductDtos.Response create(Long memberId, ProductDtos.CreateRequest request) {
        Brand brand = brandService.getOwnedBrand(memberId, request.brandId());
        if (brand.getStatus() != BrandStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "활성화된 브랜드에만 상품을 등록할 수 있어.");
        }

        Product product = Product.builder()
                .brand(brand)
                .name(request.name().trim())
                .description(request.description())
                .category(request.category())
                .normalPrice(request.normalPrice())
                .status(request.status())
                .build();

        return ProductDtos.Response.from(productRepository.save(product));
    }

    public List<ProductDtos.Response> findMine(Long memberId) {
        return productRepository.findAllByBrandSellerMemberIdAndDeletedAtIsNullOrderByIdDesc(memberId).stream()
                .map(ProductDtos.Response::from)
                .toList();
    }

    public ProductDtos.Response findById(Long id) {
        return ProductDtos.Response.from(getProduct(id));
    }

    @Transactional
    public ProductDtos.Response update(Long memberId, Long id, ProductDtos.UpdateRequest request) {
        Product product = getOwnedProduct(memberId, id);
        product.update(request.name().trim(), request.description(), request.category(), request.normalPrice(), request.status());
        return ProductDtos.Response.from(product);
    }

    @Transactional
    public ProductDtos.Response uploadThumbnail(Long memberId, Long productId, MultipartFile file) {
        Product product = getOwnedProduct(memberId, productId);
        product.updateThumbnail(fileStorageService.storeImage(file, "products/" + productId + "/thumbnail"));
        return ProductDtos.Response.from(product);
    }

    /**
     * 재고는 상품 자체가 아니라 판매 가능한 SKU(옵션 조합) 단위로 생성한다.
     * 옵션이 없는 상품도 'DEFAULT-...' 같은 SKU 한 개를 만들면 동일한 재고 로직을 사용할 수 있다.
     */
    @Transactional
    public ProductDtos.VariantResponse createVariant(Long memberId, Long productId, ProductDtos.VariantCreateRequest request) {
        Product product = getOwnedProduct(memberId, productId);
        String skuCode = request.skuCode().trim().toUpperCase();

        if (productVariantRepository.existsBySkuCode(skuCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 SKU 코드야.");
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .skuCode(skuCode)
                .optionName1(blankToNull(request.optionName1()))
                .optionValue1(blankToNull(request.optionValue1()))
                .optionName2(blankToNull(request.optionName2()))
                .optionValue2(blankToNull(request.optionValue2()))
                .additionalPrice(request.additionalPrice() == null ? BigDecimal.ZERO : request.additionalPrice())
                .onHandQuantity(request.initialStock())
                .reservedQuantity(0L)
                .safetyStockQuantity(request.safetyStock())
                .soldQuantity(0L)
                .active(true)
                .build();

        return ProductDtos.VariantResponse.from(productVariantRepository.save(variant));
    }

    public List<ProductDtos.VariantResponse> findVariants(Long productId) {
        getProduct(productId);
        return productVariantRepository.findAllByProductIdOrderByIdAsc(productId).stream()
                .map(ProductDtos.VariantResponse::from)
                .toList();
    }

    @Transactional
    public ProductDtos.VariantResponse restock(Long memberId, Long variantId, long quantity) {
        ProductVariant variant = getOwnedVariant(memberId, variantId);
        try {
            variant.restock(quantity);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return ProductDtos.VariantResponse.from(variant);
    }

    @Transactional
    public ProductDtos.VariantResponse updateSafetyStock(Long memberId, Long variantId, long quantity) {
        ProductVariant variant = getOwnedVariant(memberId, variantId);
        try {
            variant.adjustSafetyStock(quantity);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return ProductDtos.VariantResponse.from(variant);
    }

    @Transactional
    public ProductDtos.VariantResponse updateVariant(Long memberId, Long variantId, ProductDtos.VariantUpdateRequest request) {
        ProductVariant variant = getOwnedVariant(memberId, variantId);
        variant.updateOption(blankToNull(request.optionName1()), blankToNull(request.optionValue1()),
                blankToNull(request.optionName2()), blankToNull(request.optionValue2()),
                request.additionalPrice(), request.active());
        return ProductDtos.VariantResponse.from(variant);
    }

    @Transactional
    public ProductDtos.VariantResponse deactivateVariant(Long memberId, Long variantId) {
        ProductVariant variant = getOwnedVariant(memberId, variantId);
        try {
            variant.deactivate();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return ProductDtos.VariantResponse.from(variant);
    }

    public ProductVariant getVariant(Long variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 옵션을 찾을 수 없어."));
    }

    @Transactional
    public void delete(Long memberId, Long id) {
        getOwnedProduct(memberId, id).softDelete();
    }

    public Product getOwnedProduct(Long memberId, Long productId) {
        Product product = getProduct(productId);
        brandService.getOwnedBrand(memberId, product.getBrand().getId());
        return product;
    }

    public Product getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없어."));
        if (product.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없어.");
        }
        return product;
    }

    private ProductVariant getOwnedVariant(Long memberId, Long variantId) {
        ProductVariant variant = getVariant(variantId);
        getOwnedProduct(memberId, variant.getProduct().getId());
        return variant;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.movrit.movrit.brand.service;

import com.movrit.movrit.brand.dto.BrandDtos;
import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.brand.entity.BrandStatus;
import com.movrit.movrit.brand.repository.BrandRepository;
import com.movrit.movrit.common.file.LocalFileStorageService;
import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.service.SellerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;
    private final SellerProfileService sellerProfileService;
    private final LocalFileStorageService fileStorageService;

    @Transactional
    public BrandDtos.Response create(Long memberId, BrandDtos.CreateRequest request) {
        SellerProfile seller = sellerProfileService.getApprovedSeller(memberId);
        String name = request.name().trim();

        if (brandRepository.existsBySellerIdAndNameIgnoreCaseAndDeletedAtIsNull(seller.getId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 같은 이름의 브랜드를 운영하고 있어.");
        }

        Brand brand = Brand.builder()
                .seller(seller)
                .name(name)
                .description(request.description())
                .status(BrandStatus.ACTIVE)
                .build();

        return BrandDtos.Response.from(brandRepository.save(brand));
    }

    public List<BrandDtos.Response> findMine(Long memberId) {
        SellerProfile seller = sellerProfileService.getApprovedSeller(memberId);
        return brandRepository.findAllBySellerIdAndDeletedAtIsNull(seller.getId()).stream()
                .map(BrandDtos.Response::from)
                .toList();
    }

    @Transactional
    public BrandDtos.Response update(Long memberId, Long brandId, BrandDtos.UpdateRequest request) {
        Brand brand = getOwnedBrand(memberId, brandId);
        String name = request.name().trim();
        if (brandRepository.existsBySellerIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
                brand.getSeller().getId(), name, brandId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 같은 이름의 브랜드를 운영하고 있어.");
        }
        brand.update(name, request.description());
        return BrandDtos.Response.from(brand);
    }

    @Transactional
    public BrandDtos.Response uploadLogo(Long memberId, Long brandId, MultipartFile file) {
        Brand brand = getOwnedBrand(memberId, brandId);
        brand.updateLogo(fileStorageService.storeImage(file, "brands/" + brandId + "/logo"));
        return BrandDtos.Response.from(brand);
    }

    @Transactional
    public BrandDtos.Response uploadCover(Long memberId, Long brandId, MultipartFile file) {
        Brand brand = getOwnedBrand(memberId, brandId);
        brand.updateCover(fileStorageService.storeImage(file, "brands/" + brandId + "/cover"));
        return BrandDtos.Response.from(brand);
    }

    @Transactional
    public BrandDtos.Response changeStatus(Long memberId, Long brandId, BrandStatus status) {
        Brand brand = getOwnedBrand(memberId, brandId);
        if (status == BrandStatus.ACTIVE) brand.activate();
        else brand.deactivate();
        return BrandDtos.Response.from(brand);
    }

    public Brand getOwnedBrand(Long memberId, Long brandId) {
        SellerProfile seller = sellerProfileService.getApprovedSeller(memberId);
        Brand brand = getBrand(brandId);
        if (!brand.getSeller().getId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 소유한 브랜드만 관리할 수 있습니다.");
        }
        return brand;
    }

    @Transactional
    public void delete(Long memberId, Long brandId) {
        Brand brand = getOwnedBrand(memberId, brandId);
        brand.softDelete();
    }

    public Brand getBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "브랜드를 찾을 수 없어."));
        if (brand.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "브랜드를 찾을 수 없어.");
        }
        return brand;
    }
}

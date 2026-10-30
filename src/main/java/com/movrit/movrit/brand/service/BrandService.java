package com.movrit.movrit.brand.service;

import com.movrit.movrit.brand.dto.BrandDtos;
import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.brand.entity.BrandStatus;
import com.movrit.movrit.brand.repository.BrandRepository;
import com.movrit.movrit.common.file.LocalFileStorageService;
import com.movrit.movrit.seller.entity.SellerProfile;
import com.movrit.movrit.seller.service.SellerProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 업체의 브랜드를 만들고 수정·삭제합니다. 모든 작업은 승인된 업체가 본인 소유 브랜드에 대해서만 할 수 있습니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

  private final BrandRepository brandRepository;
  private final SellerProfileService sellerProfileService;
  private final LocalFileStorageService fileStorageService;

  /** 승인된 업체가 새 브랜드를 만듭니다. 같은 업체 안에 같은 이름(대소문자 무시)의 브랜드가 있으면 409로 거절합니다. */
  @Transactional
  public BrandDtos.Response create(Long memberId, BrandDtos.CreateRequest request) {
    SellerProfile seller = sellerProfileService.getApprovedSeller(memberId);
    String name = request.name().trim();

    if (brandRepository.existsBySellerIdAndNameIgnoreCaseAndDeletedAtIsNull(seller.getId(), name)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 같은 이름의 브랜드를 운영하고 있습니다.");
    }

    Brand brand =
        Brand.builder()
            .seller(seller)
            .name(name)
            .description(request.description())
            .status(BrandStatus.ACTIVE)
            .build();

    return BrandDtos.Response.from(brandRepository.save(brand));
  }

  /** 승인된 업체 본인의 삭제되지 않은 브랜드 목록을 반환합니다. */
  public List<BrandDtos.Response> findMine(Long memberId) {
    SellerProfile seller = sellerProfileService.getApprovedSeller(memberId);
    return brandRepository.findAllBySellerIdAndDeletedAtIsNull(seller.getId()).stream()
        .map(BrandDtos.Response::from)
        .toList();
  }

  /** 승인된 업체 본인의 삭제되지 않은 브랜드 엔티티 목록입니다. 새 캠페인의 운영 브랜드를 정할 때 사용합니다. */
  public List<Brand> findOwnedBrands(Long memberId) {
    SellerProfile seller = sellerProfileService.getApprovedSeller(memberId);
    return brandRepository.findAllBySellerIdAndDeletedAtIsNull(seller.getId());
  }

  /** 본인 브랜드의 이름·소개를 수정합니다. 다른 브랜드와 이름이 겹치면 409로 거절합니다. */
  @Transactional
  public BrandDtos.Response update(Long memberId, Long brandId, BrandDtos.UpdateRequest request) {
    Brand brand = getOwnedBrand(memberId, brandId);
    String name = request.name().trim();
    if (brandRepository.existsBySellerIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
        brand.getSeller().getId(), name, brandId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 같은 이름의 브랜드를 운영하고 있습니다.");
    }
    brand.update(name, request.description());
    return BrandDtos.Response.from(brand);
  }

  /** 본인 브랜드의 로고 이미지를 올리고 주소를 저장합니다. */
  @Transactional
  public BrandDtos.Response uploadLogo(Long memberId, Long brandId, MultipartFile file) {
    Brand brand = getOwnedBrand(memberId, brandId);
    brand.updateLogo(fileStorageService.storeImage(file, "brands/" + brandId + "/logo"));
    return BrandDtos.Response.from(brand);
  }

  /** 본인 브랜드의 커버 이미지를 올리고 주소를 저장합니다. */
  @Transactional
  public BrandDtos.Response uploadCover(Long memberId, Long brandId, MultipartFile file) {
    Brand brand = getOwnedBrand(memberId, brandId);
    brand.updateCover(fileStorageService.storeImage(file, "brands/" + brandId + "/cover"));
    return BrandDtos.Response.from(brand);
  }

  /** 본인 브랜드를 사용 중(ACTIVE) 또는 사용 중지로 바꿉니다. */
  @Transactional
  public BrandDtos.Response changeStatus(Long memberId, Long brandId, BrandStatus status) {
    Brand brand = getOwnedBrand(memberId, brandId);
    if (status == BrandStatus.ACTIVE) brand.activate();
    else brand.deactivate();
    return BrandDtos.Response.from(brand);
  }

  /** 승인된 업체가 소유한 브랜드인지 확인하고 반환합니다. 다른 업체 브랜드면 403입니다. 브랜드를 다루는 다른 기능이 권한 확인용으로 사용합니다. */
  public Brand getOwnedBrand(Long memberId, Long brandId) {
    SellerProfile seller = sellerProfileService.getApprovedSeller(memberId);
    Brand brand = getBrand(brandId);
    if (!brand.getSeller().getId().equals(seller.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 소유한 브랜드만 관리할 수 있습니다.");
    }
    return brand;
  }

  /** 본인 브랜드를 삭제 처리합니다(목록에서 숨김). 기존 캠페인 기록은 지우지 않습니다. */
  @Transactional
  public void delete(Long memberId, Long brandId) {
    Brand brand = getOwnedBrand(memberId, brandId);
    brand.softDelete();
  }

  /** 브랜드를 ID로 찾습니다. 없거나 삭제된 브랜드면 404입니다. */
  public Brand getBrand(Long brandId) {
    Brand brand =
        brandRepository
            .findById(brandId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    if (brand.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "브랜드를 찾을 수 없습니다.");
    }
    return brand;
  }
}

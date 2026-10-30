package com.movrit.movrit.collaboration.service;

import com.movrit.movrit.brand.entity.Brand;
import com.movrit.movrit.brand.service.BrandService;
import com.movrit.movrit.collaboration.entity.Campaign;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 캠페인을 운영하는 브랜드의 주인인지 확인하는 공통 기능입니다.
 *
 * <p>캠페인은 브랜드에 직접 속합니다(프리오더 연결은 선택). 업체 화면의 수정·선정·검수 기능은 모두 이 확인을 거쳐, 승인된 판매자가 자기 브랜드의 캠페인만 다루도록
 * 합니다.
 */
@Service
@RequiredArgsConstructor
public class CampaignOwnershipService {

  private final BrandService brandService;

  /**
   * 요청한 회원이 캠페인 운영 브랜드의 주인인지 확인합니다.
   *
   * @throws ResponseStatusException 승인된 판매자가 아니거나 다른 판매자의 브랜드이면 거절합니다
   */
  public Brand requireOwner(Long memberId, Campaign campaign) {
    return brandService.getOwnedBrand(memberId, campaign.getBrand().getId());
  }

  /**
   * 새 캠페인을 운영할 브랜드를 정합니다.
   *
   * <p>brandId를 보내면 그 브랜드가 본인 소유인지 확인합니다. 보내지 않았고 운영 중인 브랜드가 하나뿐이면 그 브랜드를 씁니다. 브랜드가 여러 개이면 선택을
   * 요청하고, 하나도 없으면 브랜드부터 등록하도록 안내합니다.
   */
  public Brand resolveBrandForNewCampaign(Long memberId, Long brandId) {
    if (brandId != null) return brandService.getOwnedBrand(memberId, brandId);
    List<Brand> brands = brandService.findOwnedBrands(memberId);
    if (brands.size() == 1) return brands.get(0);
    if (brands.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "캠페인을 운영할 브랜드가 없습니다. 브랜드를 먼저 등록해 주세요.");
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "캠페인을 운영할 브랜드를 선택해 주세요.");
  }
}

package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.collaboration.entity.Campaign;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 캠페인을 사용자에게 보여줘도 되는지 판단하는 공통 규칙입니다. 캠페인은 만들자마자 공개되며, 연결된 프리오더나 상품이 삭제되었으면 보이지 않습니다. open 값은 모집 가능
 * 여부만 뜻합니다.
 */
/** 현재 캠페인은 별도 공개 상태 없이 생성 즉시 공개되며 open은 모집 가능 여부입니다. */
public final class CampaignPublicPolicy {
  private CampaignPublicPolicy() {}

  /** 연결된 프리오더와 상품이 모두 살아 있으면 공개 대상으로 봅니다. */
  public static boolean visible(Campaign campaign) {
    return campaign != null
        && campaign.getPreorder() != null
        && campaign.getPreorder().getDeletedAt() == null
        && campaign.getPreorder().getProduct() != null
        && campaign.getPreorder().getProduct().getDeletedAt() == null;
  }

  /** 공개 대상이 아니면 존재를 드러내지 않도록 '찾을 수 없음(404)'으로 응답합니다. */
  public static void requireVisible(Campaign campaign) {
    if (!visible(campaign))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다.");
  }

  /** 신청서의 참여 방식에 방문형(VISIT)이 들어 있는지 확인합니다. 방문형일 때만 방문 장소를 공개합니다. */
  public static boolean includesVisit(CampaignApplicationForm form) {
    return form != null
        && form.getAllowedFulfillmentTypesJson() != null
        && form.getAllowedFulfillmentTypesJson().contains("\"VISIT\"");
  }
}

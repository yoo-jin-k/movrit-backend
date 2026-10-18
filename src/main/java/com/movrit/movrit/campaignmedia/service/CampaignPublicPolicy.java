package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.collaboration.entity.Campaign;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 현재 캠페인은 별도 공개 상태 없이 생성 즉시 공개되며 open은 모집 가능 여부입니다. */
public final class CampaignPublicPolicy {
  private CampaignPublicPolicy() {}

  public static boolean visible(Campaign campaign) {
    return campaign != null
        && campaign.getPreorder() != null
        && campaign.getPreorder().getDeletedAt() == null
        && campaign.getPreorder().getProduct() != null
        && campaign.getPreorder().getProduct().getDeletedAt() == null;
  }

  public static void requireVisible(Campaign campaign) {
    if (!visible(campaign))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다.");
  }

  public static boolean includesVisit(CampaignApplicationForm form) {
    return form != null
        && form.getAllowedFulfillmentTypesJson() != null
        && form.getAllowedFulfillmentTypesJson().contains("\"VISIT\"");
  }
}

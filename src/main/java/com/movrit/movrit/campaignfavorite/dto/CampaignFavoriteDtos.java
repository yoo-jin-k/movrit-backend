package com.movrit.movrit.campaignfavorite.dto;

import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import java.time.LocalDateTime;

/** 캠페인 찜 API의 응답 형식을 모아 둡니다. */
public final class CampaignFavoriteDtos {
  private CampaignFavoriteDtos() {}

  /**
   * 찜 목록의 한 항목입니다.
   *
   * @param favoritedAt 찜한 시각입니다.
   * @param campaign 공개 목록과 같은 형식의 캠페인 정보입니다. 모집이 끝난 캠페인도 포함되며 open 값으로 구분합니다.
   */
  public record Item(LocalDateTime favoritedAt, CollaborationDtos.CampaignResponse campaign) {}
}

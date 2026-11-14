package com.movrit.movrit.campaignapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.entity.FulfillmentType;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.entity.CampaignType;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.collaboration.service.CampaignOwnershipService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** 신청서 참여 방식은 배송형·방문형 중 하나만 저장되는지 확인합니다. */
class CampaignApplicationFormFulfillmentTest {
  private final CampaignRepository campaigns = mock(CampaignRepository.class);
  private final CampaignApplicationFormRepository forms =
      mock(CampaignApplicationFormRepository.class);
  private final CampaignApplicationFormService service =
      new CampaignApplicationFormService(
          campaigns,
          forms,
          mock(CampaignOwnershipService.class),
          new CampaignApplicationJson(new ObjectMapper()));

  private void campaign(CampaignType type) {
    Campaign campaign = Campaign.builder().id(10L).campaignType(type).build();
    when(campaigns.findById(10L)).thenReturn(Optional.of(campaign));
    when(forms.findByCampaignId(10L)).thenReturn(Optional.empty());
    when(forms.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private static CampaignApplicationDtos.FormSaveRequest request(FulfillmentType... types) {
    return new CampaignApplicationDtos.FormSaveRequest(List.of(types), List.of(), List.of());
  }

  @Test
  void deliveryAndVisitTogetherAreRejectedBeforeSaving() {
    campaign(CampaignType.PRODUCT);

    assertThatThrownBy(
            () ->
                service.saveForm(1L, 10L, request(FulfillmentType.DELIVERY, FulfillmentType.VISIT)))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            error -> {
              assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(error.getReason()).contains("하나만");
            });
    verify(forms, never()).save(any(CampaignApplicationForm.class));
  }

  @Test
  void singleDeliveryOrVisitIsSaved() {
    campaign(CampaignType.LOCAL);

    var saved = service.saveForm(1L, 10L, request(FulfillmentType.VISIT));

    assertThat(saved.allowedFulfillmentTypes()).containsExactly(FulfillmentType.VISIT);
  }
}

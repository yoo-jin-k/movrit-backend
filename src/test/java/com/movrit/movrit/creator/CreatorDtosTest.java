package com.movrit.movrit.creator;

import com.movrit.movrit.creator.dto.CreatorDtos;
import com.movrit.movrit.creator.entity.CreatorCampaign;
import com.movrit.movrit.creator.entity.CreatorCampaignStatus;
import com.movrit.movrit.preorder.entity.Preorder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorDtosTest {

    @Test
    void campaignResponseCalculatesSalesAndEstimatedEarnings() {
        Preorder preorder = Preorder.builder()
                .id(10L)
                .title("테스트 공동구매")
                .preorderPrice(new BigDecimal("10000"))
                .orderedQuantity(3L)
                .currency("KRW")
                .build();

        CreatorCampaign campaign = CreatorCampaign.builder()
                .id(20L)
                .preorder(preorder)
                .status(CreatorCampaignStatus.ACTIVE)
                .commissionRate(new BigDecimal("10.00"))
                .build();

        CreatorDtos.CampaignResponse response = CreatorDtos.CampaignResponse.from(campaign);

        assertEquals(3L, response.orderCount());
        assertEquals(0, new BigDecimal("30000").compareTo(response.grossSales()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(response.estimatedEarnings()));
        assertEquals("KRW", response.currency());
    }
}

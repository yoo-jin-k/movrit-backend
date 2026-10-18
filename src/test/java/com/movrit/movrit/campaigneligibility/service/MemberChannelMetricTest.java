package com.movrit.movrit.campaigneligibility.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.movrit.movrit.campaigneligibility.entity.MemberChannelMetric;
import com.movrit.movrit.campaigneligibility.entity.MetricVerificationStatus;
import org.junit.jupiter.api.Test;

class MemberChannelMetricTest {
  @Test
  void editingMetricInvalidatesPreviousVerification() {
    MemberChannelMetric metric =
        MemberChannelMetric.builder()
            .value(500L)
            .verificationStatus(MetricVerificationStatus.VERIFIED)
            .build();

    metric.updateByMember(600L, null, null);

    assertThat(metric.getValue()).isEqualTo(600L);
    assertThat(metric.getVerificationStatus()).isEqualTo(MetricVerificationStatus.UNVERIFIED);
  }
}

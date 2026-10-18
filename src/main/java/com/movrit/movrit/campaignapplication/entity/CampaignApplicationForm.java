package com.movrit.movrit.campaignapplication.entity;

import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "campaign_application_form")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CampaignApplicationForm extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false, unique = true)
  private Campaign campaign;

  @Column(name = "allowed_fulfillment_types_json", nullable = false, columnDefinition = "TEXT")
  private String allowedFulfillmentTypesJson;

  @Column(name = "questions_json", nullable = false, columnDefinition = "TEXT")
  private String questionsJson;

  @Column(name = "agreements_json", nullable = false, columnDefinition = "TEXT")
  private String agreementsJson;

  @Column(nullable = false)
  private Long version;

  /** 신청서 정의를 바꾸면 버전을 올려 기존 신청이 동의한 조건을 추적합니다. */
  public void replace(String fulfillmentTypesJson, String questionsJson, String agreementsJson) {
    allowedFulfillmentTypesJson = fulfillmentTypesJson;
    this.questionsJson = questionsJson;
    this.agreementsJson = agreementsJson;
    version += 1;
  }
}

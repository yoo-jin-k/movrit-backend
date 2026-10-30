package com.movrit.movrit.campaignapplication.entity;

import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 캠페인마다 하나씩 두는 신청서 양식입니다.
 *
 * <p>허용 체험 방식, 신청 질문, 동의 문서를 JSON으로 보관하고, 양식이 바뀔 때마다 버전을 올려 신청자가 어떤 양식에 동의했는지 구분합니다.
 */
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

  /**
   * 신청서 양식 전체를 새 내용으로 바꾸고 버전을 1 올립니다.
   *
   * <p>이미 제출된 신청은 이전 버전 번호를 그대로 가지고 있으므로, 어떤 양식에 동의했는지 나중에도 확인할 수 있습니다.
   */
  public void replace(String fulfillmentTypesJson, String questionsJson, String agreementsJson) {
    allowedFulfillmentTypesJson = fulfillmentTypesJson;
    this.questionsJson = questionsJson;
    this.agreementsJson = agreementsJson;
    version += 1;
  }
}

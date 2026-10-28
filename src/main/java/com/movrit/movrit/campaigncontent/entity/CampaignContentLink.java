package com.movrit.movrit.campaigncontent.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 하나의 콘텐츠 제출에 연결된 채널별 공개 게시물 주소입니다. */
@Entity
@Table(name = "campaign_content_link")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CampaignContentLink extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "submission_id", nullable = false)
  private CampaignContentSubmission submission;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel_type", nullable = false, length = 30)
  private ContentChannel channelType;

  @Column(name = "content_url", nullable = false, length = 1000)
  private String contentUrl;
}

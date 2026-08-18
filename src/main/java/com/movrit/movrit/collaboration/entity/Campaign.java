package com.movrit.movrit.collaboration.entity;
import com.movrit.movrit.common.entity.BaseEntity; import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="collaboration_campaign") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class Campaign extends BaseEntity { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="preorder_id",nullable=false,unique=true) private Preorder preorder;
 @Column(nullable=false) private String title; @Column(columnDefinition="TEXT") private String description;
 @Column(name="category",length=100) private String category; @Column(name="min_follower_count",nullable=false) private Long minFollowerCount;
 @Column(name="recruitment_count",nullable=false) private Integer recruitmentCount; @Column(name="reward_description",columnDefinition="TEXT") private String rewardDescription;
 @Column(name="start_at",nullable=false) private LocalDateTime startAt; @Column(name="end_at",nullable=false) private LocalDateTime endAt; @Column(nullable=false) private boolean open;
 public void update(String t,String d,String c,Long f,Integer n,String r,LocalDateTime s,LocalDateTime e){if(!s.isBefore(e))throw new IllegalArgumentException("모집 종료일은 시작일보다 뒤여야 해.");title=t;description=d;category=c;minFollowerCount=f;recruitmentCount=n;rewardDescription=r;startAt=s;endAt=e;}
 public void close(){open=false;} }

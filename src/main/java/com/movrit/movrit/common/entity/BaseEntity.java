package com.movrit.movrit.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;

/** 모든 엔티티가 공통으로 가지는 생성 시각·수정 시각을 자동으로 채워 주는 부모 클래스입니다. */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /** 처음 저장될 때 생성 시각과 수정 시각을 현재 시각으로 기록합니다. */
  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  /** 값이 바뀌어 저장될 때 수정 시각을 현재 시각으로 갱신합니다. */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}

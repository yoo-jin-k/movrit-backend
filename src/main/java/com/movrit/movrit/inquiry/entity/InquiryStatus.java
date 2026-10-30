package com.movrit.movrit.inquiry.entity;

/** 1:1 문의의 처리 단계입니다. */
public enum InquiryStatus {
  /** 접수되어 답변을 기다리는 문의입니다. */
  OPEN,
  /** 운영자가 답변을 등록한 문의입니다. */
  ANSWERED,
  /** 처리가 끝나 더 이상 답변하지 않는 문의입니다. */
  CLOSED
}

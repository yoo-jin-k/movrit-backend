package com.movrit.movrit.product.entity;

/** 상품 판매 상태입니다. 준비 중, 판매 중, 품절, 판매 중지가 있습니다. */
public enum ProductStatus {
  DRAFT,
  ACTIVE,
  SOLD_OUT,
  INACTIVE
}

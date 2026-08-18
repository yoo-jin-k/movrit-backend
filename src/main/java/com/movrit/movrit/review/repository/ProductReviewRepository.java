package com.movrit.movrit.review.repository;

import com.movrit.movrit.review.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Optional<ProductReview> findByOrderId(Long orderId);
    List<ProductReview> findAllByProductIdOrderByIdDesc(Long productId);
    List<ProductReview> findAllByMemberIdOrderByIdDesc(Long memberId);
    List<ProductReview> findAllByProductBrandSellerMemberIdOrderByIdDesc(Long memberId);
}

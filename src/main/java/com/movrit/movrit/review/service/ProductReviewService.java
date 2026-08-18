package com.movrit.movrit.review.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.order.entity.Order;
import com.movrit.movrit.order.entity.OrderStatus;
import com.movrit.movrit.order.service.OrderService;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.review.dto.ProductReviewDtos;
import com.movrit.movrit.review.entity.ProductReview;
import com.movrit.movrit.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final OrderService orderService;
    private final MemberService memberService;

    @Transactional
    public ProductReviewDtos.Response create(Long memberId, ProductReviewDtos.CreateRequest request) {
        Order order = orderService.getOrder(request.orderId());
        if (!order.getMember().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 주문에만 리뷰를 작성할 수 있어.");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "배송 완료된 주문만 리뷰를 작성할 수 있어.");
        }
        if (reviewRepository.findByOrderId(order.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 주문에 리뷰를 작성했어.");
        }

        ProductReview.validateRating(request.rating());
        Member member = memberService.getMember(memberId);
        Product product = order.getPreorder().getProduct();
        ProductReview review = ProductReview.builder()
                .order(order)
                .member(member)
                .product(product)
                .rating(request.rating())
                .content(request.content().trim())
                .build();
        return ProductReviewDtos.Response.from(reviewRepository.save(review));
    }

    public List<ProductReviewDtos.Response> findByProduct(Long productId) {
        return reviewRepository.findAllByProductIdOrderByIdDesc(productId).stream()
                .map(ProductReviewDtos.Response::from)
                .toList();
    }

    public List<ProductReviewDtos.Response> findMine(Long memberId) {
        return reviewRepository.findAllByMemberIdOrderByIdDesc(memberId).stream()
                .map(ProductReviewDtos.Response::from)
                .toList();
    }

    public List<ProductReviewDtos.Response> findSellerReviews(Long memberId) {
        return reviewRepository.findAllByProductBrandSellerMemberIdOrderByIdDesc(memberId).stream()
                .map(ProductReviewDtos.Response::from)
                .toList();
    }

    @Transactional
    public ProductReviewDtos.Response update(Long memberId, Long reviewId, ProductReviewDtos.UpdateRequest request) {
        ProductReview review = get(reviewId);
        requireBuyer(memberId, review);
        try {
            review.update(request.rating(), request.content().trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return ProductReviewDtos.Response.from(review);
    }

    @Transactional
    public void delete(Long memberId, Long reviewId) {
        ProductReview review = get(reviewId);
        requireBuyer(memberId, review);
        reviewRepository.delete(review);
    }

    @Transactional
    public ProductReviewDtos.Response reply(Long memberId, Long reviewId, ProductReviewDtos.ReplyRequest request) {
        ProductReview review = get(reviewId);
        Long ownerId = review.getProduct().getBrand().getSeller().getMember().getId();
        if (!ownerId.equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 브랜드 상품 리뷰에만 답글을 달 수 있어.");
        }
        review.reply(request.reply().trim());
        return ProductReviewDtos.Response.from(review);
    }

    private ProductReview get(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없어."));
    }

    private void requireBuyer(Long memberId, ProductReview review) {
        if (!review.getMember().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 리뷰만 수정하거나 삭제할 수 있어.");
        }
    }
}

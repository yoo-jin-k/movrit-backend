package com.movrit.movrit.review;

import com.movrit.movrit.review.entity.ProductReview;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductReviewTest {

    @Test
    void ratingMustBeBetweenOneAndFive() {
        assertDoesNotThrow(() -> ProductReview.validateRating(1));
        assertDoesNotThrow(() -> ProductReview.validateRating(5));
        assertThrows(IllegalArgumentException.class, () -> ProductReview.validateRating(0));
        assertThrows(IllegalArgumentException.class, () -> ProductReview.validateRating(6));
        assertThrows(IllegalArgumentException.class, () -> ProductReview.validateRating(null));
    }

    @Test
    void reviewCanBeUpdatedAndSellerCanReply() {
        ProductReview review = ProductReview.builder()
                .rating(5)
                .content("좋아요")
                .build();

        review.update(4, "수정한 리뷰");
        review.reply("구매 고마워요!");

        assertEquals(4, review.getRating());
        assertEquals("수정한 리뷰", review.getContent());
        assertEquals("구매 고마워요!", review.getSellerReply());
        assertNotNull(review.getSellerRepliedAt());
    }
}

CREATE TABLE IF NOT EXISTS product_review (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating INTEGER NOT NULL,
    content TEXT NOT NULL,
    seller_reply TEXT,
    seller_replied_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_review_order UNIQUE (order_id),
    CONSTRAINT fk_product_review_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_product_review_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_product_review_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT ck_product_review_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_product_review_product_id ON product_review(product_id);
CREATE INDEX IF NOT EXISTS idx_product_review_member_id ON product_review(member_id);

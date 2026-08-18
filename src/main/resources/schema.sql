CREATE TABLE IF NOT EXISTS preorder_favorite (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    preorder_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_preorder_favorite_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_preorder_favorite_preorder FOREIGN KEY (preorder_id) REFERENCES preorder(id),
    CONSTRAINT uk_preorder_favorite_member_preorder UNIQUE (member_id, preorder_id)
);

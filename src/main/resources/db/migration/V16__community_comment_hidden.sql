-- 운영자가 커뮤니티 댓글을 지우지 않고 가렸다가 다시 보이게 할 수 있도록 가림 표시를 추가합니다.
-- 가린 댓글은 자리만 남기고 "운영 정책에 따라 운영자가 가린 댓글입니다."로 보여 줍니다.
-- 글은 V11부터 hidden 컬럼이 있으므로, 가린 시각만 글·댓글 모두에 기록합니다.
ALTER TABLE public.community_comment
    ADD COLUMN hidden boolean NOT NULL DEFAULT false,
    ADD COLUMN hidden_at timestamp(6) without time zone;

ALTER TABLE public.community_post
    ADD COLUMN hidden_at timestamp(6) without time zone;

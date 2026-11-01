-- SNS 채널을 '직접 등록'(주소·팔로워 수·프로필 캡처)할 수 있게 하고, 운영자 확인 상태를 기록합니다.
-- 로그인으로 연결한 채널(OAUTH)은 SNS가 직접 알려준 값이라 처음부터 '확인됨'입니다.
-- 직접 등록한 채널(MANUAL)은 '확인 대기'로 시작하고, 운영자가 캡처와 주소를 보고 확인·반려합니다.
ALTER TABLE public.member_channel_connection
    ADD COLUMN connection_method varchar(10) NOT NULL DEFAULT 'OAUTH',
    ADD COLUMN verification_status varchar(20) NOT NULL DEFAULT 'VERIFIED',
    ADD COLUMN evidence_key varchar(100),
    ADD COLUMN review_note varchar(500),
    ADD COLUMN reviewed_at timestamp(6) without time zone,
    ADD COLUMN reviewed_by_member_id bigint,
    ADD CONSTRAINT ck_member_channel_connection_method
        CHECK (connection_method IN ('OAUTH', 'MANUAL')),
    ADD CONSTRAINT ck_member_channel_connection_status
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    ADD CONSTRAINT fk_member_channel_connection_reviewer FOREIGN KEY (reviewed_by_member_id)
        REFERENCES public.member (id) ON DELETE SET NULL;

-- 운영자 '채널 확인 대기' 목록 조회용 인덱스입니다.
CREATE INDEX idx_member_channel_connection_review
    ON public.member_channel_connection (connection_method, verification_status);

-- 채팅 메시지에 사진·파일(PDF) 첨부를 붙일 수 있게 합니다. 첨부만 보낼 때는 본문이 빈 문자열입니다.
ALTER TABLE public.chat_message
    ADD COLUMN attachment_key varchar(100),
    ADD COLUMN attachment_name varchar(200),
    ADD COLUMN attachment_type varchar(100),
    ADD COLUMN attachment_size bigint;

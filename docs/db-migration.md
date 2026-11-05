# DB 변경 관리 (Flyway)

2026-10-31부터 DB 구조 변경은 **Flyway 버전 파일**로만 합니다. 서버가 시작될 때 아직 적용하지 않은 파일을 순서대로 자동 적용합니다.

## 현재 상태

| 버전 | 파일 | 내용 |
|---|---|---|
| 1 | `V1__baseline_schema.sql` | 2026-10-31 통합 DB의 public 스키마 전체(49개 테이블). 기존 DB는 이 파일을 건너뛰고 기준선으로만 기록 |
| 2 | `V2__widen_text_and_amount_columns.sql` | 두 DB가 다르게 좁혀 둔 컬럼을 넓은 쪽으로 통일(신청 배송지 300자, 사유 500자, 알림 1000자, 금액 numeric(38,2)) |
| 3 | `V3__campaign_owned_by_brand.sql` | 캠페인이 브랜드에 직접 속하도록 brand_id 추가(기존 캠페인은 프리오더의 브랜드로 채움), 프리오더 연결은 선택 항목으로 변경 |
| 4 | `V4__campaign_type_region_channel.sql` | 탐색 메뉴용 분류 추가: 캠페인 종류(제품·지역·기자단), 방문 지역, 콘텐츠 채널, 프리미어 여부. 방문 전용 신청 폼을 가진 기존 캠페인은 지역 캠페인으로, 방문 주소 앞부분으로 지역을 채움 |
| 5 | `V5__campaign_favorite.sql` | 캠페인 찜 테이블(campaign_favorite) 추가. 회원·캠페인 쌍은 한 번만 저장되며 회원·캠페인 삭제 시 함께 정리 |
| 6 | `V6__notice.sql` | 공지사항 테이블(notice) 추가. 운영자가 작성·게시하며, 게시된 공지만 사용자 공지 목록에 표시 |
| 7 | `V7__member_channel_connection.sql` | SNS 로그인으로 연결한 채널 기록(member_channel_connection). 접근 토큰은 저장하지 않고 계정 정보와 팔로워 수만 보관 |
| 8 | `V8__channel_connection_manual_verification.sql` | 채널 직접 등록(주소·팔로워 수·캡처)과 운영자 확인 상태. 로그인 연결은 처음부터 확인됨, 직접 등록은 확인 대기로 시작 |
| 9 | `V9__chat.sql` | 채팅방·참여자·메시지(chat_room, chat_participant, chat_message). 신청 한 건마다 참여자 ↔ 브랜드 방 하나 |
| 10 | `V10__password_reset_token.sql` | 비밀번호 재설정 토큰. 원문 대신 SHA-256 해시만 저장, 30분 만료, 한 번만 사용 |
| 11 | `V11__community.sql` | 커뮤니티 글·댓글. 삭제는 deleted_at, 운영자 가림은 hidden으로 구분 |
| 12 | `V12__chat_attachment.sql` | 채팅 메시지 첨부(사진·영상·PDF) 컬럼 |
| 13 | `V13__purchase_review.sql` | 구매평 캠페인: 구매 정보(campaign_purchase_review)와 참여자 구매 인증·환급 상태(campaign_purchase_proof) |
| 14 | `V14__participation_operations.sql` | 마감 알림 기록, 방문 예약, 패널티, 참여자 평가, 신고(content_report), 채팅 메시지 가림(hidden_at) |
| 15 | `V15__campaign_shipment.sql` | 배송형 캠페인 송장(campaign_shipment). 신청 한 건에 하나, 택배사·송장번호·발송 시각 |
| 16 | `V16__community_comment_hidden.sql` | 커뮤니티 댓글 가림 표시(hidden)와 글·댓글 가린 시각(hidden_at). 운영자가 지우지 않고 가렸다가 되돌릴 수 있음 |
| 17 | `V17__campaign_content_metric.sql` | 콘텐츠 성과 기록(campaign_content_metric). (제출 건, 게시물 주소, 날짜)마다 한 줄, 조회수·좋아요·댓글과 입력한 사람 |
| 18 | `V18__participation_changes.sql` | 참여 조정. 신청별 연장 마감(content_deadline_extended_until), 선정 방식 `FIRST_COME`, 패널티 종류 `CANCEL_LIMIT`, 마감 알림 단계 `XD3`·`XD1`, 취소 횟수(member_cancel_count), 협의 취소 요청과 증빙(campaign_cancel_request), 기간 연장 요청(campaign_extension_request), 업체 미선정 기록(campaign_unselected_record) |
| 19 | `V19__experience_extension.sql` | 기간 연장이 승인된 신청의 체험 종료(experience_extended_until). 비어 있으면 캠페인 체험 종료를 씀 |
| 20 | `V20__period_extension_log.sql` | 기간 연장 기록(campaign_period_extension). 참여자 요청 승인·업체 직접·운영자 직접 연장을 모두 한 줄씩 남겨 참여자 연장 횟수로 셈. 이미 승인된 요청은 옮겨 둠 |
| 21 | `V21__extension_limit.sql` | 연장 횟수 기준: 연장 기록에 penalty_id, 패널티 종류에 EXTENSION_LIMIT 추가, 패널티 유일 조건을 신청·종류당 1건으로 변경 |
| 22 | `V22__channel_bio_code.sql` | SNS 소개글 인증 코드(channel_bio_code: 회원·서비스별 코드, 24시간 유효, 확인 횟수·사용 시각), 연결 방법에 BIO_CODE 추가, 확인에 쓴 코드(verification_code) |
| 23 | `V23__phone_verification.sql` | 휴대폰 번호 인증(phone_verification_challenge: 번호·인증번호 해시·만료·사용·무효화·틀린 횟수), member.phone_verified_at, 인증된 번호 회원 1명만(부분 유일 인덱스) |
| 24 | `V24__campaign_admin_hidden.sql` | 운영자 숨김 표시(collaboration_campaign.admin_hidden_at): 숨긴 캠페인은 소유 업체·운영자·신청자만 상세를 볼 수 있음, 다시 공개하면 비움 |
| 25 | `V25__application_metric_snapshot.sql` | 신청 당시 방문자 수 사본(campaign_application.eligibility_metric_value·eligibility_metric_measured_at): 블로그 일평균 방문자 조건이 있는 캠페인에 신청할 때 확인한 값을 남겨, 업체는 이 값만 보고 참여자는 신청 뒤 위젯을 다시 꺼도 됨 |

- 적용 이력 테이블: `movrit_schema_history`
  - 8월 실험 때 남은 `flyway_schema_history` 와는 별개입니다. 기존 표는 사용하지 않으며 지우지 않았습니다.
- 서버는 시작 시 엔티티와 DB 구조가 맞는지 검사합니다(`ddl-auto=validate`). 맞지 않으면 시작하지 않습니다.
- 적용 상태 확인: `select version, description, success from movrit_schema_history order by installed_rank;`

## 새 변경을 추가하는 방법

1. `src/main/resources/db/migration/V{다음번호}__{영문_설명}.sql` 파일을 만듭니다. 예: `V3__campaign_owned_by_brand.sql`
2. 파일 맨 위에 무엇을 왜 바꾸는지 존댓말 주석으로 적습니다.
3. 기존 데이터를 보존하는 순서로 작성합니다.
   - 컬럼 추가 → 기존 행 채우기 → 제약 추가 순서로 씁니다.
   - 컬럼 삭제나 길이 축소는 별도 승인 후에만 합니다.
4. 엔티티(`@Column`)도 같은 내용으로 맞춥니다.
5. `./gradlew test` 를 실행합니다. 이때 기본 DB `movrit` 에 적용되고 검증됩니다.
6. `scripts/integration.sh restart` 로 통합 DB `movrit_integration` 에도 적용합니다.

**이미 적용된 V 파일은 절대 수정하지 않습니다.** 내용을 고쳐야 하면 새 번호 파일로 추가합니다. 적용된 파일을 수정하면 체크섬 검사로 서버가 시작되지 않습니다.

## 백업

구조 변경 전에는 백업을 남깁니다.

```bash
pg_dump -h localhost -U movrit_user -d movrit_integration -Fc \
  -f ~/Documents/workspace/movrit-db-backups/movrit_integration_$(date +%Y%m%d_%H%M%S).dump
```

## docs/sql 폴더의 기존 파일

- `feature-*.sql`: Flyway 도입 전에 손으로 적용하던 변경 이력입니다.
  - 내용은 V1 기준 스키마에 모두 포함되어 있으므로 **다시 실행하지 않습니다.**
- `seed-*.sql`: 통합 검수용 합성 데이터입니다.
  - 필요할 때만 수동으로 실행하며 Flyway 대상이 아닙니다.
- `align-default-db-to-baseline-2026-10-31.sql`: 기본 DB를 V1 기준에 맞춘 1회성 정렬 기록입니다(이미 적용 완료).

## ERD

`docs/erd/` 에 실제 스키마로 생성한 ERD가 있습니다. 구조를 바꾼 뒤에는 다시 생성합니다.

```bash
pg_dump -h localhost -U movrit_user -d movrit_integration --schema-only --schema=public --no-owner \
  --no-privileges --exclude-table=flyway_schema_history --exclude-table=movrit_schema_history > /tmp/schema.sql
python3 scripts/generate-erd.py /tmp/schema.sql docs/erd
```

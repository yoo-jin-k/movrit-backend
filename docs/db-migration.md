# DB 변경 관리 (Flyway)

2026-10-31부터 DB 구조 변경은 **Flyway 버전 파일**로만 합니다. 서버가 시작될 때 아직 적용하지 않은 파일을 순서대로 자동 적용합니다.

## 현재 상태

| 버전 | 파일 | 내용 |
|---|---|---|
| 1 | `V1__baseline_schema.sql` | 2026-10-31 통합 DB의 public 스키마 전체(49개 테이블). 기존 DB는 이 파일을 건너뛰고 기준선으로만 기록 |
| 2 | `V2__widen_text_and_amount_columns.sql` | 두 DB가 다르게 좁혀 둔 컬럼을 넓은 쪽으로 통일(신청 배송지 300자, 사유 500자, 알림 1000자, 금액 numeric(38,2)) |
| 3 | `V3__campaign_owned_by_brand.sql` | 캠페인이 브랜드에 직접 속하도록 brand_id 추가(기존 캠페인은 프리오더의 브랜드로 채움), 프리오더 연결은 선택 항목으로 변경 |

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

#!/usr/bin/env bash
# 로컬 시연 DB(movrit_integration)와 업로드 파일을 서버로 옮길 수 있게 두 파일로 묶습니다.
#   ./export-demo-data.sh [저장 폴더]   (기본: 이 폴더의 data/)
# 결과: movrit-demo.dump(DB, pg_restore 형식), movrit-uploads.tgz(uploads·uploads-private·uploads-campaign-private)
# 비밀값은 출력하지 않습니다. DB 접속은 로컬 psql 설정(.pgpass 등)을 그대로 씁니다.
set -euo pipefail

OUT_DIR="${1:-$(cd "$(dirname "$0")" && pwd)/data}"
DB_NAME="${DEMO_DB_NAME:-movrit_integration}"
DB_HOST="${DEMO_DB_HOST:-localhost}"
DB_USER="${DEMO_DB_USER:-movrit_user}"
UPLOAD_ROOT="${DEMO_UPLOAD_ROOT:-$HOME/.movrit}"
PG_BIN="${PG_BIN:-/Applications/Postgres.app/Contents/Versions/latest/bin}"
PG_DUMP="$PG_BIN/pg_dump"
[ -x "$PG_DUMP" ] || PG_DUMP="pg_dump"

mkdir -p "$OUT_DIR"

"$PG_DUMP" -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -Fc --no-owner --no-privileges \
  -f "$OUT_DIR/movrit-demo.dump"

# 로컬 폴더 이름(integration-uploads*)을 서버 이름(uploads*)으로 바꿔 담습니다. macOS 확장 속성은 빼고 담습니다.
COPYFILE_DISABLE=1 tar -czf "$OUT_DIR/movrit-uploads.tgz" --no-mac-metadata \
  -C "$UPLOAD_ROOT" -s '/^integration-//' \
  integration-uploads integration-uploads-private integration-uploads-campaign-private

ls -lh "$OUT_DIR/movrit-demo.dump" "$OUT_DIR/movrit-uploads.tgz"

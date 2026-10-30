#!/usr/bin/env python3
"""
실제 DB 스키마(pg_dump --schema-only 결과)로 ERD 문서를 만듭니다.

사용법:
  pg_dump -h localhost -U movrit_user -d movrit_integration --schema-only --schema=public \
    --no-owner --no-privileges --exclude-table=flyway_schema_history \
    --exclude-table=movrit_schema_history > /tmp/schema.sql
  python3 scripts/generate-erd.py /tmp/schema.sql docs/erd

결과:
  docs/erd/MOVRIT.dbml        전체 테이블·컬럼·관계(dbdiagram.io에 붙여 넣어 볼 수 있습니다)
  docs/erd/NN_<도메인>.mmd    도메인별 Mermaid 관계도(GitHub에서 바로 렌더링됩니다)
  docs/erd/README.md          도메인 구분과 생성 기준 안내
"""
import os
import re
import sys

# 도메인별로 묶을 테이블입니다. 새 테이블이 생기면 여기에 추가합니다(목록에 없으면 '기타'로 분류).
DOMAINS = [
    ("01_회원_업체", "회원·인증·업체", [
        "member", "member_role", "member_social_account", "member_address", "member_channel_metric",
        "refresh_token", "email_verification_challenge", "seller_profile", "brand",
        "influencer_profile", "influencer_social_account",
    ]),
    ("02_캠페인_신청", "캠페인·신청·콘텐츠", [
        "collaboration_campaign", "campaign_application_form", "campaign_application",
        "campaign_eligibility_requirement", "campaign_content_submission", "campaign_content_link",
        "collaboration", "referral_link", "referral_click", "attribution",
    ]),
    ("03_이미지_방문증빙", "캠페인 이미지·방문 증빙", [
        "campaign_detail_image", "campaign_image_asset", "campaign_media_state",
        "campaign_visit_history", "campaign_visit_snapshot", "file_asset", "file_attachment",
    ]),
    ("04_상품_프리오더_레거시", "상품·프리오더(레거시)", [
        "product", "product_variant", "product_review", "preorder", "preorder_variant",
        "preorder_sale_variant", "preorder_history", "preorder_interest", "preorder_favorite",
    ]),
    ("05_주문_결제_보존", "주문·결제·정산(레거시 보존)", [
        "orders", "order_item", "payment", "refund", "refund_item", "shipment", "shipment_item",
        "settlement", "settlement_participant",
    ]),
    ("06_운영_공통", "알림·문의·감사", ["notification", "inquiry", "audit_log"]),
]


def parse(sql):
    """CREATE TABLE의 컬럼과 PRIMARY KEY·FOREIGN KEY·UNIQUE 제약을 읽습니다."""
    tables, fks, pks, uniques = {}, [], {}, {}
    for m in re.finditer(r"CREATE TABLE public\.(\w+) \((.*?)\n\);", sql, re.S):
        name, body = m.group(1), m.group(2)
        # 마이그레이션 이력 테이블은 서비스 데이터가 아니므로 ERD에서 제외합니다.
        if name.endswith("schema_history"):
            continue
        cols = []
        for line in body.split("\n"):
            line = line.strip().rstrip(",")
            if not line or line.startswith("CONSTRAINT"):
                continue
            parts = line.split()
            col_type = " ".join(parts[1:])
            # PostgreSQL 18은 NOT NULL 제약에 이름을 붙여 출력하므로(CONSTRAINT 이름 NOT NULL) 이름 부분을 지웁니다.
            col_type = re.sub(r"\s*CONSTRAINT \w+", "", col_type)
            col_type = re.sub(r"\s+DEFAULT .*", "", col_type)
            not_null = "NOT NULL" in col_type
            col_type = col_type.replace("NOT NULL", "").strip()
            col_type = col_type.replace("character varying", "varchar").replace("timestamp(6) without time zone", "timestamp").replace("timestamp without time zone", "timestamp")
            cols.append((parts[0], col_type, not_null))
        tables[name] = cols
    for m in re.finditer(r"ALTER TABLE ONLY public\.(\w+)\s+ADD CONSTRAINT \w+ (PRIMARY KEY|UNIQUE|FOREIGN KEY) \(([^)]*)\)(?: REFERENCES public\.(\w+)\(([^)]*)\))?", sql):
        table, kind, cols, ref_table, ref_cols = m.groups()
        cols = [c.strip() for c in cols.split(",")]
        if kind == "PRIMARY KEY":
            pks[table] = cols
        elif kind == "UNIQUE":
            uniques.setdefault(table, []).append(cols)
        else:
            fks.append((table, cols[0], ref_table, ref_cols.split(",")[0].strip()))
    return tables, fks, pks, uniques


def dbml(tables, fks, pks, uniques):
    """dbdiagram.io 형식의 전체 ERD를 만듭니다."""
    out = ["// MOVRIT 실제 스키마 ERD(DBML) — scripts/generate-erd.py로 생성합니다. 직접 수정하지 마세요.", ""]
    for name in sorted(tables):
        out.append(f"Table {name} {{")
        single_unique = {u[0] for u in uniques.get(name, []) if len(u) == 1}
        for col, col_type, not_null in tables[name]:
            notes = []
            if col in pks.get(name, []):
                notes.append("pk")
            if col in single_unique:
                notes.append("unique")
            if not_null and "pk" not in notes:
                notes.append("not null")
            suffix = f" [{', '.join(notes)}]" if notes else ""
            out.append(f'  {col} "{col_type}"{suffix}')
        multi = [u for u in uniques.get(name, []) if len(u) > 1]
        if multi:
            out.append("  indexes {")
            for u in multi:
                out.append(f"    ({', '.join(u)}) [unique]")
            out.append("  }")
        out.append("}")
        out.append("")
    for table, col, ref_table, ref_col in sorted(fks):
        out.append(f"Ref: {table}.{col} > {ref_table}.{ref_col}")
    return "\n".join(out) + "\n"


def mermaid(title, names, tables, fks, pks):
    """도메인 하나의 Mermaid erDiagram을 만듭니다. 다른 도메인 테이블은 관계선만 표시합니다."""
    out = ["%% " + title + " — scripts/generate-erd.py로 생성", "erDiagram"]
    shown = [n for n in names if n in tables]
    for name in shown:
        out.append(f"  {name} {{")
        for col, col_type, _ in tables[name]:
            kind = re.sub(r"\(.*", "", col_type).split()[0]
            key = " PK" if col in pks.get(name, []) else (" FK" if any(f[0] == name and f[1] == col for f in fks) else "")
            out.append(f"    {kind} {col}{key}")
        out.append("  }")
    for table, col, ref_table, _ in sorted(fks):
        if table in shown or ref_table in shown:
            out.append(f'  {ref_table} ||--o{{ {table} : "{col}"')
    return "\n".join(out) + "\n"


def main():
    sql = open(sys.argv[1], encoding="utf-8").read()
    out_dir = sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    tables, fks, pks, uniques = parse(sql)
    open(os.path.join(out_dir, "MOVRIT.dbml"), "w", encoding="utf-8").write(dbml(tables, fks, pks, uniques))
    grouped = set()
    readme = ["# MOVRIT ERD", "", "실제 DB 스키마로 생성한 ERD입니다. `scripts/generate-erd.py` 로 다시 만들 수 있습니다.", "",
              f"- 테이블 {len(tables)}개, 외래 키 관계 {len(fks)}개", "- `MOVRIT.dbml`: 전체 관계(dbdiagram.io에 붙여 넣기)", "", "| 파일 | 도메인 | 테이블 |", "|---|---|---|"]
    for file, title, names in DOMAINS:
        grouped.update(names)
        open(os.path.join(out_dir, f"{file}.mmd"), "w", encoding="utf-8").write(mermaid(title, names, tables, fks, pks))
        readme.append(f"| `{file}.mmd` | {title} | {', '.join(n for n in names if n in tables)} |")
    others = sorted(set(tables) - grouped)
    if others:
        open(os.path.join(out_dir, "07_기타.mmd"), "w", encoding="utf-8").write(mermaid("기타", others, tables, fks, pks))
        readme.append(f"| `07_기타.mmd` | 기타 | {', '.join(others)} |")
    open(os.path.join(out_dir, "README.md"), "w", encoding="utf-8").write("\n".join(readme) + "\n")
    print(f"tables={len(tables)} fks={len(fks)} others={others}")


if __name__ == "__main__":
    main()

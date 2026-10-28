# 캠페인 선정 방식 API 계약

캠페인 생성·수정·조회 응답의 `selectionMode`은 아래 두 값 중 하나입니다.

- `BRAND_DIRECT`: 캠페인을 소유한 승인 업체가 신청자를 선정하거나 미선정 처리합니다.
- `OPERATOR`: 관리자만 신청자를 선정하거나 미선정 처리합니다.

기존 캠페인은 DB 변경 시 `BRAND_DIRECT`로 보존됩니다. 이전 프론트 요청과의 호환을 위해 생성 요청에서 필드를 생략하면 `BRAND_DIRECT`, 수정 요청에서 생략하면 현재 값을 유지합니다.

신청 이력이 하나라도 생긴 뒤에는 선정 방식을 변경할 수 없으며 `409 Conflict`를 반환합니다.

## 업체 API

- `PATCH /api/campaigns/applications/{applicationId}/select`
- `PATCH /api/campaigns/applications/{applicationId}/reject`

`BRAND_DIRECT` 캠페인 및 소유 업체에만 허용합니다. 운영자 선정 캠페인에 요청하면 `403 Forbidden`입니다.

## 운영자 API

- `GET /api/admin/campaign-applications`
- `PATCH /api/admin/campaign-applications/{applicationId}/select`
- `PATCH /api/admin/campaign-applications/{applicationId}/reject`

관리자 계정만 사용할 수 있으며 목록에는 `OPERATOR` 캠페인의 신청만 포함합니다. 브랜드 직접 선정 캠페인에 명령하면 `403 Forbidden`입니다.

신청 응답에는 목록에서 별도 캠페인 조회 없이 표시하고 권한을 판단할 수 있도록 `campaignTitle`과 `selectionMode`을 포함합니다.

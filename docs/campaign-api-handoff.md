# 캠페인 화면 연동 계약

기준 브랜치는 `feature/brand`입니다. 모든 API 응답은 JSON이며 인증 API는 `Authorization: Bearer <accessToken>`을 사용합니다.

## 공개 조회

| 기능 | Method / URL | 주요 응답 |
|---|---|---|
| 모집 중 목록 | `GET /api/collaborations/campaigns/open` | `id`, `preorderId`, `preorderTitle`, `brandName`, `title`, `description`, `category`, `minFollowerCount`, `recruitmentCount`, `rewardDescription`, `startAt`, `endAt`, `open` |
| 캠페인 상세 | `GET /api/collaborations/campaigns/{campaignId}` | 목록 필드와 `productId`, `productName`, `mission`, `channelGuide`, `scheduleGuide`, `deliveryGuide`, `visitGuide`, `caution`, `submissionRequirement` |
| 신청 폼 | `GET /api/campaigns/{campaignId}/application-form` | `campaignId`, `version`, `allowedFulfillmentTypes`, `questions`, `agreements` |

`allowedFulfillmentTypes`는 `DELIVERY`, `VISIT` 중 하나 이상입니다. 질문은 `id`, `label`, `required`, `type`, `options`, `maxLength`, 동의 문서는 `id`, `title`, `version`, `required`, `body`를 가집니다.

## 크리에이터 신청과 진행

### 신청

`POST /api/campaigns/{campaignId}/applications`

```json
{
  "fulfillmentType": "DELIVERY",
  "expectedFormVersion": 1,
  "message": "꼼꼼하게 리뷰하겠습니다.",
  "shippingContact": {
    "recipientName": "김체험",
    "recipientPhone": "010-1234-5678",
    "postalCode": "06236",
    "address1": "서울특별시 강남구 테헤란로 1",
    "address2": "101호"
  },
  "answers": [
    {"questionId": "experience", "value": "관련 리뷰 경험이 있습니다."}
  ],
  "agreementAcceptances": [
    {"agreementId": "privacy", "version": "2026-10-01", "accepted": true}
  ]
}
```

`DELIVERY`는 수령인·연락처·우편번호·기본 주소가 필수입니다. `VISIT`은 `shippingContact`를 보내면 안 됩니다. 서버가 폼 버전, 질문 ID·형식·길이·선택지와 동의 문서의 ID·필수 여부·버전을 검증합니다. 본인 인증 제공자가 구성되기 전에는 실제 신청 성공을 허용하지 않습니다.

| 기능 | Method / URL |
|---|---|
| 내 신청 목록 | `GET /api/campaigns/applications/mine` |
| 선정 수락 | `PATCH /api/campaigns/applications/{applicationId}/activate` |
| 참여 취소 | `PATCH /api/campaigns/applications/{applicationId}/cancel` body: `{"reason":"..."}` |
| 체험 완료 확인 | `PATCH /api/campaigns/applications/{applicationId}/experience-confirm` |
| 콘텐츠 제출·수정 제출 | `POST /api/campaign-contents/applications/{applicationId}` body: `{"contentUrl":"https://...","note":"..."}` |
| 내 콘텐츠 목록 | `GET /api/campaign-contents/mine` |

신청 상태는 `SUBMITTED`, `SELECTED`, `REJECTED`, `ACTIVE`, `CONTENT_SUBMITTED`, `REVISION_REQUESTED`, `COMPLETED`, `CANCELLED`입니다. 콘텐츠 상태는 `SUBMITTED`, `REVISION_REQUESTED`, `APPROVED`입니다.

## 판매자 운영

| 기능 | Method / URL |
|---|---|
| 내 캠페인 목록 | `GET /api/collaborations/campaigns/seller` |
| 캠페인 상세 수정 | `PATCH /api/collaborations/campaigns/{campaignId}` |
| 모집 종료 | `PATCH /api/collaborations/campaigns/{campaignId}/close` |
| 신청 전 캠페인 삭제 | `DELETE /api/collaborations/campaigns/{campaignId}` |
| 신청 폼 저장 | `PUT /api/campaigns/{campaignId}/application-form` |
| 받은 신청 목록 | `GET /api/campaigns/applications/seller` |
| 선정 / 미선정 | `PATCH /api/campaigns/applications/{applicationId}/select`, `PATCH /api/campaigns/applications/{applicationId}/reject` |
| 받은 콘텐츠 목록 | `GET /api/campaign-contents/seller` |
| 수정 요청 | `PATCH /api/campaign-contents/{submissionId}/request-revision` body: `{"comment":"..."}` |
| 콘텐츠 승인 | `PATCH /api/campaign-contents/{submissionId}/approve` body: `{"comment":"..."}` |

상세 수정 본문은 상세 응답의 편집 필드 가운데 `title`, `minFollowerCount`, `recruitmentCount`, `startAt`, `endAt`이 필수입니다. 신청 이력이 있는 캠페인은 삭제 대신 모집 종료를 사용합니다.

## 공통 운영 API

| 기능 | Method / URL |
|---|---|
| 내 알림 / 안 읽은 수 | `GET /api/notifications`, `GET /api/notifications/unread-count` |
| 알림 읽음 | `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all` |
| 문의 등록 / 내 문의 | `POST /api/inquiries`, `GET /api/inquiries/mine` |

오류는 Spring 기본 Problem Detail 형식을 사용하며 HTTP 상태를 우선 처리합니다. 입력 오류는 `400`, 인증 누락은 `401`, 소유권·역할 오류는 `403`, 대상 없음은 `404`, 중복 신청·잘못된 상태 전이는 `409`입니다. 화면에는 응답의 `detail`을 우선 표시하고 없으면 상태별 기본 안내를 사용합니다.

## 로컬 샘플 데이터

판매자 4의 캠페인 `9`~`12`에 신청 폼과 상태별 신청 7건, 콘텐츠 3건이 들어 있습니다. 다시 구성할 때는 아래 순서로 실행합니다.

1. `docs/sql/feature-campaign-applications.sql`
2. `docs/sql/feature-campaign-content.sql`
3. `docs/sql/feature-campaign-detail.sql`
4. `docs/sql/seed-frontend-campaign-workflow.sql`

샘플 SQL은 재실행해도 신청·콘텐츠를 중복 생성하지 않습니다.

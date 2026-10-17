# 캠페인 화면 연동 계약

기준 브랜치는 `feature/brand`입니다. 모든 API 응답은 JSON이며 인증 API는 `Authorization: Bearer <accessToken>`을 사용합니다.

## 공개 조회

| 기능 | Method / URL | 주요 응답 |
|---|---|---|
| 모집 중 목록 | `GET /api/collaborations/campaigns/open` | `id`, `preorderId`, `preorderTitle`, `brandName`, `title`, `description`, `category`, `minFollowerCount`, `recruitmentCount`, `rewardDescription`, `startAt`, `endAt`, `open` |
| 캠페인 상세 | `GET /api/collaborations/campaigns/{campaignId}` | 목록 필드와 `productId`, `productName`, `mission`, `channelGuide`, `scheduleGuide`, `deliveryGuide`, `visitGuide`, `caution`, `submissionRequirement`, 단계별 일정과 `scheduleVersion` |
| 신청 폼 | `GET /api/campaigns/{campaignId}/application-form` | `campaignId`, `version`, `allowedFulfillmentTypes`, `questions`, `agreements` |

`allowedFulfillmentTypes`는 `DELIVERY`, `VISIT` 중 하나 이상입니다. 질문은 `id`, `label`, `required`, `type`, `options`, `maxLength`, 동의 문서는 `id`, `title`, `version`, `required`, `body`를 가집니다.

상세 일정은 모집 `startAt/endAt`, `selectionAnnouncedAt`, 체험 `experienceStartAt/experienceEndAt`, 콘텐츠 등록 `contentSubmissionStartAt/contentSubmissionEndAt`, `resultsAnnouncedAt`을 각각 제공합니다. 정의되지 않은 단계는 `null`이며 다른 기간에서 추정하지 않습니다. 기간의 `endAt`은 API 판정에서 제외 경계로 사용하므로, 자정 값은 달력에서 직전 날짜까지 포함되는 것으로 표시합니다. 모든 값은 `Asia/Seoul` 현지 시각의 오프셋 없는 `LocalDateTime`입니다.

## 크리에이터 신청과 진행

### 임시 이메일 소유 확인

실명 본인 인증과 이메일 소유 확인은 별도 상태입니다. `emailVerified=true`여도 `identityVerified`는 계속 `false`입니다.

실명 인증 공급자가 연결되기 전까지 캠페인 신청은 서버가 확인한 이메일 소유 상태를 임시 자격 조건으로 사용합니다. 클라이언트가 보내는 인증 여부는 받지 않으며, `Member.emailVerifiedAt`만 판단 근거로 사용합니다.

| 기능 | Method / URL | 계약 |
|---|---|---|
| 상태 조회 | `GET /api/email-verifications/status` | `emailVerified`, `emailVerifiedAt`, `identityVerified`, `applicationEligibilityPolicy` |
| 캠페인 신청 가능 여부 | `GET /api/campaigns/{campaignId}/eligibility` | 폼 버전, 이메일·채널·중복 여부, `canApply`, `blockingReasons` |
| 인증 메일 요청·재발송 | `POST /api/email-verifications/requests` | 성공 시 `202`, `challengeId`, `maskedEmail`, `expiresAt` |
| 번호 확인 | `POST /api/email-verifications/confirm` | body: `{"challengeId":1,"code":"123456"}` |

신청 가능 여부 응답의 `applicationEligibilityPolicy`는 현재 `EMAIL_TEMPORARY`입니다. 차단 사유 코드는 `CAMPAIGN_NOT_OPEN`, `CREATOR_NOT_APPROVED`, `FOLLOWER_REQUIREMENT_NOT_MET`, `CHANNEL_REQUIRED`, `EMAIL_VERIFICATION_REQUIRED`, `ALREADY_APPLIED`입니다. 신청 API는 이 조회 결과와 별개로 모집 기간, 크리에이터 승인·팔로워, 폼 버전, 채널, 중복, 이메일 인증을 다시 검증합니다.

인증 번호는 10분간 유효하고 한 번만 사용할 수 있습니다. 재발송 간격은 60초, 시간당 발송은 5회, 번호 확인 실패는 요청당 5회로 제한합니다. 요청 시 로그인 계정의 현재 이메일로만 발송하며 임의 수신 주소를 받지 않습니다. 이메일이 변경되면 이전 인증 요청과 확인 상태를 사용할 수 없습니다. SMTP 설정이 없으면 요청 API는 `503`을 반환하며 발송 성공을 흉내 내지 않습니다.

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

`DELIVERY`는 수령인·연락처·우편번호·기본 주소가 필수입니다. `VISIT`은 `shippingContact`를 보내면 안 됩니다. 서버가 폼 버전, 질문 ID·형식·길이·선택지와 동의 문서의 ID·필수 여부·버전을 검증합니다. 현재는 활동 채널과 이메일 소유 확인이 완료된 크리에이터만 신청할 수 있으며, 실명 인증 완료로 표시하지 않습니다.

| 기능 | Method / URL |
|---|---|
| 내 신청 목록 | `GET /api/campaigns/applications/mine` |
| 선정 수락 | `PATCH /api/campaigns/applications/{applicationId}/activate` |
| 참여 취소 | `PATCH /api/campaigns/applications/{applicationId}/cancel` body: `{"reason":"..."}` |
| 체험 완료 확인 | `PATCH /api/campaigns/applications/{applicationId}/experience-confirm` |
| 콘텐츠 제출·수정 제출 | `POST /api/campaign-contents/applications/{applicationId}` body: `{"contentUrl":"https://...","note":"..."}` |
| 내 콘텐츠 목록 | `GET /api/campaign-contents/mine` |

신청 상태는 `SUBMITTED`, `SELECTED`, `REJECTED`, `ACTIVE`, `CONTENT_SUBMITTED`, `REVISION_REQUESTED`, `COMPLETED`, `CANCELLED`입니다. 콘텐츠 상태는 `SUBMITTED`, `REVISION_REQUESTED`, `BRAND_APPROVED`, `APPROVED`입니다. `BRAND_APPROVED`는 브랜드 검토가 끝났지만 운영자 최종 승인을 기다리는 상태입니다.

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
| 브랜드 검토 완료 | `PATCH /api/campaign-contents/{submissionId}/approve` body: `{"comment":"..."}` |

운영자는 `GET /api/admin/campaign-contents`로 전체 제출물을 조회합니다. 최종 수정 요청은 `PATCH /api/admin/campaign-contents/{submissionId}/request-revision`, 최종 승인은 `PATCH /api/admin/campaign-contents/{submissionId}/approve`입니다. 두 변경 API의 본문은 각각 판매자 API와 같은 `comment` 형식을 사용합니다.

상세 수정 본문은 상세 응답의 편집 필드 가운데 `title`, `minFollowerCount`, `recruitmentCount`, `expectedScheduleVersion`, `startAt`, `endAt`이 필수입니다. `expectedScheduleVersion`에는 상세 응답의 `scheduleVersion`을 보내며, 이미 일정이 변경된 경우 서버가 `400`으로 덮어쓰기를 막습니다. 신청 이력이 있는 캠페인은 삭제 대신 모집 종료를 사용합니다.

## 공통 운영 API

| 기능 | Method / URL |
|---|---|
| 내 알림 / 안 읽은 수 | `GET /api/notifications`, `GET /api/notifications/unread-count` |
| 알림 읽음 | `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all` |
| 문의 등록 / 내 문의 | `POST /api/inquiries`, `GET /api/inquiries/mine` |

오류는 Problem Detail 형식을 사용하며 HTTP 상태를 우선 처리합니다. 입력 오류는 `400`, 인증 누락은 `401`, 소유권·역할 오류는 `403`, 대상 없음은 `404`, 중복 신청·잘못된 상태 전이는 `409`입니다. 업무 오류 안내는 `detail`, 본문 검증 오류는 `fieldErrors`에 포함됩니다.

## 로컬 샘플 데이터

판매자 4의 캠페인 `9`~`12`에 신청 폼과 상태별 신청 7건, 콘텐츠 3건이 들어 있습니다. 다시 구성할 때는 아래 순서로 실행합니다.

1. `docs/sql/feature-campaign-applications.sql`
2. `docs/sql/feature-campaign-content.sql`
3. `docs/sql/feature-campaign-detail.sql`
4. `docs/sql/seed-frontend-campaign-workflow.sql`

샘플 SQL은 재실행해도 신청·콘텐츠를 중복 생성하지 않습니다.

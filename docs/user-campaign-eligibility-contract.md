# 사용자 통합과 캠페인 신청 자격 계약

## 회원과 권한

- 구매자와 인플루언서는 가입 유형을 나누지 않고 모두 `USER` 회원으로 가입합니다.
- 업체는 `USER` 회원이 `POST /api/sellers/apply`로 판매자 심사를 신청합니다. 승인된 `SellerProfile`만 브랜드·상품·판매자 운영 API를 사용합니다.
- 운영자는 기존 `ADMIN` 권한을 유지합니다. 회원가입 요청으로 판매자나 운영자 권한을 받을 수 없습니다.
- 기존 회원, 크리에이터 프로필, 신청 이력은 삭제하거나 강제로 병합하지 않습니다.

## 사용자 채널 프로필

기존 `influencer_profile`은 이전 데이터 호환을 위해 유지하되, 캠페인 신청 관점에서는 선택형 사용자 채널 프로필로 취급합니다. 별도의 인플루언서 가입이나 승인 상태를 모든 캠페인에 요구하지 않습니다.

프론트 계약:

| 기능 | Method / URL | 설명 |
|---|---|---|
| 내 채널 프로필 조회 | `GET /api/users/me/channel-profile` | 프로필이 없으면 `profileExists=false`, 빈 지표 목록 반환 |
| 내 채널 프로필 저장 | `PUT /api/users/me/channel-profile` | 채널과 지표를 저장하되 사용자가 입력한 값은 미확인 상태 |

채널 지표는 다음 필드를 사용합니다.

- `channelType`: 예 `INSTAGRAM`, `NAVER_BLOG`
- `metricType`: 예 `FOLLOWER_COUNT`, `VISITOR_COUNT`
- `value`: 0 이상의 정수
- `measurementPeriod`: 방문자처럼 집계기간이 필요한 지표의 기간. 정책 미정이면 `null`
- `measuredAt`: 측정 기준 시각
- `verificationStatus`: `UNVERIFIED`, `VERIFIED`, `REJECTED`

사용자가 값을 수정하면 기존 확인 상태는 `UNVERIFIED`로 돌아갑니다. 외부 API 자동 수집은 현재 범위에 포함하지 않으며, 미확인 값을 확인된 값으로 표시하지 않습니다.

## 캠페인 조건

1차 구현에서 캠페인은 구조화된 신청 조건을 없거나 하나만 가집니다. 복수 조건의 AND/OR 방식은 정책 확정 전까지 제공하지 않습니다.

- 조건이 없으면 채널 프로필이나 지표 없이 신청할 수 있습니다.
- 지표 조건은 `channelType`, `metricType`, `minimumValue`, 선택적 `measurementPeriod`로 구성합니다.
- 조건이 있는데 대응 지표가 없으면 `CHANNEL_METRIC_REQUIRED`입니다.
- 지표가 있지만 확인되지 않았으면 `CHANNEL_METRIC_UNVERIFIED`입니다.
- 확인된 값이 최솟값보다 작으면 `CHANNEL_METRIC_BELOW_MINIMUM`입니다.
- 모든 조건을 충족하면 해당 지표 때문에 차단하지 않습니다.

예를 들어 인스타그램 팔로워 최소값이 `500`이면 확인된 `499`는 차단하고 확인된 `500`은 허용합니다. 이 숫자는 특정 캠페인이 설정한 조건 예시이며 플랫폼 공통 등급 기준이 아닙니다.

채널별 등급 기준, 네이버 방문자 집계기간, 미확인 지표의 등급 반영 정책은 아직 확정하지 않습니다. 확정 전에는 등급 숫자를 코드에 넣지 않습니다.

사용자 등급 표시는 캠페인 신청 조건 판정과 별도 기능입니다. 등급 정책이 정해지지 않아도 캠페인이 직접 설정한 단일 지표 조건은 판정할 수 있지만, 그 결과로 사용자 등급을 추정하거나 부여하지 않습니다. 미연동·미확인 값은 `0`이나 최하 등급으로 변환하지 않고 각각 `CHANNEL_METRIC_REQUIRED`, `CHANNEL_METRIC_UNVERIFIED` 상태로 유지합니다.

## 신청 판정

`GET /api/campaigns/{campaignId}/eligibility`는 로그인 회원을 기준으로 다음을 반환합니다.

- 기존 필드: `campaignId`, `formVersion`, `emailVerified`, `identityVerified`, `alreadyApplied`, `canApply`, `blockingReasons`
- 추가 필드: `channelProfileExists`, `campaignRequirements`, `metricEvaluations`

서버는 신청 POST에서도 동일 조건을 다시 검증합니다. 로그인, 이메일 인증, 모집기간, 중복 신청, 폼 버전, 질문·동의·배송 검증은 유지합니다. 별도 크리에이터 승인 상태는 공통 신청 조건에서 제거합니다.

신청 저장 시 기존 `campaign_application.creator_id` 호환을 위해 내부 사용자 채널 프로필을 자동 준비할 수 있지만, 이는 권한 승격이나 공개 인플루언서 승인을 의미하지 않습니다. 판매자·운영자 권한 검증은 기존대로 유지합니다.

## 데이터 변경 원칙

- `member_channel_metric`: 회원별 채널 지표와 확인 상태를 저장합니다.
- `campaign_eligibility_requirement`: 캠페인별 구조화 조건을 저장합니다.
- 기존 `min_follower_count`, `influencer_profile.follower_count`, `sns_url`은 이전 API 호환용으로 유지하며 확인된 신규 지표로 자동 간주하지 않습니다.
- 기존 신청과 프로필 데이터는 보존합니다. 신규 조건으로 자동 변환할 때 확인 근거가 없는 값은 `UNVERIFIED`입니다.

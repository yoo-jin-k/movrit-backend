# 회원 계정 설정 API 계약

## 기존 프로필

닉네임과 기본 회원 정보는 기존 `GET /api/members/me`, `PUT /api/members/me`를 그대로 사용합니다.

## 비밀번호

### 변경 가능 여부

`GET /api/members/me/password-capability`

- `hasPassword`: 로컬 비밀번호 보유 여부
- `canChangePassword`: 현재 비밀번호 변경 API 사용 가능 여부
- `unavailableReason`: 소셜 전용 계정은 `SOCIAL_ONLY_ACCOUNT`

### 변경

`PATCH /api/members/me/password`

요청은 `currentPassword`, `newPassword`입니다. 새 비밀번호는 8~100자이며 영문자와 숫자를 각각 하나 이상 포함해야 합니다. BCrypt 입력 한계에 맞춰 현재·새 비밀번호 모두 UTF-8 기준 72바이트 이하여야 하며, 바이트 길이는 해시 비교보다 먼저 검사합니다. 비밀번호 변경은 회원 행을 잠가 동일한 현재 비밀번호를 사용한 동시 변경을 직렬화합니다.

- 현재 비밀번호 불일치: `400 Bad Request`, `CURRENT_PASSWORD_MISMATCH`
- 기존 비밀번호와 동일: `409 Conflict`, `PASSWORD_REUSE`
- 비밀번호가 없는 소셜 전용 계정: `409 Conflict`, `PASSWORD_NOT_CONFIGURED`
- 형식 오류: `400 Bad Request`
- 현재 비밀번호 72바이트 초과: `400 Bad Request`, `CURRENT_PASSWORD_TOO_LONG_IN_BYTES`
- 새 비밀번호 72바이트 초과: `400 Bad Request`, `PASSWORD_TOO_LONG_IN_BYTES`
- UTF-8 인코딩 기준 72바이트 초과: `400 Bad Request`, `PASSWORD_TOO_LONG_IN_BYTES`

변경 성공 시 저장된 Refresh Token을 폐기하므로 새 토큰 발급에는 다시 로그인이 필요합니다. 이미 발급된 Access JWT는 서버 상태를 조회하지 않는 현재 구조상 만료 시각까지 유효합니다. 따라서 모든 기기에서 즉시 로그아웃된다고 표현하지 않습니다.

## 주소록

기본 경로는 `/api/members/me/addresses`입니다.

- `GET /`: 본인 주소 목록
- `POST /`: 주소 등록
- `PUT /{addressId}`: 본인 주소 수정
- `PATCH /{addressId}/default`: 기본 주소 지정
- `DELETE /{addressId}`: 본인 주소 삭제

필드:

- `label`: 필수, 최대 30자
- `recipientName`: 필수, 최대 50자
- `recipientPhone`: 필수, 숫자·`+`·`-` 조합 8~20자
- `postalCode`: 필수, 숫자 5~6자
- `address1`: 필수, 최대 300자
- `address2`: 선택, 최대 300자
- `defaultAddress`: 기본 주소 여부

회원별 최대 10개입니다. 첫 주소는 자동으로 기본 주소가 됩니다. 기본 주소 변경은 회원 행 잠금으로 직렬화하고 기존 기본값을 먼저 DB에 반영한 뒤 새 기본값을 지정합니다. DB 부분 유니크 인덱스도 회원별 기본 주소 하나를 보장합니다. 기본 주소 삭제 시 남은 주소 중 생성 ID가 가장 큰, 즉 가장 최근 생성된 항목을 기본 주소로 지정합니다. 수정 시각은 대체 순위에 사용하지 않습니다.

다른 회원의 주소 ID는 `404 Not Found`로 처리합니다. 주소록 변경은 기존 주문과 캠페인 신청에 저장된 배송지 사본을 변경하지 않습니다.

## 통합 검수 계정 분리

- 주소록·프론트 연동: `qa-account-settings-integration@movrit.local`
- 비밀번호 변경 검수: `qa-password-settings-integration@movrit.local`
- 소셜 전용 정책 검수: `qa-social-only-settings-integration@movrit.local`

주소록과 비밀번호 검수가 서로의 상태를 바꾸지 않도록 계정을 분리합니다. 로컬 비밀번호 계정은 기존 통합 QA 비밀번호 해시를 재사용하며 평문 비밀번호를 SQL에 저장하지 않습니다.

# 통합 검수 환경

- 백엔드 프로필: `integration`
- 기본 URL: `http://localhost:18080`
- DB: `movrit_integration`
- 기준 시간대: `Asia/Seoul`
- API 날짜 형식: 오프셋이 없는 `LocalDateTime` 문자열이며 서울 현지 시각으로 해석합니다.

`movrit_integration`은 개발 DB의 연결을 끊거나 데이터를 변경하지 않고 덤프·복원한 별도 로컬 DB입니다. 통합 검수 중 생성·수정되는 회원, 신청, 콘텐츠는 이 DB에만 저장합니다.

실행에 필요한 OAuth 값은 외부 로그인을 호출하지 않는 통합 검수용 더미 값을 사용하고, JWT 비밀값은 로컬 환경변수로만 전달합니다.

통합 프로필의 Google OAuth 값은 실제 자격증명이 아니라 더미이므로 로그인 URL 요청은 `503 SERVICE_UNAVAILABLE`과 `구글 로그인 설정이 아직 구성되지 않았습니다.`를 반환합니다. 실제 Google 로그인을 검수하려면 기존 Google Cloud OAuth 클라이언트의 client ID·secret을 로컬 환경변수로 제공하고, 승인된 리디렉션 URI에 `http://localhost:18080/api/auth/google/callback`을 등록해야 합니다. 비밀값은 문서나 Git에 저장하지 않습니다.

```shell
java -jar build/libs/movrit-0.0.1-SNAPSHOT.jar --spring.profiles.active=integration
```

확인된 공개 조회:

- `GET /api/collaborations/campaigns/open` → `200`
- `GET /api/collaborations/campaigns/9` → `200`
- `GET /api/campaigns/9/application-form` → `200`

신청 폼은 질문 `type/options/maxLength`와 동의 문서 `body`를 반환합니다. 실명 인증 공급자가 연결되기 전에는 서버가 확인한 이메일 소유 상태를 임시 신청 조건으로 사용하며, 실명 인증 상태는 계속 `false`입니다.

## 이메일 기반 캠페인 신청 QA

- 실행 JAR 기능 기준 커밋: `8497873`
- 실행 DB: `movrit_integration`
- 실행 주소: `http://127.0.0.1:18080`
- 프론트 개발 Origin은 `http://localhost:5173`, `http://localhost:5174`, `http://127.0.0.1:5173`, `http://127.0.0.1:5174`를 허용합니다. 브라우저 주소와 Origin을 혼용해도 되지만 프론트 프록시 대상은 `18080`으로 통일합니다.
- 대상 캠페인: `9`는 이메일 인증 신청과 중복 신청이 이미 검증됐고, 추가 신청 생성은 기존 데이터와 충돌합니다. 새 신청 검수에는 신청 이력이 없는 크리에이터와 모집 중 캠페인을 조합합니다.
- 현재 QA 크리에이터 회원 `12`는 이메일 인증 완료 상태이며 캠페인 `9` 신청 이력이 있습니다. 같은 계정으로 캠페인 `9`를 조회하면 `ALREADY_APPLIED`가 정상입니다.
- 미인증 상태는 운영 DB나 외부 메일을 사용하지 않고 통합 DB 전용 회원의 `email_verified_at`을 `NULL`로 둔 상태에서 확인합니다. 인증 상태 변경 전후에는 동일한 회원·캠페인 조합을 사용하고, 신청 생성은 인증 완료 확인 뒤 한 번만 수행합니다.
- 로그인 응답 토큰은 메시지나 문서에 기록하지 않고 `/private/tmp` 아래의 권한 제한 파일에 저장하거나 매 요청 직전에 다시 발급합니다.
- 클라이언트 요청 DTO에는 `emailVerified`, `identityVerified`, `channelReady` 필드가 없습니다. 추가 필드를 보내더라도 신청 자격은 `member.email_verified_at`과 서버의 크리에이터 정보만 사용합니다.
- 이메일 인증 메일 요청은 SMTP 미설정 환경에서 `503 SERVICE_UNAVAILABLE`이 정상입니다. 기존 QA 회원의 DB 인증 상태는 실제 메일 발송 성공을 의미하지 않습니다.
- 다른 회원의 challenge 접근 검수는 SMTP가 설정돼 실제 challenge를 만들 수 있는 격리 환경에서 진행합니다. 현재 통합 실행본에서는 발송 단계가 `503`으로 끝나므로 소유권 테스트용 challenge가 생성되지 않습니다.

### 로컬 QA 계정 접근

현재 실행 호스트에는 회원 `12`로 발급한 로그인 응답이 `/private/tmp/movrit-qa-login.json`에 있으며 파일 권한은 소유자 읽기·쓰기(`600`)로 제한합니다. 비밀번호와 토큰 값은 채팅, 문서, 명령 출력에 복사하지 않습니다.

```shell
token=$(jq -r '.accessToken' /private/tmp/movrit-qa-login.json)
curl -H "Authorization: Bearer $token" http://127.0.0.1:18080/api/auth/me
```

`/api/auth/me`가 `200`과 회원 `12`를 반환할 때만 이 파일을 사용합니다. `401`이면 임의 비밀번호 변경이나 새 권한 부여를 하지 말고, 계정 소유자가 로컬 로그인으로 파일을 다시 발급해야 합니다.

### 허용된 fixture 변경 범위

- DB는 반드시 `movrit_integration`만 사용합니다. `movrit`과 운영 DB는 변경하지 않습니다.
- 인증 전 상태는 회원 `12`의 `member.email_verified_at`만 일시적으로 `NULL`로 바꿉니다. 확인이 끝나면 원래의 인증 시각을 보관해 둔 값으로 복원하고, 임의의 현재 시각으로 덮어쓰지 않습니다.
- 캠페인 `10`은 회원 `12`의 미인증 차단 후 인증 복원·신청 성공 검수를 완료했습니다. 신청 ID `9`가 있으므로 이후 조회에서 `ALREADY_APPLIED`가 정상이며 새 신청을 만들지 않습니다.
- 캠페인 `9`도 기존 신청 fixture가 있어 이메일 단독 차단 검수에 사용하지 않습니다. 미인증 단독 검증을 반복하려면 기존 데이터를 변경하지 말고 별도의 격리 회원·캠페인을 준비합니다.
- 새 신청의 응답 `id`를 QA 기록에 남깁니다. 정리가 필요하면 그 실행에서 생성한 정확한 `id`만 대상으로 하며 기존 캠페인 신청, 폼, 콘텐츠, 알림, 감사 기록을 일괄 삭제하지 않습니다.
- QA 완료 후 회원 `12`는 이메일 인증 완료 상태여야 합니다. 캠페인 `10` 신청은 통합 검수 증거로 유지할 수 있으며, 유지한 경우 이후 테스트에서는 `ALREADY_APPLIED`가 정상입니다.

주요 확인 순서는 다음과 같습니다.

완료된 검수 결과는 캠페인 `10` 기준으로 미인증 시 `EMAIL_VERIFICATION_REQUIRED`, 원래 인증 시각 복원 후 신청 `201`, 응답의 `emailVerified=true`·`identityVerified=false`, 후속 조회의 `ALREADY_APPLIED`·`canApply=false`입니다.

기존 신청과 계정 상태에 의존하지 않는 회귀 검증은 `CampaignApplicationEligibilityTest.unverifiedEmailCannotCreateApplicationWhenOtherConditionsPass`에서 수행합니다. 합성 fixture는 모집기간·크리에이터 승인·팔로워·채널·폼 버전·배송 입력을 모두 충족시키고 이메일만 미인증으로 두며, 신청 서비스가 `409`를 반환하고 저장소에 신청을 기록하지 않는지 확인합니다. 이 테스트는 실서버 QA 및 실제 SMTP 수신 검증과 구분합니다.

### 실서버 이메일 미인증 차단 fixture

- 로그인 이메일: `qa-unverified-integration@movrit.local`
- 회원 ID: `14`
- 크리에이터 ID: `9`
- 비밀번호 파일: `/private/tmp/movrit-unverified-qa-password.txt` (`600`)
- 로그인 응답 파일: `/private/tmp/movrit-unverified-qa-login.json` (`600`)
- 검수 대상: 캠페인 `10`, 폼 버전 `1`, `DELIVERY`

이 fixture는 `ACTIVE`, 팔로워 `5000`, 활동 채널 등록 상태이며 `email_verified_at`만 `NULL`입니다. 실제 SMTP나 외부 계정은 사용하지 않았습니다. 검수 전 신청 수는 `0`, eligibility는 `EMAIL_VERIFICATION_REQUIRED`, 유효한 신청 POST는 `409`와 `이메일 소유 확인 후 캠페인에 신청할 수 있습니다.`, 검수 후 신청 수도 `0`으로 확인됐습니다.

독립 재조회 시 eligibility와 캠페인 `10`·크리에이터 `9`의 신청 수만 확인하고 인증 상태를 변경하거나 신청을 생성하지 않습니다. 반복 QA를 위해 기본적으로 보존합니다. 정리할 때는 신청이 계속 `0`인지 확인한 뒤 회원 `14`의 `refresh_token`, 크리에이터 `9`, 회원 `14`의 `member_role`, 회원 `14` 순서로 정확한 ID만 삭제하고 두 `/private/tmp/movrit-unverified-qa-*` 파일을 제거합니다.

### 브라우저 로그인 fixture

- 로그인 이메일: `qa-browser-integration@movrit.local`
- 회원 ID: `13`
- 크리에이터 ID: `8`
- 비밀번호 파일: `/private/tmp/movrit-browser-qa-password.txt` (`600`)
- 로그인 응답 파일: `/private/tmp/movrit-browser-qa-login.json` (`600`)
- 신청 검수 대상: 캠페인 `10`; 정상 UI 로그인과 신청 1회를 완료해 신청 ID `10`이 생성됐습니다.

비밀번호 파일의 값은 채팅·Git·화면 캡처에 복사하지 않고 로컬 로그인 UI에만 입력합니다. 이 회원의 `email_verified_at`과 크리에이터 `ACTIVE` 상태는 외부 메일 또는 실명 인증을 완료한 결과가 아니라 `movrit_integration` 전용 합성 상태입니다. `identityVerified`는 계속 `false`여야 합니다. 브라우저의 `/api` 프록시는 최신 검수 서버 `http://localhost:18080`을 향해야 하며, 현재 저장소 기본 Vite 설정의 `8080`과 혼동하지 않습니다.

비밀번호 파일은 UI 자동 입력 시 줄바꿈이 비밀번호에 포함되지 않도록 끝 줄바꿈을 제거한 상태입니다. 파일 내용을 읽어 입력할 때 임의 공백이나 줄바꿈을 덧붙이지 않습니다.

fixture는 반복 QA와 브라우저 검수 증거를 위해 보존합니다. 신청 ID `10`은 `SUBMITTED`, 폼 버전 `1`, `DELIVERY`, `emailVerified=true`, `identityVerified=false`로 확인됐습니다. 이후 eligibility의 `ALREADY_APPLIED`를 정상 결과로 취급하며 추가 신청을 만들지 않습니다. 이 fixture는 이미 신청이 생성됐으므로 아래 단순 계정 정리 절차를 사용하지 않습니다. 아래 절차는 신청을 생성하지 않은 새 fixture에만 적용합니다.

1. 대상이 회원 `13`, 크리에이터 `8`, 이메일 `qa-browser-integration@movrit.local`인지 다시 확인합니다.
2. `refresh_token`에서 회원 `13` 행, `influencer_profile`에서 크리에이터 `8` 행, `member_role`에서 회원 `13` 행을 순서대로 삭제합니다.
3. 마지막으로 `member`의 회원 `13` 행을 삭제합니다.
4. 신청·콘텐츠 등 추가 QA 데이터가 생겼다면 위 단순 절차를 사용하지 않고 생성한 ID별 참조 관계를 먼저 확인합니다.
5. DB 정리 후 두 `/private/tmp/movrit-browser-qa-*` 파일을 삭제합니다.

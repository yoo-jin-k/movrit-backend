# 통합 검수 환경

- 백엔드 프로필: `integration`
- 기본 URL: `http://localhost:18080`
- DB: `movrit_integration`
- 기준 시간대: `Asia/Seoul`
- API 날짜 형식: 오프셋이 없는 `LocalDateTime` 문자열이며 서울 현지 시각으로 해석합니다.

`movrit_integration`은 개발 DB의 연결을 끊거나 데이터를 변경하지 않고 덤프·복원한 별도 로컬 DB입니다. 통합 검수 중 생성·수정되는 회원, 신청, 콘텐츠는 이 DB에만 저장합니다.

실행에 필요한 OAuth 값은 외부 로그인을 호출하지 않는 통합 검수용 더미 값을 사용하고, JWT 비밀값은 로컬 환경변수로만 전달합니다.

```shell
java -jar build/libs/movrit-0.0.1-SNAPSHOT.jar --spring.profiles.active=integration
```

확인된 공개 조회:

- `GET /api/collaborations/campaigns/open` → `200`
- `GET /api/collaborations/campaigns/9` → `200`
- `GET /api/campaigns/9/application-form` → `200`

신청 폼은 질문 `type/options/maxLength`와 동의 문서 `body`를 반환합니다. 실명 인증 공급자가 연결되기 전에는 서버가 확인한 이메일 소유 상태를 임시 신청 조건으로 사용하며, 실명 인증 상태는 계속 `false`입니다.

## 이메일 기반 캠페인 신청 QA

- 실행 JAR 기준 커밋: `b5a2736`
- 실행 DB: `movrit_integration`
- 실행 주소: `http://127.0.0.1:18080`
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
- 미인증 차단 확인은 신청 이력이 없는 캠페인 `10`을 사용합니다. 캠페인 `9`는 이미 중복 신청 fixture가 있어 이메일 단독 차단 검수에 사용하지 않습니다.
- 신청 생성은 회원 `12`의 크리에이터 `7`과 캠페인 `10` 조합에 한 번만 허용합니다. 실행 직전에 해당 조합의 신청이 없는지 확인하고, 이미 있으면 새 신청을 만들지 않습니다.
- 새 신청의 응답 `id`를 QA 기록에 남깁니다. 정리가 필요하면 그 실행에서 생성한 정확한 `id`만 대상으로 하며 기존 캠페인 신청, 폼, 콘텐츠, 알림, 감사 기록을 일괄 삭제하지 않습니다.
- QA 완료 후 회원 `12`는 이메일 인증 완료 상태여야 합니다. 캠페인 `10` 신청은 통합 검수 증거로 유지할 수 있으며, 유지한 경우 이후 테스트에서는 `ALREADY_APPLIED`가 정상입니다.

주요 확인 순서는 다음과 같습니다.

1. `GET /api/campaigns/{campaignId}/eligibility`에서 미인증 회원이 `EMAIL_VERIFICATION_REQUIRED`로 차단되는지 확인합니다.
2. 같은 회원을 통합 DB에서 인증 완료 fixture로 바꾼 뒤 `emailVerified=true`, `identityVerified=false`, `canApply=true`를 확인합니다.
3. 최신 `formVersion`으로 신청하고 `201`, `emailVerified=true`, `identityVerified=false`를 확인합니다.
4. 같은 회원과 캠페인으로 다시 조회해 `ALREADY_APPLIED`, `canApply=false`를 확인합니다.

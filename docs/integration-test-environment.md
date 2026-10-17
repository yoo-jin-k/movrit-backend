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

주요 확인 순서는 다음과 같습니다.

1. `GET /api/campaigns/{campaignId}/eligibility`에서 미인증 회원이 `EMAIL_VERIFICATION_REQUIRED`로 차단되는지 확인합니다.
2. 같은 회원을 통합 DB에서 인증 완료 fixture로 바꾼 뒤 `emailVerified=true`, `identityVerified=false`, `canApply=true`를 확인합니다.
3. 최신 `formVersion`으로 신청하고 `201`, `emailVerified=true`, `identityVerified=false`를 확인합니다.
4. 같은 회원과 캠페인으로 다시 조회해 `ALREADY_APPLIED`, `canApply=false`를 확인합니다.

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

신청 폼은 질문 `type/options/maxLength`와 동의 문서 `body`를 반환합니다. 본인 인증 제공자가 아직 없으므로 인증된 크리에이터의 정상 형식 신청도 `409`로 차단하며, 프론트는 이를 성공으로 처리하면 안 됩니다.

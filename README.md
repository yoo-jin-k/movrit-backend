# MOVRIT 백엔드

브랜드가 캠페인을 등록하고 사용자가 신청·체험·콘텐츠 제출을 진행하는 플랫폼의 Spring Boot 서버입니다.

- Spring Boot 4.1 · Java 17(표준) · Spring Security(JWT) · JPA · PostgreSQL
- 기능별 패키지: `auth`, `member`, `seller`, `brand`, `collaboration`(캠페인), `campaignapplication`(신청·선정), `campaigncontent`(콘텐츠 제출·검수), `campaigneligibility`(신청 조건), `campaignmedia`(이미지·방문 증빙) 등
- 각 기능은 `controller / service / repository / entity / dto` 계층으로 나뉩니다.

## 포트폴리오 문서

- [구조 설명](docs/portfolio/architecture.md): 시스템 구성, 기능 단위 구조, 신청 상태 흐름, 설계 결정
- [시연 시나리오](docs/portfolio/demo-scenario.md): 캠페인 등록부터 최종 승인까지 10분 시연 순서
- [ERD](docs/erd/README.md) · [DB 변경 이력](docs/db-migration.md)

## 로컬 통합 환경 실행

| 구성 | 주소 |
|---|---|
| 백엔드 | http://localhost:18080 |
| 프론트 | http://localhost:5174 (movrit-frontend 에서 `npm run dev:integration`) |
| DB | PostgreSQL `movrit_integration` (localhost:5432, 사용자 `movrit_user`) |

1. 비밀값 파일 준비(처음 한 번)
   - `scripts/integration.env.example` 을 저장소 **상위 폴더**에 `.movrit-integration.env` 로 복사하고 값을 채웁니다.
   - Google·카카오 키, JWT 서명 키가 들어가며 Git에 올라가지 않습니다.
2. 서버 실행

```bash
scripts/integration.sh start     # 시작(jar가 없으면 자동 빌드)
scripts/integration.sh restart   # 최신 jar로 재시작
scripts/integration.sh build     # 테스트 없이 jar만 다시 빌드
scripts/integration.sh status    # 실행 여부
scripts/integration.sh logs      # 로그 보기(build/run/backend.log)
scripts/integration.sh stop      # 종료
```

- JDK 17이 없으면 설치된 17 이상 JDK로 자동 빌드합니다(`-PmovritJavaVersion`).
- 업로드 파일은 `~/.movrit/integration-uploads` 에 저장되어 재부팅 후에도 유지됩니다.
- 이메일 인증은 통합 환경에서 **로컬 시연 모드**(`local-preview`)로 동작합니다. 실제 메일 대신 인증 화면에 6자리 코드가 표시됩니다. 실제 발송은 `EMAIL_DELIVERY=smtp` 와 메일 계정 설정이 필요합니다.

## 검증

```bash
./gradlew test spotlessCheck bootJar            # JDK 17
./gradlew test spotlessCheck bootJar -PmovritJavaVersion=18   # JDK 17이 없을 때
```

`MovritApplicationTests.contextLoads` 는 기본 프로필 DB(`movrit`)에 접속하므로 해당 DB가 최신 스키마여야 합니다.

## DB 변경

DB 구조 변경 절차와 적용 이력은 [`docs/db-migration.md`](docs/db-migration.md) 를 따릅니다.

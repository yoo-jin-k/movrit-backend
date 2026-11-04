# 보안 점검 기록

배포 전에 확인한 항목과 처리 결과입니다. 자동 점검은 백엔드 테스트와 프론트 저장소의 `qa/e2e/access-control-check.mjs`로 다시 돌릴 수 있습니다.

## 1. 비밀값

| 항목 | 처리 |
|---|---|
| DB 비밀번호·JWT 서명 키·소셜 로그인·SNS 연결 키 | `application.properties`에는 `${환경 변수}`만 적고 값은 저장소 밖에서 받음 |
| 환경 파일·키 파일 | `.gitignore`로 `.env`, `*.env`, `*.pem`, `*.key`, `*.p12` 등 차단(값이 빈 예시 파일만 허용) |
| 커밋 전 검사 | `.githooks/check-secrets.py`가 올릴 내용에서 키·토큰·비밀번호·접속 정보가 든 DB 주소와 환경 파일을 찾아 커밋을 멈춤. 설정: `git config core.hooksPath .githooks` |
| 이력 검사 | 두 저장소 전체 커밋을 검사해 실제 키·토큰 없음 확인(2026-11-04) |
| JWT 서명 키 길이 | 32바이트 미만이면 서버가 뜨지 않음. 키 값은 오류 문구에 넣지 않음 |

양식: 로컬은 `scripts/integration.env.example`, 배포는 `scripts/production.env.example`(이름만 있고 값은 빈칸).

**결정(2026-11-04).** 프론트 저장소의 예전 커밋에 로컬 점검용 비밀번호 두 개(비밀번호 재설정 점검값, 탈퇴 점검 임시 계정)가 남아 있습니다. 로컬 통합 DB에서만 쓰던 값이라 커밋 이력을 다시 쓰지는 않고(강제 푸시 없음), **배포할 때 시연·점검 계정 비밀번호를 모두 새로 정합니다.** 지금 점검 스크립트는 이런 비밀번호를 실행할 때마다 무작위로 만들고, 점검 계정 비밀번호는 `MOVRIT_QA_PASSWORD` 환경 변수로만 받습니다.

## 2. 운영 설정(`SPRING_PROFILES_ACTIVE=prod`, `application-prod.properties`)

- API 문서(Swagger)는 기본으로 꺼짐. 필요할 때만 `API_DOCS_ENABLED=true`.
- SQL 문장 로그 끔, 오류 응답에 예외 종류·스택 넣지 않음.
- DB 주소·계정, 프론트 주소, CORS 허용 주소는 반드시 환경 변수로 지정(기본값 없음).
- HTTPS를 끝내 주는 프록시 뒤에서 실제 접속 주소·https 여부를 받음(`server.forward-headers-strategy=framework`).

## 3. 요청 보호

| 항목 | 처리 |
|---|---|
| CORS | 허용 주소를 `CORS_ALLOWED_ORIGINS`(쉼표 구분)로 받음. 기본값은 로컬 프론트. `*`는 쓸 수 없음. 다른 출처의 요청은 403 |
| OAuth 돌아갈 주소 | CORS와 같은 허용 목록에 있는 주소로만 로그인 토큰을 넘김 |
| 보안 헤더 | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, HTTPS에서는 HSTS 1년 |
| 로그인 실패 제한 | 같은 이메일 15분 안 5회, 같은 접속 주소 15분 안 30회 실패하면 429(비밀번호가 맞아도). 성공하면 그 이메일 기록을 지움. 서버 한 대 기준 메모리 기록 |
| 계정 존재·상태 노출 | 없는 이메일과 틀린 비밀번호는 같은 401. 이용 정지 같은 계정 상태(403)는 비밀번호가 맞을 때만 알려 줌 |
| 비밀번호 찾기 | 가입 여부와 관계없이 같은 안내, 1분 1회·1시간 5회 제한(기존) |

## 4. 파일

### 저장 위치

| 위치 | 설정 | 들어 있는 파일 | 내려가는 방법 |
|---|---|---|---|
| 공개 | `app.upload-dir`(환경 변수 `UPLOAD_DIR`, 기본 `uploads`) | 캠페인 대표·상세 이미지(`campaigns`), 브랜드 로고·표지(`brands`), 상품 이미지(`products`) | `/uploads/{폴더}/...` 주소, 로그인 없이 |
| 비공개 | `app.private-upload-dir`(환경 변수 `PRIVATE_UPLOAD_DIR`, 기본 `{공개 위치}-private`) | 구매 인증(`purchase-proof`), 협의 취소 증빙(`cancel-evidence`), 채팅 첨부(`chat-attachments`), 채널 확인 자료(`channel-evidence`), 프로필 사진(`member-avatars`) | 권한을 확인하는 API로만 |
| 캠페인 비공개 이미지 | `{공개 위치}-campaign-private`(기존) | 공개 전 캠페인 이미지 자산 | 권한을 확인하는 API로만 |

- 두 위치가 같거나 한쪽이 다른 쪽 안에 있으면 서버가 뜨지 않습니다(`UploadLocations`). 설정 실수로 비공개 파일이 공개 주소에 다시 연결되는 일을 막습니다.
- `/uploads`에는 공개 폴더 세 개만 연결합니다(`WebMvcConfig.PUBLIC_FOLDERS`). 위치 분리와 함께 이중으로 막습니다.
- 이미지는 실제 내용을 읽어 JPEG·PNG인지 확인하고 다시 저장합니다(기존).

| 비공개 파일 | 내려가는 API | 볼 수 있는 사람 |
|---|---|---|
| 구매 인증 사진 | `GET /api/campaigns/applications/{id}/purchase-proof/evidence` | 본인, 해당 업체, 운영자 |
| 협의 취소 증빙 | `GET /api/cancel-requests/{id}/evidence` | 본인, 해당 업체, 운영자 |
| 채팅 첨부 | `GET /api/chats/{roomId}/attachments/{messageId}` | 그 대화방 참여자 |
| 채널 확인 자료 | `GET /api/admin/channel-verifications/{id}/evidence` | 운영자 |
| 프로필 사진 | `GET /api/members/{id}/avatar?v={버전}` | 공개(사진을 올린 회원의 프로필 표시용) |

### 2026-11-04 비공개 파일 노출 문제와 조치

**문제.** 업로드 폴더 전체를 `/uploads/**` 주소로 공개해 두었고, 비공개 파일도 같은 폴더 아래에 저장했습니다. 그래서 구매 인증·취소 증빙·채팅 첨부·채널 확인 자료·프로필 사진이 주소만 알면 로그인 없이 열렸습니다(실제 서버에서 200 확인). 파일 이름은 무작위라 추측은 어렵지만, 주소가 한 번 새면 막을 방법이 없었습니다. 서버는 로컬에서만 돌았고 배포한 적이 없어 외부 노출은 없었습니다.

**1차 조치(백엔드 b07b318).** `/uploads`에는 공개 폴더 세 개만 연결하고, 로그인 없이 허용하는 주소도 그 세 폴더로 줄였습니다. 나머지 `/uploads` 주소는 401입니다.

**2차 조치(위치 분리).** 설정이 다시 `/uploads/**`로 바뀌어도 비공개 파일이 열리지 않도록 저장 위치 자체를 나눴습니다.
- 비공개 저장소 다섯 개(`PurchaseProofStorage`, `CancelEvidenceStorage`, `ChatAttachmentStorage`, `ChannelEvidenceStorage`, `MemberAvatarStorage`)가 `app.private-upload-dir` 아래에 저장합니다. 데이터베이스에는 파일 이름(키)만 있어 DB 변경은 없습니다.
- 예전 파일 옮기기: 서버가 뜰 때 `UploadLocations`가 공개 위치에 남은 다섯 폴더의 파일을 같은 상대 경로로 비공개 위치에 옮깁니다. 지우지 않고 옮기기만 하며, 같은 이름이 이미 있으면 덮어쓰지 않고 남겨 둡니다. 로그에는 개수만 남기고 파일 이름은 남기지 않습니다. 옮긴 뒤 비어 있는 예전 폴더는 그대로 두며 `/uploads`에 연결되지 않습니다.

**로컬 통합 환경에서 옮긴 결과(2026-11-04 00:56).**

| 폴더 | 옮기기 전(공개 위치) | 옮긴 뒤(비공개 위치) |
|---|---|---|
| purchase-proof | 10 | 10 |
| cancel-evidence | 5 | 5 |
| chat-attachments | 28 | 28 |
| channel-evidence | 1 | 1 |
| member-avatars | 1 | 1 |

- 서버 로그: "예전 비공개 업로드 파일을 비공개 위치로 옮겼습니다: 45건, 같은 이름이 있어 남겨 둔 파일 0건"
- 옮기기 전후 45개 파일의 경로와 SHA-256 값을 비교해 모두 일치했습니다.

**검증.**
- 백엔드 테스트 198개 통과. `UploadLocationsTest`: 겹치는 위치 거절, 예전 파일 옮기기, 같은 이름은 덮어쓰지 않음, 공개 이미지는 그대로. `SecurityConfigTest`: 비공개 폴더 다섯 곳 `/uploads` 401.
- 화면·API 점검: 구매 인증 사진 표시(purchase-review-check 24/24), 채팅 첨부(chat-extras-check 11/11), 협의 취소 증빙(participation-change-check 30/30), 남의 자료 접근(access-control-check 51/51), 채널 확인 자료(운영자 API 200, 이미지), 프로필 사진(API 200, 이미지).

**배포·다른 PC에 반영할 때.**
- 따로 할 일은 없습니다. 새 버전 서버를 띄우면 예전 파일이 자동으로 옮겨집니다.
- 배포 환경에서는 `UPLOAD_DIR`, `PRIVATE_UPLOAD_DIR`을 서로 다른 영구 저장 폴더로 지정합니다(`scripts/production.env.example`).

**되돌리기.** 이전 버전으로 돌아가야 하면, 비공개 위치의 다섯 폴더를 공개 위치 아래 같은 이름으로 다시 옮긴 뒤 이전 버전을 실행합니다. 다만 이전 버전은 비공개 파일이 주소로 열리는 문제가 있으므로 권하지 않습니다.

## 5. 남의 자료 접근(ID 바꿔 넣기)

`qa/e2e/access-control-check.mjs`(51항목)로 확인합니다.

- 다른 참여자가 남의 신청 수정·철회·취소·연장 요청, 배송·방문·구매 인증 보기, 콘텐츠 제출, 채팅 읽기·쓰기·검색 → 404
- 다른 업체가 남의 캠페인 참여자 상세·선정 편집·평가·패널티·연장 처리, 배송 목록·엑셀, 마감, 연락처, 표시 정보, 신청서 양식 → 403·404
- 일반 회원·업체가 운영자 API → 403
- 남의 배송지 고치기·지우기, 남의 알림 읽음 처리 → 404, 남의 글 고치기·지우기 → 403
- 점검 뒤 주인 자료(신청 상태·채팅·배송지)가 그대로인지 확인

## 6. 서버가 외부 페이지를 읽는 기능(소개글 인증 코드)

소개글 인증 코드는 서버가 SNS 공개 정보를 읽어야 해서 다음으로 제한합니다(`HttpChannelBioReader`, `ChannelBioCodeService`).

- 읽는 곳은 두 곳뿐: 네이버 블로그 공개 RSS(`rss.blog.naver.com/{아이디}.xml`), 메타 Graph API Business Discovery(`graph.facebook.com`). 화면(HTML)을 긁어 오지 않습니다.
- 회원이 입력한 주소를 그대로 열지 않습니다. 주소는 서비스 형식(`blog.naver.com/아이디`, `instagram.com/아이디`)만 받고, 검사한 아이디로 서버가 요청 주소를 만듭니다(다른 호스트·내부 주소 요청 불가).
- 연결 3초·응답 5초만 기다리고, 실패하면 '지금 확인할 수 없음'으로 처리합니다(인스타그램은 운영자 확인으로 넘김).
- 남용 제한: 한 코드로 확인 10번(실패도 셈), 코드 발급은 회원·서비스별 하루 5번.
- 인스타그램 조회 토큰(`INSTAGRAM_DISCOVERY_TOKEN`)은 환경 변수로만 받고 로그·응답에 남기지 않습니다. 로그에는 상태 코드와 예외 종류만 남깁니다.

## 남은 항목

- 배포 때 시연·점검 계정(demo-*, 시연 데이터 계정) 비밀번호를 로컬과 다른 새 값으로 정하기(위 결정)

- refresh 토큰 보관 위치(지금은 브라우저 저장소). 배포 도메인이 정해지면 HttpOnly 쿠키로 옮길지 결정
- 의존성 취약점 알림(Dependabot), CI에서 같은 비밀값 검사 — CI 작업 때 함께
- 로그에 토큰·연락처가 남지 않는지 운영 로그 설정 점검, DB 계정 권한·백업 정책 — 배포 작업 때 함께
- 서버를 여러 대로 늘리면 로그인 실패 기록을 Redis 같은 공용 저장소로 옮기기

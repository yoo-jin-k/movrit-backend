# 회원 프로필 사진 API 계약

## 응답 필드

헤더와 마이페이지가 별도 조회 없이 같은 사진을 표시하도록 다음 응답에 `avatarUrl`을 추가합니다.

- `GET /api/members/me`
- `PUT /api/members/me`
- `GET /api/auth/me`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- 회원가입 및 관리자 회원 응답

사진이 없으면 `avatarUrl`은 `null`입니다. 사진이 있으면 `/api/members/{memberId}/avatar?v={version}` 형식이며, 교체할 때마다 `version`이 바뀌어 브라우저 캐시가 이전 사진을 계속 사용하지 않습니다.

## 등록·교체·삭제·조회

- `PUT /api/members/me/avatar`: `multipart/form-data`의 `file`로 본인 사진을 등록하거나 교체하고 변경된 회원 응답을 반환합니다.
- `DELETE /api/members/me/avatar`: 본인 사진을 삭제하고 `204 No Content`를 반환합니다. 사진이 없어도 같은 결과를 반환합니다.
- `GET /api/members/{memberId}/avatar?v={version}`: 재인코딩된 공개 표시본을 반환합니다. 사진이 없거나 탈퇴한 회원이면 `404 Not Found`입니다.

업로드와 삭제의 회원 ID는 요청 본문이나 경로가 아니라 인증된 JWT에서만 가져옵니다. 타인의 사진을 수정하는 API는 제공하지 않습니다.

## 파일 정책

- 허용 입력: 실제 JPEG 또는 PNG
- 최대 전송 크기: 5MiB
- 최대 크기: 각 변 8,192px, 총 25,000,000픽셀
- 저장 표시본: EXIF 방향 반영 후 긴 변이 최대 1,024px가 되도록 비율 유지 축소
- 요청 MIME과 실제 디코더 형식이 다르면 `400 Bad Request`
- SVG, WebP, 애니메이션 PNG, 실행 파일, 손상된 이미지, 가짜 MIME은 거부
- 원본 파일명은 저장명과 경로 결정에 사용하지 않음
- 원본 바이트를 그대로 제공하지 않고 새 픽셀 데이터로 JPEG 또는 PNG 재인코딩
- EXIF 방향은 픽셀에 반영하고 GPS·텍스트·EXIF 등 메타데이터는 제거

저장 위치는 기존 캠페인·상품 파일과 분리된 `member-avatars` 하위 디렉터리이며 저장 키는 서버가 생성한 UUID만 사용합니다. 조회 시 DB의 현재 저장 키만 해석하고 정규화된 루트 밖의 경로는 거부합니다.

## 교체와 장애 처리

회원 행 잠금으로 같은 회원의 동시 교체·삭제를 직렬화합니다. 새 표시본을 먼저 별도 키로 준비하고 DB 커밋이 성공한 뒤에만 이전 파일을 삭제합니다. DB 저장이나 커밋이 실패하면 새 파일을 정리하고 기존 DB 참조와 기존 사진을 유지합니다.

## 캐시와 응답 보안

사진 응답은 삭제·교체 직후 브라우저가 이전 표시본을 계속 사용하지 않도록 `Cache-Control: no-cache, no-store, must-revalidate`, `X-Content-Type-Options: nosniff`를 사용합니다. URL의 버전이 현재 DB 버전과 다르면 새 사진을 대신 반환하지 않고 `404 Not Found`로 처리합니다. 현재 회원 응답에서 새 URL 또는 `null`을 받은 화면은 그 값을 기준으로 표시를 갱신합니다.

## 계정 메뉴에서 현재 연결 가능한 기능

- 회원·닉네임: `GET/PUT /api/members/me`
- 프로필 사진: 이 문서의 API
- 비밀번호: `GET /api/members/me/password-capability`, `PATCH /api/members/me/password`
- 주소록: `/api/members/me/addresses`
- 로그인 수단 연결: `/api/auth/social/accounts` 및 공급자별 연결 URL. Instagram·YouTube 같은 활동 채널 또는 미디어 연동 API가 아닙니다.
- 알림: `/api/notifications`, `/api/notifications/unread-count`
- 크리에이터 프로필: `/api/creators/me`
- 판매자 프로필: `/api/sellers/me`
- 내 캠페인 신청: `/api/campaigns/applications/mine`

존재하지 않는 찜·쿠폰·포인트·리뷰 API는 임의로 만들거나 연결하지 않습니다.

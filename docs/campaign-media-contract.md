# 캠페인 소유 이미지·원자 표시정보 계약

상태: 로컬 코드 구현 대상 계약입니다. 새 미디어 테이블 SQL 적용·서버 반영 전에는 호출하지 않습니다. 기존 프리오더 마이그레이션과 무관합니다.

## 자산

- `POST /api/campaign-image-assets` — 승인 업체 JWT, multipart `file`. JPG/PNG/WebP 5MiB, 실제 디코딩·8192px/25MP·방향/메타데이터 검증을 재사용합니다.
- 응답201: `{assetId,state,contentType,byteSize,width,height,previewPath,publicUrl}`. assetId는 UUID 문자열입니다. 미연결은 `READY_UNATTACHED`, publicUrl=null입니다.
- `GET /api/campaign-image-assets/{assetId}/preview` — 현재 승인된 소유 업체만 200 바이너리, 다른 소유자/없는 자산404. `Authorization` 헤더로 fetch 후 blob URL을 사용합니다. URL 쿼리에 토큰을 넣지 않습니다.
- 저장 위치는 공개 uploads의 **외부 형제 private 디렉터리**이며 `/uploads/**`로 접근할 수 없습니다. 이미지 원본은 보관하지 않고 정규화된 표시본만 저장합니다.
- `GET /api/campaign-image-assets/{assetId}/content` — 연결 캠페인이 공개(캠페인 존재·연결 프리오더/상품 미삭제; open은 모집 상태이므로 마감 후에도 공개 상세는 유지)이고 현재 대표/상세에 포함된 경우만 공개 바이너리. 미연결·비공개·해제된 자산404, 응답은 no-store입니다.

## configured 확장

기존 경로 `POST /api/campaigns/configured`, `PATCH /api/campaigns/configured/{id}`를 사용합니다. 기존 campaign/applicationForm/requirementMode/requirement 필드는 유지합니다.

추가 요청:

```json
{
  "expectedPresentationVersion": 0,
  "presentation": {
    "representativeAssetId": "UUID",
    "detailImages": [{"assetId":"UUID","altText":"선택 설명"}],
    "visitLocation": {"venueName":"공개 사업장", "postalCode":null, "address1":"기본 주소", "address2":null}
  }
}
```

- 신규 configured 생성은 presentation 및 대표 자산 필수입니다. 생성에 expectedPresentationVersion은 보내지 않습니다.
- 수정: presentation 생략 시 기존 표시정보 유지. 객체 안의 visitLocation 생략은 현재 활성 주소 유지, 명시적 null은 배송 전용 전환 시 비활성 보존, 객체는 신규 설정/변경입니다. Jackson setter의 제공 여부 플래그로 생략과 null을 실제 구분합니다. 대표/상세 참조는 전체 목록이며 부분 저장하지 않습니다. 보내면 expectedPresentationVersion 필수이며 최초 레거시 전환 전 버전은0입니다. 상세배열은 필수·0~5개·배열순서가 저장순서입니다. 대표와 상세 전체에서 자산ID 중복은400입니다.
- 설명은 공백 정규화 후 최대200 Unicode code point입니다. 장소명100, 주소1/2 각255, 우편번호는 선택(null) 또는 국내 숫자5자리입니다. VISIT 포함 신규/장소 신규 설정은 장소명·주소1 필수, 배송전용 전환 시 기존 활성주소가 있다면 visitLocation=null을 명시해야 합니다. 원래 주소가 없는 배송전용은 생략 가능합니다.
- 레거시 방문 캠페인에서 이미지 단독 수정 시 기존 visitGuide만 있어도 구조화 주소를 소급 필수로 요구하지 않습니다. 주소 변경·배송에서 방문 추가·재방문 활성화는 전체 장소를 명시적으로 확인합니다. 기존 구조화 주소가 있어도 이미지 단독 수정은 visitLocation 생략으로 현재 주소를 유지합니다.
- 배송전용 전환은 활성주소를 숨기고 이전주소를 소유자용 retainedVisitLocation으로 보존합니다. 다시 VISIT이면 명시적으로 전체주소를 제출합니다. 신청 이력이 있는 폼의 참여유형 변경은409입니다.
- 본문/일정, 폼, 조건, 표시정보 및 자산참조는 같은 트랜잭션입니다. 어느 검증이든 실패하면 모두 롤백합니다. 업로드 자체는 별도이며 실패한 저장의 미연결 자산은 비공개 상태로 남습니다.
- 버전충돌409의 detail: `이미지 또는 방문 정보가 변경되었습니다. 최신 내용을 확인해 주세요.` 같은 응답으로 자동 재시도하지 않습니다. 최신 관리 조회 후 사용자가 다시 저장합니다.
- 기존 대표가 없는 레거시 캠페인은 표시정보 수정 시 대표 null 유지 가능하나, 대표를 추가한 이후에는 null 제거할 수 없습니다. 신규 캠페인은 항상 대표 필수입니다.
- 새 자산 기반 표시정보가 있는 캠페인은 기존 버전없는 presentation 쓰기 API로 수정할 수 없으며409입니다. 기존 레거시 캠페인의 구 API는 유지합니다.

## 응답/조회

configured 응답에 `presentation`을 추가합니다. `GET /api/campaigns/{id}/presentation/manage`는 동일 관리 객체를 반환합니다.

관리 객체: `{version,representativeImage,detailImages,visitLocation,retainedVisitLocation,legacyRepresentativeImageUrl,legacyDetailImageUrl,legacyVisitGuide}`.
각 자산은 `{assetId,previewPath,publicUrl,altText}`입니다. 대표 altText=null입니다. 미디어 전환 전 version=0이며 레거시URL은 별도 필드에 보존합니다. 기존URL을 자산ID로 자동 변환하지 않습니다.

`GET /api/campaigns/{id}/presentation/public`는 공개 캠페인만 반환하며 `{representativeImageUrl,detailImages:[{imageUrl,altText}],visitLocation}`입니다. 소유자ID·preview·비활성주소는 없습니다. 배송전용에서는 주소가 항상null입니다. 기존 `/presentation` 읽기와 공개 본문도 같은 공개자격 및 배송전용 주소·visitGuide 숨김을 적용합니다. 신규 프론트는 위 public 계약을 사용합니다.

## 배포 전 경계

새 미디어 SQL은 2026-10-18 PM 승인 및 정식 권한 검토 후 로컬 movrit_integration에만 적용했습니다. 적용 전 두 테이블이 없음을 확인했고 신규 테이블 2개·인덱스를 단일 트랜잭션으로 생성했습니다. 기존 업무 행은 변경하지 않았으며 다른 환경에는 미적용입니다. 코드·단위테스트 결과를 실제 DB원자성·파일인가 통합QA 성공으로 보고하지 않습니다. 기존공개 uploads의 레거시 파일을 소급비공개로 바꾸지 않습니다. 파일 미사용 정리/참여시점 주소 스냅샷·내부알림은 별도 잔여이며 완료로 표시하지 않습니다.

## 로컬 검증 구분

새 DTO의 주소 생략/null 구분은 실제 Jackson 역직렬화로 검증합니다. 구 `/presentation`은 새 자산 전환 후 최신 표시 목록을 반환하며 레거시 숫자 이미지 id는 null입니다. 새 편집은 UUID assetId를 주는 관리 API만 사용합니다. 기존 데이터의 자유 주소는 이미지 수정/배송 전환으로 삭제하지 않고 공개 응답에서 숨깁니다.

트랜잭션 검수는 Spring 인터셉터의 늦은 미디어 실패 롤백 신호와 사전 충돌 시 기존 쓰기 미호출을 검증하는 단위 범위입니다. 실제 DB 커밋/롤백·병렬 경쟁·HTTP 인증·미연결 파일 다운로드 검수는 SQL 적용 후 별도 수행해야 합니다.

## 로컬 검증 인계

미디어 관련 6개 테스트 클래스의 31개 테스트가 통과했습니다. Spring 트랜잭션 인터셉터의 후반 실패 rollback 요청과 stale version 앞단 차단을 포함하지만 실제 PostgreSQL 데이터 롤백·동시성·HTTP 인가 통합 검증을 대체하지 않습니다. 프리오더 승인 대기 매핑과 마이그레이션은 포함하지 않습니다. 사용자 수정 CollaborationController/CollaborationDtos는 이번 커밋 대상에서 제외합니다.

## GAP08 주소 증빙 추가 계약

주소 변경은 정규화된 장소명·우편번호·주소1·주소2의 값 비교로 판단합니다. 이미지 순서/설명만 변경하거나 같은 주소를 다시 제출하면 이력과 알림을 생성하지 않습니다. 신규 캠페인의 최초 주소는 변경 이력 대상이 아니며, 기존 캠페인에 새 주소를 설정하는 경우는 대상입니다. 주소 활성 해제도 전후 증빙을 보존합니다. 구 주소 저장 API는 자유 문자열 전후 원문을 별도로 남기며 구조화 주소로 추론하지 않습니다.

변경 이력에는 캠페인·수정 회원·표시 버전·시각·변경 전후 주소를 추가만 합니다. 새 VISIT 신청에는 당시 구조화 주소·visitGuide·레거시 원문·표시 버전을 별도 applicationId 키의 스냅샷으로 보존합니다. 기존 신청, 기존 조건 snapshot, 회원 지표 기록은 덮어쓰거나 소급 채우지 않습니다. 주소가 없는 레거시 신청은 주소 null/버전0으로 당시 상태만 기록합니다. 배송 신청에는 방문 증빙을 만들지 않습니다.

주소 변경과 신규 신청은 동일 캠페인 행 잠금으로 순서를 정합니다. configured 저장 트랜잭션에 이력·기존 내부 DB 알림을 포함하며 실패하면 함께 롤백되어야 합니다. 진행 중인 VISIT 신청(SUBMITTED, SELECTED, ACTIVE, CONTENT_SUBMITTED, REVISION_REQUESTED)의 회원에게 1인 1회 알림을 발행합니다. 배송·거절·취소·완료 신청은 제외합니다. 알림에는 주소 원문을 넣지 않고 캠페인 상세 확인을 안내합니다. 자동 동의·자동 취소·외부 이메일·외부 푸시는 수행하지 않습니다.

인증된 조회 API:
- GET `/api/campaign-applications/{applicationId}/visit-snapshot`: 신청 본인 또는 승인된 해당 업체만 조회합니다. 기존 증빙이 없으면 `available:false`이며 현재 주소로 대체하지 않습니다.
- GET `/api/campaigns/{campaignId}/visit-history`: 승인된 해당 업체만 조회합니다. 전후 구조화 주소와 레거시 원문을 구분합니다. 수정 회원 ID는 응답에 노출하지 않습니다.

운영자 전용 열람 경로는 이번 최소 구현에 추가하지 않았습니다. 공개 응답에는 신청 당시 증빙과 변경 이력을 넣지 않습니다. 신규 SQL `feature-campaign-visit-evidence.sql`은 새 사용자 명시 승인과 정식 검토 후 2026-10-18 로컬 movrit_integration에만 적용했습니다. 기존 신청 행은 변경하지 않았으며 다른 환경은 미적용입니다. 실제 DB 원자성·동시성·HTTP 접근 권한과 프론트 증빙 조회 연결은 후속 검수 대상입니다. 562ffff의 기존 미디어 API 요청·응답은 유지합니다.

# 브랜드 참여자 포트폴리오 API 계약

## 목록

`GET /api/campaigns/applications/seller`

기존 신청 응답에 `completedCampaignCount`, `creatorChannelUrl`, `creatorMetrics`를 추가합니다. 채널과 지표는 신청자별 일괄 조회하며 신청 행마다 상세 API를 다시 호출하지 않습니다. 완료 횟수는 신청자 전체 이력 중 아래 조건을 모두 충족한 신청 수입니다.

- `campaign_application.status = COMPLETED`
- 연결된 단일 콘텐츠 제출의 `status = APPROVED`
- 신청 ID별 한 번만 집계하므로 수정 제출 횟수와 관계없이 중복되지 않습니다.

선정, 참여 확정, 콘텐츠 제출 또는 브랜드 승인만으로는 완료 횟수에 포함하지 않습니다.

## 상세

`GET /api/campaigns/applications/{applicationId}/seller-detail`

승인된 업체가 소유한 캠페인의 신청만 조회할 수 있습니다. 다른 브랜드 신청은 `403 Forbidden`, 없는 신청은 `404 Not Found`입니다.

응답 필드:

- `profile`: 크리에이터 ID, 닉네임, 소개, 주 활동 카테고리, 채널 URL, 프로필 상태
- `metrics`: 채널 유형, 지표 유형과 표시명, 값, 집계 기간, 측정일, 검증 상태, 출처
- `applicationAnswers`: 현재 조회한 신청의 답변과 질문 라벨. 현재 신청 폼 버전이 신청 당시 버전과 같을 때만 라벨을 제공하며 아니면 `labelSource=UNAVAILABLE`
- `requirementEvaluation`: 신청 당시 저장한 조건 사본과 현재 지표를 비교한 결과
- `completedCampaignCount`: 완료 이력 수
- `completedCampaigns`: 캠페인명, 카테고리, 운영자 최종 승인 시각, 공개 콘텐츠 링크

현재 DB에는 지표 출처 컬럼이 없으므로 `source=UNKNOWN`으로 제공합니다. 검증 여부는 `verificationStatus`의 `UNVERIFIED`, `VERIFIED`, `REJECTED`로 별도 제공합니다. 조건에 집계 기간이 있으면 지표의 집계 기간도 같아야 충족됩니다.

조건 충족 여부는 신규 신청 가능 여부가 아닙니다. `evaluationBasis`는 `CURRENT_METRIC_AGAINST_APPLICATION_SNAPSHOT`이며, 신청 당시 저장된 조건 사본에 현재 검증 지표를 다시 비교합니다. 이미 신청했다는 이유로 조건 미충족 처리하지 않습니다. 조건 사본과 버전이 모두 없으면 `NO_REQUIREMENT_AT_APPLICATION`, 버전은 있지만 사본이 유실된 레거시 데이터는 `APPLICATION_SNAPSHOT_UNAVAILABLE`로 구분합니다.

프로필은 채널 닉네임과 회원 닉네임을 각각 제공하고 `nicknameConsistent`로 일치 여부를 표시합니다. 배송 연락처·주소, 다른 브랜드 캠페인의 신청 답변, 비공개 또는 최종 승인 전 콘텐츠 링크는 반환하지 않습니다. 공개 링크는 저장된 `http`·`https` 주소만 로컬 형식 검사하며 외부 사이트에 접속하지 않습니다.

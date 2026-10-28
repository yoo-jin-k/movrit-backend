# 내 캠페인 통합 검수 fixture

`docs/sql/seed-integration-campaign-workspace-qa.sql`은 기존 데모 캠페인의 일정과 신청 상태를 바꾸지 않고 콘텐츠 제출·수정 요청·재제출·브랜드 승인을 확인하기 위한 독립 데이터입니다.

- 참여자: `qa-workspace-user-integration@movrit.local`
- 소유 브랜드: `qa-workspace-owner-integration@movrit.local`
- 평문 비밀번호는 SQL에 저장하지 않으며 기존 로컬 QA 비밀번호 파일을 사용합니다.
- 캠페인과 신청 ID는 적용 후 DB 조회 결과를 사용합니다.
- 재실행해도 기존 콘텐츠 제출과 검수 상태를 초기화하지 않습니다.

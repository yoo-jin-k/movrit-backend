# 내 캠페인 작업공간 API

## 내 작업공간 조회

`GET /api/campaigns/applications/mine/workspaces`

로그인 회원 본인의 신청만 반환합니다. 각 항목은 다음 필드를 포함합니다.

- `application`: 신청 상태, 참여 방식, 본인이 입력한 배송 연락처, 답변과 체험 완료 시각
- `campaign`: 모집·선정·체험·콘텐츠 제출·결과 발표 일정과 미션·채널 안내·제출 요구사항
- `brandContact`: 소유 업체가 해당 캠페인에 명시적으로 등록한 공개 담당자 연락처. 미등록이면 `null`
- `content`: 최신 제출 URL, 검수 상태, 수정 의견, 버전과 채널별 링크. 제출 전이면 `null`

```json
{
  "application": { "id": 25, "status": "ACTIVE", "fulfillmentType": "DELIVERY" },
  "campaign": {
    "id": 20,
    "contentSubmissionEndAt": "2026-12-08T01:52:00",
    "submissionRequirement": "공개 게시물 URL과 필수 해시태그를 제출해 주세요."
  },
  "brandContact": { "name": "캠페인 담당자", "phone": "02-0000-0000" },
  "content": null
}
```

## 캠페인 공개 담당자 연락처 저장

`PUT /api/collaborations/campaigns/{campaignId}/contact`

캠페인 소유 업체만 호출할 수 있습니다. 회원·대표자 전화번호를 자동 공개하지 않습니다.

```json
{ "name": "캠페인 담당자", "phone": "02-0000-0000" }
```

소유 업체는 `GET /api/collaborations/campaigns/{campaignId}/contact`로 현재 값을 다시 조회합니다. 미등록이면 `null`, 다른 업체의 캠페인이면 `403`입니다.

- 이름은 100자, 전화번호는 30자까지이며 빈 값은 저장할 수 없습니다.
- 다시 `PUT`하면 즉시 최신 값으로 교체됩니다. 현재 계약은 빈 값으로 삭제하는 기능을 제공하지 않습니다.
- 신청자는 선정 전에는 연락처를 받지 않으며, 선정·진행·콘텐츠 검수 단계에서만 자신의 작업공간으로 조회합니다.

## 채널별 콘텐츠 제출·재제출

`POST /api/campaign-contents/applications/{applicationId}/links`

신청 당사자만 호출할 수 있습니다. 기존 정책에 따라 선정·참여 확정·체험 완료 후 제출할 수 있습니다. 최초 제출은 캠페인의 콘텐츠 제출 시작·종료 시각 안에서만 허용하며, 브랜드가 수정을 요청한 `REVISION_REQUESTED` 재제출은 마감 후에도 최신 `expectedVersion`으로 제출할 수 있습니다. 일정이 설정되지 않은 경우 서버가 날짜를 만들어 내지 않습니다.

채널 enum은 `INSTAGRAM`, `BLOG`, `YOUTUBE`, `CLIP`, `OTHER`이며 한 번에 최대 5개 URL을 제출할 수 있습니다. 중복 URL은 거부합니다.

- `INSTAGRAM`은 `instagram.com` 또는 `www.instagram.com`의 `/p/`, `/reel/`, `/tv/` 게시물 URL만 허용합니다.
- 다른 채널도 외부 공개 `http`·`https` URL만 허용합니다.
- `localhost`, 사설 IP, `.local`, 사용자 인증정보가 포함된 URL과 위험 스킴은 거부합니다.
- URL 검증 과정에서 외부 사이트를 호출하거나 게시물을 스크래핑하지 않습니다.
- 링크 목록은 비어 있을 수 없고 최대 5개이며 각 URL은 최대 1,000자입니다.
- 같은 채널 링크를 여러 개 보낼 수 있지만 완전히 같은 URL의 중복은 거부합니다.
- 재제출하면 이전 채널 링크 전체를 새 목록으로 교체합니다.
- 콘텐츠 일정은 서울 현지 시각의 `LocalDateTime` 계약을 따릅니다. 시작 또는 마감만 설정되면 설정된 경계만 검사하고, 둘 다 없으면 상태 정책만 적용합니다.
- 수정 요청을 받은 재제출은 시작 전·마감 후에도 허용됩니다.

```json
{
  "links": [
    { "channelType": "INSTAGRAM", "contentUrl": "https://www.instagram.com/p/example" },
    { "channelType": "BLOG", "contentUrl": "https://blog.example.com/post/1" }
  ],
  "note": "채널별 게시물을 제출합니다.",
  "expectedVersion": null
}
```

응답의 기존 `contentUrl`은 첫 번째 링크를 담아 하위 호환성을 유지하며, `links`에 전체 채널별 URL을 반환합니다. 브랜드는 기존 `/api/campaign-contents/seller`, 수정 요청·승인 API를 그대로 사용합니다.

브랜드 담당 연락처는 신청 접수 중에는 숨기고 `SELECTED` 이후에만 신청 당사자에게 제공합니다. `REJECTED`·`CANCELLED` 신청에도 공개하지 않습니다.

# MOVRIT ERD

실제 DB 스키마로 생성한 ERD입니다. `scripts/generate-erd.py` 로 다시 만들 수 있습니다.

- 테이블 72개, 외래 키 관계 120개
- `MOVRIT.dbml`: 전체 관계(dbdiagram.io에 붙여 넣기)

| 파일 | 도메인 | 테이블 |
|---|---|---|
| `01_회원_업체.mmd` | 회원·인증·업체 | member, member_role, member_social_account, member_address, member_channel_metric, refresh_token, email_verification_challenge, password_reset_token, member_channel_connection, seller_profile, brand, influencer_profile, influencer_social_account |
| `02_캠페인_신청.mmd` | 캠페인·신청·콘텐츠 | collaboration_campaign, campaign_application_form, campaign_application, campaign_eligibility_requirement, campaign_content_submission, campaign_content_link, campaign_favorite, campaign_content_metric, collaboration, referral_link, referral_click, attribution |
| `03_이미지_방문증빙.mmd` | 캠페인 이미지·방문 증빙 | campaign_detail_image, campaign_image_asset, campaign_media_state, campaign_visit_history, campaign_visit_snapshot, file_asset, file_attachment |
| `04_상품_프리오더_레거시.mmd` | 상품·프리오더(레거시) | product, product_variant, product_review, preorder, preorder_variant, preorder_sale_variant, preorder_history, preorder_interest, preorder_favorite |
| `05_주문_결제_보존.mmd` | 주문·결제·정산(레거시 보존) | orders, order_item, payment, refund, refund_item, shipment, shipment_item, settlement, settlement_participant |
| `06_운영_공통.mmd` | 알림·문의·감사·공지 | notice, notification, inquiry, audit_log |
| `07_참여_운영.mmd` | 배송·구매평·방문 예약·패널티·평가·참여 조정 | campaign_shipment, campaign_purchase_review, campaign_purchase_proof, campaign_visit_reservation, campaign_deadline_reminder, campaign_participant_rating, member_penalty, member_cancel_count, campaign_cancel_request, campaign_extension_request, campaign_period_extension, campaign_unselected_record |
| `08_소통_신고.mmd` | 채팅·커뮤니티·신고 | chat_room, chat_participant, chat_message, community_post, community_comment, content_report |

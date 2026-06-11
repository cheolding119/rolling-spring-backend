# OpenMat Comments Plan

작성일: 2026-06-11

## 1. 목적

- 오픈매트 상세 화면에 댓글과 1단계 대댓글 기능을 추가한다.
- 댓글/대댓글의 기본 UX와 moderation 규칙은 현재 훈련일지 댓글 구조를 최대한 재사용한다.
- 신고, 차단, 수정, 삭제, 관리자 운영 흐름까지 이번 기획 범위에 포함한다.

## 2. 사용자 문제

- 참가자는 오픈매트 상세에서 일정 문의, 준비물 질문, 참가 전 확인 사항을 빠르게 남기고 답변받고 싶다.
- 호스트는 DM이나 외부 채널로 흩어지는 질문을 상세 화면 안에서 모아 관리하고 싶다.
- 운영자는 신고 누적 댓글과 차단 관계를 기존 정책 안에서 처리하고 싶다.

## 3. 출시 권장안

### 3.1 지금 출시할 범위

오픈매트 댓글 MVP는 "상세 화면 안에서 질문과 답변을 주고받고, 기본 moderation을 수행할 수 있는 기능"에 집중한다.

| 영역 | 기능 | 포함 기준 |
| --- | --- | --- |
| 조회 | 댓글 목록 조회 | 오픈매트 상세에서 진입, 원댓글 + 1단계 대댓글 |
| 작성 | 댓글 작성 | 인증 사용자만 가능 |
| 작성 | 대댓글 작성 | 인증 사용자만 가능, 1단계까지만 허용 |
| 수정 | 댓글 수정 | 작성자 본인만 가능 |
| 삭제 | 댓글 삭제 | 작성자, 오픈매트 호스트, 관리자 가능 |
| 신고 | 댓글 신고 | 인증 사용자만 가능, 중복 신고 방지 |
| 차단 | 댓글 노출 필터 | 로그인 조회자 기준 상호 차단 관계 숨김 |
| 알림 | 댓글/대댓글 알림 | 새 댓글은 호스트, 대댓글은 상위 댓글 작성자에게 저장 + 푸시 시도 |
| 운영 | 관리자 신고 목록/상세/상태 변경 | 훈련일지 댓글 신고 운영 패턴 재사용 |

### 3.2 다음 출시 범위

| 영역 | 기능 | 뒤로 미루는 이유 |
| --- | --- | --- |
| 반응 | 댓글 좋아요 | MVP 핵심 가치보다 우선순위가 낮음 |
| 정렬 | 인기순/최신순 정렬 | 초기에는 시간순만으로 충분 |
| 알림 | 멘션 알림 | 파싱, UX, 스팸 제어 복잡도 증가 |
| UX | 댓글 고정 | 호스트 운영 가치 검증 후 판단 |
| 성능 | 댓글 페이징 | 초기에는 비페이징 구조로 단순화 |

### 3.3 제외 범위

- 익명 작성
- 2단계 이상 대댓글
- 댓글 이미지 첨부
- 댓글 좋아요
- 호스트 전용 댓글 잠금
- 신고 접수만으로 자동 사용자 제재

## 4. 핵심 정책

### 4.1 인증과 노출

- `GET /api/v1/open-mats/{id}/comments`는 비회원도 호출할 수 있다.
- 댓글 작성, 수정, 삭제, 신고는 인증이 필요하다.
- 로그인 사용자가 조회하면 댓글 목록에 차단 관계를 반영한다.
- 비회원 조회는 차단 필터 없이 공개 댓글을 반환한다.

### 4.2 상세 접근과 댓글 접근의 관계

- 댓글 목록 조회 가능 여부는 먼저 오픈매트 상세 접근 가능 여부를 따른다.
- 로그인 사용자가 차단한 호스트의 오픈매트는 기존 정책대로 상세 자체가 `NOT_FOUND`이므로 댓글도 조회할 수 없다.
- 삭제된 오픈매트를 제한적으로 볼 수 있는 사용자도 기존 댓글은 읽을 수 있다.
- 삭제된 오픈매트에는 새 댓글 작성과 신고 접수를 허용하지 않는다.

### 4.3 댓글/대댓글 정책

- 원댓글과 1단계 대댓글까지만 허용한다.
- 대댓글에는 다시 대댓글을 달 수 없다.
- 삭제된 댓글에는 새 대댓글을 달 수 없다.
- 댓글은 `RECRUITING`, `CLOSED`, `FINISHED` 상태 오픈매트에서 읽을 수 있다.
- 오픈매트가 삭제되지 않았다면 종료된 오픈매트에도 댓글 작성은 허용한다.
- 댓글 본문은 trim 후 저장한다.
- 공백만 있는 댓글은 `VALIDATION_ERROR`다.

### 4.4 수정과 삭제 정책

- 댓글 작성자는 본인 댓글만 수정할 수 있다.
- 댓글 삭제는 댓글 작성자, 오픈매트 호스트, 관리자만 가능하다.
- 삭제는 soft delete를 기본으로 한다.
- 삭제된 원댓글에 대댓글이 남아 있으면 placeholder row를 유지한다.

### 4.5 차단 정책

- 조회자와 댓글 작성자 사이에 차단 관계가 있으면 댓글을 숨긴다.
- 차단된 원댓글이 숨겨지면 하위 대댓글도 함께 숨긴다.
- 차단 관계가 있는 사용자에게는 새 댓글을 작성할 수 없다.
- 차단 관계가 있는 상위 댓글 작성자에게는 대댓글을 작성할 수 없다.

### 4.6 신고 정책

- 자기 댓글은 신고할 수 없다.
- 동일 사용자의 동일 댓글 중복 신고는 허용하지 않는다.
- `ReportReason.OTHER`일 때 `customReason`은 필수다.
- 이미 삭제된 댓글은 새 신고를 받을 수 없다.
- 댓글 또는 대댓글 신고가 3회 이상 누적되면 해당 댓글은 자동 soft delete 된다.
- 신고 누적으로 원댓글이 자동 삭제되면 하위 대댓글도 함께 soft delete 한다.

### 4.7 알림 정책

- 새 원댓글이 달리면 오픈매트 호스트에게 `OPEN_MAT_COMMENT_CREATED` 알림을 저장하고 푸시 발송을 시도한다.
- 내 댓글에 대댓글이 달리면 상위 댓글 작성자에게 `OPEN_MAT_COMMENT_REPLY_CREATED` 알림을 저장하고 푸시 발송을 시도한다.
- 자기 자신의 액션에는 자기 알림을 만들지 않는다.
- 차단 관계가 생긴 사용자 간에는 새 댓글/대댓글 알림을 발행하지 않는다.
- 알림 `targetId`는 `openMatId`를 사용한다.
- 알림 `route`는 기존 오픈매트 알림과 같은 `/openmat/detail`을 사용한다.

## 5. API 초안

### 5.1 댓글 목록 조회

`GET /api/v1/open-mats/{id}/comments`

- 인증: 선택
- Response data: `List<OpenMatCommentResponse>`

### 5.2 댓글 작성

`POST /api/v1/open-mats/{id}/comments`

- 인증: 필요
- Response data: `OpenMatCommentResponse`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `content` | `String` | O | 댓글 본문 |
| `parentCommentId` | `Long?` | - | 대댓글이면 상위 댓글 ID |

### 5.3 댓글 수정

`PATCH /api/v1/open-mats/comments/{commentId}`

- 인증: 필요
- Response data: `OpenMatCommentResponse`

### 5.4 댓글 삭제

`DELETE /api/v1/open-mats/comments/{commentId}`

- 인증: 필요
- Response data: `null`

### 5.5 댓글 신고

`POST /api/v1/open-mats/comments/{commentId}/report`

- 인증: 필요
- Response data: `null`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `ReportReason` | O | 신고 사유 |
| `customReason` | `String?` | - | 기타 신고 사유 |

### 5.6 관리자 댓글 신고 목록 조회

`GET /api/v1/admin/open-mats/comments/reports`

- 인증: 필요
- 권한: `ADMIN`
- Response data: `Page<OpenMatCommentReportAdminResponse>`

### 5.7 관리자 댓글 신고 상세 조회

`GET /api/v1/admin/open-mats/comments/reports/{id}`

- 인증: 필요
- 권한: `ADMIN`
- Response data: `OpenMatCommentReportAdminResponse`

### 5.8 관리자 댓글 신고 상태 변경

`PATCH /api/v1/admin/open-mats/comments/reports/{id}/status`

- 인증: 필요
- 권한: `ADMIN`
- Response data: `OpenMatCommentReportAdminResponse`

## 6. 응답 모델 초안

### 6.1 `OpenMatCommentResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 댓글 ID |
| `openMatId` | `Long` | 대상 오픈매트 ID |
| `parentCommentId` | `Long?` | 상위 댓글 ID |
| `authorUserId` | `Long` | 작성자 ID |
| `authorNickname` | `String` | 작성자 닉네임 |
| `content` | `String?` | 삭제된 댓글이면 null |
| `deleted` | `Boolean` | 삭제 여부 |
| `editableByMe` | `Boolean` | 현재 로그인 사용자의 수정 가능 여부 |
| `deletableByMe` | `Boolean` | 현재 로그인 사용자의 삭제 가능 여부 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |
| `replies` | `List<OpenMatCommentResponse>` | 1단계 대댓글 목록 |

### 6.2 `OpenMatCommentReportAdminResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 신고 ID |
| `commentId` | `Long` | 댓글 ID |
| `openMatId` | `Long` | 오픈매트 ID |
| `openMatTitle` | `String` | 오픈매트 제목 |
| `parentCommentId` | `Long?` | 상위 댓글 ID |
| `commentContent` | `String?` | 댓글 본문 |
| `commentDeleted` | `Boolean` | 댓글 삭제 여부 |
| `commentAuthorUserId` | `Long` | 댓글 작성자 ID |
| `commentAuthorNickname` | `String` | 댓글 작성자 닉네임 |
| `reporterUserId` | `Long` | 신고자 ID |
| `reporterNickname` | `String` | 신고자 닉네임 |
| `reason` | `ReportReason` | 신고 사유 |
| `customReason` | `String?` | 기타 신고 사유 |
| `status` | `ReportStatus` | 처리 상태 |
| `processedByUserId` | `Long?` | 처리 관리자 ID |
| `processedAt` | `LocalDateTime?` | 처리 시각 |
| `processingMemo` | `String?` | 처리 메모 |
| `finalAction` | `String?` | 최종 조치 메모 |
| `createdAt` | `LocalDateTime` | 신고 생성 시각 |
| `updatedAt` | `LocalDateTime` | 신고 수정 시각 |

## 7. 수용 기준

- 오픈매트 상세에서 원댓글과 1단계 대댓글을 생성, 수정, 삭제할 수 있다.
- 비회원은 댓글을 읽을 수 있지만 댓글 작성 시 `401 UNAUTHORIZED`를 받는다.
- 로그인 조회자는 차단 관계에 있는 댓글 작성자의 댓글을 보지 못한다.
- 댓글 작성자, 호스트, 관리자 외 사용자는 댓글 삭제 시 `403 FORBIDDEN`을 받는다.
- 동일 사용자는 같은 댓글을 두 번 신고할 수 없다.
- 신고 3회 누적 댓글은 soft delete 되고 목록에서 placeholder 형태로 보인다.
- 새 원댓글 작성 시 호스트에게, 새 대댓글 작성 시 상위 댓글 작성자에게 알림함 저장이 시도된다.
- 오픈매트 메인 조회/신청/참가자 관리 계약은 이번 기능 추가로 깨지지 않는다.

## 8. 테스트 관점

- 정상 경로: 원댓글 작성, 대댓글 작성, 호스트 삭제
- 실패 경로: 비회원 작성, 2단계 대댓글 시도, 자기 댓글 신고, 중복 신고
- 권한 경계: 작성자 수정, 호스트 삭제, 일반 사용자 삭제 거부
- 차단 경계: 차단 관계 댓글 숨김, 차단 관계 대댓글 작성 거부
- 운영 경계: 신고 3회 누적 자동 삭제, 관리자 신고 상태 변경
- 회귀 경계: 기존 `GET /api/v1/open-mats/{id}` 공개 조회와 오픈매트 신청/신고 흐름 유지

## 9. Phase별 체크리스트

### Phase 1. 범위와 출시 기준 확정

- [ ] 댓글 읽기는 비회원 허용, 쓰기 액션은 인증 필요라는 MVP 경계를 최종 확정한다.
- [ ] 이번 범위가 `댓글/대댓글 + 신고 + 차단 + 수정/삭제 + 관리자 운영`까지인지 product owner와 합의한다.
- [ ] `댓글 좋아요`, `멘션`, `댓글 고정`, `페이징`을 이번 출시 범위에서 제외하는 결정에 동의받는다.
- [ ] 성공 신호를 최소 3개로 고정한다.
- [ ] 출시 보류 조건을 명시한다.
출시 보류 예시: 차단 정책 합의 미완료, 관리자 운영 API 부재, 신고 자동 삭제 기준 미합의

### Phase 2. 계약과 도메인 모델 확정

- [ ] `GET /api/v1/open-mats/{id}/comments` 공개 조회 계약을 확정한다.
- [ ] `POST/PATCH/DELETE /api/v1/open-mats/comments*` 경로와 request/response 계약을 확정한다.
- [ ] `OpenMatCommentResponse`와 `OpenMatCommentReportAdminResponse` 필드 집합을 확정한다.
- [ ] `open_mat_comments`, `open_mat_comment_reports` 저장 모델과 unique/index 정책을 확정한다.
- [ ] 신고 3회 자동 soft delete 기준과 원댓글 삭제 시 대댓글 연쇄 삭제 범위를 확정한다.

### Phase 3. 사용자 상호작용 MVP

- [ ] 오픈매트 상세 화면에서 원댓글과 1단계 대댓글만 지원하는 UX 흐름을 고정한다.
- [ ] 비회원이 댓글 작성 시 로그인 유도 흐름이 필요한지 프론트와 합의한다.
- [ ] 종료된 오픈매트에도 댓글 작성을 허용할지 최종 결정한다.
- [ ] 삭제된 댓글 placeholder 노출 문구와 방식은 프론트가 결정하도록 계약 경계를 정리한다.
- [ ] `OpenMatResponse`는 그대로 두고 댓글 API를 별도 호출하는 구조로 확정한다.

### Phase 4. 안전장치와 운영 정책

- [ ] 조회자 기준 양방향 차단 정책을 댓글 조회/작성/알림에 동일 적용하는지 확정한다.
- [ ] 호스트 삭제 권한과 관리자 삭제 권한의 책임 경계를 정리한다.
- [ ] 자기 댓글 신고 불가, 중복 신고 불가, 삭제 댓글 신고 불가 규칙을 확정한다.
- [ ] 관리자 신고 목록/상세/상태 변경 API에 필요한 운영 메모 필드를 확정한다.
- [ ] 신고 상태 변경이 자동 사용자 제재로 이어지지 않는다는 운영 정책을 명시한다.

### Phase 5. 알림과 후속 행동 설계

- [ ] 새 원댓글 알림 수신자는 호스트 1명으로 고정한다.
- [ ] 대댓글 알림 수신자는 상위 댓글 작성자 1명으로 고정한다.
- [ ] 호스트와 상위 댓글 작성자가 같은 경우 중복 알림을 만들지 않는 정책을 확정한다.
- [ ] 알림 `targetId = openMatId`, `route = /openmat/detail` 계약을 확정한다.
- [ ] 알림 실패 시 알림함 저장 우선, 푸시 실패 허용 정책을 유지하는지 확인한다.

### Phase 6. 구현 전 검증 체크

- [x] 훈련일지 댓글 구조를 재사용할 수 있는 클래스/서비스 범위를 먼저 식별한다.
- [x] 커뮤니티/훈련일지 댓글 신고 운영과 enum/raw value가 충돌하지 않는지 확인한다.
- [x] 보안 설정에 관리자 전용 경로를 추가할 때 기존 공개 오픈매트 조회 경로가 영향받지 않는지 점검한다.
- [x] 차단 필터가 댓글 수 집계와 댓글 목록 구조에 모두 반영되는지 설계상 확인한다.
- [x] 삭제된 오픈매트 제한 조회 정책과 댓글 조회 정책이 충돌하지 않는지 확인한다.

### Phase 7. 테스트와 출시 판단

- [x] 서비스 테스트: 원댓글 작성, 대댓글 작성, 작성자 수정, 호스트 삭제를 검증한다.
- [x] 실패 테스트: 비회원 작성, 2단계 대댓글, 자기 댓글 신고, 중복 신고를 검증한다.
- [x] 권한 테스트: 작성자/호스트/관리자 외 삭제 불가를 검증한다.
- [x] 차단 테스트: 차단 관계 댓글 숨김, 차단 관계 대댓글 작성 차단, 알림 미발행을 검증한다.
- [x] 운영 테스트: 신고 3회 누적 soft delete, 관리자 신고 상태 변경을 검증한다.
- [x] 회귀 테스트: 기존 오픈매트 상세 조회, 신청, 신고, 참가자 관리가 깨지지 않는지 확인한다.
- [x] 테스트 미실행 항목과 운영 smoke test 필요 항목을 출시 판단 메모에 남긴다.

출시 판단 메모:

- 대상 테스트 기준으로 댓글/대댓글, 신고, 관리자 운영, 보안 경계는 검증 완료
- 운영 smoke test는 아직 미실행
- 비회원 둘러보기 공개 조회는 `Authorization` 헤더 없이 호출하는 계약을 유지해야 함
- Flyway `V42`~`V45` 적용 여부는 배포 환경에서 별도 확인 필요

## 10. Product Owner 확인 필요 항목

- [ ] 종료된 오픈매트에 신규 댓글 작성을 허용할지 결정 필요
- [ ] 비회원 읽기 허용을 유지할지, 로그인 사용자만 댓글을 보게 할지 결정 필요
- [ ] 신고 3회 자동 삭제 임계값을 그대로 쓸지 조정할지 결정 필요
- [ ] 대댓글 알림을 호스트에게도 추가 발송할지 결정 필요
- [ ] 댓글 페이징을 MVP에서 제외해도 되는지 결정 필요

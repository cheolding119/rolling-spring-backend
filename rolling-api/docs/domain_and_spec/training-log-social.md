# Training Log Social

- 훈련일지 친구 관계, 친구 열람, 좋아요, 댓글, 대댓글, 댓글 알림 도메인과 API 스펙을 관리한다.
- 원본 훈련 기록 모델과 개인 기록 API는 [training-log.md](training-log.md)를 따른다.
- 공통 응답, 인증, 사용자 차단, 알림함 API는 [shared/common-models.md](shared/common-models.md)를 따른다.
- 현재 문서는 `친구 기반 제한 공유 MVP` 기준이며, 공개 피드와 추천은 포함하지 않는다.
- 현재 문서는 Phase 1~6 구현 기준의 source of truth다.

## 1. 도메인 개요

Training Log Social은 개인 훈련일지를 `친구 관계 안에서만` 제한적으로 공유하고 상호작용할 수 있게 만드는 확장 도메인이다.

현재 범위:

- 친구 검색(`이름`으로 노출되는 `User.nickname` 기준)
- 친구 요청 전송, 수락, 거절, 삭제
- 기록별 공개 범위 `PRIVATE`, `FRIENDS`
- 친구의 훈련일지 목록/상세 조회
- 친구가 볼 수 있는 기록에 대한 좋아요
- 댓글과 1단계 대댓글
- 댓글/대댓글 생성 시 알림함 저장과 푸시 발송 시도
- 차단 관계 우선 적용

제외 범위:

- 공개 피드
- 추천/랭킹
- 멘션
- 댓글 좋아요
- 무제한 대댓글
- 좋아요 알림

## 2. 도메인 모델

### 2.1 `TrainingLogEntry` 확장

기존 [training-log.md](training-log.md)의 `TrainingLogEntry`에 아래 필드를 추가한다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `visibility` | `TrainingLogVisibility` | 기록 공개 범위 |

구현 메모:

- 기존 데이터의 기본값은 `PRIVATE`다.
- `FRIENDS` 기록만 친구가 조회할 수 있다.
- DB 컬럼은 `training_log_entries.visibility`를 사용하고 `varchar` enum raw value로 저장한다.

### 2.2 `FriendSearchResultResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `Long` | 검색 결과 사용자 ID |
| `nickname` | `String` | 앱에서 `이름`으로 노출하는 사용자 닉네임 |
| `affiliation` | `String?` | 소속 |
| `beltColor` | `BeltColor` | 현재 벨트 |

구현 메모:

- 친구 검색 결과는 `nickname`, `affiliation`, `beltColor`를 함께 노출한다.
- 현재 백엔드 사용자 엔티티의 source field는 `User.nickname`, `User.affiliation`, `User.beltColor`다.
- 소속이 없으면 `affiliation = null`로 반환하고, 화면 문구는 클라이언트가 결정한다.

### 2.3 `FriendRequestResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 친구 요청 ID |
| `senderUserId` | `Long` | 요청 보낸 사용자 ID |
| `senderNickname` | `String` | 요청 보낸 사용자 닉네임 |
| `receiverUserId` | `Long` | 요청 받은 사용자 ID |
| `receiverNickname` | `String` | 요청 받은 사용자 닉네임 |
| `status` | `FriendRequestStatus` | 요청 상태 |
| `respondedAt` | `LocalDateTime?` | 수락/거절/취소 시각 |
| `createdAt` | `LocalDateTime` | 요청 생성 시각 |

### 2.4 `FriendResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `Long` | 친구 사용자 ID |
| `nickname` | `String` | 앱에서 `이름`으로 노출하는 친구 닉네임 |
| `beltColor` | `BeltColor` | 현재 벨트 |
| `affiliation` | `String?` | 소속 |
| `friendedAt` | `LocalDateTime` | 친구 관계 생성 시각 |

### 2.5 `TrainingLogFriendEntrySummaryResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 기록 ID |
| `authorUserId` | `Long` | 작성자 ID |
| `authorNickname` | `String` | 앱에서 `이름`으로 노출하는 작성자 닉네임 |
| `trainingDate` | `LocalDate` | 기록 날짜 |
| `category` | `TrainingLogCategory` | 기록 카테고리 |
| `color` | `TrainingLogColor?` | 기록 색상 |
| `title` | `String` | 기록 제목 |
| `content` | `String` | 기록 본문 요약 |
| `likeCount` | `Long` | 좋아요 수 |
| `commentCount` | `Long` | 댓글 수 |
| `likedByMe` | `Boolean` | 현재 로그인 사용자의 좋아요 여부 |
| `createdAt` | `LocalDateTime` | 생성 시각 |

### 2.6 `TrainingLogFriendEntryDetailResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 기록 ID |
| `authorUserId` | `Long` | 작성자 ID |
| `authorNickname` | `String` | 앱에서 `이름`으로 노출하는 작성자 닉네임 |
| `trainingDate` | `LocalDate` | 기록 날짜 |
| `visibility` | `TrainingLogVisibility` | 공개 범위 |
| `category` | `TrainingLogCategory` | 기록 카테고리 |
| `color` | `TrainingLogColor?` | 기록 색상 |
| `trainingIntensity` | `Integer?` | 훈련 강도 |
| `gymAttendance` | `Boolean?` | 체육관 출석 여부 |
| `condition` | `Integer?` | 컨디션 |
| `trainingMinutes` | `Integer?` | 훈련 시간 |
| `title` | `String` | 기록 제목 |
| `content` | `String` | 기록 본문 |
| `checklist` | `List<TrainingLogChecklistItem>` | 체크리스트 |
| `hashtags` | `List<String>` | 해시태그 |
| `imageUrls` | `List<String>` | 이미지 목록 |
| `externalLinks` | `List<TrainingLogExternalLink>` | 외부 링크 목록 |
| `likeCount` | `Long` | 좋아요 수 |
| `commentCount` | `Long` | 댓글 수 |
| `likedByMe` | `Boolean` | 현재 로그인 사용자의 좋아요 여부 |
| `commentableByMe` | `Boolean` | 댓글 작성 가능 여부 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

### 2.7 `TrainingLogCommentResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 댓글 ID |
| `entryId` | `Long` | 기록 ID |
| `parentCommentId` | `Long?` | 상위 댓글 ID. 원댓글이면 null |
| `authorUserId` | `Long` | 작성자 ID |
| `authorNickname` | `String` | 앱에서 `이름`으로 노출하는 작성자 닉네임 |
| `content` | `String?` | 댓글 본문. 삭제된 댓글이면 null 허용 |
| `deleted` | `Boolean` | 삭제 여부 |
| `editableByMe` | `Boolean` | 현재 로그인 사용자의 수정 가능 여부 |
| `deletableByMe` | `Boolean` | 현재 로그인 사용자의 삭제 가능 여부 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |
| `replies` | `List<TrainingLogCommentResponse>` | 1단계 대댓글 목록 |

구현 메모:

- `replies`는 원댓글에서만 사용한다.
- 대댓글의 `replies`는 항상 빈 배열이다.
- 삭제된 원댓글에 대댓글이 남아 있으면 스레드 구조를 유지하기 위해 row는 보존한다.
- 삭제된 댓글은 `deleted = true`, `content = null` 형태로 반환한다.

### 2.8 `FriendRequest` 저장 모델 설계

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `senderUserId` | `Long` | 요청 발신 사용자 ID |
| `receiverUserId` | `Long` | 요청 수신 사용자 ID |
| `status` | `FriendRequestStatus` | 요청 상태 |
| `respondedAt` | `LocalDateTime?` | 수락/거절/취소 시각 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

구현 메모:

- 테이블명은 `friend_requests`를 기준으로 설계한다.
- `sender_user_id != receiver_user_id`를 전제로 한다.
- 중복 `PENDING` 요청 방지는 서비스 검증을 기본으로 하고, 조회 최적화를 위해 `sender_user_id,status,created_at`, `receiver_user_id,status,created_at` 인덱스를 둔다.
- 요청 수락 시 요청 상태는 `ACCEPTED`로 갱신하고 별도 `Friendship` row를 생성한다.
- 현재 구현에는 발신자 취소 API가 없으므로 `CANCELED`는 후속 확장 상태로만 남겨둔다.

### 2.9 `Friendship` 저장 모델 설계

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `userId` | `Long` | 친구 목록 소유 사용자 ID |
| `friendUserId` | `Long` | 연결된 친구 사용자 ID |
| `friendedAt` | `LocalDateTime` | 친구 관계 생성 시각 |

구현 메모:

- 테이블명은 `user_friends`를 기준으로 설계한다.
- 양방향 조회 단순화를 위해 친구 수락 시 `(A -> B)`, `(B -> A)` 두 row를 같은 트랜잭션에서 생성한다.
- 같은 관계의 중복 생성을 막기 위해 `(user_id, friend_user_id)` unique 제약을 둔다.
- 친구 삭제 시 두 방향 row를 함께 삭제한다.
- 친구 목록/친구 기록 접근 경로 최적화를 위해 `user_id,friended_at` 인덱스를 둔다.

### 2.10 `TrainingLogLike` 저장 모델 설계

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `entryId` | `Long` | 대상 훈련일지 ID |
| `userId` | `Long` | 좋아요 사용자 ID |
| `createdAt` | `LocalDateTime` | 생성 시각 |

구현 메모:

- 테이블명은 `training_log_likes`를 기준으로 설계한다.
- `(entry_id, user_id)` unique 제약으로 중복 좋아요를 막는다.
- 목록/상세 like count 집계를 위해 `entry_id` 인덱스를 둔다.

### 2.11 `TrainingLogComment` 저장 모델 설계

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `entryId` | `Long` | 대상 훈련일지 ID |
| `parentCommentId` | `Long?` | 상위 댓글 ID. 원댓글이면 null |
| `authorUserId` | `Long` | 작성자 ID |
| `content` | `String` | 댓글 본문 |
| `deleted` | `Boolean` | soft delete 여부 |
| `deletedAt` | `LocalDateTime?` | 삭제 시각 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

구현 메모:

- 테이블명은 `training_log_comments`를 기준으로 설계한다.
- `parent_comment_id = null`이면 원댓글, 값이 있으면 대댓글이다.
- depth 제한은 row 구조가 아니라 서비스 검증으로 강제한다.
- 원댓글 조회와 대댓글 묶음을 위해 `entry_id,parent_comment_id,created_at` 인덱스를 둔다.
- 삭제된 원댓글에 대댓글이 남아 있을 수 있으므로 hard delete 대신 placeholder 가능한 soft delete를 기준으로 한다.

## 3. Enum

### 3.1 `TrainingLogVisibility`

| Raw value | 설명 |
| --- | --- |
| `PRIVATE` | 작성자만 조회 가능 |
| `FRIENDS` | 친구만 조회 가능 |

### 3.2 `FriendRequestStatus`

| Raw value | 설명 |
| --- | --- |
| `PENDING` | 응답 대기 |
| `ACCEPTED` | 수락됨 |
| `REJECTED` | 거절됨 |
| `CANCELED` | 발신자가 취소함 |

### 3.3 `PushNotificationType`

훈련일지 소셜에서 추가되는 알림 raw value:

- `TRAINING_LOG_COMMENT_CREATED`
- `TRAINING_LOG_COMMENT_REPLY_CREATED`

## 4. 공통 정책

### 4.1 인증과 권한

- 모든 Training Log Social API는 인증이 필요하다.
- 친구 관계, 좋아요, 댓글은 모두 로그인 사용자 기준으로 처리한다.
- 제재 사용자는 전역 제재 정책에 따라 일부 쓰기 액션이 차단될 수 있다.

### 4.2 친구 관계

- 친구는 `양방향 승인형` 관계다.
- 친구 검색은 `User.nickname` 기준으로 수행한다.
- 친구 검색 결과에는 `nickname`, `affiliation`, `beltColor`를 함께 노출한다.
- 친구 검색은 최소 2자 이상 키워드부터 허용하고 최대 20건까지 반환한다.
- 자기 자신에게 친구 요청을 보낼 수 없다.
- 이미 친구인 사용자에게 재요청할 수 없다.
- 이미 `PENDING`인 요청이 있으면 중복 요청할 수 없다.
- 차단한 사용자 또는 차단당한 사용자와는 친구 요청, 수락, 열람이 모두 불가하다.
- 탈퇴, 탈퇴 예약, 비활성 상태 사용자는 검색 결과와 요청 대상에서 제외한다.
- 친구 검색 결과는 요청 가능한 후보만 반환하며, 이미 친구이거나 요청 대기 중인 사용자는 별도 요청함 화면에서 처리한다.
- 친구 삭제는 양방향 관계를 제거한다.

### 4.3 열람 정책

- 작성자는 본인 기록을 항상 조회할 수 있다.
- 친구는 `FRIENDS` 공개 기록만 조회할 수 있다.
- `PRIVATE` 기록은 친구도 볼 수 없다.
- 친구가 아니면 다른 사용자의 훈련일지 목록/상세를 조회할 수 없다.
- 차단 관계가 생기면 기존 친구 관계보다 차단 정책이 우선한다.

### 4.4 좋아요 정책

- 좋아요는 친구가 열람 가능한 기록에만 허용한다.
- 한 사용자당 한 기록에 좋아요 1개만 허용한다.
- 본인 기록에는 좋아요를 누를 수 없다.
- 좋아요 취소는 idempotent 하게 처리한다.

### 4.5 댓글과 대댓글 정책

- 댓글은 친구가 열람 가능한 기록에만 작성 가능하다.
- 대댓글도 같은 기록 열람 권한이 있는 사용자만 작성할 수 있다.
- 댓글은 원댓글과 1단계 대댓글까지만 허용한다.
- 대댓글에는 다시 대댓글을 달 수 없다.
- 원댓글 1개 아래에 대댓글은 여러 개 달 수 있다.
- 댓글 작성자는 본인 댓글만 수정할 수 있다.
- 댓글 삭제는 댓글 작성자, 기록 작성자, 관리자만 가능하다.
- 삭제된 원댓글에 대댓글이 남아 있으면 원댓글은 placeholder 형태로 유지한다.
- placeholder는 hard delete가 아니라 soft delete row 유지 방식으로 구현한다.
- 댓글과 대댓글 작성 진입점은 훈련일지 상세 페이지다.
- 친구 기록 목록이나 피드에서는 댓글 수만 노출하고, 작성 액션은 상세 진입 후 제공한다.

### 4.6 알림 정책

- 친구가 내 기록에 댓글을 달면 기록 작성자에게 `TRAINING_LOG_COMMENT_CREATED` 알림을 저장하고 푸시 발송을 시도한다.
- 내 댓글에 대댓글이 달리면 상위 댓글 작성자에게 `TRAINING_LOG_COMMENT_REPLY_CREATED` 알림을 저장하고 푸시 발송을 시도한다.
- 자기 자신의 댓글/대댓글 액션에는 자기 알림을 만들지 않는다.
- 좋아요 알림은 MVP 범위에 포함하지 않는다.
- 알림 route는 친구 훈련일지 상세 진입 경로로 연결되어야 한다.
- route payload는 API path가 아니라 앱 상세 화면 route를 기준으로 관리한다.
- 현재 구현 route payload는 `/training-logs/friends/entries/{entryId}`다.

## 5. API

### 5.1 기존 훈련 기록 API 확장

기존 [training-log.md](training-log.md)의 아래 계약에 `visibility`가 추가된다.

- `POST /api/v1/training-logs/me/entries/{date}`
- `PATCH /api/v1/training-logs/me/entries/{id}`
- `GET /api/v1/training-logs/me/entries/{id}`

추가 필드:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `visibility` | `TrainingLogVisibility` | - | 기본값은 `PRIVATE` |

구현 메모:

- `POST`, `PATCH` request body와 `GET 상세` response에 동일한 raw value를 사용한다.
- `GET /api/v1/training-logs/me/entries?date=...` 요약 카드 목록 응답에는 `visibility`를 추가하지 않는다.

### 5.2 친구 검색

`GET /api/v1/friends/search?q=민준`

- 인증: 필요
- Response data: `List<FriendSearchResultResponse>`

Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `q` | `String` | O | 최소 2자, 사용자 `nickname` 검색어 |

에러:

- `VALIDATION_ERROR`

### 5.3 친구 목록 조회

`GET /api/v1/friends`

- 인증: 필요
- Response data: `List<FriendResponse>`

### 5.4 받은 친구 요청 조회

`GET /api/v1/friends/requests/received`

- 인증: 필요
- Response data: `List<FriendRequestResponse>`

### 5.5 보낸 친구 요청 조회

`GET /api/v1/friends/requests/sent`

- 인증: 필요
- Response data: `List<FriendRequestResponse>`

### 5.6 친구 요청 전송

`POST /api/v1/friends/requests/{targetUserId}`

- 인증: 필요
- Response data: `FriendRequestResponse`

에러:

- `VALIDATION_ERROR`
- `FORBIDDEN`
- `NOT_FOUND`

### 5.7 친구 요청 수락

`POST /api/v1/friends/requests/{requestId}/accept`

- 인증: 필요
- Response data: `FriendResponse`

### 5.8 친구 요청 거절

`POST /api/v1/friends/requests/{requestId}/reject`

- 인증: 필요
- Response data: `null`

### 5.9 친구 삭제

`DELETE /api/v1/friends/{friendUserId}`

- 인증: 필요
- Response data: `null`

### 5.10 친구 공유 기록 피드 조회

`GET /api/v1/training-logs/friends`

- 인증: 필요
- Response data: paging 된 `TrainingLogFriendEntrySummaryResponse`

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `20` | 페이지 크기 |
| `sort` | `String` | `trainingDate,createdAt,id DESC` | 미지정 시 기본 정렬 |

### 5.11 특정 친구의 공유 기록 목록 조회

`GET /api/v1/training-logs/friends/{friendUserId}/entries`

- 인증: 필요
- Response data: paging 된 `TrainingLogFriendEntrySummaryResponse`

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `20` | 페이지 크기 |
| `dateFrom` | `Date` | - | 기간 시작일 |
| `dateTo` | `Date` | - | 기간 종료일 |

### 5.12 친구 공유 기록 상세 조회

`GET /api/v1/training-logs/friends/entries/{entryId}`

- 인증: 필요
- Response data: `TrainingLogFriendEntryDetailResponse`

현재 구현 방향 메모:

- 댓글과 대댓글 작성 UI는 이 상세 응답을 사용하는 페이지에서 제공한다.
- 목록 응답에서는 댓글 작성 editor를 열지 않는다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`

### 5.13 기록 좋아요

`POST /api/v1/training-logs/entries/{entryId}/like`

- 인증: 필요
- Response data: `null`

### 5.14 기록 좋아요 취소

`DELETE /api/v1/training-logs/entries/{entryId}/like`

- 인증: 필요
- Response data: `null`

### 5.15 댓글 목록 조회

`GET /api/v1/training-logs/entries/{entryId}/comments`

- 인증: 필요
- Response data: `List<TrainingLogCommentResponse>`

구현 메모:

- 원댓글 기준 생성 시각 오름차순으로 반환한다.
- 각 원댓글의 `replies`는 생성 시각 오름차순으로 반환한다.

### 5.16 댓글 작성

`POST /api/v1/training-logs/entries/{entryId}/comments`

- 인증: 필요
- Response data: `TrainingLogCommentResponse`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `content` | `String` | O | 댓글 본문 |
| `parentCommentId` | `Long?` | - | 대댓글이면 상위 댓글 ID |

검증:

- 댓글은 1자 이상 1000자 이하여야 한다.
- `parentCommentId`가 있으면 같은 기록의 원댓글이어야 한다.
- 상위 댓글이 이미 대댓글이면 `VALIDATION_ERROR`다.
- 삭제된 댓글에는 대댓글을 달 수 없다.

### 5.17 댓글 수정

`PATCH /api/v1/training-logs/comments/{commentId}`

- 인증: 필요
- Response data: `TrainingLogCommentResponse`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `content` | `String` | O | 수정할 댓글 본문 |

### 5.18 댓글 삭제

`DELETE /api/v1/training-logs/comments/{commentId}`

- 인증: 필요
- Response data: `null`

### 5.19 알림 API 재사용

훈련일지 댓글/대댓글 알림 조회와 읽음 처리는 공용 알림 API를 그대로 사용한다.

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/badge`
- `PATCH /api/v1/notifications/{id}/read`

## 6. 구현 메모

- 친구 요청 수락은 `FriendRequest.status` 갱신과 양방향 `Friendship` row 생성이 하나의 트랜잭션으로 끝나야 한다.
- 친구 열람용 API는 기존 `/api/v1/training-logs/me/*`와 분리한다.
- 커뮤니티의 좋아요/댓글/알림 패턴을 최대한 재사용하는 것이 맞다.
- 조회 필터에는 기존 사용자 차단 정책을 그대로 우선 적용한다.
- 댓글/대댓글 수는 캐시 컬럼 또는 집계 쿼리 중 하나로 일관되게 선택해야 한다.
- 대댓글은 `parentCommentId` 단일 구조로 처리하되, 깊이는 1로 제한한다.
- 댓글 알림은 `@TransactionalEventListener(AFTER_COMMIT)`에서 알림함 저장 후 푸시 발송을 시도한다.

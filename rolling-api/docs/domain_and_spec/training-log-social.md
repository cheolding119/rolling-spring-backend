# Training Log Social

- 훈련일지 친구 관계, 친구 열람, 좋아요, 댓글, 대댓글, 댓글 알림 도메인과 API 스펙을 관리한다.
- 원본 훈련 기록 모델과 개인 기록 API는 [training-log.md](training-log.md)를 따른다.
- 공통 응답, 인증, 사용자 차단, 알림함 API는 [shared/common-models.md](shared/common-models.md)를 따른다.
- 현재 문서는 `친구 기반 제한 공유 MVP` 기준이며, 공개 피드와 추천은 포함하지 않는다.
- 현재 문서는 `기록별 visibility` 초안 대신 `사용자 단위 친구 공유 설정`을 목표 계약으로 사용한다.

## 1. 도메인 개요

Training Log Social은 개인 훈련일지를 `친구 관계 안에서만` 제한적으로 공유하고 상호작용할 수 있게 만드는 확장 도메인이다.

현재 목표 범위:

- 친구 검색(`이름`으로 노출되는 `User.nickname` 기준)
- 친구 요청 전송, 수락, 거절, 삭제
- 친구 요청 전송 시 수신자 알림함 저장과 푸시 발송 시도
- 친구 설정에서 제어하는 `훈련일지 친구 공개 on/off`
- 친구 월간 캘린더 조회
- 친구 특정 날짜 기록 목록 조회
- 친구 기록 상세 조회
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

### 2.1 `UserTrainingLogShareSetting`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `Long` | 설정 소유 사용자 ID |
| `shareWithFriends` | `Boolean` | 친구에게 훈련일지를 공개할지 여부 |
| `updatedAt` | `LocalDateTime` | 마지막 설정 변경 시각 |

구현 메모:

- 친구 관계와 별개로, 훈련일지 열람 가능 여부는 `shareWithFriends`가 최종 결정한다.
- 친구 관계가 있어도 `shareWithFriends = false`면 친구는 해당 사용자의 훈련일지를 볼 수 없다.
- 친구 관계가 있고 `shareWithFriends = true`일 때만 친구 캘린더, 날짜별 목록, 상세 조회가 가능하다.
- 저장 위치는 `user_training_log_share_settings` 별도 테이블이다.
- 설정 row가 없으면 기본값은 `shareWithFriends = false`로 간주한다.
- 설정을 켜면 기존 작성 기록도 함께 친구에게 노출한다.

### 2.2 `FriendSearchResultResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `Long` | 검색 결과 사용자 ID |
| `nickname` | `String` | 앱에서 `이름`으로 노출하는 사용자 닉네임 |
| `affiliation` | `String?` | 소속 |
| `beltColor` | `BeltColor` | 현재 벨트 |
| `friendRequestStatus` | `FriendSearchRelationshipStatus` | 현재 검색 사용자와의 관계 상태 |
| `outgoingRequestId` | `Long?` | 내가 보낸 `PENDING` 요청 ID. 취소 가능할 때만 값 존재 |

구현 메모:

- 친구 검색 결과는 `nickname`, `affiliation`, `beltColor`를 함께 노출한다.
- 친구 검색 결과는 `friendRequestStatus`, `outgoingRequestId`를 함께 노출한다.
- 현재 백엔드 사용자 엔티티의 source field는 `User.nickname`, `User.affiliation`, `User.beltColor`다.
- 소속이 없으면 `affiliation = null`로 반환하고, 화면 문구는 클라이언트가 결정한다.
- 검색 결과에서는 차단 관계만 제외하고, 친구/보낸 요청/받은 요청 대상은 상태 필드로 구분해 반환한다.

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

구현 메모:

- 현재 구현은 기존 친구 목록 응답을 그대로 재사용하며 `trainingLogShareEnabled`는 포함하지 않는다.

### 2.5 친구 열람 응답 모델

친구 훈련일지는 일반 훈련일지 화면을 read-only로 재사용하는 방향을 기준으로 한다.

| 화면/API | 권장 응답 구조 |
| --- | --- |
| 친구 월간 캘린더 | `TrainingLogMonthlyCalendarResponse` 재사용 |
| 친구 특정 날짜 기록 목록 | `List<TrainingLogEntrySummaryResponse>` 재사용 |
| 친구 기록 상세 | `TrainingLogFriendEntryDetailResponse` |

상세 구현 메모:

- 친구 상세 화면은 일반 훈련일지 상세 화면처럼 읽기 전용으로 표시 가능해야 한다.
- 현재 구현은 `TrainingLogEntryResponse`의 핵심 필드에 아래 additive field를 합친 DTO를 사용한다.
  - `authorUserId`
  - `authorNickname`
  - `likeCount`
  - `commentCount`
  - `likedByMe`
  - `commentableByMe`
  - `createdAt`
  - `updatedAt`

### 2.6 `TrainingLogCommentResponse`

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

### 2.7 `FriendRequest` 저장 모델 설계

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

### 2.8 `Friendship` 저장 모델 설계

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

### 2.9 `TrainingLogLike` 저장 모델 설계

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `entryId` | `Long` | 대상 훈련일지 ID |
| `userId` | `Long` | 좋아요 사용자 ID |
| `createdAt` | `LocalDateTime` | 생성 시각 |

구현 메모:

- 테이블명은 `training_log_likes`를 기준으로 설계한다.
- `(entry_id, user_id)` unique 제약으로 중복 좋아요를 막는다.

### 2.10 `TrainingLogComment` 저장 모델 설계

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `entryId` | `Long` | 대상 훈련일지 ID |
| `parentCommentId` | `Long?` | 상위 댓글 ID. 원댓글이면 null |
| `authorUserId` | `Long` | 작성자 ID |
| `content` | `String` | 댓글 본문 |
| `reportCount` | `Long` | 누적 신고 수 |
| `deleted` | `Boolean` | soft delete 여부 |
| `deletedAt` | `LocalDateTime?` | 삭제 시각 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

구현 메모:

- 테이블명은 `training_log_comments`를 기준으로 설계한다.
- `parent_comment_id = null`이면 원댓글, 값이 있으면 대댓글이다.
- depth 제한은 row 구조가 아니라 서비스 검증으로 강제한다.

### 2.11 `TrainingLogCommentReport` 저장 모델 설계

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `commentId` | `Long` | 신고 대상 댓글 ID |
| `reporterId` | `Long` | 신고 사용자 ID |
| `reason` | `ReportReason` | 신고 사유 |
| `customReason` | `String?` | 기타 신고 사유 |
| `status` | `ReportStatus` | 신고 처리 상태 |
| `processedByUserId` | `Long?` | 처리 관리자 ID |
| `processedAt` | `LocalDateTime?` | 처리 시각 |
| `processingMemo` | `String?` | 처리 메모 |
| `finalAction` | `String?` | 최종 조치 메모 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

구현 메모:

- 테이블명은 `training_log_comment_reports`를 기준으로 설계한다.
- `(comment_id, reporter_id)` unique 제약으로 동일 사용자의 중복 신고를 막는다.
- 신고 처리 상태는 커뮤니티 댓글 신고와 동일하게 `ReportStatus` 공용 enum을 사용한다.

## 3. Enum

### 3.1 `FriendRequestStatus`

| Raw value | 설명 |
| --- | --- |
| `PENDING` | 응답 대기 |
| `ACCEPTED` | 수락됨 |
| `REJECTED` | 거절됨 |
| `CANCELED` | 발신자가 취소함 |

### 3.2 `FriendSearchRelationshipStatus`

| Raw value | 설명 |
| --- | --- |
| `NONE` | 요청 가능한 상태 |
| `PENDING_SENT` | 내가 보낸 친구 요청 대기 중 |
| `PENDING_RECEIVED` | 내가 받은 친구 요청 대기 중 |
| `FRIEND` | 이미 친구 관계 |

### 3.3 `PushNotificationType`

훈련일지 소셜에서 추가되는 알림 raw value:

- `FRIEND_REQUEST_RECEIVED`
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
- 친구 검색 결과에는 `friendRequestStatus`, `outgoingRequestId`를 함께 노출한다.
- 친구 검색은 최소 2자 이상 키워드부터 허용하고 최대 20건까지 반환한다.
- 자기 자신에게 친구 요청을 보낼 수 없다.
- 이미 친구인 사용자에게 재요청할 수 없다.
- 이미 `PENDING`인 요청이 있으면 중복 요청할 수 없다.
- 친구 요청 전송이 성공하면 수신자에게 `FRIEND_REQUEST_RECEIVED` 알림함 저장과 푸시 발송을 시도한다.
- 차단한 사용자 또는 차단당한 사용자와는 친구 요청, 수락, 열람이 모두 불가하다.
- 탈퇴, 탈퇴 예약, 비활성 상태 사용자는 검색 결과와 요청 대상에서 제외한다.
- 친구 삭제는 양방향 관계를 제거한다.
- 발신자는 본인이 보낸 `PENDING` 친구 요청을 취소할 수 있다.

### 4.3 친구 훈련일지 공개 정책

- 친구 관계만으로는 훈련일지 열람 권한이 생기지 않는다.
- 훈련일지 작성자가 `친구에게 훈련일지 공개` 설정을 켰을 때만 친구가 열람할 수 있다.
- 작성자가 공개 설정을 끄면 친구 관계를 유지해도 친구 훈련일지 API에서는 기록이 보이지 않는다.
- 이 설정은 `기록별`이 아니라 `사용자 단위`로 동작한다.
- 설정을 켜면 기존 작성 기록도 친구에게 함께 노출한다.

### 4.4 열람 정책

- 작성자는 본인 기록을 항상 조회할 수 있다.
- 친구는 아래 두 조건을 모두 만족할 때만 친구 훈련일지를 조회할 수 있다.
  - 친구 관계 존재
  - 작성자의 `shareWithFriends = true`
- 차단 관계가 생기면 기존 친구 관계보다 차단 정책이 우선한다.
- 친구 월간 캘린더와 특정 날짜 기록 목록은 일반 훈련일지 화면 재사용을 위해 read-only 응답을 반환한다.
- 친구 상세는 직접 URL을 알아도 위 조건을 만족하지 않으면 `NOT_FOUND` 또는 `FORBIDDEN`으로 차단한다.
- 현재 구현은 친구 관계는 있으나 공개 설정이 꺼져 있으면 `FORBIDDEN`을 반환한다.

### 4.5 좋아요 정책

- 좋아요는 친구가 열람 가능한 기록에만 허용한다.
- 한 사용자당 한 기록에 좋아요 1개만 허용한다.
- 본인 기록에는 좋아요를 누를 수 없다.
- 좋아요 취소는 idempotent 하게 처리한다.

### 4.6 댓글과 대댓글 정책

- 댓글은 친구가 열람 가능한 기록에만 작성 가능하다.
- 대댓글도 같은 기록 열람 권한이 있는 사용자만 작성할 수 있다.
- 댓글은 원댓글과 1단계 대댓글까지만 허용한다.
- 대댓글에는 다시 대댓글을 달 수 없다.
- 원댓글 1개 아래에 대댓글은 여러 개 달 수 있다.
- 댓글 작성자는 본인 댓글만 수정할 수 있다.
- 댓글 삭제는 댓글 작성자, 기록 작성자, 관리자만 가능하다.
- 삭제된 원댓글에 대댓글이 남아 있으면 원댓글은 placeholder 형태로 유지한다.
- 댓글과 대댓글 작성 진입점은 친구 훈련일지 상세 페이지다.
- 조회자와 댓글 작성자 사이에 차단 관계가 있으면 댓글 목록에서 숨긴다.
- 차단된 원댓글이 숨겨지면 그 하위 대댓글도 함께 숨긴다.
- 차단 관계가 있는 기록 작성자에게는 새 댓글을 작성할 수 없다.
- 차단 관계가 있는 상위 댓글 작성자에게는 대댓글을 작성할 수 없다.
- 댓글 수는 조회자에게 실제로 노출되는 댓글 기준으로 계산한다.

### 4.6.1 댓글 신고 정책

- 댓글 신고는 `POST /api/v1/training-logs/comments/{commentId}/report`로 처리한다.
- 자기 댓글은 신고할 수 없다.
- 동일 사용자의 동일 댓글 중복 신고는 허용하지 않는다.
- `ReportReason.OTHER`일 때 `customReason`은 필수다.
- 이미 삭제된 댓글은 새 신고를 받을 수 없다.
- 댓글 또는 대댓글 신고가 3회 이상 누적되면 해당 댓글은 자동 soft delete 된다.
- 원댓글이 신고 누적으로 자동 삭제되면 하위 대댓글도 함께 soft delete 된다.

### 4.7 알림 정책

- 친구가 내 기록에 댓글을 달면 기록 작성자에게 `TRAINING_LOG_COMMENT_CREATED` 알림을 저장하고 푸시 발송을 시도한다.
- 내 댓글에 대댓글이 달리면 상위 댓글 작성자에게 `TRAINING_LOG_COMMENT_REPLY_CREATED` 알림을 저장하고 푸시 발송을 시도한다.
- 자기 자신의 댓글/대댓글 액션에는 자기 알림을 만들지 않는다.
- 차단 관계가 생긴 사용자 간에는 새 댓글/대댓글 알림을 발행하지 않는다.
- 좋아요 알림은 MVP 범위에 포함하지 않는다.
- 알림 route는 `"/training-logs/friends/entries/{entryId}"` 형식의 API path를 payload에 저장한다.
- 앱 라우트로의 변환은 프론트가 `targetId` 또는 `route`를 해석해서 처리한다.

## 5. API

### 5.1 친구 목록 조회

`GET /api/v1/friends`

- 인증: 필요
- Response data: `List<FriendResponse>`
- 기존 API 재사용

### 5.1.1 친구 검색 조회

`GET /api/v1/friends/search?q={keyword}`

- 인증: 필요
- Response data: `List<FriendSearchResultResponse>`

구현 메모:

- 검색 결과에서 `PENDING` 관계 사용자를 제거하지 않는다.
- `friendRequestStatus`와 `outgoingRequestId`로 프론트가 `요청`, `요청됨`, `받은 요청`, `친구` 상태를 그릴 수 있어야 한다.

### 5.1.2 친구 요청 전송

`POST /api/v1/friends/requests/{targetUserId}`

- 인증: 필요
- Response data: `FriendRequestResponse`

구현 메모:

- 친구 요청 저장 성공 후 수신자에게 `FRIEND_REQUEST_RECEIVED` 알림함 저장과 푸시 발송을 시도한다.
- 알림 `targetId`는 생성된 `friendRequest.id`를 사용한다.
- 알림 `route`는 `/training-log/social/friends`를 사용한다.

### 5.2 내 친구 훈련일지 공개 설정 조회

`GET /api/v1/training-logs/me/friend-sharing`

- 인증: 필요
- Response data: `UserTrainingLogShareSetting`

응답 예시:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `shareWithFriends` | `Boolean` | 친구 공개 여부 |
| `updatedAt` | `DateTime` | 마지막 변경 시각 |

### 5.3 내 친구 훈련일지 공개 설정 변경

`PATCH /api/v1/training-logs/me/friend-sharing`

- 인증: 필요
- Response data: `UserTrainingLogShareSetting`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `shareWithFriends` | `Boolean` | O | 친구 공개 여부 |

### 5.3.1 친구 요청 취소

`POST /api/v1/friends/requests/{requestId}/cancel`

- 인증: 필요
- Response data: `null`

정책:

- 로그인 사용자가 직접 보낸 `PENDING` 요청만 취소할 수 있다.
- `ACCEPTED`, `REJECTED`, `CANCELED` 상태 요청은 취소할 수 없다.
- 성공 시 상태는 `CANCELED`로 변경하고 `respondedAt`을 갱신한다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`
- `VALIDATION_ERROR`

### 5.4 친구 월간 캘린더 요약 조회

`GET /api/v1/training-logs/friends/{friendUserId}/calendar?year=2026&month=5`

- 인증: 필요
- Response data: `TrainingLogMonthlyCalendarResponse`

구현 메모:

- 기존 월간 캘린더 응답 구조를 그대로 재사용하는 것을 권장한다.
- 친구 관계와 `shareWithFriends = true`를 모두 만족할 때만 조회 가능하다.

### 5.5 친구 특정 날짜 기록 목록 조회

`GET /api/v1/training-logs/friends/{friendUserId}/entries?date=2026-05-18`

- 인증: 필요
- Response data: `List<TrainingLogEntrySummaryResponse>`

Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `date` | `Date` | O | 조회 날짜 |

구현 메모:

- 일반 훈련일지 날짜별 카드 목록 화면을 read-only로 재사용할 수 있게 기존 요약 응답 구조를 재사용한다.
- 친구 관계와 `shareWithFriends = true`를 모두 만족할 때만 조회 가능하다.

### 5.6 친구 기록 상세 조회

`GET /api/v1/training-logs/friends/entries/{entryId}`

- 인증: 필요
- Response data: `TrainingLogFriendEntryDetailResponse`

권장 응답:

- 일반 훈련일지 상세 필드
- 작성자 식별 필드
- 좋아요/댓글 카운트와 현재 사용자 상호작용 필드

구현 메모:

- 일반 훈련일지 상세 화면을 read-only로 재사용할 수 있어야 한다.
- 좋아요, 댓글 영역이 필요하면 additive field를 포함한다.
- `commentCount`는 조회자에게 노출되는 댓글 기준으로 계산한다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`

### 5.6.1 본인 기록 상세의 소셜 메타 확장

`GET /api/v1/training-logs/me/entries/{id}`

- 인증: 필요
- Response data: `TrainingLogEntryResponse`

구현 메모:

- 본인 상세 조회도 친구 상세와 동일한 읽기 메타 필드 `likeCount`, `commentCount`, `likedByMe`, `commentableByMe`를 포함한다.
- 댓글 목록 조회, 댓글 작성/수정/삭제, 좋아요/좋아요 취소 API는 아래 소셜 액션 API를 그대로 재사용한다.
- 현재 정책 기준 본인 기록 상세에서는 `likedByMe = false`, `commentableByMe = true`로 반환한다.
- `commentCount`는 본인에게 실제로 노출되는 댓글 기준으로 계산한다.

### 5.7 호환용 친구 피드 조회

`GET /api/v1/training-logs/friends`

- 인증: 필요
- Response data: `Page<TrainingLogFriendEntrySummaryResponse>`

구현 메모:

- 기존 프론트 호환을 위해 유지하는 API다.
- `shareWithFriends = true`인 친구의 기록만 반환한다.
- `TrainingLogEntry.visibility` 값과 무관하게 해당 친구의 모든 기록이 반환된다.
- `commentCount`는 조회자에게 노출되는 댓글 기준으로 계산한다.

### 5.8 좋아요

`POST /api/v1/training-logs/entries/{entryId}/like`

`DELETE /api/v1/training-logs/entries/{entryId}/like`

- 인증: 필요
- Response data: `null`

### 5.9 댓글 목록 조회

`GET /api/v1/training-logs/entries/{entryId}/comments`

- 인증: 필요
- Response data: `List<TrainingLogCommentResponse>`

구현 메모:

- 조회자와 차단 관계인 댓글 작성자의 댓글은 응답에서 제외한다.
- 차단된 원댓글이 제외되면 해당 스레드의 대댓글도 함께 제외한다.

### 5.10 댓글 작성

`POST /api/v1/training-logs/entries/{entryId}/comments`

- 인증: 필요
- Response data: `TrainingLogCommentResponse`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `content` | `String` | O | 댓글 본문 |
| `parentCommentId` | `Long?` | - | 대댓글이면 상위 댓글 ID |

구현 메모:

- 기록 작성자와 차단 관계면 댓글 작성이 불가하다.
- 대댓글 작성 시 상위 댓글 작성자와 차단 관계면 작성이 불가하다.

### 5.11 댓글 수정

`PATCH /api/v1/training-logs/comments/{commentId}`

- 인증: 필요
- Response data: `TrainingLogCommentResponse`

### 5.12 댓글 삭제

`DELETE /api/v1/training-logs/comments/{commentId}`

- 인증: 필요
- Response data: `null`

### 5.12.1 댓글 신고

`POST /api/v1/training-logs/comments/{commentId}/report`

- 인증: 필요
- Response data: `null`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `ReportReason` | O | 신고 사유 |
| `customReason` | `String?` | - | 기타 신고 사유 |

에러:

- `NOT_FOUND`
- `SELF_REPORT_NOT_ALLOWED`
- `ALREADY_REPORTED`
- `VALIDATION_ERROR`
- `FORBIDDEN`

### 5.12.2 관리자 댓글 신고 목록 조회

`GET /api/v1/admin/training-logs/comments/reports`

- 인증: 필요
- 권한: `ADMIN`
- Response data: `Page<TrainingLogCommentReportAdminResponse>`

Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | `ReportStatus?` | - | 처리 상태 필터 |

구현 메모:

- 기본 정렬은 `createdAt desc, id desc`다.
- 허용 정렬 필드는 `createdAt`, `updatedAt`, `status`, `processedAt`다.
- 응답에는 댓글 작성자, 훈련일지 작성자, 신고자 식별 정보와 처리 상태가 함께 포함된다.

### 5.12.3 관리자 댓글 신고 상세 조회

`GET /api/v1/admin/training-logs/comments/reports/{id}`

- 인증: 필요
- 권한: `ADMIN`
- Response data: `TrainingLogCommentReportAdminResponse`

구현 메모:

- 목록 응답 필드에 더해 `parentCommentId`, `commentDeleted`, `processingMemo`, `finalAction`을 함께 확인할 수 있다.
- 삭제된 댓글도 신고 이력을 유지한 채 운영자가 조회할 수 있다.

### 5.12.4 관리자 댓글 신고 상태 변경

`PATCH /api/v1/admin/training-logs/comments/reports/{id}/status`

- 인증: 필요
- 권한: `ADMIN`
- Response data: `TrainingLogCommentReportAdminResponse`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | `ReportStatus` | O | 처리 상태 |
| `processingMemo` | `String?` | - | 처리 메모 |
| `finalAction` | `String?` | - | 최종 조치 메모 |

구현 메모:

- 상태 변경 시 `processedByUserId`, `processedAt`을 함께 기록한다.
- `finalAction`은 `CONTENT_HIDDEN`, `WARNING`, `TEMP_SUSPENSION` 같은 운영 메모 raw value를 저장하는 용도로 사용한다.
- 사용자 제재가 필요하면 기존 `user_sanctions` 관리자 흐름에서 후속 조치를 수행한다. 현재 훈련일지 댓글 신고 상태 변경이 자동으로 제재 row를 생성하지는 않는다.

### 5.13 알림 API 재사용

훈련일지 댓글/대댓글 알림 조회와 읽음 처리는 공용 알림 API를 그대로 사용한다.

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/badge`
- `PATCH /api/v1/notifications/{id}/read`

## 6. 구현 메모

- 친구 훈련일지 읽기 경험은 일반 훈련일지 화면을 read-only로 재사용하는 것을 목표로 한다.
- 친구 열람용 API는 기존 `/api/v1/training-logs/me/*`와 분리한다.
- 친구 관계 확인과 `shareWithFriends` 확인은 모든 친구 훈련일지 읽기 API의 공통 가드다.
- `TrainingLogEntry.visibility`는 개인 훈련일지 메타데이터로 유지하되, 친구 열람 권한 결정에는 사용하지 않는다.
- 차단 정책은 친구 공개 설정보다 우선한다.
- 커뮤니티의 좋아요/댓글/알림 패턴을 최대한 재사용하는 것이 맞다.
- 댓글 신고는 공용 `ReportTargetType` 확장 대신 전용 `TrainingLogCommentReport` 모델로 저장한다.
- 신고 누적 삭제 기준은 `training_log_comments.reportCount >= 3`이다.
- 관리자 댓글 신고 조회/처리는 `/api/v1/admin/training-logs/comments/reports*` 경로로 운영하고, 일반 사용자 댓글 삭제와 별도로 운영 메모를 남긴다.
- 댓글 삭제 책임은 현재 공용 댓글 삭제 API에서 분기한다. 댓글 작성자, 기록 작성자, 관리자는 `DELETE /api/v1/training-logs/comments/{commentId}`로 삭제할 수 있고, 관리자 신고 상태 변경 API는 삭제를 자동 수행하지 않는다.
- 댓글 알림은 `@TransactionalEventListener(AFTER_COMMIT)`에서 알림함 저장 후 푸시 발송을 시도한다.

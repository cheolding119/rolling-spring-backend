# Community Domain Spec

## Status

- 커뮤니티 도메인은 현재 Phase 1, 2, 3, 4, 5, 6, 7의 핵심 기능이 구현되어 있다.
- 현재 구현 범위는 커뮤니티 전용 닉네임, 게시글, 댓글, 좋아요, 신고, 이미지 첨부, 관리자 숨김/신고 처리, 댓글 알림이다.
- 사용자 차단 기반 필터링과 Flutter UX 분기는 아직 구현하지 않았다.

## Model

### User community nickname

- 오픈매트와 대회는 기존 `User.nickname`을 사용한다.
- 커뮤니티 게시글과 댓글은 `User.communityNickname`을 사용한다.
- `communityNickname`이 없으면 게시글/댓글 작성 API는 `COMMUNITY_NICKNAME_REQUIRED`로 거절한다.
- `communityNickname`은 커뮤니티 전용 표시명이며, `configured` 플래그는 없다.

### CommunityPost

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 게시글 ID |
| `author` | `User` | 작성자 |
| `category` | `CommunityPostCategory` | 게시글 카테고리 |
| `title` | `String` | 제목 |
| `content` | `String` | 본문 |
| `viewCount` | `Long` | 조회 수 |
| `likeCount` | `Long` | 좋아요 수 캐시 |
| `commentCount` | `Long` | 댓글 수 캐시 |
| `reportCount` | `Long` | 신고 수 캐시 |
| `status` | `CommunityPostStatus` | 게시글 상태 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |
| `deletedAt` | `LocalDateTime?` | 삭제 시각 |

### CommunityPostImage

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 이미지 ID |
| `post` | `CommunityPost` | 대상 게시글 |
| `imageUrl` | `String` | 공개 이미지 URL |
| `sortOrder` | `Integer` | 노출 순서 |
| `createdAt` | `LocalDateTime` | 생성 시각 |

### CommunityPostLike

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 좋아요 ID |
| `post` | `CommunityPost` | 대상 게시글 |
| `user` | `User` | 좋아요한 사용자 |
| `createdAt` | `LocalDateTime` | 생성 시각 |

### CommunityPostReport

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 신고 ID |
| `post` | `CommunityPost` | 대상 게시글 |
| `reporter` | `User` | 신고한 사용자 |
| `reason` | `ReportReason` | 신고 사유 |
| `customReason` | `String?` | 기타 사유 |
| `status` | `ReportStatus` | 신고 처리 상태 |
| `processedByUserId` | `Long?` | 처리한 관리자 ID |
| `processedAt` | `LocalDateTime?` | 처리 시각 |
| `processingMemo` | `String?` | 처리 메모 |
| `finalAction` | `String?` | 최종 조치 |
| `createdAt` | `LocalDateTime` | 생성 시각 |

### CommunityCommentReport

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 신고 ID |
| `comment` | `CommunityComment` | 대상 댓글 |
| `reporter` | `User` | 신고한 사용자 |
| `reason` | `ReportReason` | 신고 사유 |
| `customReason` | `String?` | 기타 사유 |
| `status` | `ReportStatus` | 신고 처리 상태 |
| `processedByUserId` | `Long?` | 처리한 관리자 ID |
| `processedAt` | `LocalDateTime?` | 처리 시각 |
| `processingMemo` | `String?` | 처리 메모 |
| `finalAction` | `String?` | 최종 조치 |
| `createdAt` | `LocalDateTime` | 생성 시각 |

`CommunityPostCategory` 현재 값:

- `FREE`
- `TECHNIQUE_QNA`
- `SPARRING_FEEDBACK`
- `GYM_INFO`
- `GEAR`
- `EVENT_REVIEW`
- `CONDITIONING`
- `NOTICE`

`CommunityPostStatus` 현재 값:

- `ACTIVE`
- `HIDDEN`
- `DELETED`

### CommunityComment

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 댓글 ID |
| `post` | `CommunityPost` | 대상 게시글 |
| `author` | `User` | 작성자 |
| `content` | `String` | 댓글 본문 |
| `reportCount` | `Long` | 신고 수 캐시 |
| `status` | `CommunityCommentStatus` | 댓글 상태 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |
| `deletedAt` | `LocalDateTime?` | 삭제 시각 |

`CommunityCommentStatus` 현재 값:

- `ACTIVE`
- `HIDDEN`
- `DELETED`

## API

### 1. 커뮤니티 닉네임 조회

`GET /api/v1/users/me/community-profile`

- 인증: 필요
- 용도: 현재 로그인 사용자의 커뮤니티 표시명 조회

Response data:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `communityNickname` | `String?` | 커뮤니티 전용 닉네임 |

### 2. 커뮤니티 닉네임 수정

`PATCH /api/v1/users/me/community-profile`

- 인증: 필요
- 용도: 현재 로그인 사용자의 커뮤니티 표시명 설정 또는 변경

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `communityNickname` | `String` | O | 커뮤니티 전용 닉네임 |

Response data:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `communityNickname` | `String` | 변경된 커뮤니티 전용 닉네임 |

검증:

- 2자 이상 20자 이하
- 앞뒤 공백 trim 후 저장
- 공백만 있는 값은 `VALIDATION_ERROR`
- MVP에서는 중복 닉네임 허용

### 3. 게시글 목록 조회

`GET /api/v1/community/posts`

- 인증: 선택
- 비회원: 가능
- 용도: 커뮤니티 게시글 목록 조회

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `category` | `String` | - | 카테고리 필터 |
| `q` | `String` | - | 제목/본문 검색어 |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `20` | 페이지 크기 |
| `sort` | `String` | `createdAt,desc` | 정렬 |

Response item:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 게시글 ID |
| `category` | `CommunityPostCategory` | 카테고리 |
| `title` | `String` | 제목 |
| `authorNickname` | `String` | 작성자의 `communityNickname` |
| `likeCount` | `Long` | 좋아요 수 |
| `commentCount` | `Long` | 댓글 수 |
| `viewCount` | `Long` | 조회 수 |
| `createdAt` | `LocalDateTime` | 생성 시각 |

비회원 응답에는 개인화된 `likedByMe`, `editableByMe` 같은 필드는 포함하지 않는다.

### 4. 게시글 상세 조회

`GET /api/v1/community/posts/{id}`

- 인증: 선택
- 비회원: 가능
- 용도: 게시글 상세 조회

Response item:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 게시글 ID |
| `category` | `CommunityPostCategory` | 카테고리 |
| `title` | `String` | 제목 |
| `content` | `String` | 본문 |
| `authorNickname` | `String` | 작성자의 `communityNickname` |
| `commentCount` | `Long` | 댓글 수 |
| `viewCount` | `Long` | 조회 수 |
| `likeCount` | `Long` | 좋아요 수 |
| `editableByMe` | `Boolean` | 로그인 사용자의 수정/삭제 가능 여부 |
| `likedByMe` | `Boolean` | 로그인 사용자의 좋아요 여부 |
| `images` | `List<CommunityPostImageResponse>` | 이미지 목록 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

### 5. 게시글 작성

`POST /api/v1/community/posts`

- 인증: 필요
- 비회원: 불가
- 선행 조건: `communityNickname` 설정 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `category` | `CommunityPostCategory` | O | 게시글 카테고리 |
| `title` | `String` | O | 제목 |
| `content` | `String` | O | 본문 |
| `imageUrls` | `List<String>` | - | 첨부 이미지 URL 목록 |

Response item:

- `CommunityPostDetailResponse`

검증:

- 제목은 2자 이상 80자 이하
- 본문은 10자 이상 5000자 이하
- 이미지는 최대 5장

### 6. 게시글 수정

`PATCH /api/v1/community/posts/{id}`

- 인증: 필요
- 권한: 작성자 또는 관리자

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `category` | `CommunityPostCategory` | - | 게시글 카테고리 |
| `title` | `String` | - | 제목 |
| `content` | `String` | - | 본문 |
| `imageUrls` | `List<String>` | - | 전체 교체할 이미지 URL 목록 |

Response item:

- `CommunityPostDetailResponse`

정책:

- 최소 1개 필드는 전달해야 한다.
- 수정은 작성자 또는 관리자만 가능하다.
- 삭제된 게시글은 수정 대상이 아니다.
- 수정 시 이미지 목록은 전체 교체 방식으로 저장한다.

### 7. 게시글 삭제

`DELETE /api/v1/community/posts/{id}`

- 인증: 필요
- 권한: 작성자 또는 관리자
- Response data: `null`

정책:

- soft delete로 처리한다.
- 삭제된 게시글은 일반 조회에서 `NOT_FOUND`로 처리한다.

### 8. 댓글 목록 조회

`GET /api/v1/community/posts/{postId}/comments`

- 인증: 선택
- 비회원: 가능

Query parameters:

| 파라미터 | 타입 | 기본값 |
| --- | --- | --- |
| `page` | `Integer` | `0` |
| `size` | `Integer` | `50` |
| `sort` | `String` | `createdAt,asc` |

Response item:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 댓글 ID |
| `postId` | `Long` | 게시글 ID |
| `authorNickname` | `String` | 작성자의 `communityNickname` |
| `content` | `String` | 댓글 본문 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

### 9. 댓글 작성

`POST /api/v1/community/posts/{postId}/comments`

- 인증: 필요
- 비회원: 불가
- 선행 조건: `communityNickname` 설정 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `content` | `String` | O | 댓글 본문 |

Response item:

- `CommunityCommentResponse`

검증:

- 댓글은 1자 이상 1000자 이하
- 삭제되거나 숨김 처리된 게시글에는 댓글 작성 불가

### 10. 댓글 수정

`PATCH /api/v1/community/comments/{commentId}`

- 인증: 필요
- 권한: 작성자 또는 관리자

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `content` | `String` | O | 댓글 본문 |

Response item:

- `CommunityCommentResponse`

### 11. 댓글 삭제

`DELETE /api/v1/community/comments/{commentId}`

- 인증: 필요
- 권한: 작성자 또는 관리자
- Response data: `null`

### 12. 게시글 좋아요

`POST /api/v1/community/posts/{id}/like`

- 인증: 필요
- 비회원: 불가
- Response data: `null`

### 13. 게시글 좋아요 취소

`DELETE /api/v1/community/posts/{id}/like`

- 인증: 필요
- 비회원: 불가
- Response data: `null`

### 14. 게시글 신고

`POST /api/v1/community/posts/{id}/report`

- 인증: 필요
- 비회원: 불가

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `String` | O | 신고 사유 |
| `customReason` | `String?` | - | `OTHER` 선택 시 필요 |

Response data: `null`

### 15. 댓글 신고

`POST /api/v1/community/comments/{commentId}/report`

- 인증: 필요
- 비회원: 불가

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `String` | O | 신고 사유 |
| `customReason` | `String?` | - | `OTHER` 선택 시 필요 |

Response data: `null`

### 16. 관리자 커뮤니티 API

- `GET /api/v1/admin/community/posts`
- `GET /api/v1/admin/community/posts/{id}`
- `PATCH /api/v1/admin/community/posts/{id}/hide`
- `PATCH /api/v1/admin/community/posts/{id}/unhide`
- `GET /api/v1/admin/community/comments`
- `GET /api/v1/admin/community/comments/{id}`
- `PATCH /api/v1/admin/community/comments/{id}/hide`
- `PATCH /api/v1/admin/community/comments/{id}/unhide`
- `GET /api/v1/admin/community/posts/reports`
- `GET /api/v1/admin/community/comments/reports`
- `PATCH /api/v1/admin/community/posts/reports/{id}/status`
- `PATCH /api/v1/admin/community/comments/reports/{id}/status`

관리자 API는 모두 `ROLE_ADMIN` accessToken이 필요하다.

### 17. 댓글 알림

- 댓글 작성 시 게시글 작성자에게 `PushNotificationType.COMMUNITY_COMMENT_CREATED` 알림을 저장한다.
- 자기 게시글에 직접 단 댓글은 알림을 만들지 않는다.
- 알림 route는 `/community/posts/{postId}`를 사용한다.

## Policy

- 비회원은 게시글 목록, 게시글 상세, 댓글 목록만 조회할 수 있다.
- 공개 조회는 `Authorization` 헤더 없이 호출할 수 있다.
- 작성, 수정, 삭제는 로그인 사용자만 가능하다.
- 커뮤니티 응답은 `authorNickname`만 노출하고 `affiliation`, `email`, `phone`, `socialProvider`는 노출하지 않는다.
- 내부 권한 검사는 표시명이 아니라 `userId` 기준으로 처리한다.
- Flutter 쪽 진입 UX와 비회원 액션 분기는 아직 앱 레이어에서 마무리해야 한다.

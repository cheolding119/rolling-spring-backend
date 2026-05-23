# Shared Common Models And APIs

- 공통 모델과 도메인 간 공유 API 스펙을 관리한다.
- 도메인별 세부 계약은 각 도메인 문서를 본다.

## 5.1 인증 API

### 5.1.1 소셜 로그인

`POST /api/v1/auth/login`

- 인증: 불필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `provider` | `String` | O | 현재 구현 허용값 `GOOGLE`, `KAKAO`, `APPLE` |
| `accessToken` | `String` | O | 소셜 제공자 access token. `APPLE`은 identity token |

Response data:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `accessToken` | `String` | JWT access token |
| `refreshToken` | `String` | JWT refresh token |
| `tokenType` | `String` | 항상 `Bearer` |
| `expiresIn` | `Long` | access token 만료 시간(초) |
| `newUser` | `Boolean` | 신규 회원 여부 |
| `userId` | `Long` | 사용자 ID |
| `email` | `String` | 사용자 이메일 |
| `name` | `String` | 사용자 이름 |
| `isAdmin` | `Boolean` | 관리자 여부 |
| `accountStatus` | `String` | 현재 계정 상태 |
| `suspensionUntil` | `DateTime?` | 정지 종료 시각 |
| `sanctionReasonSummary` | `String?` | 제재 사유 요약 |

에러:

- `UNSUPPORTED_PROVIDER`
- `KAKAO_API_ERROR`
- `GOOGLE_API_ERROR`
- `APPLE_API_ERROR`
- `VALIDATION_ERROR`

현재 구현 메모:

- Apple 로그인은 iOS 네이티브 `identityToken` 검증 방식으로 지원한다.
- Apple 로그인 계약은 `C:\rolling\.codex-shared\api-spec.md`와 `C:\rolling\.codex-shared\domain-models.md`에도 반영되어 있다.
- 로그인 응답에는 현재 사용자 기준 `isAdmin`이 포함된다.
- 로그인 응답에는 제재 상태 확인용 `accountStatus`, `suspensionUntil`, `sanctionReasonSummary`가 포함된다.
- 현재 서버는 사용자당 활성 refresh token 1개만 유지한다.

### 5.1.2 토큰 갱신

`POST /api/v1/auth/refresh`

- 인증: 불필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `refreshToken` | `String` | O |

Response data:

- `accessToken`
- `refreshToken`
- `tokenType`
- `expiresIn`
- `isAdmin`
- `accountStatus`
- `suspensionUntil`
- `sanctionReasonSummary`

에러:

- `INVALID_REFRESH_TOKEN`
- `EXPIRED_REFRESH_TOKEN`
- `VALIDATION_ERROR`


현재 구현 메모:

- 토큰 갱신 응답에도 현재 사용자 기준 `isAdmin`이 포함된다.
- 토큰 갱신 응답에도 제재 상태 확인용 `accountStatus`, `suspensionUntil`, `sanctionReasonSummary`가 포함된다.
- 토큰 갱신은 전달한 `refreshToken` 검증 후 기존 refresh token 을 폐기하고 새 refresh token 을 발급한다.
- 동일 사용자에 대해서는 동시에 여러 refresh token 을 유지하지 않는다.

### 5.1.3 로그아웃

`POST /api/v1/auth/logout`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `fcmToken` | `String` | - | 현재 디바이스 토큰을 함께 제거할 때 전달 |

- Response data: `null`

현재 구현 메모:

- 요청 본문에 `fcmToken`을 보내면 현재 사용자에게 연결된 해당 디바이스 토큰도 같이 제거한다.
- `fcmToken` 없이 호출하면 Refresh Token만 무효화한다.
- 로그아웃 시 토큰 제거와 `DELETE /api/v1/users/me/fcm`의 역할 분리는 현재 동작은 가능하지만, 운영 문서 기준으로는 한 번 더 정리할 예정이다.

### 5.1.4 회원 탈퇴 요청

`DELETE /api/v1/auth/withdraw`

- 인증: 필요

Response data:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `withdrawalPending` | `Boolean` | 탈퇴 예약 상태 |
| `scheduledAt` | `DateTime` | 탈퇴 예정 시각 |

현재 구현 메모:

- 탈퇴는 즉시 실행되지 않는다.
- 요청 다음 날 `21:00` (`Asia/Seoul`)에 실행된다.
- `SUSPENDED` 상태에서도 탈퇴 요청은 허용된다.

### 5.1.5 회원 탈퇴 취소

`POST /api/v1/auth/withdraw/cancel`

- 인증: 필요

Response data:

| 필드 | 타입 |
| --- | --- |
| `withdrawalPending` | `Boolean` |
| `scheduledAt` | `DateTime?` |

현재 구현 메모:

- `SUSPENDED` 상태에서도 탈퇴 취소는 허용된다.

에러:

- `WITHDRAWAL_NOT_PENDING`


## 5.2 사용자 API

### 5.2.1 내 정보 조회

`GET /api/v1/users/me`

- 인증: 필요

Response data:

| 필드 | 타입 |
| --- | --- |
| `id` | `Long` |
| `nickname` | `String` |
| `email` | `String?` |
| `phone` | `String?` |
| `affiliation` | `String?` |
| `socialProvider` | `String` |
| `beltColor` | `String` |
| `createdAt` | `DateTime` |
| `withdrawalPending` | `Boolean` |
| `withdrawalScheduledAt` | `DateTime?` |
| `accountStatus` | `String` |
| `suspensionUntil` | `DateTime?` |
| `sanctionReasonSummary` | `String?` |
| `isAdmin` | `Boolean` |
| `settings` | `Object` |
| `settings.pushNotificationEnabled` | `Boolean` |

현재 구현 메모:

- `/users/me` 응답에는 현재 사용자 기준 `isAdmin` 필드가 포함된다.
- `/users/me` 응답에는 사용자 설정 `settings.pushNotificationEnabled`와 소속 `affiliation`이 포함된다.
- `/users/me` 응답에는 제재 상태 확인용 `accountStatus`, `suspensionUntil`, `sanctionReasonSummary`가 포함된다.
- 로그인 응답과 토큰 갱신 응답에도 같은 의미의 `isAdmin`이 포함된다.
- 프론트는 요청 시 `ROLE` 값을 따로 보내지 않고 `Authorization: Bearer {accessToken}`만 보낸다.
- 서버는 accessToken에서 확인한 `userId`와 `admin.user-ids` 설정값으로 `ROLE_USER`/`ROLE_ADMIN`을 내부 판단한다.
- 프론트는 `isAdmin=true`일 때 관리자 UI를 노출할 수 있다.
- 실제 보호는 계속 서버의 관리자 API 권한 검사와 `403 FORBIDDEN` 응답으로 처리한다.
- `isAdmin`은 UI 제어용 보조 정보이고, 최종 권한 판단 기준은 항상 서버다.

### 5.2.2 내 정보 수정

`PUT /api/v1/users/me`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `nickname` | `String` | - |
| `affiliation` | `String` | - |
| `beltColor` | `String` | - |

현재 구현 메모:

- `phone` 수정은 아직 미지원이다.
- `affiliation`은 선택 입력이며, 빈 값 없이 전달된 문자열을 그대로 반영한다.

### 5.2.3 내 설정 수정

`PATCH /api/v1/users/me/settings`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `pushNotificationEnabled` | `Boolean` | O |

Response data:

| 필드 | 타입 |
| --- | --- |
| `pushNotificationEnabled` | `Boolean` |

현재 구현 메모:

- 이 설정은 사용자 전역 푸시 수신 설정이다.
- `false`면 해당 사용자는 모든 디바이스에서 FCM 발송 대상에서 제외된다.
- `Notification` 알림함 저장 자체는 계속 유지된다.
- 앱은 마이페이지 진입 시 `GET /api/v1/users/me`의 `settings.pushNotificationEnabled`를 source of truth로 사용한다.
- 앱에서 스위치를 변경하면 `PATCH /api/v1/users/me/settings`를 호출한다.
- 푸시를 꺼도 `GET /api/v1/notifications` 알림함 데이터는 계속 조회 가능해야 한다.

### 5.2.4 FCM 토큰 등록

`POST /api/v1/users/me/fcm`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `fcmToken` | `String` | O |
| `platform` | `String` | - |
| `deviceId` | `String` | - |
| `appVersion` | `String` | - |

Response data: `null`

현재 구현 메모:

- 토큰 저장 구조는 `user_devices` 1:N 이다.
- 동일 토큰 재등록 시 기존 디바이스 레코드를 재사용한다.
- 동일 토큰 재등록 시 `platform`, `deviceId`, `appVersion`, `updatedAt`도 최신값으로 갱신한다.
- 로그아웃/탈퇴/기기 변경 시점별 토큰 정리 규칙은 운영 문서에서 추가 정리 중이다.

### 5.2.5 FCM 토큰 삭제

`DELETE /api/v1/users/me/fcm`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `fcmToken` | `String` | O |

Response data: `null`

현재 구현 메모:

- 현재 로그인한 사용자에게 연결된 토큰만 삭제한다.
- 존재하지 않는 토큰이어도 성공 응답을 반환한다.
- 로그아웃 API의 선택적 `fcmToken` 제거와 함께 같은 토큰 라이프사이클 정책으로 관리한다.

### 5.2.6 사용자 차단

`POST /api/v1/users/{id}/block`

- 인증: 필요
- Response data: `null`

현재 구현 메모:

- 차단은 조회자 기준으로 동작한다.
- 차단한 사용자의 오픈매트/대회 콘텐츠는 목록과 상세에서 제외된다.
- 차단 정보는 `user_blocked_users` 조인 테이블에 저장되며 `blocked_at`으로 차단 시각을 기록한다.
- `GET /api/v1/users/blocks`로 차단한 사용자 목록과 차단 시각을 조회할 수 있다.

### 5.2.7 사용자 차단 해제

`DELETE /api/v1/users/{id}/block`

- 인증: 필요
- Response data: `null`

현재 구현 메모:

- 차단 해제는 기존 차단 관계를 제거한다.
- 차단 해제 이후에는 오픈매트/대회 콘텐츠가 다시 일반 조회 결과에 노출된다.

### 5.2.8 관리자 사용자 제재

관리자 사용자 제재는 운영 상태로 구현되어 있다.

핵심 원칙:

- 제재 이력은 `user_sanctions` 테이블에 저장한다.
- `users`에는 `accountStatus`, `suspensionUntil`, `sanctionReasonSummary`를 두어 현재 상태를 빠르게 보여준다.
- 상태 예시: `ACTIVE`, `WARNING`, `SUSPENDED`, `WITHDRAWN`
- `TEMP_SUSPEND`는 로그인 허용 + 제한 모드다.
- 무기한 정지도 별도 상태가 아니라 장기 `TEMP_SUSPEND`로 처리한다.
- `RELEASE`는 제재 타입이 아니라 해제 동작이다.

현재 관리자 API:

- `GET /api/v1/admin/users`
- `GET /api/v1/admin/users/{id}`
- `GET /api/v1/admin/users/{id}/sanctions`
- `POST /api/v1/admin/users/{id}/sanctions`
- `DELETE /api/v1/admin/users/{id}/sanctions/{sanctionId}`

`GET /api/v1/admin/users` 응답 필드:

- `id`
- `nickname`
- `email`
- `affiliation`
- `createdAt`
- `accountStatus`
- `suspensionUntil`
- `lastSanctionAt`

`GET /api/v1/admin/users/{id}` 응답 필드:

- `id`
- `nickname`
- `email`
- `phone`
- `affiliation`
- `socialProvider`
- `beltColor`
- `createdAt`
- `accountStatus`
- `suspensionUntil`
- `sanctionReasonSummary`
- `isWithdrawn`
- `withdrawalPending`
- `withdrawalScheduledAt`
- `lastSanctionAt`

`GET /api/v1/admin/users/{id}/sanctions` 응답 필드:

- `id`
- `type`
- `reason`
- `memo`
- `startsAt`
- `endsAt`
- `createdByUserId`
- `createdAt`
- `releasedByUserId`
- `releasedAt`

`POST /api/v1/admin/users/{id}/sanctions` 요청 body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `type` | `String` | O | `WARNING`, `TEMP_SUSPEND` |
| `reason` | `String` | O | 제재 사유 |
| `memo` | `String` | - | 운영 메모 |
| `endsAt` | `DateTime` | - | `TEMP_SUSPEND`일 때만 필요 |

`POST /api/v1/admin/users/{id}/sanctions` 응답:

- `UserSanctionResponse`

`DELETE /api/v1/admin/users/{id}/sanctions/{sanctionId}` 응답:

- `null`

제한 모드 메모:

- 허용 범위는 문의, 도움말, 알림 on/off, 차단한 사용자 관리, 탈퇴 요청/취소, 로그아웃으로 최소화한다.
- 사용자 정보 수정은 기본 차단한다.
- 제재 만료는 스케줄러가 자동 해제한다.
- 장기 정지도 제한 모드에서 문의와 탈퇴 경로를 유지한다.

### 5.2.9 커뮤니티 닉네임 API

커뮤니티 전용 닉네임은 `User.nickname`과 분리해 관리한다.

#### 5.2.9.1 커뮤니티 닉네임 조회

`GET /api/v1/users/me/community-profile`

- 인증: 필요

Response data:

| 필드 | 타입 |
| --- | --- |
| `communityNickname` | `String?` |

현재 구현 메모:

- `configured` 같은 별도 플래그는 없다.
- `communityNickname`이 없으면 앱에서 설정 화면으로 유도한다.

#### 5.2.9.2 커뮤니티 닉네임 수정

`PATCH /api/v1/users/me/community-profile`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `communityNickname` | `String` | O |

Response data:

| 필드 | 타입 |
| --- | --- |
| `communityNickname` | `String` |

현재 구현 메모:

- 2자 이상 20자 이하만 허용한다.
- 앞뒤 공백은 trim 후 저장한다.
- 공백만 있는 값은 `VALIDATION_ERROR`다.
- MVP에서는 중복 닉네임을 허용한다.


## 5.3 알림 API

구현 상태 메모:

- 현재 서버는 `GET /api/v1/notifications` 목록 조회, `GET /api/v1/notifications/badge` 미읽음 개수 조회, `PATCH /api/v1/notifications/{id}/read` 읽음 처리를 지원한다.
- notification badge 같은 미읽음 개수 확장은 `새로운 version을 올리기보다 /api/v1 안에서 additive change`로 진행하는 것을 기본 원칙으로 한다.
- 현재 알림 type 예시에는 `COMMUNITY_COMMENT_CREATED`, `TRAINING_LOG_COMMENT_CREATED`, `TRAINING_LOG_COMMENT_REPLY_CREATED`, `INQUIRY_ANSWERED`가 포함된다.

### 5.3.1 알림 목록 조회

`GET /api/v1/notifications`

- 인증: 필요

Query parameters:

| 파라미터 | 타입 | 기본값 |
| --- | --- | --- |
| `page` | `Integer` | `0` |
| `size` | `Integer` | `20` |

기본 정렬:

- `createdAt DESC`

Response item:

| 필드 | 타입 |
| --- | --- |
| `id` | `Long` |
| `type` | `String` |
| `targetId` | `Long` |
| `route` | `String` |
| `title` | `String` |
| `body` | `String` |
| `readAt` | `DateTime?` |
| `createdAt` | `DateTime` |

### 5.3.2 알림 배지 조회

`GET /api/v1/notifications/badge`

- 인증: 필요

Response data:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `unreadCount` | `Long` | 현재 로그인한 사용자의 미읽음 알림 개수 |

계산 기준:

- `readAt` 이 `null` 인 알림만 count 한다.
- 현재 로그인한 사용자 본인 알림만 count 한다.

클라이언트 동기화 메모:

- 읽음 처리 성공 후 클라이언트는 `GET /api/v1/notifications/badge`를 재호출해 최신 unread count를 반영한다.

### 5.3.3 알림 읽음 처리

`PATCH /api/v1/notifications/{id}/read`

- 인증: 필요
- Response data: `null`


## 5.6 공지사항 API

구현 상태 메모:

- 현재 서버는 조회 API(`GET /api/v1/notices`, `GET /api/v1/notices/{id}`)와 운영 API(`POST/PUT/DELETE /api/v1/notices`)를 지원한다.
- 앱 범위에서는 계속 조회 API만 사용한다.
- 운영자는 Apidog 또는 관리자 페이지에서 `Authorization: Bearer {accessToken}`으로 공지사항 작성/수정/삭제를 수행한다.
- 공지사항 운영 API는 `ROLE_ADMIN` accessToken이 필요하며, 관리자는 `admin.user-ids` 설정으로 판별한다.

### 5.6.1 공지사항 목록 조회

`GET /api/v1/notices`

- 인증: 불필요

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `20` | 페이지 크기 |

기본 정렬:

- `createdAt DESC`

Response: 페이징된 `NoticeModel`

프론트 메모:

- 목록 item에 `content`가 포함되어도 된다.
- 현재 구현 기준으로 목록 item에는 `updatedAt`이 포함되지 않는다.
- 앱에서는 목록에서 본문 일부만 잘라 보여줘도 되고, 상세에서는 전체 본문을 보여주면 된다.

### 5.6.2 공지사항 상세 조회

`GET /api/v1/notices/{id}`

- 인증: 불필요
- Response: `NoticeModel`

에러:

- `NOT_FOUND`

### 5.6.3 공지사항 생성

`POST /api/v1/notices`

- 인증: `Authorization: Bearer {accessToken}` 필요 (`ROLE_ADMIN`)

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | `String` | O | 공지사항 제목 |
| `content` | `String` | O | 공지사항 본문 |
| `authorName` | `String` | O | 앱에 노출할 작성자 이름 |
| `createdBy` | `String` | - | 운영 내부 추적용 작성자 식별자 |

Response: `NoticeModel`

현재 구현 메모:

- `createdBy`가 없으면 `authorName` 값을 그대로 저장한다.
- `X-Crawler-Admin-Key`와 `tournament.crawler.admin-key` 기반 우회 정책은 제거됐다.

### 5.6.4 공지사항 수정

`PUT /api/v1/notices/{id}`

- 인증: `Authorization: Bearer {accessToken}` 필요 (`ROLE_ADMIN`)

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | `String` | - | 공지사항 제목 |
| `content` | `String` | - | 공지사항 본문 |
| `authorName` | `String` | - | 앱에 노출할 작성자 이름 |
| `createdBy` | `String` | - | 운영 내부 추적용 작성자 식별자 |

Response: `NoticeModel`

현재 구현 메모:

- 최소 1개 필드는 전달해야 한다.
- 전달하지 않은 필드는 기존 값을 유지한다.
- `X-Crawler-Admin-Key`와 `tournament.crawler.admin-key` 기반 우회 정책은 제거됐다.

에러:

- `VALIDATION_ERROR`
- `NOT_FOUND`

### 5.6.5 공지사항 삭제

`DELETE /api/v1/notices/{id}`

- 인증: `Authorization: Bearer {accessToken}` 필요 (`ROLE_ADMIN`)
- Response data: `null`

현재 구현 메모:

- 삭제는 soft delete가 아니라 `hard delete`다.
- `X-Crawler-Admin-Key`와 `tournament.crawler.admin-key` 기반 우회 정책은 제거됐다.

에러:

- `NOT_FOUND`


## 5.7 문의 API

구현 상태 메모:

- 문의 도메인 명칭은 `Inquiry`로 확정했다.
- 사용자 API는 `POST /api/v1/inquiries`, `GET /api/v1/inquiries`, `GET /api/v1/inquiries/{id}`다.
- 관리자 API는 `GET /api/v1/admin/inquiries`, `GET /api/v1/admin/inquiries/{id}`, `PATCH /api/v1/admin/inquiries/{id}/answer`, `PATCH /api/v1/admin/inquiries/{id}/status`다.
- 관리자 API는 `Authorization: Bearer {accessToken}` + `ROLE_ADMIN` 기준으로 보호한다.
- 첫 답변 완료 시 알림함에 `INQUIRY_ANSWERED` 알림을 저장한다.

### 5.7.1 문의 생성

`POST /api/v1/inquiries`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | `String` | O | 문의 제목 |
| `content` | `String` | O | 문의 본문 |

Response: `InquiryModel`

현재 구현 메모:

- 생성 시 상태는 항상 `RECEIVED`다.
- 생성자는 accessToken 기준 현재 로그인 사용자로 결정한다.

### 5.7.2 내 문의 목록 조회

`GET /api/v1/inquiries`

- 인증: 필요
- Response: 페이징된 `InquiryModel`

Query parameters:

| 파라미터 | 타입 | 기본값 |
| --- | --- | --- |
| `page` | `Integer` | `0` |
| `size` | `Integer` | `20` |
| `sort` | `String` | `createdAt,desc` |

현재 구현 메모:

- 현재 로그인한 사용자의 문의만 최신순으로 조회한다.

### 5.7.3 내 문의 상세 조회

`GET /api/v1/inquiries/{id}`

- 인증: 필요
- Response: `InquiryModel`

에러:

- `NOT_FOUND`

현재 구현 메모:

- 현재 로그인한 사용자 본인 문의가 아니면 `NOT_FOUND`로 처리한다.

### 5.7.4 관리자 문의 목록 조회

`GET /api/v1/admin/inquiries`

- 인증: `Authorization: Bearer {accessToken}` 필요 (`ROLE_ADMIN`)
- Response: 페이징된 `InquiryModel`

Query parameters:

| 파라미터 | 타입 | 기본값 |
| --- | --- | --- |
| `page` | `Integer` | `0` |
| `size` | `Integer` | `20` |
| `sort` | `String` | `createdAt,desc` |

### 5.7.5 관리자 문의 상세 조회

`GET /api/v1/admin/inquiries/{id}`

- 인증: `Authorization: Bearer {accessToken}` 필요 (`ROLE_ADMIN`)
- Response: `InquiryModel`

에러:

- `NOT_FOUND`

### 5.7.6 관리자 문의 답변 저장

`PATCH /api/v1/admin/inquiries/{id}/answer`

- 인증: `Authorization: Bearer {accessToken}` 필요 (`ROLE_ADMIN`)

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `answerContent` | `String` | O | 운영자 답변 본문 |

Response: `InquiryModel`

현재 구현 메모:

- 답변 저장 시 `answerContent`, `answeredByUserId`, `answeredAt`을 기록한다.
- 답변 저장 시 상태는 `ANSWERED`로 변경된다.
- 이미 `ANSWERED` 상태에서 답변 내용을 수정하는 것은 가능하지만, 추가 알림은 보내지 않는다.

### 5.7.7 관리자 문의 상태 변경

`PATCH /api/v1/admin/inquiries/{id}/status`

- 인증: `Authorization: Bearer {accessToken}` 필요 (`ROLE_ADMIN`)

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | `String` | O | `RECEIVED`, `IN_REVIEW`, `ANSWERED` |

Response: `InquiryModel`

에러:

- `VALIDATION_ERROR`
- `NOT_FOUND`

현재 구현 메모:

- 답변이 없는 문의는 `ANSWERED` 상태로 변경할 수 없다.
- 답변이 저장된 문의는 `ANSWERED` 외 상태로 되돌리지 않는다.

## 6. 날짜/시간 형식

| 타입 | 형식 | 예시 |
| --- | --- | --- |
| `DateTime` | ISO 8601 | `2026-03-17T21:00:00` |
| `Date` | ISO 8601 | `2026-03-17` |



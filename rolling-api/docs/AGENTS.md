
## 1. 공용 도메인 참조

- 공용 enum, 공용 모델, 공용 도메인 규칙의 source of truth는 [domain-models.md](/C:/rolling/.codex-shared/domain-models.md)다.
- 이 문서에서는 도메인 원문을 중복 정의하지 않고, 작업 중 자주 보는 포인트만 요약으로 남긴다.

빠르게 보는 도메인 포인트:

- 오픈매트는 정원이 차면 `CLOSED`, 종료 시점이 지나면 `FINISHED`가 되며, 신고 누적 3건 이상이면 신규 신청이 차단된다.
- 호스트는 자신이 주최한 오픈매트에 신청할 수 없다.
- 사용자 차단은 조회자 기준 개인화 필터이며, 차단한 작성자의 오픈매트/대회는 목록과 상세에서 숨긴다.
- 로그인 사용자의 오픈매트/대회 목록·검색 요청은 `Authorization: Bearer {accessToken}`을 함께 보내야 차단 필터가 적용된다.
- 알림의 source of truth는 FCM 성공 여부가 아니라 백엔드 `Notification` 저장 데이터다.
- 공지사항은 일반 사용자 앱에서 읽기 전용 기능으로 다룬다.
- 문의 도메인 명칭은 `Inquiry`로 통일하며, 첫 답변 완료 시 `INQUIRY_ANSWERED` 알림을 저장한다.

## 2. 프론트엔드 구현 원칙

### 4.1 기술 스택

- Framework: Flutter
- Language: Dart
- Pattern: MVVM
- State Management: GetX
- Directory Structure: Feature-based
- HTTP Client: `http`
- Local Storage: `flutter_secure_storage`

### 4.2 프론트 구현 메모

- 날짜/시간은 ISO 8601 문자열을 `DateTime`으로 파싱한다.
- `Date` 타입 값은 `YYYY-MM-DD` 그대로 다룬다.
- enum은 Flutter 내부 camelCase로 관리하고 API 송수신 시 raw value 매핑을 명시적으로 둔다.
- `OpenMatModel.reported`는 서버 응답 필드 그대로 신뢰한다.
- `NotificationModel.readAt == null`이면 미읽음이다.
- 로그인 버튼/UI는 `GOOGLE`, `KAKAO`, `APPLE` 3개 기준으로 설계하되, 서버 호출 가능 여부는 구현 상태를 따른다.
- 공지사항은 별도 작성 플로우 없이 `목록 -> 상세` 읽기 흐름만 잡으면 된다.
- 공지사항 목록 item은 `id`, `title`, `content`, `authorName`, `createdAt`를 사용한다.
- 공지사항 상세는 같은 필드를 그대로 사용해 상세 페이지를 구성하면 된다.
- 문의는 인증 사용자 기준으로 `목록 -> 상세 -> 답변 확인` 흐름을 구현하면 된다.

### 4.3 프론트 주의사항

- 현재 `/users/me` 수정 API는 `phone` 수정 미지원이다.
- 현재 `/open-mats/my`는 배열이 아니라 페이징 응답이다.
- 현재 오픈매트 생성/수정 요청에는 `region`이 포함된다.
- 오픈매트 작성자 관리 UI는 현재 상세 화면 안에서 바로 노출한다.
- 작성자 전용 관리 범위는 `참가자 강제 취소`, `모집 상태 수동 변경(RECRUITING, CLOSED)`이다.
- 현재 클라이언트는 작성자 권한을 상세 응답의 `hostId == 현재 사용자 id`로 판단한다.
- 다만 작성자 관리 API는 아직 `api-spec.json`에 정식 반영되지 않았다. 프론트는 현재 연결 기준으로 선반영했고, 최종 완료 판단은 백엔드 계약/실서버 검증 후 닫는다.
- 현재 로그인 API 요청 허용값은 `GOOGLE`, `KAKAO`다. `APPLE` 버튼을 노출하더라도 실제 호출 가능 여부는 출시 정책 확정 후 다시 맞춘다.
- 현재 FCM은 서버 저장 데이터 기반 알림함과 함께 동작한다. 다만 실제 릴리스 전에는 iOS 실기기 수신, 권한 거부 상태 UX, 리뷰어 안내 문구까지 별도 확인이 필요하다.
- 현재 사용자 전역 푸시 설정은 `/users/me.settings.pushNotificationEnabled`와 `PATCH /api/v1/users/me/settings`로 관리한다.
- 알림 권한을 거부해도 오픈매트/대회/공지 핵심 조회와 신청 흐름은 막히지 않게 설계한다.


## 3. Rolling API 명세서

## 5.1 인증 API

### 5.1.1 소셜 로그인

`POST /api/v1/auth/login`

- 인증: 불필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `provider` | `String` | O | 현재 구현 허용값 `GOOGLE`, `KAKAO` |
| `accessToken` | `String` | O | 소셜 제공자 access token |

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

에러:

- `UNSUPPORTED_PROVIDER`
- `KAKAO_API_ERROR`
- `GOOGLE_API_ERROR`
- `VALIDATION_ERROR`

현재 구현 메모:

- Apple 로그인은 아직 서버 미구현이다.
- 로그인 응답에는 현재 사용자 기준 `isAdmin`이 포함된다.

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

에러:

- `INVALID_REFRESH_TOKEN`
- `EXPIRED_REFRESH_TOKEN`
- `VALIDATION_ERROR`


현재 구현 메모:

- 토큰 갱신 응답에도 현재 사용자 기준 `isAdmin`이 포함된다.

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

### 5.1.5 회원 탈퇴 취소

`POST /api/v1/auth/withdraw/cancel`

- 인증: 필요

Response data:

| 필드 | 타입 |
| --- | --- |
| `withdrawalPending` | `Boolean` |
| `scheduledAt` | `DateTime?` |

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
| `isAdmin` | `Boolean` |
| `settings` | `Object` |
| `settings.pushNotificationEnabled` | `Boolean` |

현재 구현 메모:

- `/users/me` 응답에는 현재 사용자 기준 `isAdmin` 필드가 포함된다.
- `/users/me` 응답에는 사용자 설정 `settings.pushNotificationEnabled`와 소속 `affiliation`이 포함된다.
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
- 차단 정보는 `user_blocked_users` 조인 테이블에 저장된다.
- 현재는 차단 목록 조회 API가 없다.

### 5.2.7 사용자 차단 해제

`DELETE /api/v1/users/{id}/block`

- 인증: 필요
- Response data: `null`

현재 구현 메모:

- 차단 해제는 기존 차단 관계를 제거한다.
- 차단 해제 이후에는 오픈매트/대회 콘텐츠가 다시 일반 조회 결과에 노출된다.

## 5.3 알림 API

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

### 5.3.2 알림 읽음 처리

`PATCH /api/v1/notifications/{id}/read`

- 인증: 필요
- Response data: `null`

## 5.4 오픈매트 API

### 5.4.1 오픈매트 리스트 조회

`GET /api/v1/open-mats`

- 인증: 불필요

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `region` | `String` | - | 지역 필터 |
| `status` | `String` | - | 상태 필터 |
| `q` | `String` | - | 제목/장소명/주소 부분 일치 검색 |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `20` | 페이지 크기 |
| `sort` | `String` | `startDateTime,asc` | 정렬 |

Response: 페이징된 `OpenMatModel`

현재 구현 메모:

- 로그인 사용자가 차단한 작성자의 오픈매트는 목록과 검색 결과에서 제외된다.
- 비로그인 사용자는 기존 공개 목록과 동일하게 조회된다.
- 로그인한 앱 클라이언트는 목록·검색 요청에 `Authorization` 헤더를 포함해야 차단 필터가 적용된다.

### 5.4.2 오픈매트 상세 조회

`GET /api/v1/open-mats/{id}`

- 인증: 불필요
- Response: `OpenMatModel`

현재 구현 메모:

- 현재 상세 응답 모델은 `OpenMatModel`이며 `hostNickname`을 포함한다.
- 프론트 상세 화면은 별도 생성자 조회 API 없이 상세 응답의 `hostNickname`을 그대로 사용하면 된다.
- soft delete된 오픈매트는 비로그인 사용자에게는 `NOT_FOUND`다.
- 삭제 알림을 받은 신청자, 호스트, 관리자에게는 `deleted=true`, `deletedAt`이 포함된 상세 응답을 반환한다.
- 로그인 사용자가 차단한 작성자의 오픈매트는 상세 조회에서 `NOT_FOUND`로 처리한다.

### 5.4.3 오픈매트 등록

`POST /api/v1/open-mats`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `title` | `String` | O |
| `description` | `String` | O |
| `startDateTime` | `DateTime` | O |
| `endDateTime` | `DateTime` | O |
| `locationName` | `String` | O |
| `address` | `String` | O |
| `region` | `Region` | O |
| `maxCapacity` | `Integer` | O |
| `hostInstagramId` | `String?` | - |

Response: `OpenMatModel`

검증:

- 종료 시간은 시작 시간보다 이후여야 한다.
- `maxCapacity`는 `-1` 또는 `1 이상`이어야 한다.

### 5.4.4 오픈매트 수정

`PUT /api/v1/open-mats/{id}`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `title` | `String?` | - |
| `description` | `String?` | - |
| `startDateTime` | `DateTime?` | - |
| `endDateTime` | `DateTime?` | - |
| `locationName` | `String?` | - |
| `address` | `String?` | - |
| `region` | `Region?` | - |
| `maxCapacity` | `Integer?` | - |
| `hostInstagramId` | `String?` | - |

Response: `OpenMatModel`

현재 구현 메모:

- 작성자만 수정 가능
- 참가자가 있고 일정/장소 필드가 바뀌면 수정 알림 저장 후 FCM 발송 시도
- 수정은 작성자의 accessToken이 반드시 필요하고 비인증 우회 정책은 없다.

### 5.4.5 오픈매트 삭제

`DELETE /api/v1/open-mats/{id}`

- 인증: 필요


Response data: `null`

현재 구현 메모:

- 작성자만 삭제 가능
- 신청자가 있어도 바로 삭제 가능
- 실제로는 soft delete
- 참가자가 있으면 삭제 알림 저장 후 FCM 발송 시도
- 삭제는 작성자의 accessToken이 반드시 필요하고 비인증 우회 정책은 없다.

### 5.4.6 오픈매트 신청

`POST /api/v1/open-mats/{id}/apply`

- 인증: 필요
- Response data: `null`

에러:

- `HOST_CANNOT_APPLY`
- `OPEN_MAT_REPORTED`
- `OPEN_MAT_CLOSED`
- `OPEN_MAT_FINISHED`
- `ALREADY_APPLIED`
- `CAPACITY_FULL`

### 5.4.7 오픈매트 신청 취소

`DELETE /api/v1/open-mats/{id}/apply`

- 인증: 필요
- Response data: `null`

### 5.4.8 내가 신청한 오픈매트 목록

`GET /api/v1/open-mats/my`

- 인증: 필요
- Response: 페이징된 `OpenMatModel`

Query parameters:

| 파라미터 | 타입 | 기본값 |
| --- | --- | --- |
| `page` | `Integer` | `0` |
| `size` | `Integer` | `10` |
| `sort` | `String` | `startDateTime,asc` |

현재 구현 메모:

- 현재 `/api/v1/open-mats/my`는 내가 신청한 오픈매트 목록만 조회한다.
- 응답은 `OpenMatResponse`를 사용하므로 각 항목에 `hostNickname`이 포함된다.
- 내가 개최한 오픈매트 목록은 `/api/v1/open-mats/my-hosting`으로 별도 조회한다.

### 5.4.9 내가 개최한 오픈매트 목록

`GET /api/v1/open-mats/my-hosting`

- 인증: 필요
- Response: 페이징된 `OpenMatModel`

Query parameters:

| 파라미터 | 타입 | 기본값 |
| --- | --- | --- |
| `page` | `Integer` | `0` |
| `size` | `Integer` | `10` |
| `sort` | `String` | `startDateTime,asc` |

현재 구현 메모:

- 내가 개최한 오픈매트만 조회한다.
- soft delete된 오픈매트는 제외한다.
- 응답은 `OpenMatResponse`를 사용하므로 각 항목에 `hostNickname`이 포함된다.

### 5.4.10 오픈매트 참가자 목록 조회

`GET /api/v1/open-mats/{id}/participants`

- 인증: 필요
- 권한: 로그인 사용자
- Response data: `List<OpenMatParticipantResponse>`

Response item 메모:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `Long` | 참가자 사용자 ID |
| `name` | `String` | 참가자 이름. 현재 구현에서는 `nickname`을 사용 |
| `affiliation` | `String?` | 참가자 소속 |
| `beltColor` | `String` | 벨트 색상 |

프론트 구현 메모:

- 오픈매트 상세 화면에서 로그인한 사용자에게 참가자 목록을 노출한다.
- 참가자 목록은 현재 신청 순서대로 반환한다.
- 현재 프론트는 API 실패 시 상세 응답의 `participantUids`로 최소 fallback 목록을 구성한다.
- 이 endpoint는 아직 `api-spec.json`에 반영되지 않았으므로 실서버 검증 전까지 확정 계약으로 보지 않는다.

### 5.4.11 오픈매트 참가자 강제 취소

`DELETE /api/v1/open-mats/{openMatId}/participants/{participantUserId}`

- 인증: 필요
- 권한: 작성자 전용
- Response data: `null`

프론트 구현 메모:

- 작성자는 상세 화면의 참가자 목록에서 개별 참가자를 강제 취소할 수 있다.
- 성공 시 참가자 목록과 상세 데이터를 다시 조회해 현재 신청 인원 수를 맞춘다.
- 이 endpoint는 아직 `api-spec.json`에 반영되지 않았으므로 실서버 검증 전까지 확정 계약으로 보지 않는다.

### 5.4.12 오픈매트 모집 상태 수동 변경

`PATCH /api/v1/open-mats/{id}/status`

- 인증: 필요
- 권한: 작성자 전용

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | `String` | O | `RECRUITING`, `CLOSED` |

Response: `OpenMatModel`

프론트 구현 메모:

- 상세 화면에서 작성자는 `RECRUITING`, `CLOSED` 상태를 직접 변경할 수 있다.
- `FINISHED`는 자동 상태로 보고 수동 변경 대상에서 제외한다.
- 현재 프론트는 `PATCH /status`가 `404 NOT_FOUND`이면 기존 `PUT /api/v1/open-mats/{id}`의 `status` 필드 변경으로 한 번 더 fallback 시도한다.
- 이 endpoint는 아직 `api-spec.json`에 반영되지 않았으므로 실서버 검증 전까지 확정 계약으로 보지 않는다.

### 5.4.13 오픈매트 신고

`POST /api/v1/open-mats/{id}/report`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `reason` | `String` | O |
| `customReason` | `String?` | - |

Response data: `null`

에러:

- `ALREADY_REPORTED`
- `SELF_REPORT_NOT_ALLOWED`
- `VALIDATION_ERROR`
- `NOT_FOUND`

## 5.5 대회 API

### 5.5.1 대회 리스트 조회

`GET /api/v1/tournaments`

- 인증: 불필요
- Response: 페이징된 `TournamentResponse`
- `posterUrl`은 수동 등록 대회의 `posterKey` 또는 크롤링 저장 URL을 기준으로 조립된 공개 이미지 URL이다.
- 리스트 응답에는 `posterKey`가 없고, 프론트는 `posterUrl`만 사용한다.

현재 구현 메모:

- 로그인 사용자가 차단한 작성자의 대회는 목록에서 제외된다.
- 비로그인 사용자는 기존 공개 목록과 동일하게 조회된다.
- 로그인한 앱 클라이언트는 목록·검색 요청에 `Authorization` 헤더를 포함해야 차단 필터가 적용된다.

Query parameters:

| 파라미터 | 타입 | 기본값 |
| --- | --- | --- |
| `source` | `String` | - |
| `page` | `Integer` | `0` |
| `size` | `Integer` | `20` |

### 5.5.2 대회 상세 조회

`GET /api/v1/tournaments/{id}`

- 인증: 불필요
- Response: `TournamentResponse`
- 작성자 포함 모든 사용자가 동일한 응답을 받는다.
- 상세 응답에도 `posterKey`는 없고, `posterUrl`만 사용한다.
- 상세의 `posterUrl` 역시 브라우저에서 직접 열 수 있는 공개 URL이어야 한다.
- 로그인 사용자가 차단한 작성자의 대회는 상세 조회에서 `NOT_FOUND`로 처리한다.

### 5.5.3 대회 등록

`POST /api/v1/tournaments`

- 인증: 필요
- 포스터 이미지는 먼저 `POST /api/v1/tournaments/poster-upload-url`로 업로드 URL을 발급받은 뒤 S3에 직접 업로드한다.
- 업로드 성공 후 생성 API에는 업로드된 파일이 아니라 `posterKey`만 전달한다.
- 허용 이미지 형식은 `jpg`, `png`이며 `webp`, `gif`는 선택 확장 형식이다.
- Response: `TournamentResponse`
- `posterUrl`은 응답 시 `posterKey`를 기준으로 조립된다.
- `posterKey`가 없으면 생성 요청은 실패한다.

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `title` | `String` | O |
| `organizer` | `String?` | - |
| `posterKey` | `String` | O |
| `competitionDate` | `Date` | O |
| `registrationDeadline` | `Date` | O |
| `location` | `String?` | - |
| `applyLink` | `String` | O |

Response: `TournamentResponse`

현재 개편 메모:

- 대회 생성/상세/신고 개편 기준 문서는 [BACKEND_TOURNAMENT_CREATE_DETAIL_REPORT_PLAN.md](/C:/rolling/rolling-spring-backend/rolling-api/docs/BACKEND_TOURNAMENT_CREATE_DETAIL_REPORT_PLAN.md)다.
- 대회 생성은 `POST /api/v1/tournaments/poster-upload-url`로 받은 `posterKey`를 사용한다.
- 응답의 `posterUrl`은 `posterKey`를 기반으로 조립된 공개 URL이다.
- 프론트는 `poster-upload-url` 응답의 `uploadUrl`로 S3에 PUT 업로드를 수행하고, 그 다음 생성 API를 호출해야 한다.
- 업로드 요청의 `contentType`은 실제 파일 MIME type과 일치해야 한다. 예: `image/jpeg`, `image/png`, `image/webp`, `image/gif`.
- `fileName` 또는 `contentType`이 비정상이면 업로드 URL 발급 단계에서 실패한다.
- 상세 조회는 작성자 포함 공용 `TournamentResponse`를 그대로 사용한다.

### 5.5.4 대회 수정

`PUT /api/v1/tournaments/{id}`

- 인증: 필요
- Request body: 현재는 등록 API와 별개로 `posterUrl`을 포함한 기존 수정 필드를 유지한다.
- Response: `TournamentResponse`

현재 구현 메모:

- 최소 1개 필드는 전달해야 한다.
- `registrationDeadline <= competitionDate` 규칙 유지

### 5.5.5 대회 삭제

`DELETE /api/v1/tournaments/{id}`

- 인증: 필요
- Response data: `null`

### 5.5.6 대회 크롤링 수동 실행

`POST /api/v1/tournaments/crawl`

- 인증: `Authorization: Bearer {accessToken}` 필요 (`ROLE_ADMIN`)

Query parameters:

| 파라미터 | 타입 | 기본값 |
| --- | --- | --- |
| `source` | `String` | - |

Response data:

| 필드 | 타입 |
| --- | --- |
| `crawledCount` | `Integer` |
| `createdCount` | `Integer` |
| `updatedCount` | `Integer` |
| `skippedCount` | `Integer` |

권한 메모:

- 관리자 userId 목록: `admin.user-ids`
- 관리자 권한 판별 기준: `admin.user-ids`
- 인증된 사용자는 기본 `ROLE_USER`, 관리자 대상은 `ROLE_ADMIN` 권한을 가진다.
- 클라이언트는 `ROLE` 값을 요청에 따로 보내지 않으며 `Authorization: Bearer {accessToken}`만 전달한다.
- 관리자 여부 최종 판별은 항상 서버가 수행한다.
- 관리자 페이지 버튼 노출 여부는 프론트 UX 정책이고, 실제 관리자 액션 보호는 서버 `403 FORBIDDEN` 응답으로 처리한다.
- `X-Crawler-Admin-Key`와 `tournament.crawler.admin-key` 기반 우회 정책은 제거됐다.

### 5.5.7 대회 포스터 업로드 URL 발급

`POST /api/v1/tournaments/poster-upload-url`

- 인증: 필요
- Response: `TournamentPosterUploadUrlResponse`

현재 구현 메모:

- 클라이언트는 먼저 업로드 URL을 발급받고 S3에 직접 업로드한 뒤, 생성 API에 `posterKey`를 전달한다.
- 응답에는 `posterKey`와 업로드 URL이 내려가고, 생성/조회 응답의 `posterUrl`은 `posterKey` 기준으로 조립한다.
- 운영 환경에서는 `AWS_S3_PUBLIC_BASE_URL`에 CloudFront 또는 공개 이미지 도메인을 넣고, 누락 시 서버 시작이 실패해야 한다.
- 운영 배포 전 smoke test는 `poster-upload-url` 발급 -> S3 PUT 업로드 -> `POST /api/v1/tournaments` -> 목록/상세 `posterUrl` 200 확인 순서로 진행한다.
- 출시 성공 기준은 업로드/생성/신고 성공률과 `posterUrl` GET 성공률을 분리해서 본다.
- `posterUrl`이 S3 직링크로 보이면 운영 설정 누락으로 보고 우선 점검한다.

### 5.5.8 대회 신고

`POST /api/v1/tournaments/{id}/report`

- 인증: 필요
- Response: `null`

현재 구현 메모:

- 동일 사용자는 같은 대회를 한 번만 신고할 수 있다.
- 자기 작성 대회 신고는 차단한다.
- 신고 저장은 공통 `Report` 도메인을 재사용한다.
- 신고 운영은 제출 성공률과 중복/자기신고 차단 실패율을 함께 본다.

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



# Rolling Shared AGENTS

이 문서는 Rolling 프론트엔드/백엔드가 함께 보는 공용 계약 문서다.

- 목적: Flutter와 Spring Boot가 같은 enum, 모델, API 계약을 기준으로 작업하게 한다.
- 원칙: 기획 목표와 현재 구현이 다르면 반드시 둘을 분리해서 적는다.
- 우선순위: 현재 서버 구현 > 이 문서. 다만 서버/문서 불일치가 보이면 바로 이 문서를 갱신한다.

## 0. 문서 상태

### 0.1 로그인 제공자 정책

- 제품 목표 로그인 제공자: `GOOGLE`, `KAKAO`, `APPLE`
- 현재 서버 구현: `GOOGLE`, `KAKAO`만 지원
- `APPLE`은 목표 계약에는 포함하지만 아직 서버 미구현 상태

프론트 작업 규칙:

- Flutter enum과 UI 설계는 `GOOGLE`, `KAKAO`, `APPLE` 기준으로 가져간다.
- 실제 호출 가능 여부는 현재 서버 구현 상태를 따른다.
- Apple 로그인 서버 지원 전까지는 `APPLE`을 기본 활성 플로우로 가정하지 않는다.

### 0.2 API 공통 규약

- Base URL: `/api/v1`
- 인증 방식: `Authorization: Bearer {accessToken}`
- Content-Type: `application/json`
- 시간대 기준: `Asia/Seoul`

### 0.3 공통 응답 형식

성공 응답:

```json
{
  "success": true,
  "data": {}
}
```

에러 응답:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 설명"
  }
}
```

페이징 응답:

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  }
}
```

### 0.4 공통 에러 코드

| 코드 | HTTP Status | 설명 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 인증 필요 또는 인증 실패 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스를 찾을 수 없음 |
| `VALIDATION_ERROR` | 400 | 요청 데이터 유효성 검증 실패 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

## 1. Enum 정의

### SocialProvider

```dart
enum SocialProvider {
  google, // Google Login
  kakao,  // Kakao Login
  apple,  // Apple Login
}
```

- 목표 API raw value: `GOOGLE`, `KAKAO`, `APPLE`
- 현재 서버 구현 지원값: `GOOGLE`, `KAKAO`

### BeltColor

```dart
enum BeltColor {
  white,
  blue,
  purple,
  brown,
  black,
}
```

- API raw value: `WHITE`, `BLUE`, `PURPLE`, `BROWN`, `BLACK`

### Region

```dart
enum Region {
  seoul,
  gyeonggi,
  incheon,
  daejeon,
  sejong,
  chungbuk,
  chungnam,
  busan,
  daegu,
  ulsan,
  gyeongbuk,
  gyeongnam,
  gwangju,
  jeonbuk,
  jeonnam,
  gangwon,
  jeju,
}
```

- API raw value: `SEOUL`, `GYEONGGI`, `INCHEON`, `DAEJEON`, `SEJONG`, `CHUNGBUK`, `CHUNGNAM`, `BUSAN`, `DAEGU`, `ULSAN`, `GYEONGBUK`, `GYEONGNAM`, `GWANGJU`, `JEONBUK`, `JEONNAM`, `GANGWON`, `JEJU`

### OpenMatStatus

```dart
enum OpenMatStatus {
  recruiting, // 모집중
  closed,     // 모집 마감
  finished,   // 종료됨
}
```

- API raw value: `RECRUITING`, `CLOSED`, `FINISHED`

### TournamentSource

```dart
enum TournamentSource {
  streetJiuJitsu,   // 스트릿 주짓수 크롤링
  koreaJiu,         // 코리아 주짓수 크롤링
  heroesOfJiuJitsu, // 히어로즈 오브 주짓수 크롤링
  manual,           // 수동 등록
}
```

- API raw value: `STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU`, `MANUAL`
- 수동 크롤링 API의 `source`는 `MANUAL`을 지원하지 않는다.

### ReportReason

```dart
enum ReportReason {
  falseInfo,     // 허위 정보
  inappropriate, // 부적절한 내용
  spam,          // 스팸/광고
  other,         // 기타
}
```

- API raw value: `FALSE_INFO`, `INAPPROPRIATE`, `SPAM`, `OTHER`

### ReportTargetType

```dart
enum ReportTargetType {
  openMat,
  tournament,
}
```

- API raw value: `OPEN_MAT`, `TOURNAMENT`

### NotificationType

```dart
enum NotificationType {
  openMatUpdated, // 오픈매트 일정/장소 변경
  openMatDeleted, // 오픈매트 삭제/취소
}
```

- API raw value: `OPEN_MAT_UPDATED`, `OPEN_MAT_DELETED`

## 2. 공용 도메인 모델

이 섹션의 모델은 Flutter와 백엔드가 같이 이해하기 위한 공용 모델 정의다.
현재 서버 응답에 없는 필드는 별도 메모로 표시한다.

### 2.1 User

도메인 네임: `UserModel`

| 필드명 | 타입 | 설명 | 비고 |
| --- | --- | --- | --- |
| `id` | `int` | 고유 식별자 | PK |
| `email` | `String?` | 이메일 | |
| `nickname` | `String` | 프로필 닉네임 | |
| `phone` | `String?` | 연락처 | 현재 응답에는 포함 |
| `beltColor` | `BeltColor` | 주짓수 벨트 색상 | Enum |
| `socialProvider` | `SocialProvider` | 소셜 로그인 제공자 | Enum |
| `devices` | `List<UserDeviceModel>` | 등록된 사용자 디바이스 목록 | 공용 개념 모델, `/users/me` 직접 응답에는 미포함 |
| `joinedOpenMats` | `List<int>` | 신청한 오픈매트 ID 리스트 | 공용 개념 모델, 현재는 `/open-mats/my`로 조회 |
| `withdrawalPending` | `bool` | 탈퇴 예약 여부 | 현재 응답 포함 |
| `withdrawalScheduledAt` | `DateTime?` | 탈퇴 예정 시각 | 현재 응답 포함 |
| `createdAt` | `DateTime` | 계정 생성 일시 | |

### 2.2 UserDevice

도메인 네임: `UserDeviceModel`

| 필드명 | 타입 | 설명 | 비고 |
| --- | --- | --- | --- |
| `id` | `int` | 사용자 디바이스 고유 ID | PK |
| `userId` | `int` | 소유 사용자 ID | FK |
| `fcmToken` | `String` | 디바이스 FCM 토큰 | Unique |
| `createdAt` | `DateTime` | 등록 일시 | |

### 2.3 OpenMat

도메인 네임: `OpenMatModel`

현재 프론트에서 직접 쓰는 응답 모델 기준:

| 필드명 | 타입 | 설명 | 비고 |
| --- | --- | --- | --- |
| `id` | `int` | 오픈매트 고유 ID | PK |
| `hostId` | `int` | 호스트 유저 ID | 응답 필드 |
| `title` | `String` | 오픈매트 제목 | |
| `description` | `String` | 상세 설명 및 공지 | |
| `startDateTime` | `DateTime` | 시작 시간 | |
| `endDateTime` | `DateTime` | 종료 시간 | |
| `locationName` | `String` | 장소 명칭 | |
| `address` | `String` | 상세 주소 | |
| `region` | `Region` | 지역 | 현재 서버 응답 포함 |
| `maxCapacity` | `int` | 정원 제한 수 | `-1`이면 무제한 |
| `currentParticipants` | `int` | 현재 참여 인원 수 | 응답 계산 필드 |
| `status` | `OpenMatStatus` | 현재 모집 상태 | Enum |
| `reported` | `bool` | 신고 누적 차단 여부 | `reportCount >= 3`의 클라이언트용 표현 |
| `hostNickname` | `String` | 호스트 닉네임 | 응답 필드 |
| `hostInstagramId` | `String?` | 호스트 인스타그램 ID | 응답 필드 |
| `createdAt` | `DateTime` | 작성 일시 | |

백엔드 내부 개념 메모:

- `participantUids`는 내부 저장 필드다.
- `reportCount`는 내부 저장 필드다.
- 현재 클라이언트 응답에는 `participantUids`, `reportCount`를 직접 내려주지 않는다.

### 2.4 Tournament

도메인 네임: `TournamentModel`

| 필드명 | 타입 | 설명 | 비고 |
| --- | --- | --- | --- |
| `id` | `int` | 대회 고유 ID | PK |
| `hostUserId` | `int?` | 작성자 유저 ID | 수동 등록 대회만 값 존재, 크롤링 데이터는 null 가능 |
| `source` | `TournamentSource` | 등록 출처 | Enum |
| `title` | `String` | 대회 명칭 | |
| `organizer` | `String?` | 주최사 정보 | |
| `competitionDate` | `Date` | 대회 개최일 | `YYYY-MM-DD` |
| `registrationDeadline` | `Date?` | 접수 마감일 | `YYYY-MM-DD` |
| `location` | `String?` | 개최 장소 | |
| `posterUrl` | `String?` | 대회 포스터 이미지 URL | |
| `applyLink` | `String` | 외부 접수처 링크 | URL |
| `registrationClosed` | `bool` | 접수 마감 여부 | 서버 계산 필드 |
| `createdAt` | `DateTime` | 작성 일시 | |

### 2.5 Report

도메인 네임: `ReportModel`

| 필드명 | 타입 | 설명 | 비고 |
| --- | --- | --- | --- |
| `id` | `int` | 신고 고유 ID | PK |
| `reporterUserId` | `int` | 신고자 유저 ID | FK |
| `targetType` | `ReportTargetType` | 신고 대상 타입 | Enum |
| `targetId` | `int` | 신고 대상 ID | OpenMat 또는 Tournament ID |
| `reason` | `ReportReason` | 신고 사유 | Enum |
| `customReason` | `String?` | 기타 사유 직접 입력 | `reason == other`일 때 사용 |
| `createdAt` | `DateTime` | 신고 일시 | |

### 2.6 Notification

도메인 네임: `NotificationModel`

| 필드명 | 타입 | 설명 | 비고 |
| --- | --- | --- | --- |
| `id` | `int` | 알림 고유 ID | PK |
| `userId` | `int` | 알림 소유 사용자 ID | FK, 현재 응답 직접 노출 없음 |
| `type` | `NotificationType` | 알림 타입 | Enum |
| `targetId` | `int` | 관련 대상 ID | 현재는 OpenMat ID |
| `route` | `String` | 앱 이동 경로 | `/openmat/detail`, `/openmat` |
| `title` | `String` | 알림 제목 | |
| `body` | `String` | 알림 본문 | |
| `readAt` | `DateTime?` | 읽음 처리 일시 | `null`이면 미읽음 |
| `createdAt` | `DateTime` | 알림 생성 일시 | 최신순 정렬 기준 |

## 3. 프로젝트/기능 규칙

### 3.1 프로젝트 정의

주짓수 라이프스타일 통합 플랫폼 `Rolling`

- 오픈매트와 대회 정보를 한 곳에서 본다.
- MVP 단계에서는 정보 탐색과 외부 활동 참여를 우선한다.

### 3.2 주요 대상

- 자신의 체육관 밖의 오픈매트/대회 정보에 적극적으로 참여하는 수련생

### 3.3 오픈매트 핵심 규칙

- 유저는 오픈매트를 등록할 수 있다.
- 유저는 오픈매트에 신청/취소할 수 있다.
- 호스트는 자신이 주최한 오픈매트에 신청할 수 없다.
- `maxCapacity != -1`이고 정원이 가득 차면 상태는 `CLOSED`가 된다.
- 신청 취소로 자리가 다시 생기면 종료 전에는 `RECRUITING`으로 복귀한다.
- `endDateTime <= now`가 되면 상태는 `FINISHED`가 된다.
- 신고가 3건 이상 누적되면 신규 신청이 차단된다.
- 삭제는 hard delete가 아니라 soft delete(`isHidden = true`)다.

### 3.4 대회 핵심 규칙

- 대회는 수동 등록 또는 크롤링 수집 데이터로 존재한다.
- 수동 등록 대회는 항상 `source = MANUAL`이다.
- 대회 리스트는 접수 가능한 대회가 먼저, 마감된 대회가 뒤로 간다.
- `registrationClosed`는 서버 계산 필드다.
- 수동 크롤링 실행 API는 관리자만 호출 가능하다.

### 3.5 알림 핵심 규칙

- 알림 리스트의 source of truth는 FCM 수신 성공 여부가 아니라 백엔드 `Notification` 저장 데이터다.
- 오픈매트 수정/삭제 이벤트 발생 시 알림 레코드를 먼저 저장하고 그 다음 FCM 발송을 시도한다.
- 읽음 여부는 `isRead`가 아니라 `readAt == null` 여부로 판단한다.
- 현재 알림 클릭 규칙은 `route` 우선이다.

알림 클릭 규칙:

| type | route | 프론트 동작 |
| --- | --- | --- |
| `OPEN_MAT_UPDATED` | `/openmat/detail` | `targetId`로 상세 진입 후 최신 데이터 재조회 |
| `OPEN_MAT_DELETED` | `/openmat` | 오픈매트 목록 이동 |

추가 규칙:

- 알림 클릭 시 `PATCH /api/v1/notifications/{id}/read` 호출
- 읽음 처리 API는 idempotent
- `OPEN_MAT_UPDATED` 상세 재조회가 `404 NOT_FOUND`면 `/openmat` fallback

## 4. 프론트엔드 구현 원칙

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

### 4.3 프론트 주의사항

- 현재 `/users/me` 수정 API는 `phone` 수정 미지원이다.
- 현재 `/open-mats/my`는 배열이 아니라 페이징 응답이다.
- 현재 오픈매트 생성/수정 요청에는 `region`이 포함된다.
- 테스트용 서버 설정으로 비인증 오픈매트 수정/삭제가 열릴 수 있지만, 일반 앱 플로우에서 이를 전제로 구현하면 안 된다.

## 5. Rolling API 명세서

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

에러:

- `UNSUPPORTED_PROVIDER`
- `KAKAO_API_ERROR`
- `GOOGLE_API_ERROR`
- `VALIDATION_ERROR`

현재 구현 메모:

- Apple 로그인은 아직 서버 미구현이다.

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

에러:

- `INVALID_REFRESH_TOKEN`
- `EXPIRED_REFRESH_TOKEN`
- `VALIDATION_ERROR`

### 5.1.3 로그아웃

`POST /api/v1/auth/logout`

- 인증: 필요
- Response data: `null`

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
| `socialProvider` | `String` |
| `beltColor` | `String` |
| `createdAt` | `DateTime` |
| `withdrawalPending` | `Boolean` |
| `withdrawalScheduledAt` | `DateTime?` |

### 5.2.2 내 정보 수정

`PUT /api/v1/users/me`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `nickname` | `String` | - |
| `beltColor` | `String` | - |

현재 구현 메모:

- `phone` 수정은 아직 미지원이다.

### 5.2.3 FCM 토큰 등록

`POST /api/v1/users/me/fcm`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `fcmToken` | `String` | O |

Response data: `null`

현재 구현 메모:

- 토큰 저장 구조는 `user_devices` 1:N 이다.
- 동일 토큰 재등록 시 기존 디바이스 레코드를 재사용한다.

### 5.2.4 사용자 차단

`POST /api/v1/users/{id}/block`

- 인증: 필요
- Response data: `null`

### 5.2.5 사용자 차단 해제

`DELETE /api/v1/users/{id}/block`

- 인증: 필요
- Response data: `null`

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

### 5.4.2 오픈매트 상세 조회

`GET /api/v1/open-mats/{id}`

- 인증: 불필요
- Response: `OpenMatModel`

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
- 테스트 설정 `openmat.testing.allow-unauthenticated-update=true`면 비인증 수정 허용 가능

### 5.4.5 오픈매트 삭제

`DELETE /api/v1/open-mats/{id}`

- 인증: 필요

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `force` | `Boolean` | `false` | 신청자가 있어도 강제 삭제 |

Response data: `null`

현재 구현 메모:

- 작성자만 삭제 가능
- 신청자가 있으면 `force=true` 필요
- 실제로는 soft delete
- 참가자가 있으면 삭제 알림 저장 후 FCM 발송 시도
- 테스트 설정 `openmat.testing.allow-unauthenticated-update=true`면 비인증 삭제 허용 가능

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

### 5.4.9 오픈매트 신고

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
- Response: 페이징된 `TournamentModel`

Query parameters:

| 파라미터 | 타입 | 기본값 |
| --- | --- | --- |
| `source` | `String` | - |
| `page` | `Integer` | `0` |
| `size` | `Integer` | `20` |

### 5.5.2 대회 상세 조회

`GET /api/v1/tournaments/{id}`

- 인증: 불필요
- Response: `TournamentModel`

### 5.5.3 대회 등록

`POST /api/v1/tournaments`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `title` | `String` | O |
| `organizer` | `String?` | - |
| `posterUrl` | `String` | O |
| `competitionDate` | `Date` | O |
| `registrationDeadline` | `Date` | O |
| `location` | `String?` | - |
| `applyLink` | `String` | O |

Response: `TournamentModel`

### 5.5.4 대회 수정

`PUT /api/v1/tournaments/{id}`

- 인증: 필요
- Request body: 등록 API와 동일 필드, 모두 optional
- Response: `TournamentModel`

현재 구현 메모:

- 최소 1개 필드는 전달해야 한다.
- `registrationDeadline <= competitionDate` 규칙 유지

### 5.5.5 대회 삭제

`DELETE /api/v1/tournaments/{id}`

- 인증: 필요
- Response data: `null`

### 5.5.6 대회 크롤링 수동 실행

`POST /api/v1/tournaments/crawl`

- 인증: 관리자만 가능

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
- 운영용 우회 헤더: `X-Crawler-Admin-Key`
- 운영 키 설정: `tournament.crawler.admin-key`

## 6. 날짜/시간 형식

| 타입 | 형식 | 예시 |
| --- | --- | --- |
| `DateTime` | ISO 8601 | `2026-03-17T21:00:00` |
| `Date` | ISO 8601 | `2026-03-17` |

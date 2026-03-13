## 0. Enum 정의

### SocialProvider
소셜 로그인 제공자 타입
```dart
enum SocialProvider {
  kakao,    // Kakao Login
  google,   // Google Login
  naver     // Naver Login
}
```

### OpenMatStatus
오픈매트 모집 상태
```dart
enum OpenMatStatus {
  recruiting, // 모집중
  closed,     // 모집 마감
  finished    // 종료됨
}
```

### TournamentSource
대회 등록 출처
```dart
enum TournamentSource {
  streetJiuJitsu,    // 스트릿 주짓수 크롤링
  koreaJiu,          // 코리아 주짓수 크롤링
  heroesOfJiuJitsu,  // 히어로즈 오브 주짓수 크롤링
  manual             // 수동 등록
}
```
- API raw value: `STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU`, `MANUAL`
- 수동 크롤링 API의 `source`는 `MANUAL`을 지원하지 않는다.

### ReportReason
신고 사유 타입
```dart
enum ReportReason {
  falseInfo,      // 허위 정보
  inappropriate,  // 부적절한 내용
  spam,           // 스팸/광고
  other           // 기타
}
```

### ReportTargetType
신고 대상 타입
```dart
enum ReportTargetType {
  openMat,    // 오픈매트
  tournament  // 대회
}
```

---

## 1. 유저 (User)

**도메인 네임**: `UserModel`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `int` | 고유 식별자 | PK |
| `email`| `String`|이메일|
| `nickname` | `String` | 프로필 닉네임 |  |
| `beltColor` | `BeltColor` | 주짓수 벨트 색상 | Enum 타입 |
| `socialProvider` | `SocialProvider` | 소셜 로그인 제공자 | Enum 타입 |
| `joinedOpenMats` | `List<int>` | 신청한 오픈매트 ID 리스트 |  |
| `createdAt` | `DateTime` | 계정 생성 일시 |  |

---

## 2. 오픈매트 (OpenMat)

**도메인 네임**: `OpenMatModel`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `int` | 오픈매트 고유 ID | PK |
| `hostUserId` | `int` | 호스트 유저 ID | FK |
| `title` | `String` | 오픈매트 제목 |  |
| `description` | `String` | 상세 설명 및 공지 |  |
| `startDateTime` | `DateTime` | 오픈매트 시작 시간 |  |
| `endDateTime` | `DateTime` | 오픈매트 종료 시간 |  |
| `locationName` | `String` | 장소 명칭 |  |
| `address` | `String` | 상세 주소 |  |
| `participantUids` | `List<int>` | 신청자 ID 리스트 |  |
| `maxCapacity` | `int` | 정원 제한 수 | 제한 없을 시 -1 |
| `status` | `OpenMatStatus` | 현재 모집 상태 | Enum 타입 |
| `reportCount` | `int` | 신고 누적 건수 | 3건 이상 시 신청 차단 |
| `createdAt` | `DateTime` | 작성 일시 |  |

---

## 3. 대회 정보 (Tournament)

**도메인 네임**: `TournamentModel`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `int` | 대회 고유 ID | PK |
| `hostUserId` | `int` | 작성자 유저 ID | 수동 등록 대회만 값 존재, 크롤링 데이터는 null 가능 |
| `source` | `TournamentSource` | 등록 출처 | `STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU`, `MANUAL` |
| `title` | `String` | 대회 명칭 |  |
| `organizer` | `String` | 주최사 정보 |  |
| `competitionDate` | `Date` | 대회 개최일 | `YYYY-MM-DD` |
| `registrationDeadline` | `Date` | 접수 마감일 | `YYYY-MM-DD`, 접수 마감/배지 표시 기준 |
| `location` | `String` | 개최 장소 |  |
| `posterUrl` | `String` | 대회 포스터 이미지 URL |  |
| `applyLink` | `String` | 외부 접수처 링크 | URL 연동 |
| `registrationClosed` | `bool` | 접수 마감 여부 | 서버 계산 필드 |
| `createdAt` | `DateTime` | 작성 일시 |  |

---

## 4. 신고 (Report)

**도메인 네임**: `ReportModel`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `int` | 신고 고유 ID | PK |
| `reporterUserId` | `int` | 신고자 유저 ID | FK |
| `targetType` | `ReportTargetType` | 신고 대상 타입 | Enum 타입 |
| `targetId` | `int` | 신고 대상 ID | OpenMat 또는 Tournament ID |
| `reason` | `ReportReason` | 신고 사유 | Enum 타입 |
| `customReason` | `String?` | 기타 사유 직접 입력 | reason이 other일 때 사용 |
| `createdAt` | `DateTime` | 신고 일시 |  |


## 1. 프로젝트 정의 (Project Definition)

**주짓수 라이프스타일 통합 플랫폼 '롤링(Rolling)'**
파편화된 주짓수 정보(오픈매트, 대회)를 하나로 모아 제공하며, 복잡한 관리 기능을 걷어내고 정보 탐색과 외부 활동 참여에 집중한 MVP(Minimum Viable Product) 모델입니다.

## 2. 주요 대상 (Target Audience)

- **수련생**: 자신의 체육관 밖에서 일어나는 오픈매트, 대회의 '외부 활동'을 즐기는 적극적인 수련자.
---

## 3. 핵심 기능 상세 (Functional Requirements)

### 3.1 사용자 앱 (B2C)

- **[이벤트: 외부 활동 참여]**
  - **오픈매트**: 신청 및 명단 등록, 정원 관리(정원 초과 시 자동 마감), 종료 시간 기준 자동 상태 전환(finished).
  - **대회**: 일정 확인 및 외부 접수처 링크 연동, 접수 마감일 기준 자동 정렬 및 배지 표시.

---

## 4. 아키텍처 및 설계 원칙

- **MVVM Pattern**: View - Controller - Repository의 엄격한 분리.
- **Reactive UI**: GetX의 `Obx`를 활용하여 실시간 상태 반영.
- **Modular Features**: 기능별 독립 모듈 구성으로 유지보수성 확보.


## 1. 인증 및 사용자 관리 (Auth & User)

- **소셜 로그인**: Kakao, Google, Naver 로그인을 지원한다.
- **회원 탈퇴**: 탈퇴 시 사용자의 개인정보를 삭제한다.

---

## 2. 오픈매트 (OpenMat)

### 2.1 오픈매트 조회
- 오픈매트 리스트를 조회할 수 있다.
- 모집중/모집마감/종료 상태에 따라 필터링할 수 있다.
- 지역, 검색어(`q`) 기준으로 필터링할 수 있다.
- 시작 시간 기준으로 정렬하여 표시한다.

### 2.2 오픈매트 상세
- 오픈매트의 상세 정보(제목, 설명, 장소, 시간, 정원)를 확인할 수 있다.
- 현재 참여 인원 수를 확인할 수 있다.

### 2.3 오픈매트 참여
- **참여 신청**: 유저가 '신청하기' 버튼을 누르면 참여자 명단에 추가된다.
- **정원 관리**: `maxCapacity`가 -1이 아닐 경우, 정원이 찼다면 신청을 차단하고 '모집 마감' 상태로 변경한다.
- **참여 취소**: 유저가 신청한 오픈매트를 취소할 수 있다.
- **신고된 오픈매트**: 신고가 3건 이상 누적된 오픈매트는 신청이 불가능하다.

### 2.4 오픈매트 등록/수정/삭제
- 로그인한 유저는 오픈매트를 등록할 수 있다.
- 작성자 본인만 수정/삭제가 가능하다.
- 신청자가 1명이라도 있을 경우, 삭제 시 재확인 절차를 거친다.

### 2.5 오픈매트 관리 (작성자 전용)
- 작성자는 상세 페이지에서 '관리' 버튼을 통해 관리 기능에 접근할 수 있다.
- **참가자 목록 확인**: 신청한 참가자 명단을 확인할 수 있다.
- **참가자 강제 취소**: 특정 참가자의 신청을 강제로 취소할 수 있다.
- **오픈매트 수정**: 제목, 설명, 장소, 시간, 정원 등의 정보를 수정할 수 있다.
- **오픈매트 삭제**: 오픈매트를 삭제할 수 있다. 참가자가 있을 경우 재확인 절차를 거친다.
- **모집 상태 변경**: 모집중/모집마감 상태를 수동으로 변경할 수 있다.

### 2.6 오픈매트 신고
- 로그인한 유저는 오픈매트를 신고할 수 있다.
- 신고 사유를 선택하거나 직접 입력할 수 있다. (허위 정보, 부적절한 내용, 스팸/광고, 기타)
- 동일 유저가 같은 오픈매트를 중복 신고할 수 없다.
- 신고가 3건 이상 누적된 오픈매트는 '신고됨' 상태로 표시되며, 신규 신청이 차단된다.
- 작성자는 자신의 오픈매트를 신고할 수 없다.

### 2.7 상태 자동화
- `endDateTime`이 현재 시간을 지나면 `status`를 자동으로 `finished`로 전환한다.
---

## 3. 대회 정보 (Tournament)

### 3.1 대회 조회
- 대회 리스트를 조회할 수 있다.
- `source`, 검색어(`q`) 기준으로 필터링할 수 있다.
- 접수 마감일 기준으로 자동 정렬하여 표시한다.
- 접수 마감된 대회는 '접수 종료' 배지를 표시하고 리스트 하단으로 정렬한다.

### 3.2 대회 상세
- 대회의 상세 정보(제목, 주최사, 대회일, 접수 마감일, 장소, 외부 접수 링크)를 확인할 수 있다.
- 대회 포스터 이미지를 확인할 수 있다.
- 서버는 접수 마감 여부를 `registrationClosed` 필드로 함께 내려준다.

### 3.3 대회 등록/수정/삭제
- 로그인한 유저는 대회 정보를 등록할 수 있다.
- 수동 등록 대회는 `source = MANUAL`로 저장된다.
- 작성자 본인만 수정/삭제가 가능하다.

### 3.4 대회 관리 (작성자 전용)
- 작성자는 상세 페이지에서 '관리' 버튼을 통해 관리 기능에 접근할 수 있다.
- **대회 정보 수정**: 제목, 주최사, 일시, 장소, 포스터 이미지 등의 정보를 수정할 수 있다.
- **대회 삭제**: 대회 정보를 삭제할 수 있다.

### 3.5 대회 크롤링 수동 실행
- 운영/개발자는 대회 크롤링 수동 실행 API를 호출할 수 있다.
- `source`를 생략하면 전체 크롤러를 실행한다.
- `source`를 지정하면 해당 출처 크롤러만 실행한다. 지원값: `STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU`
- 크롤링 결과는 DB에 upsert 저장되며 `crawledCount`, `createdCount`, `updatedCount`, `skippedCount`를 반환한다.

### 3.6 외부 연동
- 대회 정보 클릭 시 `applyLink`를 통해 외부 브라우저로 연결한다.
- 수동 등록과 크롤링 수집 데이터 모두 동일한 `applyLink` 필드를 사용한다.



이 문서는 **롤링 (Rolling)** 프로젝트의 기술 스택을 정의합니다. 본 스택은 **웹 앱** 출시를 목표로 최적화되었습니다.

---

## Backend Stack

- **Framework**: Java Spring Boot 3.x
- **Language**: Java 17+
- **Database**: MySQL 8.0
- **ORM**: JPA (Hibernate) + QueryDSL
- **Security**: Spring Security + JWT
- **API**: RESTful API
- **Documentation**: Swagger/OpenAPI
---

## Frontend Stack (Web App)

## 1. Core Framework

- **Framework**: Flutter (v3.x)
- **Language**: Dart
- **Target Platform**: Web

## 2. Architecture & State Management

- **Pattern**: **MVVM (Model-View-ViewModel)**
- **State Management**: **GetX**
  - 사용 목적: 반응형 상태 관리(`Rx`), 의존성 주입(`DI`), Context가 필요 없는 라우팅(Navigation).
- **Directory Structure**: **Feature-based** (기능 중심 폴더 구조)
  - 각 기능별로 `view`, `controller`, `repository`, `binding`을 모듈화하여 관리.

## 3. Network & Data

- **HTTP Client**: **http**
  - 사용 목적: REST API 통신
  - Base URL: Spring Boot REST API 서버
- **Local Storage**:
  - `flutter_secure_storage`: JWT Access Token / Refresh Token 보안 저장.

## 4. Authentication (OAuth - Web)

- **Backend 인증**: **Spring Security + JWT**
  - Access Token / Refresh Token 기반 인증 처리.
- **Social Logins**: OAuth 2.0
  - Kakao, Google, Naver 소셜 로그인
  - 서버 측 OAuth 처리 후 JWT 발급

## 5. UI/UX Components

- **Typography**: **Pretendard** (SIL Open Font License)
- **Calendar**: `table_calendar` (오픈매트/대회 일정 표시)
- **Date Formatting**: `intl` (날짜/시간 포맷)
- **External Links**: `url_launcher` (대회 외부 접수처 연동)
- **Theme**: Custom Theme

## 6. Development Tools

- **VCS**: Git, GitHub
- **Linting**: `flutter_lints`


# Rolling API 명세서

> **Base URL**: `/api/v1`
>
> **인증 방식**: `Authorization: Bearer {accessToken}`
>
> **Content-Type**: `application/json`

---

## 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "data": { ... }
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 설명"
  }
}
```

### 페이징 응답
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

### 공통 에러 코드

| 코드 | HTTP Status | 설명 |
|------|:-----------:|------|
| `UNAUTHORIZED` | 401 | 인증 토큰 없음 또는 만료 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스를 찾을 수 없음 |
| `VALIDATION_ERROR` | 400 | 요청 데이터 유효성 검증 실패 |

### 공통 Enum

**SocialProvider**
| 값 | 설명 |
|------|------|
| `KAKAO` | 카카오 로그인 |
| `GOOGLE` | 구글 로그인 |
| `APPLE` | 애플 로그인 (예정) |
| `NAVER` | 네이버 로그인 (예정) |

**BeltColor**
| 값 | 설명 |
|------|------|
| `WHITE` | 화이트 벨트 |
| `BLUE` | 블루 벨트 |
| `PURPLE` | 퍼플 벨트 |
| `BROWN` | 브라운 벨트 |
| `BLACK` | 블랙 벨트 |

**OpenMatStatus**
| 값 | 설명 |
|------|------|
| `RECRUITING` | 모집중 |
| `CLOSED` | 정원 마감 |
| `FINISHED` | 종료됨 (endDateTime 경과) |

**TournamentSource**
| 값 | 설명 |
|------|------|
| `STREET_JIU_JITSU` | 스트릿 주짓수 크롤링 수집 |
| `KOREA_JIU` | 코리아 주짓수 크롤링 수집 |
| `HEROES_OF_JIU_JITSU` | 히어로즈 오브 주짓수 크롤링 수집 |
| `MANUAL` | 사용자 수동 등록 |

---

## 1. 인증 API (Auth)

### 1.1 소셜 로그인

소셜 제공자에서 발급받은 Access Token으로 JWT를 발급합니다.
신규 유저인 경우 자동으로 회원가입 처리됩니다.

```
POST /api/v1/auth/login
```

**인증**: 불필요

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `provider` | `String` | O | 소셜 로그인 제공자 (`KAKAO`, `GOOGLE`) |
| `accessToken` | `String` | O | 소셜 제공자에서 발급받은 Access Token |

```json
{
  "provider": "GOOGLE",
  "accessToken": "ya29.a0AfH6SMB..."
}
```

**Response** `200 OK`
| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | `String` | JWT Access Token (만료: 30분) |
| `refreshToken` | `String` | JWT Refresh Token (만료: 14일) |
| `tokenType` | `String` | 항상 `"Bearer"` |
| `expiresIn` | `Long` | Access Token 만료 시간 (초) |
| `newUser` | `Boolean` | 신규 회원 여부 |
| `userId` | `Long` | 사용자 ID |
| `email` | `String` | 사용자 이메일 |
| `name` | `String` | 사용자 이름 |

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "newUser": true,
    "userId": 1,
    "email": "user@gmail.com",
    "name": "홍길동"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `UNSUPPORTED_PROVIDER` | 지원하지 않는 provider |
| `KAKAO_API_ERROR` | 카카오 토큰 검증 실패 |
| `GOOGLE_API_ERROR` | 구글 토큰 검증 실패 |

---

### 1.2 토큰 갱신

```
POST /api/v1/auth/refresh
```

**인증**: 불필요

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `refreshToken` | `String` | O | 기존에 발급받은 Refresh Token |

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

---

### 1.3 로그아웃

```
POST /api/v1/auth/logout
```

**인증**: 필요

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

### 1.4 회원 탈퇴

```
DELETE /api/v1/auth/withdraw
```

**인증**: 필요

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

> Apple 로그인 사용자의 경우 Apple Revoke Token API가 함께 호출됩니다.

---

## 2. 사용자 API (User)

### 2.1 내 정보 조회

```
GET /api/v1/users/me
```

**인증**: 필요

**Response** `200 OK`
| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `Long` | 사용자 ID |
| `nickname` | `String` | 닉네임 |
| `email` | `String` | 이메일 |
| `phone` | `String` | 연락처 |
| `socialProvider` | `String` | 소셜 로그인 제공자 |
| `beltColor` | `String` | 벨트 색상 (`WHITE`, `BLUE`, `PURPLE`, `BROWN`, `BLACK`) |
| `createdAt` | `String` | 가입 일시 (ISO 8601) |

```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "홍길동",
    "email": "user@gmail.com",
    "phone": "010-1234-5678",
    "socialProvider": "GOOGLE",
    "createdAt": "2026-01-15T10:30:00"
  }
}
```

---

### 2.2 내 정보 수정

```
PUT /api/v1/users/me
```

**인증**: 필요

**Request Body** (변경할 필드만 전송)
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `nickname` | `String` | - | 닉네임 |
| `phone` | `String` | - | 연락처 |
| `beltColor` | `String` | - | 벨트 색상 (`WHITE`, `BLUE`, `PURPLE`, `BROWN`, `BLACK`) |

```json
{
  "nickname": "새닉네임",
  "phone": "010-9876-5432"
}
```

**Response** `200 OK` — 2.1과 동일한 형식

---

### 2.3 FCM 토큰 등록

```
POST /api/v1/users/me/fcm
```

**인증**: 필요

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `fcmToken` | `String` | O | Firebase Cloud Messaging 토큰 |

```json
{
  "fcmToken": "dK1x..."
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

### 2.4 사용자 차단

```
POST /api/v1/users/{id}/block
```

**인증**: 필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 차단할 사용자 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

### 2.5 차단 해제

```
DELETE /api/v1/users/{id}/block
```

**인증**: 필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 차단 해제할 사용자 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

## 3. 오픈매트 API (OpenMat)

### 3.1 오픈매트 리스트 조회

```
GET /api/v1/open-mats
```

**인증**: 불필요

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:----:|--------|------|
| `region` | `String` | - | - | 지역 필터 |
| `status` | `String` | - | - | 상태 필터 (`RECRUITING`, `CLOSED`, `FINISHED`) |
| `q` | `String` | - | - | 검색어 (`title`, `locationName`, `address` 대상 부분 일치) |
| `page` | `Integer` | - | `0` | 페이지 번호 (0부터 시작) |
| `size` | `Integer` | - | `20` | 페이지 크기 |
| `sort` | `String` | - | `startDateTime,asc` | 정렬 기준 |

**Response** `200 OK` — 페이징 응답
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "주말 오픈매트",
        "description": "자유롭게 참가 가능한 주말 오픈매트입니다.",
        "startDateTime": "2026-03-01T10:00:00",
        "endDateTime": "2026-03-01T12:00:00",
        "locationName": "롤링 주짓수 아카데미",
        "address": "서울시 강남구 역삼동 123-45",
        "maxCapacity": 20,
        "currentParticipants": 5,
        "status": "RECRUITING",
        "hostId": 1,
        "hostNickname": "관장님",
        "hostInstagramId": "rolling_bjj",
        "createdAt": "2026-02-15T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

> 숨김 처리된 오픈매트(`isHidden = true`)는 리스트에 노출되지 않습니다.
> `status`를 지정하지 않으면 전체 상태를 조회하며, 기본 정렬은 `startDateTime ASC` 입니다.
> `q`를 지정하면 제목, 장소명, 주소에서 부분 일치 검색을 수행합니다.

---

### 3.2 오픈매트 상세 조회

```
GET /api/v1/open-mats/{id}
```

**인증**: 불필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "주말 오픈매트",
    "description": "자유롭게 참가 가능한 주말 오픈매트입니다.",
    "startDateTime": "2026-03-01T10:00:00",
    "endDateTime": "2026-03-01T12:00:00",
    "locationName": "롤링 주짓수 아카데미",
    "address": "서울시 강남구 역삼동 123-45",
    "maxCapacity": 20,
    "currentParticipants": 5,
    "participantUids": [2, 3, 5, 7, 11],
    "status": "RECRUITING",
    "hostId": 1,
    "hostNickname": "관장님",
    "hostInstagramId": "rolling_bjj",
    "createdAt": "2026-02-15T09:00:00"
  }
}
```

---

### 3.3 오픈매트 등록

```
POST /api/v1/open-mats
```

**인증**: 필요

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `title` | `String` | O | 오픈매트 제목 |
| `description` | `String` | O | 상세 설명 |
| `startDateTime` | `String` | O | 시작 일시 (ISO 8601) |
| `endDateTime` | `String` | O | 종료 일시 (ISO 8601) |
| `locationName` | `String` | O | 장소명 |
| `address` | `String` | O | 상세 주소 |
| `maxCapacity` | `Integer` | O | 최대 정원 (`-1` = 제한 없음) |
| `hostInstagramId` | `String` | - | 호스트 인스타그램 ID |

```json
{
  "title": "주말 오픈매트",
  "description": "자유롭게 참가 가능한 주말 오픈매트입니다.",
  "startDateTime": "2026-03-01T10:00:00",
  "endDateTime": "2026-03-01T12:00:00",
  "locationName": "롤링 주짓수 아카데미",
  "address": "서울시 강남구 역삼동 123-45",
  "maxCapacity": 20,
  "hostInstagramId": "rolling_bjj"
}
```

**Response** `200 OK` — 3.2 상세 조회와 동일한 형식

**에러**
| 코드 | 상황 |
|------|------|
| `VALIDATION_ERROR` | 필수 필드 누락 |
| `VALIDATION_ERROR` | 종료 시간이 시작 시간 이전 |

---

### 3.4 오픈매트 수정

```
PUT /api/v1/open-mats/{id}
```

**인증**: 필요 (작성자 본인만 가능)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Request Body** (변경할 필드만 전송)
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `title` | `String` | - | 오픈매트 제목 |
| `description` | `String` | - | 상세 설명 |
| `startDateTime` | `String` | - | 시작 일시 (ISO 8601) |
| `endDateTime` | `String` | - | 종료 일시 (ISO 8601) |
| `locationName` | `String` | - | 장소명 |
| `address` | `String` | - | 상세 주소 |
| `maxCapacity` | `Integer` | - | 최대 정원 |
| `hostInstagramId` | `String` | - | 호스트 인스타그램 ID |

**Response** `200 OK` — 3.2 상세 조회와 동일한 형식

> 신청자가 있는 경우, 일시/장소 변경 시 FCM 푸시 알림이 발송됩니다.

---

### 3.5 오픈매트 삭제

```
DELETE /api/v1/open-mats/{id}
```

**인증**: 필요 (작성자 본인만 가능)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:----:|--------|------|
| `force` | `Boolean` | - | `false` | 신청자가 있어도 강제 삭제 |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

> 신청자가 있는 경우 `force=true`가 필요하며, 삭제 시 신청자들에게 취소 알림이 발송됩니다.

---

### 3.6 오픈매트 신청

```
POST /api/v1/open-mats/{id}/apply
```

**인증**: 필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `OPEN_MAT_CLOSED` | 모집이 마감된 오픈매트 |
| `OPEN_MAT_FINISHED` | 이미 종료된 오픈매트 |
| `OPEN_MAT_REPORTED` | 신고 누적으로 신청이 차단된 오픈매트 |
| `ALREADY_APPLIED` | 이미 신청한 오픈매트 |
| `CAPACITY_FULL` | 정원 초과 |

---

### 3.7 오픈매트 신청 취소

```
DELETE /api/v1/open-mats/{id}/apply
```

**인증**: 필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

### 3.8 내가 신청한 오픈매트 목록

```
GET /api/v1/open-mats/my
```

**인증**: 필요

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "주말 오픈매트",
      "startDateTime": "2026-03-01T10:00:00",
      "endDateTime": "2026-03-01T12:00:00",
      "locationName": "롤링 주짓수 아카데미",
      "address": "서울시 강남구 역삼동 123-45",
      "maxCapacity": 20,
      "currentParticipants": 5,
      "status": "RECRUITING",
      "hostNickname": "관장님",
      "hostInstagramId": "rolling_bjj"
    }
  ]
}
```

---

## 4. 대회 API (Tournament)

### 4.1 대회 리스트 조회

```
GET /api/v1/tournaments
```

**인증**: 불필요

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:----:|--------|------|
| `source` | `String` | - | - | 출처 필터 (`STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU`, `MANUAL`) |
| `q` | `String` | - | - | 검색어 (`title`, `organizer`, `location` 대상 부분 일치) |
| `page` | `Integer` | - | `0` | 페이지 번호 |
| `size` | `Integer` | - | `20` | 페이지 크기 |

**Response** `200 OK` — 페이징 응답

> 접수 가능한 대회가 상단, 마감된 대회가 하단으로 정렬됩니다.
> `source`를 지정하면 해당 출처의 대회만 조회합니다.
> `q`를 지정하면 대회명, 주최사, 장소에서 부분 일치 검색을 수행합니다.

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "source": "MANUAL",
        "title": "제5회 롤링컵",
        "organizer": "롤링 주짓수",
        "posterUrl": "https://cdn.rolling.com/posters/1.jpg",
        "competitionDate": "2026-04-15",
        "registrationDeadline": "2026-04-01",
        "location": "서울 올림픽공원 체조경기장",
        "applyLink": "https://forms.google.com/...",
        "registrationClosed": false,
        "createdAt": "2026-02-01T12:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

### 4.2 대회 상세 조회

```
GET /api/v1/tournaments/{id}
```

**인증**: 불필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 대회 ID |

**Response** `200 OK` — 4.1 리스트 항목과 동일한 형식

---

### 4.3 대회 등록

```
POST /api/v1/tournaments
```

**인증**: 필요 (작성자 본인)

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `title` | `String` | O | 대회 제목 |
| `organizer` | `String` | - | 주최사 |
| `posterUrl` | `String` | O | 포스터 이미지 URL |
| `competitionDate` | `String` | O | 대회 개최일 (`YYYY-MM-DD`) |
| `registrationDeadline` | `String` | O | 접수 마감일 (`YYYY-MM-DD`) |
| `location` | `String` | - | 개최 장소 |
| `applyLink` | `String` | O | 외부 접수 링크 |

```json
{
  "title": "제5회 롤링컵",
  "organizer": "롤링 주짓수",
  "posterUrl": "https://cdn.rolling.com/posters/1.jpg",
  "competitionDate": "2026-04-15",
  "registrationDeadline": "2026-04-01",
  "location": "서울 올림픽공원 체조경기장",
  "applyLink": "https://forms.google.com/..."
}
```

**Response** `200 OK` — 4.2 상세 조회와 동일한 형식

> 수동 등록 대회는 `source = MANUAL`로 저장됩니다.

**에러**
| 코드 | 상황 |
|------|------|
| `VALIDATION_ERROR` | 필수 필드 누락 |
| `VALIDATION_ERROR` | 접수 마감일이 대회일보다 늦음 |

---

### 4.4 대회 수정

```
PUT /api/v1/tournaments/{id}
```

**인증**: 필요 (작성자 본인)

**Request Body** — 4.3과 동일 (변경할 필드만 전송)

**Response** `200 OK` — 4.2 상세 조회와 동일한 형식

> 최소 1개 필드는 전달해야 하며, 적용 후 `registrationDeadline`은 `competitionDate`보다 늦을 수 없습니다.

---

### 4.5 대회 삭제

```
DELETE /api/v1/tournaments/{id}
```

**인증**: 필요 (작성자 본인)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 대회 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

### 4.6 대회 크롤링 수동 실행

```
POST /api/v1/tournaments/crawl
```

**인증**: 환경 설정에 따라 다름

> `tournament.crawler.manual.endpoint-public=true`이면 인증 없이 호출할 수 있습니다.
> 현재 기본 `application.yml`은 `true`로 설정되어 있습니다.

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:----:|--------|------|
| `source` | `String` | - | - | 실행할 출처 (`STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU`) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "crawledCount": 12,
    "createdCount": 3,
    "updatedCount": 8,
    "skippedCount": 1
  }
}
```

> `source`를 생략하면 전체 크롤러를 실행합니다.
> 지원 `source`: `STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU`
> `MANUAL`은 조회/수동 등록용 source이며 크롤링 대상이 아닙니다.
> 실행 전 접수 마감일이 지난 대회를 삭제하고, 수집 결과는 DB에 upsert 저장합니다.

**에러**
| 코드 | 상황 |
|------|------|
| `VALIDATION_ERROR` | 지원하지 않는 source |
| `VALIDATION_ERROR` | `MANUAL` 등 크롤러가 없는 출처 지정 |
| `UNAUTHORIZED` | 엔드포인트 비공개 설정에서 인증 없이 호출 |

---

## 날짜/시간 형식

| 타입 | 형식 | 예시 |
|------|------|------|
| DateTime | ISO 8601 | `2026-03-01T10:00:00` |
| Date | ISO 8601 | `2026-03-01` |



---

## 변경사항 (2026-02-19)

### OpenMat "내가 신청한 목록" 정책
- `GET /api/v1/open-mats/my`는 기본적으로 10개만 반환한다.
- 기본 페이지 크기: `size=10`
- 프론트 메인 화면은 첫 페이지(`page=0`) 10건을 사용한다.
- 프론트 "전체보기" 화면은 동일 API를 사용하고 `page` 값을 증가시키며 10개 단위로 조회한다.

### 백엔드 개발 가이드
- 엔드포인트 추가/수정 시 Swagger 문서와 API 명세를 동시에 갱신한다.
- OpenMat "my" 목록 기능은 페이징 응답(`content/page/size/totalElements/totalPages/last`)을 표준으로 사용한다.

---

## 변경사항 (2026-02-28)

### User 벨트 필드 추가
- `User` 엔티티에 `beltColor` 필드가 추가되었습니다.
- 타입: `BeltColor` (Enum, `@Enumerated(EnumType.STRING)`)
- 소셜 로그인 신규 가입 시 기본값: `WHITE`

### BeltColor Enum
```java
enum BeltColor {
  WHITE,
  BLUE,
  PURPLE,
  BROWN,
  BLACK
}
```

### 사용자 모델 반영
- `GET /api/v1/users/me` 응답에 `beltColor`가 포함됩니다.
- 예시:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "rolling_user",
    "email": "user@gmail.com",
    "phone": "010-1234-5678",
    "socialProvider": "GOOGLE",
    "beltColor": "PURPLE",
    "createdAt": "2026-02-28T16:20:00"
  }
}
```

### 내 정보 수정 API 반영
- `PUT /api/v1/users/me` 요청 필드에 `beltColor`가 추가되었습니다.
- 허용값: `WHITE`, `BLUE`, `PURPLE`, `BROWN`, `BLACK`
- 요청 예시:
```json
{
  "nickname": "rolling_user",
  "phone": "010-1234-5678",
  "beltColor": "BLUE"
}
```

- 요청 유효성:
  - `nickname`, `phone`, `beltColor` 중 최소 1개는 필수
  - `phone` 형식: `010-1234-5678`

---

## 변경사항 (2026-03-08)

### Tournament 접수 마감일 필드 명시
- `Tournament` 도메인 정의에 `registrationDeadline`(접수 마감일) 필드를 명시했습니다.
- 대회 상세 기능 설명에 접수 마감일 확인 항목을 추가했습니다.
- 스트릿 주짓수 크롤링 데이터는 상세 페이지의 `접수 마감 : YYYY년 M월 D일` 문구에서 날짜를 추출해 `registrationDeadline`에 저장합니다.

---

## 변경사항 (2026-03-11)

### TournamentSource 필드 및 API 정합성 반영
- `Tournament` 엔티티와 대회 응답 모델에 `source` 필드가 추가되었습니다.
- 수동 등록 대회는 서버에서 `MANUAL`로 저장됩니다.
- `GET /api/v1/tournaments`는 `source` 필터를 지원합니다.
- 대회 크롤링 수동 실행 API는 `POST /api/v1/tournaments/crawl?source=...` 형식으로 통합되었습니다.
- 대회 조회/필터 지원 출처 값은 `STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU`, `MANUAL` 입니다.
- 대회 크롤링 수동 실행 지원 출처 값은 `STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU` 입니다.
- `MANUAL`은 수동 등록/조회용 source이며 크롤링 대상은 아닙니다.

---

## 변경사항 (2026-03-10)

### OpenMat 상태 자동화/정합성 보강
- 오픈매트 상태는 `endDateTime`과 정원 상태를 기준으로 자동 보정됩니다.
- 정원이 가득 차면 `RECRUITING -> CLOSED`로 자동 전환됩니다.
- 신청 취소로 여유가 생기고 종료 전이라면 `CLOSED -> RECRUITING`으로 자동 복귀합니다.
- `endDateTime`이 지난 오픈매트는 스케줄러와 조회 시점 보정으로 `FINISHED` 처리됩니다.
- 오픈매트 리스트 조회 API는 `status` 필터를 지원하며 기본 정렬은 `startDateTime ASC` 입니다.
- 신고 누적 3건 이상 오픈매트는 신규 신청이 차단됩니다.

---

## 변경사항 (2026-03-12)

### Tournament 문서 정합성 보정
- `Tournament` 도메인 정의에서 `competitionDate`, `registrationDeadline` 타입을 `Date`로 명시했습니다.
- 대회 응답 모델의 계산 필드 `registrationClosed`를 문서에 반영했습니다.
- 대회 크롤링 수동 실행 API의 인증 조건을 설정값 기반으로 정리했습니다.
- `MANUAL`은 대회 조회/수동 등록용 source이며 크롤링 source가 아님을 명시했습니다.

---

## 변경사항 (2026-03-13)

### OpenMat / Tournament 목록 검색 기능 추가
- `GET /api/v1/open-mats`는 검색어 파라미터 `q`를 지원합니다.
- 오픈매트 검색 대상 필드: `title`, `locationName`, `address`
- `GET /api/v1/tournaments`는 검색어 파라미터 `q`를 지원합니다.
- 대회 검색 대상 필드: `title`, `organizer`, `location`
- 대회 목록 조회는 메모리 정렬/페이징 대신 DB 기반 페이징 조회로 변경되었습니다.
- 대회 목록 정렬 정책은 기존과 동일하게 `접수 가능 우선 -> registrationDeadline -> competitionDate -> id` 순서를 유지합니다.

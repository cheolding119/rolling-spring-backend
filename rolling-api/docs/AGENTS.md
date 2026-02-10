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
| `hostUserId` | `int` | 작성자 유저 ID | FK |
| `title` | `String` | 대회 명칭 |  |
| `organizer` | `String` | 주최사 정보 |  |
| `posterUrl` | `String` | 대회 포스터 이미지 URL |  |
| `competitionDate` | `DateTime` | 대회 개최일 |  |
| `registrationDeadline` | `DateTime` | 접수 마감 기한 |  |
| `location` | `String` | 개최 장소 |  |
| `applyLink` | `String` | 외부 접수처 링크 | URL 연동 |
| `categoryTags` | `List<String>` | 종목 태그 | 예: Gi, No-Gi 등 |
| `reportCount` | `int` | 신고 누적 건수 | 3건 이상 시 외부 링크 차단 |
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
- 접수 마감일 기준으로 자동 정렬하여 표시한다.
- 접수 마감된 대회는 '접수 종료' 배지를 표시하고 리스트 하단으로 정렬한다.

### 3.2 대회 상세
- 대회의 상세 정보(제목, 주최사, 일시, 장소, 종목 태그)를 확인할 수 있다.
- 대회 포스터 이미지를 확인할 수 있다.

### 3.3 대회 등록/수정/삭제
- 로그인한 유저는 대회 정보를 등록할 수 있다.
- 작성자 본인만 수정/삭제가 가능하다.

### 3.4 대회 관리 (작성자 전용)
- 작성자는 상세 페이지에서 '관리' 버튼을 통해 관리 기능에 접근할 수 있다.
- **대회 정보 수정**: 제목, 주최사, 일시, 장소, 포스터 이미지 등의 정보를 수정할 수 있다.
- **대회 삭제**: 대회 정보를 삭제할 수 있다.

### 3.5 대회 신고
- 로그인한 유저는 대회를 신고할 수 있다.
- 신고 사유를 선택하거나 직접 입력할 수 있다. (허위 정보, 부적절한 내용, 스팸/광고, 기타)
- 동일 유저가 같은 대회를 중복 신고할 수 없다.
- 신고가 3건 이상 누적된 대회는 '신고됨' 상태로 표시되며, 외부 링크 연결이 차단된다.
- 작성자는 자신이 등록한 대회를 신고할 수 없다.

### 3.6 외부 연동
- 대회 정보 클릭 시 `applyLink`를 통해 외부 브라우저로 연결한다.
- 신고가 3건 이상인 대회는 외부 연동이 비활성화된다.



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



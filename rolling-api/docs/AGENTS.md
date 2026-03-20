### 0.6 다음 백엔드 개발 범위

- 아래 4개 항목을 다음 묶음의 우선 범위로 관리한다.
- `Phase 1` 점검 완료: 오픈매트 상세 응답은 이미 `hostNickname`을 포함하며, 프론트 상세 화면은 별도 생성자 조회 API 없이 이 필드를 사용하면 된다.
- `Phase 2` 완료: `GET /api/v1/open-mats/my-hosting`으로 내가 개최한 오픈매트 목록을 기존 `/my`와 같은 페이징 형식으로 조회할 수 있다.
- `Phase 3` 완료: 삭제 알림을 받은 신청자, 호스트, 관리자는 `/openmat/detail`로 진입해 `deleted=true` 상태의 삭제된 오픈매트 정보를 다시 볼 수 있다.
- `Phase 4` 정리: 현재 필요한 관리자 백엔드 기능은 이미 준비돼 있다.
- 관리자 페이지에서 바로 사용할 API는 `POST /api/v1/tournaments/crawl`, `POST /api/v1/notices`, `PUT /api/v1/notices/{id}`, `DELETE /api/v1/notices/{id}`다.
- 위 API는 모두 `Authorization: Bearer {accessToken}` + `ROLE_ADMIN` 기준으로 동작한다.
- 남은 `Phase 4` 범위는 로그인/리프레시/`/users/me` 응답의 `isAdmin`을 활용한 관리자 UI 제어와 기존 ADMIN API 연결 같은 프론트 작업에 가깝다.
- 참고: 현재 `POST /api/v1/open-mats`, `GET /api/v1/open-mats`, `GET /api/v1/open-mats/{id}`, `PUT /api/v1/open-mats/{id}`, `GET /api/v1/open-mats/my`는 모두 `OpenMatResponse`를 사용하므로 `hostNickname` 필드가 공통으로 내려간다.

### 0.7 프론트 연동 관점의 현재 상태

- 오픈매트 작성자 관리 API는 아직 미구현이다. 프론트는 아직 `참가자 목록 조회`, `참가자 강제 취소`, `모집 상태 수동 변경` 화면을 전제로 잡지 않는다.
- 소셜 로그인 버튼/UI는 `GOOGLE`, `KAKAO`, `APPLE` 3종 기준으로 설계할 수 있지만, 현재 서버 구현은 `GOOGLE`, `KAKAO`만 지원한다.
- 특히 iOS 출시 범위에서 `APPLE` 로그인을 실제로 열지는 아직 최종 결정 전이다. 출시 전 정책 확정이 필요하다.
- 관리자 페이지에서 필요한 백엔드 API는 준비돼 있으므로, 관리자 기능의 남은 핵심은 프론트 라우팅, 진입 가드, `isAdmin` 기반 UI 제어다.
- FCM 백엔드 연동은 동작하지만 릴리스 관점의 남은 항목이 있다. 실제 iOS 디바이스 푸시 수신 검증, 알림 권한 거부 상태 UX, 스토어 제출 메타데이터는 별도 확인이 필요하다.
- 앱 출시 전에는 회원 탈퇴 UI 노출 여부와 `DELETE /api/v1/auth/withdraw`, `POST /api/v1/auth/withdraw/cancel` 연결 상태를 한 번 더 점검한다.

### 0.8 운영 준비 관점의 공용 이슈

- 요청 추적용 `requestId`와 Actuator/Prometheus 기반 health, metrics endpoint는 준비돼 있다.
- 다만 운영 채널 알림, 대시보드/로그 질의 기준, 장애별 런북은 아직 문서화가 끝나지 않았다. 장애 대응은 현재 개발자 수동 확인 비중이 높다.
- 신고 기능은 현재 `접수`만 가능하다. 관리자용 `신고 목록/상세/상태 변경/처리 이력` API는 아직 없다.
- 따라서 관리자 화면은 아직 신고 처리 워크플로우를 전제로 잡지 않는다. 현재 신고 데이터는 운영 참고용 raw data에 가깝다.
- 앱 내 1:1 문의 도메인 명칭은 `Inquiry`로 확정됐다. 사용자 문의 생성/내 목록/내 상세, 관리자 목록/상세/답변 저장/상태 변경 API가 추가됐다.
- 운영 제재 모델은 아직 없다. 현재 사용자 간 `block` 기능과 운영자 제재 기능은 같은 개념으로 취급하면 안 된다.
- 관리자 검색/필터 API는 아직 제한적이다. 신고/문의/운영 데이터에 대한 상태별 검색, 기간 필터, 공통 정렬 규칙은 추후 확정 예정이다.
- FCM 토큰 정책은 현재 동작하지만 문서/운영 관점 정합성 작업이 남아 있다. 로그아웃 요청의 `fcmToken` 처리와 `DELETE /api/v1/users/me/fcm` 사용 기준은 같은 라이프사이클 문서로 다시 정리할 예정이다.
- FAQ/도움말을 정적 문서로 둘지, 별도 API/DB로 운영할지는 아직 결정되지 않았다. 프론트는 서버 관리형 FAQ를 전제로 먼저 결합하지 않는다.
- 관리자 권한은 현재 `ROLE_ADMIN` 단일 기준이다. 공지 운영, 제재 운영, 크롤링 운영처럼 세분화된 권한 모델은 아직 없다.

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
  inquiryAnswered, // 문의 답변 등록
}
```

- API raw value: `OPEN_MAT_UPDATED`, `OPEN_MAT_DELETED`, `INQUIRY_ANSWERED`

### InquiryStatus

```dart
enum InquiryStatus {
  received, // 접수됨
  inReview, // 검토중
  answered, // 답변 완료
}
```

- API raw value: `RECEIVED`, `IN_REVIEW`, `ANSWERED`

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
| `platform` | `String?` | 디바이스 플랫폼 | 예: `ANDROID`, `IOS` |
| `deviceId` | `String?` | 앱이 관리하는 디바이스 식별자 | optional |
| `appVersion` | `String?` | 등록 시점 앱 버전 | optional |
| `createdAt` | `DateTime` | 최초 등록 일시 | |
| `updatedAt` | `DateTime` | 마지막 토큰 등록/갱신 일시 | |

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
- `hostNickname`은 `OpenMatResponse` 공통 필드라서 생성/목록/상세/수정/내 신청 목록 응답에서 모두 내려간다.

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
| `targetId` | `int` | 관련 대상 ID | 현재는 OpenMat ID 또는 Inquiry ID |
| `route` | `String` | 앱 이동 경로 | 현재 오픈매트 알림은 `/openmat/detail`, 문의 답변 알림은 `/inquiry/detail` 사용 |
| `title` | `String` | 알림 제목 | |
| `body` | `String` | 알림 본문 | |
| `readAt` | `DateTime?` | 읽음 처리 일시 | `null`이면 미읽음 |
| `createdAt` | `DateTime` | 알림 생성 일시 | 최신순 정렬 기준 |

### 2.7 Notice

도메인 네임: `NoticeModel`

| 필드명 | 타입 | 설명 | 비고 |
| --- | --- | --- | --- |
| `id` | `int` | 공지사항 고유 ID | PK |
| `title` | `String` | 공지사항 제목 | 리스트/상세 공통 |
| `content` | `String` | 공지사항 본문 | 리스트/상세 공통 |
| `authorName` | `String` | 작성 관리자 이름 | 프론트 노출용 |
| `createdAt` | `DateTime` | 작성 일시 | 최신순 정렬 기준 |
| `updatedAt` | `DateTime` | 마지막 수정 일시 | 상세/운영 응답에서 사용 |

프론트 구현 메모:

- 현재 목표 계약은 목록 응답 item과 상세 응답 모두 위 필드를 동일하게 사용한다.
- 목록 화면에서 본문 전체를 그대로 노출할지, 일부만 잘라서 노출할지는 프론트 UI에서 결정한다.
- 현재 구현 기준으로 목록 응답에는 `updatedAt`만 제외되고 나머지 필드는 포함된다.
- 작성자 표기가 필요하면 응답의 `authorName`을 그대로 사용한다.
- 작성/수정/삭제 UI는 앱 범위 밖이며, 일반 사용자 앱에서는 조회만 고려한다.

백엔드 내부 개념 메모:

- 운영 작성자 추적용 내부 필드는 `createdBy`로 관리한다.
- 운영 생성 시 `createdBy`를 생략하면 `authorName` 값을 그대로 저장한다.
- 일반 사용자 앱 응답에는 내부 식별자 대신 `authorName`만 노출하는 방향을 기본 계약으로 본다.

### 2.8 Inquiry

도메인 네임: `InquiryModel`

| 필드명 | 타입 | 설명 | 비고 |
| --- | --- | --- | --- |
| `id` | `int` | 문의 고유 ID | PK |
| `userId` | `int` | 문의 작성자 사용자 ID | FK |
| `userNickname` | `String` | 문의 작성자 닉네임 | 관리자 화면 표시용 |
| `title` | `String` | 문의 제목 | |
| `content` | `String` | 문의 본문 | |
| `status` | `InquiryStatus` | 문의 상태 | Enum |
| `answerContent` | `String?` | 운영자 답변 본문 | 답변 전에는 `null` |
| `answeredByUserId` | `int?` | 답변한 관리자 사용자 ID | 답변 전에는 `null` |
| `answeredAt` | `DateTime?` | 답변 시각 | 답변 전에는 `null` |
| `createdAt` | `DateTime` | 문의 생성 일시 | 최신순 정렬 기준 |
| `updatedAt` | `DateTime` | 문의 수정 일시 | |

프론트 구현 메모:

- 현재 사용자 앱 범위에서 필요한 흐름은 `문의 작성 -> 내 문의 목록 -> 내 문의 상세 -> 답변 확인`이다.
- 사용자 목록/상세 응답과 관리자 목록/상세 응답은 같은 필드 집합을 사용한다.
- `status == ANSWERED`이고 `answerContent != null`이면 답변 완료 문의로 본다.
- 문의 답변 알림 route는 `/inquiry/detail`이고 `targetId`는 문의 ID다.
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
- `withdrawalPending = true` 사용자는 FCM 발송 대상에서 제외한다.
- 로그아웃 시 현재 디바이스 토큰은 `POST /api/v1/auth/logout` 요청 본문 또는 `DELETE /api/v1/users/me/fcm`로 제거한다.
- FCM 발송 실패 시 서버는 자동 재시도하지 않는다. `UNREGISTERED`, `INVALID_ARGUMENT`는 토큰 정리 대상으로 처리하고, 그 외 오류는 로그 후 예외로 남긴다.

알림 클릭 규칙:

| type | route | 프론트 동작 |
| --- | --- | --- |
| `OPEN_MAT_UPDATED` | `/openmat/detail` | `targetId`로 상세 진입 후 최신 데이터 재조회 |
| `OPEN_MAT_DELETED` | `/openmat/detail` | `targetId`로 삭제 상세 진입 후 `deleted=true` 상태 화면 표시 |

추가 규칙:

- 알림 클릭 시 `PATCH /api/v1/notifications/{id}/read` 호출
- 읽음 처리 API는 idempotent
- `OPEN_MAT_UPDATED` 상세 재조회가 `404 NOT_FOUND`면 `/openmat` fallback
- `INQUIRY_ANSWERED`는 로그인 사용자 기준 문의 상세를 다시 조회해 최신 답변 상태를 반영한다.

### 3.6 공지사항 핵심 규칙

- 공지사항은 일반 사용자 앱에서 `읽기 전용` 기능으로 다룬다.
- 앱에서 필요한 화면은 `목록 페이지`와 `상세 페이지` 두 가지다.
- 목록은 페이징 조회를 기준으로 하고, 기본 정렬은 최신 작성일(`createdAt DESC`)이다.
- 상세는 `id`로 단건 조회한다.
- 운영 작성/수정/삭제는 같은 JWT 인증 플로우를 사용하되 `ROLE_ADMIN` 권한으로 제한한다.
- 운영 삭제 정책은 soft delete가 아니라 `hard delete`다.
- 프론트는 공지사항 작성/수정/삭제 버튼이나 화면을 전제로 구현하지 않는다.

### 3.7 문의 핵심 규칙

- 문의 도메인 명칭은 `Inquiry`로 통일한다.
- 사용자는 본인 문의만 생성/조회할 수 있다.
- 관리자만 전체 문의 목록/상세 조회, 답변 저장, 상태 변경을 할 수 있다.
- 문의 생성 시 기본 상태는 `RECEIVED`다.
- 관리자 답변 저장 API는 답변 본문을 저장하고 상태를 `ANSWERED`로 변경한다.
- 답변이 없는 문의는 `ANSWERED` 상태로 직접 변경할 수 없다.
- 이미 답변이 저장된 문의는 `ANSWERED` 외 상태로 되돌리지 않는다.
- 첫 답변 완료 시 사용자 알림함에 `INQUIRY_ANSWERED` 알림을 저장한다.
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
- 공지사항은 별도 작성 플로우 없이 `목록 -> 상세` 읽기 흐름만 잡으면 된다.
- 공지사항 목록 item은 `id`, `title`, `content`, `authorName`, `createdAt`를 사용한다.
- 공지사항 상세는 같은 필드를 그대로 사용해 상세 페이지를 구성하면 된다.
- 문의는 인증 사용자 기준으로 `목록 -> 상세 -> 답변 확인` 흐름을 구현하면 된다.

### 4.3 프론트 주의사항

- 현재 `/users/me` 수정 API는 `phone` 수정 미지원이다.
- 현재 `/open-mats/my`는 배열이 아니라 페이징 응답이다.
- 현재 오픈매트 생성/수정 요청에는 `region`이 포함된다.
- 현재 오픈매트 작성자 관리용 API는 없다. 참가자 관리나 수동 마감/재모집 화면은 백엔드 추가 전까지 구현 대상으로 잡지 않는다.
- 현재 로그인 API 요청 허용값은 `GOOGLE`, `KAKAO`다. `APPLE` 버튼을 노출하더라도 실제 호출 가능 여부는 출시 정책 확정 후 다시 맞춘다.
- 현재 FCM은 서버 저장 데이터 기반 알림함과 함께 동작한다. 다만 실제 릴리스 전에는 iOS 실기기 수신, 권한 거부 상태 UX, 리뷰어 안내 문구까지 별도 확인이 필요하다.
- 알림 권한을 거부해도 오픈매트/대회/공지 핵심 조회와 신청 흐름은 막히지 않게 설계한다.


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
| `socialProvider` | `String` |
| `beltColor` | `String` |
| `createdAt` | `DateTime` |
| `withdrawalPending` | `Boolean` |
| `withdrawalScheduledAt` | `DateTime?` |
| `isAdmin` | `Boolean` |

현재 구현 메모:

- `/users/me` 응답에는 현재 사용자 기준 `isAdmin` 필드가 포함된다.
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
| `platform` | `String` | - |
| `deviceId` | `String` | - |
| `appVersion` | `String` | - |

Response data: `null`

현재 구현 메모:

- 토큰 저장 구조는 `user_devices` 1:N 이다.
- 동일 토큰 재등록 시 기존 디바이스 레코드를 재사용한다.
- 동일 토큰 재등록 시 `platform`, `deviceId`, `appVersion`, `updatedAt`도 최신값으로 갱신한다.
- 로그아웃/탈퇴/기기 변경 시점별 토큰 정리 규칙은 운영 문서에서 추가 정리 중이다.

### 5.2.4 FCM 토큰 삭제

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

### 5.2.5 사용자 차단

`POST /api/v1/users/{id}/block`

- 인증: 필요
- Response data: `null`

### 5.2.6 사용자 차단 해제

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

현재 구현 메모:

- 현재 상세 응답 모델은 `OpenMatModel`이며 `hostNickname`을 포함한다.
- 프론트 상세 화면은 별도 생성자 조회 API 없이 상세 응답의 `hostNickname`을 그대로 사용하면 된다.
- soft delete된 오픈매트는 비로그인 사용자에게는 `NOT_FOUND`다.
- 삭제 알림을 받은 신청자, 호스트, 관리자에게는 `deleted=true`, `deletedAt`이 포함된 상세 응답을 반환한다.

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

### 5.4.10 오픈매트 신고

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

















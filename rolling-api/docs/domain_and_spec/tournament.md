# Tournament

- 대회 도메인 모델과 API 스펙을 관리한다.
- 공통 응답, 인증, 날짜/시간 포맷, 사용자 전역 푸시 정책은 [shared/common-models.md](shared/common-models.md)를 따른다.
- 지역 raw value는 오픈매트와 같은 `Region` enum을 재사용한다.

## 1. 도메인 개요

대회는 공개 목록/상세 조회를 기본으로 하고, 로그인 사용자가 수동 등록 대회를 생성/수정/삭제하거나 관심 대회를 찜하고 접수 마감 전 리마인드를 설정할 수 있는 도메인이다.

현재 구현 범위:

- 대회 목록 공개 조회
- 대회 상세 공개 조회
- 대회 생성/수정/삭제
- 대회 포스터 업로드 URL 발급
- 대회 신고
- 대회 찜 추가/해제
- 찜한 대회 목록 조회
- 찜한 대회 리마인드 on/off, 날짜/시간 설정
- 대회 리마인드 Notification 저장 + FCM 발송 시도
- 관리자 수동 크롤링 실행
- 로그인 사용자 기준 차단한 작성자의 대회 숨김

## 2. 도메인 모델

### 2.1 TournamentResponse

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 대회 ID |
| `source` | `TournamentSource` | 대회 출처 |
| `title` | `String` | 대회 제목 |
| `organizer` | `String?` | 주최사 |
| `posterUrl` | `String?` | 표시용 포스터 URL |
| `competitionDate` | `Date` | 대회 개최일 |
| `registrationDeadline` | `Date?` | 접수 마감일 |
| `location` | `String?` | 개최 장소 |
| `region` | `Region?` | 지역 |
| `applyLink` | `String` | 외부 접수 링크 |
| `registrationClosed` | `Boolean` | 현재 접수 마감 여부 |
| `createdAt` | `DateTime?` | 생성 시각 |

현재 구현 메모:

- `source=null`인 레거시 데이터는 응답에서 `MANUAL`로 보정한다.
- `posterKey`가 있으면 `posterUrl`은 S3 public URL 기준으로 계산한다.
- `registrationClosed`는 `Asia/Seoul` 기준 현재 날짜가 `registrationDeadline`을 지난 경우 `true`다.

### 2.2 TournamentCreateRequest

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `title` | `String` | 대회 제목 |
| `organizer` | `String?` | 주최사 |
| `posterKey` | `String?` | S3 포스터 object key |
| `competitionDate` | `Date` | 대회 개최일 |
| `registrationDeadline` | `Date` | 접수 마감일 |
| `location` | `String?` | 개최 장소 |
| `region` | `Region?` | 지역 |
| `applyLink` | `String` | 외부 접수 링크 |

현재 구현 메모:

- 수동 등록 대회는 항상 `source=MANUAL`로 저장한다.
- `registrationDeadline`은 `competitionDate`보다 이후일 수 없다.

### 2.3 TournamentUpdateRequest

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `title` | `String?` | 대회 제목 |
| `organizer` | `String?` | 주최사 |
| `posterUrl` | `String?` | 포스터 이미지 URL |
| `competitionDate` | `Date?` | 대회 개최일 |
| `registrationDeadline` | `Date?` | 접수 마감일 |
| `location` | `String?` | 개최 장소 |
| `region` | `Region?` | 지역 |
| `applyLink` | `String?` | 외부 접수 링크 |

현재 구현 메모:

- 최소 1개 필드는 전달해야 한다.
- 전달하지 않은 필드는 기존 값을 유지한다.
- 수정도 `registrationDeadline <= competitionDate` 검증을 유지한다.

### 2.4 TournamentFavoriteResponse

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `tournamentId` | `Long` | 대회 ID |
| `source` | `TournamentSource` | 대회 출처 |
| `title` | `String` | 대회 제목 |
| `organizer` | `String?` | 주최사 |
| `posterUrl` | `String?` | 표시용 포스터 URL |
| `competitionDate` | `Date` | 대회 개최일 |
| `registrationDeadline` | `Date?` | 접수 마감일 |
| `location` | `String?` | 개최 장소 |
| `region` | `Region?` | 지역 |
| `applyLink` | `String` | 외부 접수 링크 |
| `registrationClosed` | `Boolean` | 현재 접수 마감 여부 |
| `notificationEnabled` | `Boolean` | 리마인드 활성화 여부 |
| `remindDate` | `Date?` | 알림 날짜 |
| `remindTime` | `Time?` | 알림 시간 |
| `favoritedAt` | `DateTime?` | 찜한 시각 |

현재 구현 메모:

- 찜 목록은 별도 응답 타입을 사용하고, 일반 대회 목록/상세 응답에는 `favorited` 필드를 포함하지 않는다.

### 2.5 TournamentFavoriteReminderUpdateRequest

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `notificationEnabled` | `Boolean` | 알림 활성화 여부 |
| `remindDate` | `Date?` | 알림 날짜 |
| `remindTime` | `Time?` | 알림 시간 |

현재 구현 메모:

- `notificationEnabled=true`면 `remindDate`, `remindTime`이 모두 필요하다.
- `notificationEnabled=false`면 기존 날짜/시간과 pending 발송 정보는 모두 제거한다.

### 2.6 TournamentPosterUploadUrlRequest

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `fileName` | `String` | 원본 파일명 |
| `contentType` | `String` | 파일 content type |

### 2.7 TournamentPosterUploadUrlResponse

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `uploadUrl` | `String` | S3 업로드용 presigned URL |
| `posterKey` | `String` | 서버 저장용 S3 object key |
| `expiresAt` | `DateTime` | URL 만료 시각 |

### 2.8 TournamentCrawlResult

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `crawledCount` | `Integer` | 수집한 원본 건수 |
| `createdCount` | `Integer` | 신규 생성 건수 |
| `updatedCount` | `Integer` | 기존 업데이트 건수 |
| `skippedCount` | `Integer` | 검증 실패 또는 스킵 건수 |

## 3. Enum

### 3.1 TournamentSource

현재 값:

- `STREET_JIU_JITSU`
- `KOREA_JIU`
- `HEROES_OF_JIU_JITSU`
- `SPOTLITE`
- `MANUAL`

### 3.2 Region

대회는 오픈매트와 동일한 `Region` raw value를 사용한다.

현재 값:

- `SEOUL`
- `GYEONGGI`
- `INCHEON`
- `DAEJEON`
- `SEJONG`
- `CHUNGBUK`
- `CHUNGNAM`
- `BUSAN`
- `DAEGU`
- `ULSAN`
- `GYEONGBUK`
- `GYEONGNAM`
- `GWANGJU`
- `JEONBUK`
- `JEONNAM`
- `GANGWON`
- `JEJU`

### 3.3 PushNotificationType

대회 리마인드에서 사용하는 알림 raw value:

- `TOURNAMENT_FAVORITE_REMINDER`

## 4. 공통 정책

### 4.1 인증과 공개 조회

- `GET /api/v1/tournaments`
- `GET /api/v1/tournaments/{id}`

위 두 API는 인증 없이 호출할 수 있다.

- 로그인 사용자가 호출하면 차단한 작성자의 대회는 목록과 상세에서 숨긴다.
- `GET /api/v1/tournaments/favorites`는 공개 상세 경로보다 우선해 인증을 요구한다.
- 찜, 리마인드, 생성, 수정, 삭제, 신고, 포스터 업로드 URL 발급은 모두 인증이 필요하다.
- 관리자 수동 크롤링은 `ROLE_ADMIN`이 필요하다.

### 4.2 지역

- `region`은 nullable이다.
- 크롤링 데이터는 `region`을 자동 수집하거나 추론하지 않는다.
- 수동 등록/수정 흐름에서만 `region`을 직접 저장한다.

### 4.3 목록 정렬

- 대회 목록과 찜한 대회 목록은 접수 가능한 대회를 먼저 보여준다.
- 기본 정렬 우선순위는 `registrationClosed asc`, `registrationDeadline asc`, `competitionDate asc`, `id asc`다.

### 4.4 포스터

- 포스터 업로드 URL 발급은 `jpg`, `jpeg`, `png`, `webp`, `gif`만 허용한다.
- 생성 시 `posterKey`가 있으면 서버가 public URL을 계산해 저장한다.
- 상세/목록/찜 목록 응답은 `posterKey`가 있으면 계산된 public URL을 우선 사용한다.

### 4.5 찜과 리마인드

- 찜은 사용자-대회당 1건만 허용한다.
- `POST /favorite`는 멱등적으로 동작한다.
- 리마인드는 찜한 대회에만 설정할 수 있다.
- 현재 구현은 사용자-대회당 리마인드 1건 모델이다.
- 리마인드 내부 기준 마감 시각은 `registrationDeadline 23:59:59` (`Asia/Seoul`)이다.
- `scheduledAt`은 현재 시각 이후이면서 내부 기준 마감 시각 이전이어야 한다.
- 접수 마감일이 없으면 리마인드를 설정할 수 없다.
- 대회 마감일 변경으로 기존 `scheduledAt`이 유효 범위를 벗어나면 pending reminder를 비활성화한다.
- 대회 삭제 시 찜과 pending reminder는 함께 제거한다.

### 4.6 알림

- 리마인드 발송 시 `Notification` 저장이 source of truth다.
- FCM 발송은 best effort이며, 실패해도 이미 저장된 알림을 롤백하지 않는다.
- 사용자 전역 푸시 설정 `pushNotificationEnabled=false`면 FCM 발송 대상 디바이스에서 제외된다.
- 전역 푸시를 꺼도 `Notification` 알림함 저장은 유지한다.
- 동일 리마인드는 `sentAt`으로 중복 발송을 방지한다.

## 5. API 스펙

### 5.1 대회 목록 조회

`GET /api/v1/tournaments`

- 인증: 선택

Query params:

- `source`
- `region`
- `q`
- pageable

Response data: `Page<TournamentResponse>`

현재 구현 메모:

- `source`는 `STREET_JIU_JITSU`, `KOREA_JIU`, `HEROES_OF_JIU_JITSU`, `SPOTLITE`, `MANUAL` 중 하나다.
- `region`은 optional이다.
- `q`는 대회명, 주최사, 장소, 접수 링크를 대상으로 검색한다.

### 5.2 대회 상세 조회

`GET /api/v1/tournaments/{id}`

- 인증: 선택
- Response data: `TournamentResponse`

에러:

- `NOT_FOUND`

### 5.3 대회 등록

`POST /api/v1/tournaments`

- 인증: 필요
- Response data: `TournamentResponse`

에러:

- `UNAUTHORIZED`
- `VALIDATION_ERROR`

### 5.4 대회 포스터 업로드 URL 발급

`POST /api/v1/tournaments/poster-upload-url`

- 인증: 필요
- Request body: `TournamentPosterUploadUrlRequest`
- Response data: `TournamentPosterUploadUrlResponse`

에러:

- `UNAUTHORIZED`
- `VALIDATION_ERROR`

### 5.5 대회 수정

`PUT /api/v1/tournaments/{id}`

- 인증: 필요
- Request body: `TournamentUpdateRequest`
- Response data: `TournamentResponse`

에러:

- `UNAUTHORIZED`
- `FORBIDDEN`
- `NOT_FOUND`
- `VALIDATION_ERROR`

현재 구현 메모:

- 작성자 또는 관리자만 수정할 수 있다.

### 5.6 대회 삭제

`DELETE /api/v1/tournaments/{id}`

- 인증: 필요
- Response data: `null`

에러:

- `UNAUTHORIZED`
- `FORBIDDEN`
- `NOT_FOUND`

현재 구현 메모:

- 삭제는 hard delete다.

### 5.7 대회 신고

`POST /api/v1/tournaments/{id}/report`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `ReportReason` | O | `FALSE_INFO`, `INAPPROPRIATE`, `SPAM`, `OTHER` |
| `customReason` | `String?` | - | 기타 신고 사유 |

Response data: `null`

에러:

- `UNAUTHORIZED`
- `NOT_FOUND`
- `VALIDATION_ERROR`

### 5.8 대회 찜 추가

`POST /api/v1/tournaments/{id}/favorite`

- 인증: 필요
- Response data: `TournamentFavoriteResponse`

에러:

- `UNAUTHORIZED`
- `NOT_FOUND`

### 5.9 대회 찜 해제

`DELETE /api/v1/tournaments/{id}/favorite`

- 인증: 필요
- Response data: `null`

에러:

- `UNAUTHORIZED`
- `NOT_FOUND`

### 5.10 찜한 대회 목록 조회

`GET /api/v1/tournaments/favorites`

- 인증: 필요
- Response data: `Page<TournamentFavoriteResponse>`

현재 구현 메모:

- 로그인 사용자가 차단한 작성자의 대회는 찜 목록에서도 숨긴다.

### 5.11 찜한 대회 리마인드 설정

`PATCH /api/v1/tournaments/{id}/favorite-reminder`

- 인증: 필요
- Request body: `TournamentFavoriteReminderUpdateRequest`
- Response data: `TournamentFavoriteResponse`

에러:

- `UNAUTHORIZED`
- `NOT_FOUND`
- `VALIDATION_ERROR`

현재 구현 메모:

- `notificationEnabled=false`면 기존 리마인드를 즉시 해제한다.
- `notificationEnabled=true`면 `remindDate`, `remindTime`이 모두 필요하다.

요청 예시:

```json
{
  "notificationEnabled": true,
  "remindDate": "2026-06-13",
  "remindTime": "09:00"
}
```

### 5.12 대회 크롤링 수동 실행

`POST /api/v1/tournaments/crawl`

- 인증: `Authorization: Bearer {accessToken}` 필요 (`ROLE_ADMIN`)

Query params:

- `source`

Response data: `TournamentCrawlResult`

에러:

- `UNAUTHORIZED`
- `FORBIDDEN`
- `VALIDATION_ERROR`

현재 구현 메모:

- `source`가 없으면 전체 크롤러를 실행한다.
- `source`가 있으면 해당 출처만 실행한다.

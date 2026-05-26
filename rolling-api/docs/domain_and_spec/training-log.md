# Training Log

- 개인 훈련 기록 도메인과 API 스펙을 관리한다.
- 공통 응답, 인증, 날짜/시간 포맷은 [shared/common-models.md](shared/common-models.md)를 따른다.
- 현재 구현 범위는 `training-log-product-plan.md` 기준 Phase 1~10, 훈련 강도/체육관 출석/컨디션 확장, 365일 출석 잔디와 주간/월간 인사이트 조회다.
- 친구 공개 범위, 친구 열람, 좋아요, 댓글 확장 계약은 [training-log-social.md](training-log-social.md)에서 별도 관리한다.

## 1. 도메인 개요

훈련 기록은 로그인한 사용자가 특정 날짜에 카테고리 기반 기록을 남기고, 같은 날짜의 요약 카드 목록이나 기록 상세를 조회/수정/삭제하고, 해시태그를 자동완성하며, 월간 캘린더 요약과 최근 기록 목록을 조회하고, 이미지 업로드용 presigned URL을 발급받는 도메인이다.

현재 구현 범위:

- 특정 날짜 훈련 기록 요약 카드 목록 조회
- 특정 훈련 기록 상세 조회
- 특정 날짜 훈련 기록 생성
- 훈련 기록 수정
- 훈련 기록 삭제
- 본인 데이터 소유권 검증
- 체크리스트 JSON 저장/조회
- 해시태그 정규화, 중복 제거, 자동완성
- 외부 링크 JSON 저장/조회
- 월간 캘린더 요약 조회
- 최근 훈련 기록 조회
- 이미지 업로드용 presigned URL 발급
- 365일 출석 잔디 조회
- 주간/월간 훈련 인사이트 조회
- `PROMOTION` 카테고리 전용 검증 및 최신 벨트 동기화

## 2. 도메인 모델

### 2.1 `TrainingLogEntry`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 훈련 기록 ID |
| `user` | `User` | 소유 사용자 |
| `trainingDate` | `LocalDate` | 기록 날짜 |
| `category` | `TrainingLogCategory` | 기록 카테고리 |
| `title` | `String` | 기록 제목 |
| `content` | `String` | 기록 본문 |
| `checklistJson` | `String?` | 체크리스트 JSON |
| `hashtagsJson` | `String?` | 해시태그 JSON |
| `externalLinksJson` | `String?` | 외부 링크 JSON |
| `imageUrl` | `String?` | 대표 이미지 URL |
| `imageUrlsJson` | `String?` | 이미지 목록 JSON |
| `color` | `TrainingLogColor?` | 기록 색상 |
| `visibility` | `TrainingLogVisibility` | 기록 공개 범위 |
| `trainingIntensity` | `Integer?` | 훈련 강도(1~5) |
| `gymAttendance` | `Boolean?` | 체육관 출석 여부 |
| `condition` | `Integer?` | 컨디션(1~5) |
| `trainingMinutes` | `Integer?` | 훈련 시간(분) |
| `beltColor` | `BeltColor?` | `PROMOTION` 전용 |
| `stripeCount` | `Integer?` | `PROMOTION` 전용 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

구현 메모:

- DB에는 `checklist_json`, `hashtags_json`, `external_links_json` 문자열 컬럼으로 저장한다.
- `checklist_json`의 각 항목은 `text`, `checked`, `favorite`, `emoji` 필드를 가진다.
- `imageUrl`, `imageUrls`, `color`, `visibility`, `trainingIntensity`, `gymAttendance`, `condition`, `trainingMinutes`, `beltColor`, `stripeCount`는 response DTO에 노출된다.
- 최신 `PROMOTION` 기록이 있으면 그 값으로 `User.beltColor`를 동기화한다.
- 친구 소셜 열람 권한은 `visibility`가 아니라 [training-log-social.md](training-log-social.md)의 `shareWithFriends` 설정을 따른다.

### 2.2 `TrainingLogChecklistItem`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `text` | `String` | 체크리스트 항목 내용 |
| `checked` | `boolean` | 완료 여부 |
| `favorite` | `boolean` | 즐겨찾기 여부 |
| `emoji` | `String?` | 즐겨찾기 표시 이모지 |

### 2.3 `TrainingLogExternalLink`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `type` | `TrainingLogLinkType` | 링크 타입 |
| `url` | `String` | 정규화된 링크 URL |

### 2.4 `TrainingLogEntrySummaryResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 기록 ID |
| `title` | `String` | 기록 제목 |
| `content` | `String` | 기록 본문 |
| `category` | `TrainingLogCategory` | 기록 카테고리 |
| `color` | `TrainingLogColor?` | 기록 색상 |
| `likeCount` | `Long` | 좋아요 수 |
| `commentCount` | `Long` | 댓글 수 |
| `createdAt` | `LocalDateTime` | 생성 시각 |

### 2.5 `TrainingLogMonthlyCalendarDailySummary`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `date` | `LocalDate` | 훈련 날짜 |
| `colors` | `List<TrainingLogColor>` | 해당 일자의 색상 목록 |
| `categories` | `List<TrainingLogCategory>` | 해당 일자의 카테고리 목록 |
| `recordCount` | `Integer` | 해당 일자의 기록 수 |

### 2.6 `TrainingLogMonthlyCalendarResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `year` | `Integer` | 조회 연도 |
| `month` | `Integer` | 조회 월 |
| `dailySummaries` | `List<TrainingLogMonthlyCalendarDailySummary>` | 일별 요약 목록 |

### 2.7 `TrainingLogImageUploadUrlResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `uploadUrl` | `String` | S3 업로드용 presigned URL |
| `imageKey` | `String` | 서버 저장용 S3 object key |
| `imageUrl` | `String` | 업로드 후 접근 가능한 공개 이미지 URL |
| `expiresAt` | `LocalDateTime` | presigned URL 만료 시각 |

## 3. Enum

### 3.1 `TrainingLogCategory`

| Raw value | 설명 |
| --- | --- |
| `TECHNIQUE` | 기술 기록 |
| `SPARRING` | 스파링 기록 |
| `TOURNAMENT` | 대회 기록 |
| `PROMOTION` | 승급 기록 |
| `OPEN_MAT` | 오픈매트 기록 |
| `DRILL` | 드릴 기록 |
| `PERSONAL_TRAINING` | 개인 훈련 기록 |

구현 메모:

- API와 DB 모두 enum raw value를 그대로 사용한다.
- JPA 저장 방식은 `EnumType.STRING`이다.

### 3.2 `TrainingLogLinkType`

| Raw value | 설명 |
| --- | --- |
| `INSTAGRAM` | 인스타그램 링크 |
| `YOUTUBE` | 유튜브 링크 |

구현 메모:

- 외부 링크 요청/응답과 저장 JSON에서 이 두 값만 사용한다.

### 3.3 `TrainingLogColor`

| Raw value | 설명 |
| --- | --- |
| `RED` | 기본 색상 |
| `ORANGE` | 기본 색상 |
| `YELLOW` | 기본 색상 |
| `GREEN` | 기본 색상 |
| `BLUE` | 기본 색상 |
| `NAVY` | 기본 색상 |
| `PURPLE` | 기본 색상 |
| `PINK` | 기본 색상 |
| `TEAL` | 기본 색상 |
| `BROWN` | 기본 색상 |
| `GRAY` | 기본 색상 |
| `BLACK` | 기본 색상 |

구현 메모:

- `TrainingLogColor`는 기록의 독립 색상 필드로 사용한다.
- 실제 색상 코드 매핑은 프론트 UI 레이어에서 관리한다.

### 3.4 `TrainingLogVisibility`

| Raw value | 설명 |
| --- | --- |
| `PRIVATE` | 작성자만 조회 가능 |
| `FRIENDS` | 친구만 조회 가능 |

구현 메모:

- 생성/수정 request에서 생략하면 `PRIVATE`로 처리한다.
- 개인 기록 API의 상세 응답에는 항상 포함한다.

## 4. 공통 정책

### 4.1 인증과 권한

- 모든 training log API는 인증이 필요하다.
- 모든 조회/수정/삭제는 본인 데이터 범위에서만 허용한다.
- 다른 사용자의 기록 ID를 수정/삭제하려고 하면 `FORBIDDEN`을 반환한다.

### 4.2 날짜와 정렬

- `trainingDate` path variable은 `YYYY-MM-DD` 포맷을 사용한다.
- 특정 날짜 요약 카드 목록은 `createdAt ASC`로 반환한다.
- 특정 기록 상세는 `id`로 조회한다.
- 최근 기록 목록은 `trainingDate DESC`, 같은 날짜에서는 `createdAt DESC` 기준으로 최대 10건 반환한다.
- 생성 시 `trainingDate`는 미래 날짜일 수 없다.
- 캘린더 집계 `year`는 `2000..2100` 범위만 허용한다.
- 월간 캘린더 집계 `month`는 `1..12` 범위만 허용한다.

### 4.3 제목/내용/훈련 강도/체육관 출석/컨디션

- `title`, `content`는 생성 시 필수다.
- 수정 시 `title`, `content`를 보내면 trim 후 저장한다.
- `trainingIntensity`는 1~5 범위의 선택 값이다.
- `gymAttendance`는 체육관 출석 여부를 나타내는 선택 값이다.
- `condition`은 1~5 범위의 선택 값이며 훈련 당일 컨디션을 나타낸다.

### 4.4 체크리스트

- 체크리스트는 최대 20개까지 허용한다.
- 각 항목의 `text`는 필수이며 trim 후 저장한다.
- `checked`가 없으면 `false`로 저장한다.
- 수정 시 `checklist: []` 또는 `checklist: null`을 보내면 기존 체크리스트를 비운다.

### 4.5 해시태그

- 해시태그는 최대 10개까지 허용한다.
- trim 후 소문자로 정규화한다.
- 앞의 `#`는 제거한다.
- 중복은 제거하고 입력 순서를 유지한다.
- 허용 문자는 영문 소문자, 숫자, 한글, 하이픈(`-`)이다.
- 자동완성은 본인 데이터 기준으로만 수행한다.
- 자동완성은 저장된 해시태그 JSON을 최근 생성 순으로 순회하며 최대 20개를 반환한다.

### 4.6 외부 링크

- 허용 타입은 `INSTAGRAM`, `YOUTUBE` 두 가지다.
- 허용 도메인은 `instagram.com`, `youtube.com`, `youtu.be` 및 `www.` 접두어가 붙은 변형이다.
- URL은 `https://` 기준으로 정규화한다.
- 기록당 외부 링크는 최대 3개까지 허용한다.

### 4.7 카테고리별 검증

- `PROMOTION`에서는 `beltColor`가 필수다.
- `PROMOTION`에서 `stripeCount`를 보내면 0 이상이어야 한다.
- `PROMOTION`이 아닌 카테고리에서 `beltColor`, `stripeCount`를 보내면 `VALIDATION_ERROR`를 반환한다.
- `PROMOTION` 기록의 생성/수정/삭제 시 최신 `PROMOTION` 기록을 기준으로 `User.beltColor`를 동기화한다.

### 4.8 이미지 업로드

- 업로드 API는 presigned PUT URL을 발급한다.
- 허용 파일 형식은 `jpg`, `jpeg`, `png`와 대응 content type(`image/jpeg`, `image/jpg`, `image/png`)이다.
- object key prefix는 `training/logs/images/`다.
- 응답의 `imageUrl`은 `cloud.aws.s3.public-base-url`이 있으면 그 값을, 없으면 S3 bucket URL을 사용한다.

## 5. Training Log API

### 5.1 특정 날짜 훈련 기록 요약 카드 목록 조회

`GET /api/v1/training-logs/me/entries?date=2026-05-18`

- 인증: 필요
- Response data: `List<TrainingLogEntrySummaryResponse>`

### 5.2 특정 훈련 기록 상세 조회

`GET /api/v1/training-logs/me/entries/{id}`

- 인증: 필요
- Response data: `TrainingLogEntryResponse`

구현 메모:

- 본인 상세 조회 응답에는 소셜 메타 필드 `likeCount`, `commentCount`, `likedByMe`, `commentableByMe`를 함께 포함한다.
- 현재 정책 기준 본인 기록은 좋아요 대상이 아니므로 `likedByMe = false`로 반환한다.
- `settings.showOwnReactions = false`면 본문 데이터만 반환하고 `likeCount = 0`, `commentCount = 0`, `likedByMe = false`, `commentableByMe = false`로 반환한다.

### 5.3 특정 날짜 훈련 기록 생성

`POST /api/v1/training-logs/me/entries/{date}`

- 인증: 필요
- Response data: `TrainingLogEntryResponse`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `category` | `TrainingLogCategory` | O | 기록 카테고리 |
| `title` | `String` | O | 기록 제목 |
| `content` | `String` | O | 기록 본문 |
| `checklist` | `List<TrainingLogChecklistItemRequest>?` | - | 체크리스트 목록 |
| `hashtags` | `List<String>?` | - | 해시태그 목록 |
| `externalLinks` | `List<TrainingLogExternalLinkRequest>?` | - | 외부 링크 목록 |
| `imageUrls` | `List<String>?` | - | 이미지 목록 |
| `imageUrl` | `String?` | - | 대표 이미지 URL |
| `color` | `TrainingLogColor?` | - | 기록 색상 |
| `visibility` | `TrainingLogVisibility?` | - | 기록 공개 범위. 기본값 `PRIVATE` |
| `trainingIntensity` | `Integer?` | - | 훈련 강도 |
| `gymAttendance` | `Boolean?` | - | 체육관 출석 여부 |
| `condition` | `Integer?` | - | 컨디션(1~5) |
| `trainingMinutes` | `Integer?` | - | 훈련 시간(분) |
| `beltColor` | `BeltColor?` | `PROMOTION`일 때 필수 | 승급 기록 전용 |
| `stripeCount` | `Integer?` | - | 승급 기록 전용 |

### 5.4 훈련 기록 수정

`PATCH /api/v1/training-logs/me/entries/{id}`

- 인증: 필요
- Response data: `TrainingLogEntryResponse`

Request body:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `category` | `TrainingLogCategory?` | 전달 시 카테고리 변경 |
| `title` | `String?` | 전달 시 trim 후 반영 |
| `content` | `String?` | 전달 시 trim 후 반영 |
| `checklist` | `List<TrainingLogChecklistItemRequest>?` | `[]` 또는 `null`이면 비움 |
| `hashtags` | `List<String>?` | `[]` 또는 `null`이면 비움 |
| `externalLinks` | `List<TrainingLogExternalLinkRequest>?` | `[]` 또는 `null`이면 비움 |
| `imageUrls` | `List<String>?` | `[]` 또는 `null`이면 비움 |
| `imageUrl` | `String?` | `null`이면 비움 |
| `color` | `TrainingLogColor?` | `null`이면 비움 |
| `visibility` | `TrainingLogVisibility?` | 전달 시 반영, `null`이면 `PRIVATE` |
| `trainingIntensity` | `Integer?` | `null`이면 비움 |
| `gymAttendance` | `Boolean?` | `null`이면 비움 |
| `condition` | `Integer?` | `null`이면 비움 |
| `trainingMinutes` | `Integer?` | `null`이면 비움 |
| `beltColor` | `BeltColor?` | `PROMOTION` 전용, `null`이면 비움 |
| `stripeCount` | `Integer?` | `PROMOTION` 전용, `null`이면 비움 |

구현 메모:

- 현재 구현에서는 `trainingDate` 수정은 지원하지 않는다.

### 5.5 훈련 기록 삭제

`DELETE /api/v1/training-logs/me/entries/{id}`

- 인증: 필요
- Response data: `null`
- 삭제 방식: hard delete

### 5.6 해시태그 자동완성

`GET /api/v1/training-logs/me/tags?q=triangle`

- 인증: 필요
- Response data: `List<String>`

### 5.7 월간 캘린더 요약 조회

`GET /api/v1/training-logs/me/calendar?year=2026&month=5`

- 인증: 필요
- Response data: `TrainingLogMonthlyCalendarResponse`

### 5.8 최근 훈련 기록 조회

`GET /api/v1/training-logs/me/recent`

- 인증: 필요
- Response data: `List<TrainingLogEntryResponse>`
- 최대 10건 반환

### 5.9 이미지 업로드 URL 발급

`POST /api/v1/training-logs/me/upload-url`

- 인증: 필요
- Response data: `TrainingLogImageUploadUrlResponse`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `fileName` | `String` | O | 원본 파일명 |
| `contentType` | `String` | O | 업로드 파일 content type |

### 5.10 365일 출석 잔디 조회

`GET /api/v1/training-logs/me/attendance-grass?date=2026-05-22`

- 인증: 필요
- Response data: `TrainingLogAttendanceGrassResponse`
- 상세 계산 규칙과 응답 필드는 [training-log-insight.md](training-log-insight.md)를 따른다.

### 5.11 주간/월간 훈련 인사이트 조회

`GET /api/v1/training-logs/me/insights?period=WEEK&date=2026-05-22`

`GET /api/v1/training-logs/me/insights?period=MONTH&date=2026-05-22`

- 인증: 필요
- `period`: `WEEK` 또는 `MONTH`
- Response data: `TrainingLogInsightResponse`
- 상세 계산 규칙과 응답 필드는 [training-log-insight.md](training-log-insight.md)를 따른다.

## 6. DTO 노트

### 6.1 `TrainingLogEntryResponse`

- `checklist`는 `List<TrainingLogChecklistItem>`이다.
- `hashtags`는 정규화된 `List<String>`이다.
- `externalLinks`는 `List<TrainingLogExternalLink>`이다.
- `color`, `visibility`는 기록 메타데이터다.
- `trainingIntensity`, `gymAttendance`, `condition`은 훈련 상태 메타데이터로 함께 반환된다.
- `imageUrls`와 대표 `imageUrl`, `beltColor`, `stripeCount`, `trainingMinutes`가 함께 반환된다.
- 본인 상세 조회에서는 `likeCount`, `commentCount`, `likedByMe`, `commentableByMe`를 함께 반환한다.
- 생성/수정/최근 목록 응답에서는 소셜 메타 필드가 생략될 수 있다.

### 6.2 `TrainingLogEntrySummaryResponse`

- 날짜 선택 후 아래 카드 목록에 사용하는 요약 응답이다.
- `trainingDate`는 별도 컨텍스트에서 이미 선택되어 있으므로 포함하지 않는다.
- 현재 구현은 선택 날짜 카드에서도 `likeCount`, `commentCount`를 함께 반환한다.

### 6.3 `TrainingLogMonthlyCalendarResponse`

- `dailySummaries`는 월간 캘린더의 날짜별 표시 데이터다.
- 각 항목의 `colors`는 해당 날짜의 기록 색상 목록이다.

### 6.4 `TrainingLogAttendanceGrassResponse`, `TrainingLogInsightResponse`

- 365일 출석 잔디와 주간/월간 인사이트 응답 DTO다.
- DTO 필드와 집계 규칙의 source of truth는 [training-log-insight.md](training-log-insight.md)다.

### 6.5 `TrainingLogExternalLinkRequest`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `type` | `TrainingLogLinkType` | 링크 타입 |
| `url` | `String` | 원본 링크 URL |

### 6.6 `TrainingLogChecklistItemRequest`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `text` | `String` | 체크리스트 항목 내용 |
| `checked` | `Boolean?` | 완료 여부 |

## 7. 구현 메모

- `TrainingLogEntryRepository`는 날짜 조회, 최근 조회, 캘린더 집계, 해시태그 자동완성용 쿼리를 분리한다.
- 이미지 업로드는 `TrainingLogImageUploadService`가 담당한다.
- 외부 링크는 서비스 계층에서 도메인과 URL을 검증하고 `https://`로 정규화한다.
- 벨트 동기화는 `PROMOTION` 기록의 최신값을 기준으로 처리한다.

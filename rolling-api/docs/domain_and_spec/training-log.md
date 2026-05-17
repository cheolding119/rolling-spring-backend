# Training Log

- 개인 훈련 기록 도메인과 API 스펙을 관리한다.
- 공통 응답, 인증, 날짜/시간 포맷은 [shared/common-models.md](shared/common-models.md)를 따른다.
- 현재 구현 범위는 `training-log-product-plan.md` 기준 Phase 1~6이다.

## 1. 도메인 개요

훈련 기록은 로그인한 사용자가 특정 날짜에 카테고리 기반 기록을 남기고, 같은 날짜의 기록 목록을 조회하거나 수정/삭제하고, 자신의 해시태그를 자동완성하며, 연간 캘린더 집계와 최근 기록 목록을 조회할 수 있는 도메인이다.

현재 구현 범위:

- 특정 날짜 훈련 기록 목록 조회
- 특정 날짜 훈련 기록 생성
- 훈련 기록 수정
- 훈련 기록 삭제
- 본인 데이터 소유권 검증
- 체크리스트 JSON 저장/조회
- 해시태그 정규화, 중복 제거, 자동완성
- 연간 캘린더 요약 조회
- 최근 훈련 기록 조회
- 이미지 업로드용 presigned URL 발급
- `PROMOTION` 카테고리 전용 검증

후속 Phase 범위:

- 외부 링크 JSON 구조 및 검증
- 최신 `PROMOTION` 기준 `User.beltColor` 동기화
- DTO/Swagger 추가 정리

## 2. 도메인 모델

### 2.1 TrainingLogEntryModel

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 훈련 기록 ID |
| `trainingDate` | `Date` | 훈련 날짜 |
| `category` | `TrainingLogCategory` | 기록 카테고리 |
| `title` | `String` | 기록 제목 |
| `content` | `String` | 기록 내용 |
| `checklist` | `List<TrainingLogChecklistItem>` | 체크리스트 목록 |
| `hashtags` | `List<String>` | 정규화된 해시태그 목록 |
| `imageUrl` | `String?` | 대표 이미지 URL |
| `trainingMinutes` | `Integer?` | 훈련 시간(분) |
| `beltColor` | `BeltColor?` | `PROMOTION` 전용 벨트 색상 |
| `stripeCount` | `Integer?` | `PROMOTION` 전용 stripe 수 |
| `createdAt` | `DateTime` | 생성 시각 |
| `updatedAt` | `DateTime` | 수정 시각 |

구현 메모:

- DB에는 `checklist_json`, `hashtags_json` 문자열 컬럼으로 저장한다.
- `external_links_json` 컬럼은 존재하지만 현재 API request/response에는 포함하지 않는다.
- `imageUrl`, `beltColor`, `stripeCount`는 현재 public DTO에 노출된다.

### 2.2 TrainingLogChecklistItem

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `text` | `String` | 체크리스트 항목 내용 |
| `checked` | `Boolean` | 완료 여부 |

### 2.3 TrainingLogCalendarSummaryModel

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `year` | `Integer` | 조회 연도 |
| `totalTrainingMinutes` | `Integer` | 해당 연도의 총 훈련 시간 |
| `activeDays` | `Integer` | 기록이 존재한 일수 |
| `monthlySummaries` | `List<TrainingLogCalendarMonthlySummary>` | 월별 요약 목록 |
| `dailySummaries` | `List<TrainingLogCalendarDailySummary>` | 일별 요약 목록 |

### 2.4 TrainingLogCalendarMonthlySummary

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `month` | `Integer` | 월(1~12) |
| `totalMinutes` | `Integer` | 해당 월의 총 훈련 시간 |
| `activeDays` | `Integer` | 해당 월의 활동 일수 |

### 2.5 TrainingLogCalendarDailySummary

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `date` | `Date` | 훈련 날짜 |
| `totalMinutes` | `Integer` | 해당 일자의 총 훈련 시간 |
| `recordCount` | `Integer` | 해당 일자의 기록 수 |

### 2.6 TrainingLogImageUploadUrlModel

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `uploadUrl` | `String` | S3 업로드용 presigned URL |
| `imageKey` | `String` | 서버 저장용 S3 object key |
| `imageUrl` | `String` | 업로드 후 접근 가능한 공개 이미지 URL |
| `expiresAt` | `DateTime` | presigned URL 만료 시각 |

## 3. Enum

### 3.1 TrainingLogCategory

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

### 3.2 TrainingLogLinkType

후속 외부 링크 Phase에서 사용할 raw value는 아래 두 값으로 고정되어 있다.

| Raw value | 설명 |
| --- | --- |
| `INSTAGRAM` | 인스타그램 링크 |
| `YOUTUBE` | 유튜브 링크 |

구현 메모:

- enum과 DB 컬럼은 추가되었지만 외부 링크 request/response와 검증 로직은 아직 구현하지 않았다.

## 4. 공통 정책

### 4.1 인증과 권한

- 모든 training log API는 인증이 필요하다.
- 모든 조회/수정/삭제는 본인 데이터 범위에서만 허용한다.
- 다른 사용자의 기록 ID를 수정/삭제하려고 하면 `FORBIDDEN`을 반환한다.

### 4.2 날짜와 정렬

- `trainingDate` path variable은 `YYYY-MM-DD` 포맷을 사용한다.
- 특정 날짜 기록 목록은 `createdAt ASC`로 반환한다.
- 최근 기록 목록은 `trainingDate DESC`, 같은 날짜에서는 `createdAt DESC` 기준으로 최대 10건 반환한다.
- 생성 시 `trainingDate`는 미래 날짜일 수 없다.
- 캘린더 집계 `year`는 `2000..2100` 범위만 허용한다.

### 4.3 제목/내용/훈련 시간

- `title`, `content`는 생성 시 필수다.
- 수정 시 `title`, `content`를 보내면 trim 후 저장한다.
- `trainingMinutes`는 `0..600` 범위만 허용한다.
- 수정 시 `trainingMinutes: null`을 명시하면 기존 값을 비운다.

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

### 4.6 카테고리별 검증

- `PROMOTION`에서는 `beltColor`가 필수다.
- `PROMOTION`에서 `stripeCount`를 보내면 0 이상이어야 한다.
- `PROMOTION`이 아닌 카테고리에서 `beltColor`, `stripeCount`를 보내면 `VALIDATION_ERROR`를 반환한다.
- 현재 구현은 `PROMOTION` 전용 필드를 기록에만 저장하며, `User.beltColor` 동기화는 아직 수행하지 않는다.

### 4.7 이미지 업로드

- 업로드 API는 presigned PUT URL을 발급한다.
- 허용 파일 형식은 `jpg`, `jpeg`, `png`와 대응 content type(`image/jpeg`, `image/jpg`, `image/png`)이다.
- object key prefix는 `training/logs/images/`다.
- 응답의 `imageUrl`은 `cloud.aws.s3.public-base-url`이 있으면 그 값을, 없으면 S3 bucket URL을 사용한다.

## 5. Training Log API

### 5.1 특정 날짜 훈련 기록 목록 조회

`GET /api/v1/training-logs/me/entries/{date}`

- 인증: 필요
- Response data: `List<TrainingLogEntryModel>`

Path parameters:

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `date` | `Date` | 조회할 훈련 날짜 |

### 5.2 특정 날짜 훈련 기록 생성

`POST /api/v1/training-logs/me/entries/{date}`

- 인증: 필요
- Response data: `TrainingLogEntryModel`

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `category` | `TrainingLogCategory` | O |
| `title` | `String` | O |
| `content` | `String` | O |
| `checklist` | `List<TrainingLogChecklistItemRequest>?` | - |
| `hashtags` | `List<String>?` | - |
| `imageUrl` | `String?` | - |
| `trainingMinutes` | `Integer?` | - |
| `beltColor` | `BeltColor?` | `PROMOTION`일 때 필수 |
| `stripeCount` | `Integer?` | - |

### 5.3 훈련 기록 수정

`PATCH /api/v1/training-logs/me/entries/{id}`

- 인증: 필요
- Response data: `TrainingLogEntryModel`

Request body:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `category` | `TrainingLogCategory?` | 전달 시 카테고리 변경 |
| `title` | `String?` | 전달 시 trim 후 반영 |
| `content` | `String?` | 전달 시 trim 후 반영 |
| `checklist` | `List<TrainingLogChecklistItemRequest>?` | `[]` 또는 `null`이면 비움 |
| `hashtags` | `List<String>?` | `[]` 또는 `null`이면 비움 |
| `imageUrl` | `String?` | `null`이면 비움 |
| `trainingMinutes` | `Integer?` | `null`이면 비움 |
| `beltColor` | `BeltColor?` | `PROMOTION` 전용, `null`이면 비움 |
| `stripeCount` | `Integer?` | `PROMOTION` 전용, `null`이면 비움 |

구현 메모:

- 현재 구현에서는 `trainingDate` 수정은 지원하지 않는다.

### 5.4 훈련 기록 삭제

`DELETE /api/v1/training-logs/me/entries/{id}`

- 인증: 필요
- Response data: `null`
- 삭제 방식: hard delete

### 5.5 해시태그 자동완성

`GET /api/v1/training-logs/me/tags?q=triangle`

- 인증: 필요
- Response data: `List<String>`

### 5.6 연간 캘린더 집계 조회

`GET /api/v1/training-logs/me/calendar?year=2026`

- 인증: 필요
- Response data: `TrainingLogCalendarSummaryModel`

Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `year` | `Integer` | O | 조회 연도 |

### 5.7 최근 훈련 기록 조회

`GET /api/v1/training-logs/me/recent`

- 인증: 필요
- Response data: `List<TrainingLogEntryModel>`
- 최대 10건 반환

### 5.8 이미지 업로드 URL 발급

`POST /api/v1/training-logs/me/upload-url`

- 인증: 필요
- Response data: `TrainingLogImageUploadUrlModel`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `fileName` | `String` | O | 원본 파일명 |
| `contentType` | `String` | O | 업로드 파일 content type |

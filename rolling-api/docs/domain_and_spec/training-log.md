# Training Log

- 개인 훈련 기록 도메인 모델과 API 스펙을 관리한다.
- 공통 응답, 인증, 날짜/시간 형식은 [shared/common-models.md](shared/common-models.md)를 따른다.
- 현재 구현 상태는 `training-log-product-plan.md` 기준 Phase 1~3이다.

## 1. 도메인 개요

훈련 기록은 로그인 사용자가 특정 날짜에 카테고리 기반 개인 훈련 기록을 남기고, 같은 날짜의 기록 목록을 조회하거나 수정/삭제하며, 본인 기록에 저장된 해시태그를 자동완성으로 재사용하는 도메인이다.

현재 구현 범위:

- 특정 날짜 훈련 기록 목록 조회
- 특정 날짜 훈련 기록 생성
- 훈련 기록 수정
- 훈련 기록 삭제
- 본인 데이터 소유권 검증
- 체크리스트 JSON 저장/조회
- 해시태그 정규화, 중복 제거, 자동완성

후속 Phase 범위:

- 월간/연간 집계
- 최근 기록 조회
- 이미지 업로드 URL 발급
- 외부 링크 저장/검증
- `PROMOTION` 전용 검증과 `User.beltColor` 동기화

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
| `trainingMinutes` | `Integer?` | 훈련 시간(분) |
| `createdAt` | `DateTime` | 생성 시각 |
| `updatedAt` | `DateTime` | 수정 시각 |

### 2.2 TrainingLogChecklistItem

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `text` | `String` | 체크리스트 항목 내용 |
| `checked` | `Boolean` | 완료 여부 |

현재 구현 메모:

- 체크리스트는 DB에 `checklist_json` 문자열로 저장한다.
- 해시태그는 DB에 `hashtags_json` 문자열로 저장한다.
- `imageUrl`, `externalLinksJson`, `beltColor`, `stripeCount` 컬럼은 Phase 1에서 추가됐지만 현재 API request/response에는 아직 노출하지 않는다.

## 3. Enum

### 3.1 TrainingLogCategory

| Raw value | 의미 |
| --- | --- |
| `TECHNIQUE` | 기술 기록 |
| `SPARRING` | 스파링 기록 |
| `TOURNAMENT` | 대회 기록 |
| `PROMOTION` | 승급 기록 |
| `OPEN_MAT` | 오픈매트 기록 |
| `DRILL` | 드릴 기록 |
| `PERSONAL_TRAINING` | 개인 훈련 기록 |

현재 구현 메모:

- API와 DB 모두 enum raw value를 그대로 사용한다.
- JPA 저장 방식은 `EnumType.STRING`이다.

### 3.2 TrainingLogLinkType

후속 외부 링크 Phase에서 사용할 raw value는 아래 두 값으로 고정되어 있다.

| Raw value | 의미 |
| --- | --- |
| `INSTAGRAM` | 인스타그램 링크 |
| `YOUTUBE` | 유튜브 링크 |

현재 구현 메모:

- enum과 DB 컬럼은 추가됐지만, 외부 링크 request/response와 검증 로직은 아직 구현하지 않았다.

## 4. 공통 정책

### 4.1 인증과 권한

- 현재 구현된 training log API는 모두 인증이 필요하다.
- 모든 기록 조회/수정/삭제는 본인 데이터 범위에서만 허용한다.
- 다른 사용자의 기록 ID를 수정/삭제하려고 하면 `FORBIDDEN`을 반환한다.

### 4.2 날짜와 정렬

- `trainingDate` path variable은 `YYYY-MM-DD` 형식을 사용한다.
- 특정 날짜 기록 목록은 `createdAt` 오름차순으로 반환한다.
- 생성 시 `trainingDate`는 미래 날짜일 수 없다.

### 4.3 제목/내용/훈련 시간

- `title`, `content`는 생성 시 필수다.
- 수정 시 `title`, `content`를 보내면 trim 후 저장한다.
- `trainingMinutes`는 `0..600` 범위만 허용한다.
- 수정 시 `trainingMinutes: null`을 명시하면 기존 값을 비운다.

### 4.4 체크리스트

- 체크리스트는 최대 20개까지 허용한다.
- 각 항목의 `text`는 필수이며 trim 후 저장한다.
- 각 항목의 `checked`가 없으면 `false`로 저장한다.
- 수정 시 `checklist: []` 또는 `checklist: null`을 보내면 기존 체크리스트를 비운다.

### 4.5 해시태그

- 해시태그는 최대 10개까지 허용한다.
- trim 후 소문자로 정규화한다.
- 앞에 붙은 `#`는 제거한다.
- 중복은 제거하고 입력 순서를 유지한다.
- 허용 문자는 한글, 영문 소문자, 숫자, 하이픈(`-`)이다.
- 자동완성 검색은 본인 데이터 기준으로만 수행한다.
- 자동완성은 최근 기록에 저장된 해시태그 순서를 우선 유지하며 최대 20개까지 반환한다.

## 5.9 Training Log API

### 5.9.1 특정 날짜 훈련 기록 목록 조회

`GET /api/v1/training-logs/me/entries/{date}`

- 인증: 필요
- Response: `List<TrainingLogEntryModel>`

Path parameters:

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `date` | `Date` | 조회할 훈련 날짜 |

현재 구현 메모:

- 본인 기록만 조회한다.
- 데이터가 없으면 빈 배열을 반환한다.

### 5.9.2 특정 날짜 훈련 기록 생성

`POST /api/v1/training-logs/me/entries/{date}`

- 인증: 필요
- Response: `TrainingLogEntryModel`

Path parameters:

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| `date` | `Date` | 생성할 훈련 날짜 |

Request body:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `category` | `TrainingLogCategory` | O |
| `title` | `String` | O |
| `content` | `String` | O |
| `checklist` | `List<TrainingLogChecklistItemRequest>?` | - |
| `hashtags` | `List<String>?` | - |
| `trainingMinutes` | `Integer?` | - |

검증:

- 미래 날짜는 저장할 수 없다.
- `title`, `content`는 blank일 수 없다.
- `trainingMinutes`는 `0..600`이어야 한다.
- 체크리스트는 최대 20개다.
- 해시태그는 정규화 후 최대 10개다.

### 5.9.3 훈련 기록 수정

`PATCH /api/v1/training-logs/me/entries/{id}`

- 인증: 필요
- Response: `TrainingLogEntryModel`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `category` | `TrainingLogCategory?` | - | 전달 시 카테고리 변경 |
| `title` | `String?` | - | 전달 시 trim 후 반영 |
| `content` | `String?` | - | 전달 시 trim 후 반영 |
| `checklist` | `List<TrainingLogChecklistItemRequest>?` | - | `[]` 또는 `null`이면 비움 |
| `hashtags` | `List<String>?` | - | `[]` 또는 `null`이면 비움 |
| `trainingMinutes` | `Integer?` | - | `null`이면 비움 |

현재 구현 메모:

- 현재 구현에서는 `trainingDate` 수정은 지원하지 않는다.
- 다른 사용자의 기록은 수정할 수 없다.

### 5.9.4 훈련 기록 삭제

`DELETE /api/v1/training-logs/me/entries/{id}`

- 인증: 필요
- Response data: `null`

현재 구현 메모:

- hard delete 방식이다.
- 다른 사용자의 기록은 삭제할 수 없다.

### 5.9.5 해시태그 자동완성

`GET /api/v1/training-logs/me/tags?q=triangle`

- 인증: 필요
- Response data: `List<String>`

Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `q` | `String` | O | 자동완성 검색어 |

현재 구현 메모:

- 검색어도 해시태그와 같은 방식으로 trim, 소문자화, `#` 제거를 적용한다.
- 본인 기록에서 찾은 해시태그만 반환한다.
- 부분 일치 기준으로 검색한다.

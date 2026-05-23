# Training Log Insight

- 훈련일지 데이터를 기반으로 365일 출석 잔디와 주간/월간 훈련 인사이트를 제공하는 도메인이다.
- 원본 기록 데이터의 source of truth는 [../training-log.md](../training-log.md)다.
- 공통 응답, 인증, 날짜 형식은 [../shared/common-models.md](../shared/common-models.md)를 따른다.
- 이 문서는 인사이트 집계 모델, 계산 정책, API 계약의 기준을 관리한다.

## 1. 도메인 개요

Training Log Insight는 현재 로그인한 사용자의 `TrainingLogEntry`를 읽어서 출석과 훈련 성과를 조회용 데이터로 계산한다. 별도 쓰기 모델은 만들지 않으며, MVP에서는 조회 시 service 계층에서 기간 데이터를 집계한다.

현재 구현 범위:

- 최근 365일 출석 잔디 조회
- GitHub 스타일 잔디 렌더링을 위한 일별 출석 데이터 제공
- 기준 날짜가 포함된 주간 인사이트 조회
- 기준 날짜가 포함된 월간 인사이트 조회
- 기간 요약 지표 계산
- 기간 내 모든 날짜의 일별 통계 계산
- 카테고리별 기록 수와 훈련 시간 계산
- 상위 해시태그 5개 계산
- 강도와 컨디션 평균 계산
- 체크리스트 완료율 계산

## 2. 도메인 모델

### 2.1 기준 원본 데이터

| 원본 필드 | 타입 | 인사이트 활용 |
| --- | --- | --- |
| `trainingDate` | `LocalDate` | 기간 필터, 날짜 칸 |
| `category` | `TrainingLogCategory` | 일별 카테고리, 카테고리 분포 |
| `trainingMinutes` | `Integer?` | 총 훈련 시간, 잔디 level |
| `trainingIntensity` | `Integer?` | 평균 훈련 강도 |
| `gymAttendance` | `Boolean?` | 출석 여부 |
| `condition` | `Integer?` | 평균 컨디션 |
| `checklistJson` | `String?` | 체크리스트 완료율 |
| `hashtagsJson` | `String?` | 상위 해시태그 |

### 2.2 `TrainingLogAttendanceGrassResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `startDate` | `LocalDate` | 365일 범위 시작일 |
| `endDate` | `LocalDate` | 기준 날짜 |
| `totalDays` | `Integer` | 항상 365 |
| `attendanceDays` | `Integer` | 365일 내 출석일 수 |
| `currentStreakDays` | `Integer` | `endDate`부터 이어진 연속 출석일 수 |
| `longestStreakDays` | `Integer` | 365일 범위 내 최장 연속 출석일 수 |
| `recent30DaysAttendanceDays` | `Integer` | 최근 30일 출석일 수 |
| `days` | `List<TrainingLogAttendanceGrassDay>` | 날짜별 잔디 데이터 |

### 2.3 `TrainingLogAttendanceGrassDay`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `date` | `LocalDate` | 날짜 |
| `dayOfWeek` | `String` | `MON`~`SUN` |
| `attended` | `Boolean` | 출석 여부 |
| `level` | `Integer` | 잔디 농도 0~3 |
| `recordCount` | `Integer` | 해당 날짜 기록 수 |
| `totalTrainingMinutes` | `Integer` | 해당 날짜 총 훈련 시간 |
| `averageTrainingIntensity` | `Double?` | 해당 날짜 평균 강도 |
| `averageCondition` | `Double?` | 해당 날짜 평균 컨디션 |
| `categories` | `List<TrainingLogCategory>` | 해당 날짜 카테고리 목록 |

### 2.4 `TrainingLogInsightResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `period` | `TrainingLogInsightPeriod` | `WEEK` 또는 `MONTH` |
| `startDate` | `LocalDate` | 기간 시작일 |
| `endDate` | `LocalDate` | 기간 종료일 |
| `summary` | `TrainingLogInsightSummary` | 기간 요약 |
| `dailyStats` | `List<TrainingLogDailyInsight>` | 날짜별 인사이트 |
| `categoryBreakdown` | `List<TrainingLogCategoryInsight>` | 카테고리 분포 |
| `topHashtags` | `List<TrainingLogHashtagInsight>` | 상위 해시태그 |

### 2.5 `TrainingLogInsightSummary`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `recordCount` | `Integer` | 기간 내 기록 수 |
| `trainingDays` | `Integer` | 기록이 1개 이상 있는 날짜 수 |
| `attendanceDays` | `Integer` | 출석일 수 |
| `attendanceRate` | `Double` | 기간 전체 일수 대비 출석률 |
| `totalTrainingMinutes` | `Integer` | 기간 내 총 훈련 시간 |
| `averageTrainingMinutesPerAttendanceDay` | `Double?` | 출석일 기준 평균 훈련 시간 |
| `averageTrainingIntensity` | `Double?` | 평균 훈련 강도 |
| `averageCondition` | `Double?` | 평균 컨디션 |
| `checklistCompletionRate` | `Double?` | 체크리스트 완료율 |

### 2.6 `TrainingLogDailyInsight`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `date` | `LocalDate` | 날짜 |
| `recordCount` | `Integer` | 해당 날짜 기록 수 |
| `gymAttendance` | `Boolean` | 해당 날짜 출석 여부 |
| `totalTrainingMinutes` | `Integer` | 해당 날짜 총 훈련 시간 |
| `averageTrainingIntensity` | `Double?` | 해당 날짜 평균 강도 |
| `averageCondition` | `Double?` | 해당 날짜 평균 컨디션 |
| `categories` | `List<TrainingLogCategory>` | 해당 날짜 카테고리 목록 |

### 2.7 `TrainingLogCategoryInsight`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `category` | `TrainingLogCategory` | 기록 카테고리 |
| `recordCount` | `Integer` | 해당 카테고리 기록 수 |
| `totalTrainingMinutes` | `Integer` | 해당 카테고리 총 훈련 시간 |

### 2.8 `TrainingLogHashtagInsight`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `tag` | `String` | 정규화된 해시태그 |
| `count` | `Integer` | 기간 내 등장 횟수 |

## 3. Enum

### 3.1 `TrainingLogInsightPeriod`

| Raw value | 설명 |
| --- | --- |
| `WEEK` | 기준 날짜가 포함된 월요일~일요일 |
| `MONTH` | 기준 날짜가 포함된 월의 1일~말일 |

구현 메모:

- API query parameter와 응답의 `period`는 enum raw value를 그대로 사용한다.
- 허용되지 않는 값은 `VALIDATION_ERROR`로 처리한다.

## 4. 공통 정책

### 4.1 인증과 권한

- 모든 Training Log Insight API는 인증이 필요하다.
- 현재 로그인한 사용자의 훈련일지 데이터만 조회한다.
- 사용자 없음 또는 탈퇴 사용자는 `NOT_FOUND`로 처리한다.

### 4.2 날짜와 기간

- `date` query parameter는 `YYYY-MM-DD` 형식이다.
- 기준 날짜가 없으면 서버의 `Asia/Seoul` 오늘 날짜를 사용한다.
- 365일 출석 잔디의 `endDate`는 기준 날짜다.
- 365일 출석 잔디의 `startDate`는 `endDate.minusDays(364)`다.
- `WEEK`는 기준 날짜가 포함된 월요일부터 일요일까지다.
- `MONTH`는 기준 날짜가 포함된 월의 1일부터 말일까지다.

### 4.3 출석과 잔디

- 같은 날짜에 `gymAttendance == true`인 기록이 하나 이상 있으면 해당 날짜는 출석일이다.
- 같은 날짜에 기록은 있지만 `gymAttendance == true`가 하나도 없으면 출석일이 아니다.
- 출석일 계산은 기록 수가 아니라 날짜 수 기준이다.
- 응답은 기간 내 모든 날짜를 빠짐없이 포함한다.
- 기록이 없는 날짜는 `attended=false`, `gymAttendance=false`, `recordCount=0`, `totalTrainingMinutes=0`으로 반환한다.

잔디 level:

| level | 조건 |
| --- | --- |
| `0` | 출석 없음 |
| `1` | 출석했고 훈련 시간 미입력 또는 1~59분 |
| `2` | 출석했고 60~119분 |
| `3` | 출석했고 120분 이상 |

### 4.4 GitHub 스타일 잔디 레이아웃

- 한 칸은 하루를 의미한다.
- row는 요일, column은 주 단위를 의미한다.
- 최신 날짜가 포함된 주가 가장 오른쪽에 온다.
- 요일 row 순서는 `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`, `SUN`을 기본값으로 한다.
- 365일을 주 단위 그리드로 배치하면 최대 53개 column이 필요하다.
- 365일 범위 밖의 앞쪽/뒤쪽 빈 칸은 API가 별도 날짜로 내려주지 않는다. 프론트에서 placeholder로 렌더링한다.

### 4.5 평균과 합계

- `trainingMinutes == null`인 기록은 훈련 시간 합계에서 제외한다.
- 강도와 컨디션 평균은 null 값을 제외하고 계산한다.
- 평균값 계산 대상이 없으면 `null`을 반환한다.
- 비율과 평균은 소수점 첫째 자리까지 반올림한다.
- `averageTrainingMinutesPerAttendanceDay`는 `totalTrainingMinutes / attendanceDays`로 계산하며, 출석일이 없으면 `null`이다.
- `checklistCompletionRate`는 완료 checklist 수 / 전체 checklist 수 * 100으로 계산하며, checklist가 없으면 `null`이다.

### 4.6 카테고리와 해시태그

- 일별 `categories`는 해당 날짜의 기록 카테고리를 중복 제거 후 기록 조회 순서 기준으로 반환한다.
- `categoryBreakdown`은 기록이 있는 카테고리만 반환한다.
- `categoryBreakdown.totalTrainingMinutes`는 null 제외 합계다.
- `topHashtags`는 기간 내 등장 횟수 내림차순으로 최대 5개를 반환한다.
- 해시태그 등장 횟수가 같으면 먼저 집계된 태그를 우선한다.
- 같은 기록에 같은 태그가 중복 저장되지 않는다는 [../training-log.md](../training-log.md)의 정규화 규칙을 신뢰한다.

## 5. Training Log Insight API

### 5.1 365일 출석 잔디 조회

`GET /api/v1/training-logs/me/attendance-grass?date=2026-05-22`

- 인증: 필요
- Response data: `TrainingLogAttendanceGrassResponse`

Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `date` | `LocalDate` | - | 기준 날짜. 없으면 서버의 `Asia/Seoul` 오늘 날짜 |

에러:

- `UNAUTHORIZED`
- `NOT_FOUND`
- `VALIDATION_ERROR`

### 5.2 주간/월간 인사이트 조회

`GET /api/v1/training-logs/me/insights?period=WEEK&date=2026-05-22`

`GET /api/v1/training-logs/me/insights?period=MONTH&date=2026-05-22`

- 인증: 필요
- Response data: `TrainingLogInsightResponse`

Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `period` | `TrainingLogInsightPeriod` | O | `WEEK` 또는 `MONTH` |
| `date` | `LocalDate` | - | 기준 날짜. 없으면 서버의 `Asia/Seoul` 오늘 날짜 |

에러:

- `UNAUTHORIZED`
- `NOT_FOUND`
- `VALIDATION_ERROR`

## 6. DTO 노트

- `TrainingLogAttendanceGrassResponse.days`는 항상 365개 날짜 데이터를 가진다.
- `TrainingLogAttendanceGrassDay.dayOfWeek`는 앱의 잔디 row 배치를 위한 보조 필드다.
- `TrainingLogInsightResponse.dailyStats`는 기간 내 모든 날짜를 포함한다.
- `TrainingLogInsightSummary.attendanceRate`는 기간 전체 일수 기준이다.
- `TrainingLogInsightSummary.trainingDays`와 `attendanceDays`는 서로 다를 수 있다.
- `TrainingLogDailyInsight.gymAttendance`는 일별 출석 여부이며, 원본 기록의 `gymAttendance` 중 하나라도 true면 true다.
- 응답 enum은 `TrainingLogCategory`, `TrainingLogInsightPeriod` raw value를 그대로 사용한다.

## 7. 구현 메모

- `TrainingLogEntryRepository.findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc` 조회를 재사용한다.
- MVP에서는 조회 범위의 `TrainingLogEntry`를 service 계층에서 메모리 집계한다.
- 별도 DB 테이블은 MVP 범위에서 만들지 않는다.
- 연간 리포트, 다년 집계, 사용자 증가로 응답 지연이 생기면 projection query 또는 집계 테이블을 검토한다.

## 8. 테스트 기준

- `attendance-grass`는 항상 `days.size == 365`를 반환해야 한다.
- 기준 날짜가 포함된 최근 365일의 `startDate`, `endDate`가 정확해야 한다.
- `gymAttendance == true`가 하나라도 있는 날짜는 출석일이어야 한다.
- 훈련 시간 기준 level 0~3 계산을 검증한다.
- 현재 streak와 최장 streak를 검증한다.
- `WEEK`는 월요일~일요일 범위로 계산한다.
- `MONTH`는 월 1일~말일 범위로 계산한다.
- null 강도/컨디션/훈련 시간은 평균과 합계 계산에서 깨지지 않아야 한다.
- checklist가 없으면 `checklistCompletionRate`는 `null`이어야 한다.
- 허용되지 않는 `period`와 날짜 파싱 실패는 `VALIDATION_ERROR`여야 한다.


# Training Log Insight API

- 365일 출석 잔디와 주간/월간 훈련 인사이트 API 계약을 관리한다.
- 도메인 계산 규칙은 [domain.md](domain.md)를 따른다.
- 원본 훈련 기록 계약은 [../training-log.md](../training-log.md)를 따른다.
- 공통 응답 형식과 에러 형식은 [../shared/common-models.md](../shared/common-models.md)를 따른다.

## 1. 공통 규칙

- Base path: `/api/v1/training-logs/me`
- 인증: 모든 API는 필요하다.
- 본인 훈련일지 데이터만 조회한다.
- 날짜 형식은 `YYYY-MM-DD`다.
- 날짜 계산 기준 시간대는 `Asia/Seoul`이다.
- 기준 날짜가 없으면 서버의 `Asia/Seoul` 오늘 날짜를 사용한다.

## 2. 365일 출석 잔디 조회

`GET /api/v1/training-logs/me/attendance-grass?date=2026-05-22`

- 인증: 필요
- 용도: GitHub 스타일 잔디 UI에 필요한 최근 365일 출석 데이터를 조회한다.
- Response data: `TrainingLogAttendanceGrassResponse`

Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `date` | `LocalDate` | - | 기준 날짜. 없으면 서버의 `Asia/Seoul` 오늘 날짜 |

Response data:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `startDate` | `LocalDate` | `date.minusDays(364)` |
| `endDate` | `LocalDate` | 기준 날짜 |
| `totalDays` | `Integer` | 항상 365 |
| `attendanceDays` | `Integer` | `attended=true`인 날짜 수 |
| `currentStreakDays` | `Integer` | `endDate`부터 역순으로 이어진 출석일 수 |
| `longestStreakDays` | `Integer` | 365일 범위 내 최장 연속 출석일 수 |
| `recent30DaysAttendanceDays` | `Integer` | `endDate` 포함 최근 30일 출석일 수 |
| `days` | `List<TrainingLogAttendanceGrassDay>` | `startDate`부터 `endDate`까지 365개 날짜 데이터 |

`TrainingLogAttendanceGrassDay`:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `date` | `LocalDate` | 잔디 칸 날짜 |
| `dayOfWeek` | `String` | `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`, `SUN` |
| `attended` | `Boolean` | 해당 날짜에 `gymAttendance == true`인 기록이 하나 이상 있는지 |
| `level` | `Integer` | 잔디 농도 0~3 |
| `recordCount` | `Integer` | 해당 날짜 기록 수 |
| `totalTrainingMinutes` | `Integer` | null 제외 훈련 시간 합계. 없으면 0 |
| `averageTrainingIntensity` | `Double?` | null 제외 평균. 대상 없으면 null |
| `averageCondition` | `Double?` | null 제외 평균. 대상 없으면 null |
| `categories` | `List<TrainingLogCategory>` | 해당 날짜 기록 카테고리 중복 제거 목록 |

Level rules:

| level | 조건 |
| --- | --- |
| `0` | `attended=false` |
| `1` | `attended=true`이고 훈련 시간 미입력 또는 1~59분 |
| `2` | `attended=true`이고 60~119분 |
| `3` | `attended=true`이고 120분 이상 |

Response example:

```json
{
  "startDate": "2025-05-23",
  "endDate": "2026-05-22",
  "totalDays": 365,
  "attendanceDays": 128,
  "currentStreakDays": 3,
  "longestStreakDays": 14,
  "recent30DaysAttendanceDays": 12,
  "days": [
    {
      "date": "2025-05-23",
      "dayOfWeek": "FRI",
      "attended": false,
      "level": 0,
      "recordCount": 0,
      "totalTrainingMinutes": 0,
      "averageTrainingIntensity": null,
      "averageCondition": null,
      "categories": []
    },
    {
      "date": "2026-05-22",
      "dayOfWeek": "FRI",
      "attended": true,
      "level": 2,
      "recordCount": 1,
      "totalTrainingMinutes": 90,
      "averageTrainingIntensity": 3.0,
      "averageCondition": 4.0,
      "categories": ["TECHNIQUE"]
    }
  ]
}
```

에러:

- `UNAUTHORIZED`
- `NOT_FOUND`
- `VALIDATION_ERROR`

## 3. 주간/월간 인사이트 조회

`GET /api/v1/training-logs/me/insights?period=WEEK&date=2026-05-22`

`GET /api/v1/training-logs/me/insights?period=MONTH&date=2026-05-22`

- 인증: 필요
- 용도: 기준 날짜가 포함된 주간 또는 월간 훈련 성과 요약을 조회한다.
- Response data: `TrainingLogInsightResponse`

Query parameters:

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `period` | `TrainingLogInsightPeriod` | O | `WEEK` 또는 `MONTH` |
| `date` | `LocalDate` | - | 기준 날짜. 없으면 서버의 `Asia/Seoul` 오늘 날짜 |

Period rules:

| period | startDate | endDate |
| --- | --- | --- |
| `WEEK` | 기준 날짜가 포함된 주의 월요일 | 기준 날짜가 포함된 주의 일요일 |
| `MONTH` | 기준 날짜가 포함된 월의 1일 | 기준 날짜가 포함된 월의 말일 |

Response data:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `period` | `TrainingLogInsightPeriod` | `WEEK` 또는 `MONTH` |
| `startDate` | `LocalDate` | 기간 시작일 |
| `endDate` | `LocalDate` | 기간 종료일 |
| `summary` | `TrainingLogInsightSummary` | 기간 요약 |
| `dailyStats` | `List<TrainingLogDailyInsight>` | 기간 내 모든 날짜의 일별 통계 |
| `categoryBreakdown` | `List<TrainingLogCategoryInsight>` | 카테고리별 기록 수와 훈련 시간 |
| `topHashtags` | `List<TrainingLogHashtagInsight>` | 상위 해시태그 최대 5개 |

`TrainingLogInsightSummary`:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `recordCount` | `Integer` | 기간 내 기록 수 |
| `trainingDays` | `Integer` | 기록이 1개 이상 있는 날짜 수 |
| `attendanceDays` | `Integer` | `gymAttendance == true`인 날짜 수 |
| `attendanceRate` | `Double` | `attendanceDays / 기간 전체 일수 * 100` |
| `totalTrainingMinutes` | `Integer` | null 제외 훈련 시간 합계 |
| `averageTrainingMinutesPerAttendanceDay` | `Double?` | 출석일 기준 평균 훈련 시간. 출석일 없으면 null |
| `averageTrainingIntensity` | `Double?` | null 제외 평균 강도 |
| `averageCondition` | `Double?` | null 제외 평균 컨디션 |
| `checklistCompletionRate` | `Double?` | 완료 checklist 수 / 전체 checklist 수 * 100. checklist 없으면 null |

`TrainingLogDailyInsight`:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `date` | `LocalDate` | 날짜 |
| `recordCount` | `Integer` | 해당 날짜 기록 수 |
| `gymAttendance` | `Boolean` | 해당 날짜 출석 여부 |
| `totalTrainingMinutes` | `Integer` | 해당 날짜 총 훈련 시간 |
| `averageTrainingIntensity` | `Double?` | 해당 날짜 평균 강도 |
| `averageCondition` | `Double?` | 해당 날짜 평균 컨디션 |
| `categories` | `List<TrainingLogCategory>` | 해당 날짜 카테고리 목록 |

`TrainingLogCategoryInsight`:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `category` | `TrainingLogCategory` | 카테고리 |
| `recordCount` | `Integer` | 해당 카테고리 기록 수 |
| `totalTrainingMinutes` | `Integer` | 해당 카테고리 null 제외 훈련 시간 합계 |

`TrainingLogHashtagInsight`:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `tag` | `String` | 정규화된 해시태그 |
| `count` | `Integer` | 기간 내 등장 횟수 |

Response example:

```json
{
  "period": "WEEK",
  "startDate": "2026-05-18",
  "endDate": "2026-05-24",
  "summary": {
    "recordCount": 7,
    "trainingDays": 4,
    "attendanceDays": 4,
    "attendanceRate": 57.1,
    "totalTrainingMinutes": 360,
    "averageTrainingMinutesPerAttendanceDay": 90.0,
    "averageTrainingIntensity": 3.8,
    "averageCondition": 3.2,
    "checklistCompletionRate": 66.7
  },
  "dailyStats": [
    {
      "date": "2026-05-18",
      "recordCount": 2,
      "gymAttendance": true,
      "totalTrainingMinutes": 120,
      "averageTrainingIntensity": 4.0,
      "averageCondition": 3.0,
      "categories": ["TECHNIQUE", "SPARRING"]
    }
  ],
  "categoryBreakdown": [
    {
      "category": "TECHNIQUE",
      "recordCount": 3,
      "totalTrainingMinutes": 180
    }
  ],
  "topHashtags": [
    {
      "tag": "guard-pass",
      "count": 4
    }
  ]
}
```

에러:

- `UNAUTHORIZED`
- `NOT_FOUND`
- `VALIDATION_ERROR`

## 4. 테스트 기준

- `attendance-grass`는 항상 `days` 365개를 반환한다.
- 기록이 없는 날짜의 `level`은 0이다.
- `gymAttendance == true`이고 훈련 시간이 null이면 `level`은 1이다.
- 기준 날짜가 2026-05-22면 `startDate`는 2025-05-23이다.
- `WEEK`는 월요일~일요일 범위다.
- `MONTH`는 월 1일~말일 범위다.
- null 강도/컨디션은 평균에서 제외한다.
- checklist가 없으면 `checklistCompletionRate`는 null이다.
- 허용되지 않는 `period`와 날짜 파싱 실패는 `VALIDATION_ERROR`다.


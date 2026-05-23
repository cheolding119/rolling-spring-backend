# 훈련 성과 인사이트 기획서

## 1. 문서 목적

- 현재 훈련일지 데이터로 365일 출석 잔디와 주간/월간 그래프를 제공하기 위한 제품 방향을 정리한다.
- 목표는 사용자가 "이번 주/이번 달에 얼마나 꾸준히, 어떤 방식으로 훈련했는지"를 빠르게 이해하게 만드는 것이다.
- 이번 문서는 제품 방향을 정의하며, 세부 도메인 규칙과 API 계약은 `docs/domain_and_spec/training-log-insight.md`에 둔다.

## 2. 사용자 문제

### 2.1 해결하려는 문제

- 사용자는 훈련 기록을 남겨도 시간이 지나면 성장 흐름을 체감하기 어렵다.
- 기록 목록만으로는 훈련 빈도, 훈련량, 컨디션, 강도 변화, 카테고리 편향을 한눈에 보기 어렵다.
- 주짓수는 성과가 천천히 누적되므로 "이번 기간에 내가 무엇을 쌓았는지"를 보여주는 피드백 루프가 필요하다.

### 2.2 사용자 결과

- 사용자는 최근 365일 출석 흐름과 주간/월간 훈련 패턴을 10초 안에 파악할 수 있다.
- 사용자는 훈련량과 컨디션/강도의 관계를 보고 다음 훈련 계획을 조정할 수 있다.
- 사용자는 특정 카테고리에 치우쳤는지 확인하고 부족한 훈련 영역을 보완할 수 있다.

## 3. 현재 사용 가능한 데이터

`TrainingLogEntry` 기준으로 바로 활용 가능한 필드는 다음과 같다.

| 데이터 | 필드 | 활용 |
| --- | --- | --- |
| 훈련 날짜 | `trainingDate` | 주간/월간 그룹핑, 연속 출석 계산 |
| 카테고리 | `category` | 기술/스파링/대회/승급 등 훈련 분포 |
| 훈련 시간 | `trainingMinutes` | 총 훈련량, 일별/주별 훈련 시간 |
| 훈련 강도 | `trainingIntensity` | 평균 강도, 고강도 훈련일 표시 |
| 체육관 출석 | `gymAttendance` | 출석일 수, 출석률, 연속 출석 |
| 컨디션 | `condition` | 평균 컨디션, 강도 대비 컨디션 변화 |
| 체크리스트 | `checklistJson` | 완료율, 실천 항목 수 |
| 해시태그 | `hashtagsJson` | 자주 반복한 기술/주제 |
| 색상 | `color` | 캘린더/차트 보조 표시 |

데이터 해석 기준:

- `gymAttendance == true`인 기록이 있는 날짜는 체육관 출석일로 계산한다.
- `trainingMinutes`가 `null`인 기록은 총 훈련 시간 계산에서 제외한다.
- `trainingIntensity`, `condition`이 `null`인 기록은 평균 계산에서 제외한다.
- 하루에 기록이 여러 개 있으면 기록 단위 지표와 날짜 단위 지표를 구분한다.

## 4. 제품 권장안

### 4.1 지금 출시할 범위

MVP는 "365일 출석 잔디 + 주간/월간 요약 대시보드"로 시작한다.

포함 기능:

| 영역 | 기능 | 권장 UI |
| --- | --- | --- |
| 기간 선택 | 이번 주, 지난 주, 이번 달, 지난 달 | segmented control |
| 핵심 요약 | 출석일, 총 훈련 시간, 기록 수, 평균 강도, 평균 컨디션 | 숫자 카드 4~5개 |
| 훈련량 추이 | 날짜별 훈련 시간 또는 기록 수 | bar chart |
| 출석 흐름 | 최근 365일 출석 여부 | GitHub 스타일 365일 잔디심기 |
| 카테고리 분포 | 카테고리별 기록 수 또는 훈련 시간 | donut chart 또는 stacked bar |
| 강도/컨디션 | 날짜별 평균 강도와 평균 컨디션 | line chart |
| 많이 한 주제 | 상위 해시태그 5개 | tag list |

MVP에서 제외:

- AI 코칭 문장 자동 생성
- 벨트별 성장 점수
- 다른 사용자와 비교
- 공개 랭킹
- 복잡한 운동 부하 모델

### 4.2 다음 출시 범위

- 최근 4주 비교: 이번 주와 이전 3주 훈련량 비교
- 카테고리별 목표 설정: 예를 들어 스파링 주 2회 목표
- 컨디션 낮은 날의 고강도 훈련 경고
- 체크리스트 완료율 추이
- 월간 리포트 공유 이미지 생성

### 4.3 나중에 검토할 범위

- 사용자의 벨트, 체급, 목표 기반 개인화 분석
- 대회 준비 모드
- 훈련 파트너/코치 피드백 연동
- 자동 추천 훈련 계획

## 5. 화면 구성 제안

### 5.1 365일 출석 잔디

상단:

- 제목: `훈련 출석 잔디`
- 요약 문장: "최근 365일 중 128일 출석"
- 보조 지표: 현재 연속 출석, 최장 연속 출석, 최근 30일 출석일

잔디:

- GitHub contribution graph처럼 최근 365일을 작은 사각형 칸으로 표시한다.
- 열은 주 단위, 행은 요일 단위로 배치한다.
- 가장 오른쪽 열이 가장 최근 주가 되도록 한다.
- 날짜가 없는 앞쪽 여백 칸은 렌더링하지 않거나 비활성 placeholder로 둔다.

상호작용:

- 잔디 칸을 누르면 해당 날짜의 기록 목록 또는 상세 진입점을 보여준다.
- 툴팁에는 날짜, 출석 여부, 총 훈련 시간, 기록 수, 평균 강도, 평균 컨디션을 표시한다.

### 5.2 주간 인사이트

상단:

- 기간: `2026.05.18 - 2026.05.24`
- 요약 문장: "이번 주 4일 출석, 총 360분 훈련"

핵심 카드:

- 출석일: `4일`
- 총 훈련 시간: `360분`
- 기록 수: `7개`
- 평균 강도: `3.8 / 5`
- 평균 컨디션: `3.2 / 5`

차트:

- 7일 bar chart: 일별 훈련 시간
- line chart: 일별 평균 강도와 컨디션
- category bar: 카테고리별 기록 수

하단:

- 상위 해시태그: `guard-pass`, `triangle`, `sparring`
- 부족한 영역 힌트: "이번 주 드릴 기록이 없습니다"

### 5.3 월간 인사이트

상단:

- 월 선택: `2026년 5월`
- 요약 문장: "이번 달 13일 출석, 총 1,180분 훈련"

핵심 카드:

- 출석일: `13일`
- 출석률: `42%`
- 총 훈련 시간: `1,180분`
- 평균 강도: `3.5 / 5`
- 평균 컨디션: `3.4 / 5`

차트:

- weekly stacked bar: 주차별 카테고리 훈련 시간
- donut chart: 카테고리 비중
- line chart: 주차별 평균 강도/컨디션

하단:

- 가장 많이 기록한 카테고리
- 가장 많이 반복한 해시태그
- 지난 달 대비 증감은 다음 출시 범위로 둔다.

### 5.4 출석 잔디심기 UI

출석은 GitHub contribution graph처럼 작은 사각형 칸을 날짜별로 배치하는 365일 잔디심기 형태를 권장한다.

표현 기준:

- 한 칸은 하루를 의미한다.
- 기본 조회 범위는 기준 날짜 포함 최근 365일이다.
- `gymAttendance == true`인 날짜만 잔디가 심어진 날로 표시한다.
- `gymAttendance == false`이거나 기록이 없는 날짜는 비활성 칸으로 표시한다.
- 색상 농도는 우선 `trainingMinutes` 기준을 권장한다.
- `trainingMinutes`가 없지만 `gymAttendance == true`이면 가장 낮은 활성 단계로 표시한다.

권장 intensity 단계:

| 단계 | 조건 | UI 의미 |
| --- | --- | --- |
| `0` | 출석 없음 | 빈 칸 |
| `1` | 출석, 훈련 시간 미입력 또는 1~59분 | 연한 잔디 |
| `2` | 60~119분 | 보통 잔디 |
| `3` | 120분 이상 | 진한 잔디 |

툴팁 또는 날짜 상세:

- 날짜
- 출석 여부
- 총 훈련 시간
- 기록 수
- 평균 강도
- 평균 컨디션
- 주요 카테고리

레이아웃 기준:

- 365일을 주 단위 column, 요일 단위 row로 배치한다.
- 요일 row 순서는 `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`, `SUN`을 권장한다.
- 기준 날짜가 포함된 주가 가장 오른쪽에 오도록 한다.
- 365일 범위 밖의 빈 칸은 클릭 불가 placeholder로 처리한다.
- 모바일에서는 가로 스크롤을 허용하고 최신 날짜가 먼저 보이게 오른쪽 끝으로 초기 스크롤한다.

## 6. 지표 정의

### 6.1 핵심 지표

| 지표 | 계산 |
| --- | --- |
| `recordCount` | 기간 내 기록 수 |
| `attendanceDays` | `gymAttendance == true`인 기록이 1개 이상 있는 날짜 수 |
| `trainingDays` | 기록이 1개 이상 있는 날짜 수 |
| `totalTrainingMinutes` | `trainingMinutes` 합계 |
| `averageTrainingMinutesPerAttendanceDay` | `totalTrainingMinutes / attendanceDays` |
| `averageTrainingIntensity` | null 제외 `trainingIntensity` 평균 |
| `averageCondition` | null 제외 `condition` 평균 |
| `categoryBreakdown` | 카테고리별 기록 수와 훈련 시간 |
| `topHashtags` | 기간 내 해시태그 빈도 상위 N개 |

### 6.2 파생 지표

| 지표 | 계산 |
| --- | --- |
| `attendanceRate` | `attendanceDays / 기간 전체 일수` |
| `highIntensityDays` | 일 평균 `trainingIntensity >= 4`인 날짜 수 |
| `lowConditionHighIntensityDays` | 일 평균 `condition <= 2`이고 `trainingIntensity >= 4`인 날짜 수 |
| `checklistCompletionRate` | 완료 checklist 수 / 전체 checklist 수 |
| `categoryBalanceScore` | 카테고리 쏠림을 0~100으로 표현, MVP에서는 제외 권장 |

## 7. API 설계 방향

### 7.1 권장 엔드포인트

출석 잔디는 365일 전용 API로 분리하고, 주간/월간 인사이트는 하나의 period API로 처리한다.

```http
GET /api/v1/training-logs/me/attendance-grass?date=2026-05-22
GET /api/v1/training-logs/me/insights?period=WEEK&date=2026-05-22
GET /api/v1/training-logs/me/insights?period=MONTH&date=2026-05-22
```

요청 규칙:

- `attendance-grass.date`: 기준 날짜. 기준 날짜를 포함한 최근 365일을 반환한다.
- `period`: `WEEK`, `MONTH`
- `date`: 기준 날짜
- `WEEK`는 `date`가 포함된 월요일~일요일 기준을 권장한다.
- `MONTH`는 `date`가 포함된 월의 1일~말일 기준이다.
- 시간대 기준은 `Asia/Seoul`이다.

### 7.2 출석 잔디 응답 초안

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

### 7.3 주간/월간 인사이트 응답 초안

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

### 7.4 DTO 후보

- `TrainingLogAttendanceGrassResponse`
- `TrainingLogAttendanceGrassDay`
- `TrainingLogInsightPeriod`
- `TrainingLogInsightResponse`
- `TrainingLogInsightSummary`
- `TrainingLogDailyInsight`
- `TrainingLogCategoryInsight`
- `TrainingLogHashtagInsight`

## 8. 백엔드 구현 방향

### 8.1 조회 방식

- MVP는 기간 내 `TrainingLogEntry`를 한 번 조회한 뒤 service 계층에서 집계한다.
- 출석 잔디는 최대 365일, 주간/월간 범위는 최대 31일이므로 초기에는 별도 집계 테이블이 필요 없다.
- 이후 사용자가 많아지거나 연간 리포트가 필요해지면 projection query 또는 집계 테이블을 검토한다.

### 8.2 Repository 후보

```java
List<TrainingLogEntry> findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
        Long userId,
        LocalDate startDate,
        LocalDate endDateExclusive
);
```

현재 월간 캘린더에서 유사한 조회가 이미 있으므로 재사용 가능하다.

### 8.3 계산 주의사항

- 하루에 기록이 여러 개면 `dailyStats`에서 합계와 평균을 계산한다.
- `gymAttendance`는 같은 날짜에 하나라도 `true`가 있으면 true로 본다.
- `attendance-grass` 응답은 최근 365일의 모든 날짜를 빠짐없이 포함한다. 기록이 없는 날짜도 `attended=false`, `level=0`으로 반환한다.
- `days.level`은 출석 여부와 일별 총 훈련 시간으로 계산한다.
- `condition`, `trainingIntensity` 평균은 null을 제외한다.
- `trainingMinutes`는 null을 0으로 합산하지 말고 제외하거나 0 처리 정책을 명확히 한다. MVP 권장은 null 제외다.
- `attendanceRate`는 기간 전체 일수 기준이다. 주간은 7일, 월간은 월 일수다.

## 9. 프론트엔드 구현 방향

### 9.1 차트 우선순위

1. 365일 출석 잔디심기 grass chart
2. 일별 훈련 시간 bar chart
3. 강도/컨디션 line chart
4. 카테고리 분포 donut 또는 stacked bar
5. 상위 해시태그 list

### 9.2 UX 원칙

- 사용자가 기록을 적게 남긴 경우에도 빈 화면이 아니라 "기록을 쌓으면 보이는 항목"을 안정적으로 보여준다.
- 평균값은 데이터가 없으면 `null`로 받고 UI에서 `-` 또는 "기록 없음"으로 표시한다.
- 출석 잔디는 데이터가 없는 날짜도 빈 칸으로 렌더링해 최근 365일의 전체 리듬을 보여준다.
- 출석 잔디는 주간/월간 필터와 별개로 항상 최근 365일을 기본 노출한다.
- 강도와 컨디션은 같은 1~5 스케일이므로 같은 그래프에 두 선으로 표시할 수 있다.
- 훈련 시간은 분 단위로 받고, UI에서 `1시간 30분`처럼 변환한다.

## 10. 성공 신호

제품 성공 신호:

- 훈련일지 작성 후 인사이트 화면 진입률 증가
- 주간/월간 인사이트 화면 재방문율 증가
- `trainingMinutes`, `trainingIntensity`, `condition`, `gymAttendance` 입력률 증가
- 기록 작성 사용자 중 2주 이상 연속 기록 사용자 비율 증가

기능 품질 신호:

- 기간별 집계 결과가 상세 기록 합계와 일치한다.
- null 데이터가 있어도 API와 UI가 깨지지 않는다.
- 주간/월간 전환이 빠르게 응답한다.

## 11. 수용 기준

### 11.1 백엔드

- 사용자는 본인 훈련일지 기준으로만 인사이트를 조회할 수 있다.
- 출석 잔디 API는 기준 날짜 포함 최근 365일 데이터를 반환한다.
- 출석 잔디 API는 기록이 없는 날짜도 `attended=false`, `level=0`으로 포함한다.
- `period=WEEK`는 기준 날짜가 포함된 월요일~일요일 데이터를 반환한다.
- `period=MONTH`는 기준 날짜가 포함된 월 전체 데이터를 반환한다.
- 응답에는 summary, dailyStats, categoryBreakdown, topHashtags가 포함된다.
- `condition`, `trainingIntensity` 평균은 null 값을 제외하고 계산한다.
- `gymAttendance`는 일자별 true 여부와 기간 합산 출석일 수로 제공된다.

### 11.2 프론트엔드

- 사용자는 주간/월간을 전환할 수 있다.
- 사용자는 기본 화면에서 최근 365일 출석 잔디를 볼 수 있다.
- 사용자는 출석일, 총 훈련 시간, 평균 강도, 평균 컨디션을 볼 수 있다.
- 사용자는 잔디 칸을 눌러 날짜별 훈련 기록으로 진입할 수 있다.
- 사용자는 날짜별 훈련량과 카테고리 분포를 차트로 볼 수 있다.
- 데이터가 없는 기간에는 빈 상태 화면을 보여준다.

## 12. 리스크와 완화

| 리스크 | 영향 | 완화 |
| --- | --- | --- |
| 사용자가 필드를 입력하지 않음 | 평균 강도/컨디션 차트가 빈약함 | 입력 UI를 간단한 slider/stepper로 만들고 선택 입력으로 유지 |
| 지표가 너무 많아 복잡함 | 사용자가 핵심을 놓침 | MVP는 핵심 카드 5개와 차트 3개 이하로 제한 |
| 기록 수와 출석일이 혼동됨 | 사용자 해석 오류 | `기록 수`와 `출석일`을 명확히 분리 |
| 잔디 색상 기준이 모호함 | 사용자 해석 오류 | MVP는 훈련 시간 기준 level 0~3으로 고정 |
| 365일 집계 성능 저하 | 응답 지연 | 초기에는 사용자 1명, 365일 범위 조회로 시작하고 필요 시 projection 또는 집계 테이블 검토 |
| 성과라는 표현이 부담스러움 | 기록 동기 저하 | "성과 점수"보다 "훈련 흐름", "이번 기간 요약" 표현 사용 |

## 13. 미해결 의사결정

| 질문 | 선택지 | 권장 | 결정 주체 |
| --- | --- | --- | --- |
| 주간 기준 | 월~일 / 일~토 | 월~일 | 제품 |
| 출석일 계산 | `gymAttendance` 기준 / 기록 존재 기준 | `gymAttendance == true` 기준 | 제품 |
| 잔디 농도 기준 | 훈련 시간 / 기록 수 / 강도 | 훈련 시간 | 제품+프론트 |
| 잔디 조회 범위 | 최근 365일 / 올해 1월~오늘 | 최근 365일 | 제품 |
| 훈련 시간 null 처리 | 0 처리 / 평균 제외 | 합계에서는 제외, UI에서는 미입력 표시 | 제품+백엔드 |
| 카테고리 분포 기준 | 기록 수 / 훈련 시간 | 둘 다 제공, UI 기본은 기록 수 | 제품 |
| 성과 문구 | 점수화 / 요약형 | 요약형 | 제품 |
| 첫 출시 차트 수 | 3개 / 5개 이상 | 3개 | 제품+프론트 |

## 14. 권장 출시 순서

### Phase 1. 집계 API MVP

- 365일 attendance-grass API 추가
- 기간별 summary 계산
- dailyStats 계산
- categoryBreakdown 계산
- topHashtags 계산
- service/controller 테스트 추가

### Phase 2. 인사이트 화면 MVP

- 주간/월간 전환
- 365일 출석 잔디심기 chart
- 핵심 요약 카드
- 일별 훈련 시간 chart
- 강도/컨디션 line chart
- 카테고리 분포 chart

### Phase 3. 학습과 개선

- 입력률과 화면 재방문율 확인
- 어떤 지표가 실제로 많이 보는지 확인
- 최근 4주 비교와 월간 리포트로 확장 여부 결정

## 15. 권장 결론

- 지금은 복잡한 성장 점수보다 "출석, 훈련 시간, 강도, 컨디션, 카테고리 분포"를 명확히 보여주는 것이 좋다.
- 현재 데이터만으로도 365일 출석 잔디와 주간/월간 인사이트 MVP는 충분히 만들 수 있다.
- 백엔드는 별도 집계 테이블 없이 기간 조회 후 service 집계로 시작하고, 프론트는 핵심 카드와 3개 차트 중심으로 출시하는 것을 권장한다.

## 16. 실행 체크리스트

### Phase 0. 계약 확정

- [x] 365일 출석 잔디를 GitHub contribution graph 방식으로 정의한다.
- [x] 잔디 조회 범위를 기준 날짜 포함 최근 365일로 확정한다.
- [x] 잔디 한 칸을 하루로 정의하고, 최대 53주 column과 7개 요일 row로 렌더링한다.
- [x] 출석 기준을 `gymAttendance == true`로 확정한다.
- [x] 잔디 농도 기준을 훈련 시간 기반 `level 0~3`으로 확정한다.
- [x] 도메인 규칙과 API 계약 문서를 `domain_and_spec/training-log-insight.md`에 작성한다.

### Phase 1. 백엔드 DTO와 기간 계산

- [x] `TrainingLogAttendanceGrassResponse` DTO를 추가한다.
- [x] `TrainingLogAttendanceGrassDay` DTO를 추가한다.
- [x] `TrainingLogInsightPeriod` enum을 추가한다.
- [x] `TrainingLogInsightResponse` DTO를 추가한다.
- [x] `TrainingLogInsightSummary` DTO를 추가한다.
- [x] `TrainingLogDailyInsight` DTO를 추가한다.
- [x] `TrainingLogCategoryInsight` DTO를 추가한다.
- [x] `TrainingLogHashtagInsight` DTO를 추가한다.
- [x] 365일 `startDate`, `endDate` 계산 유틸을 구현한다.
- [x] `WEEK` 월요일~일요일 기간 계산을 구현한다.
- [x] `MONTH` 월 1일~말일 기간 계산을 구현한다.

### Phase 2. 365일 출석 잔디 API

- [x] `GET /api/v1/training-logs/me/attendance-grass` controller endpoint를 추가한다.
- [x] 기준 날짜가 없으면 `Asia/Seoul` 오늘 날짜를 사용한다.
- [x] 최근 365일의 모든 날짜를 빠짐없이 반환한다.
- [x] 기록이 없는 날짜는 `attended=false`, `level=0`으로 반환한다.
- [x] 같은 날짜에 `gymAttendance == true`인 기록이 하나라도 있으면 `attended=true`로 계산한다.
- [x] 훈련 시간 기준으로 `level 0~3`을 계산한다.
- [x] `attendanceDays`, `currentStreakDays`, `longestStreakDays`, `recent30DaysAttendanceDays`를 계산한다.
- [x] service 테스트로 365일 개수, 시작/종료 날짜, level, streak를 검증한다.
- [x] controller 테스트로 인증과 응답 JSON 구조를 검증한다.

### Phase 3. 주간/월간 인사이트 API

- [x] `GET /api/v1/training-logs/me/insights` controller endpoint를 추가한다.
- [x] `period=WEEK`와 `period=MONTH`를 지원한다.
- [x] `summary` 지표를 계산한다.
- [x] `dailyStats`를 기간 내 모든 날짜 기준으로 계산한다.
- [x] `categoryBreakdown`을 카테고리별 기록 수와 훈련 시간 기준으로 계산한다.
- [x] `topHashtags` 상위 5개를 계산한다.
- [x] null 강도/컨디션/훈련 시간 처리 기준을 테스트한다.
- [x] 잘못된 `period`와 날짜 파싱 실패 응답을 검증한다.

### Phase 4. 문서 동기화

- [x] 구현 후 `training-log-insight.md`의 계산 규칙과 실제 코드를 대조한다.
- [x] 구현 후 `training-log-insight.md`의 request/response 예시를 실제 DTO와 대조한다.
- [x] 필요한 경우 `training-log.md`의 현재 구현 범위에 인사이트 API를 연결한다.
- [x] `AGENTS.md` 문서 맵에 `training-log-insight` 문서를 추가할지 검토한다.

### Phase 5. 프론트 연동 준비

- [ ] 365일 잔디의 요일 row 순서와 placeholder 처리 기준을 프론트에 전달한다.
- [ ] 모바일에서 최신 날짜가 보이도록 초기 스크롤 기준을 정한다.
- [ ] 잔디 칸 클릭 시 날짜별 기록 목록으로 이동하는 UX를 확정한다.
- [ ] level별 색상 토큰은 프론트 디자인 시스템에서 확정한다.
- [ ] 데이터가 없는 사용자에게 보여줄 빈 상태 문구를 확정한다.

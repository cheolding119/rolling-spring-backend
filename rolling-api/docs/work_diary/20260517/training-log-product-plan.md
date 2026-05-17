# 훈련 일지 기능 기획안

## 1. 문서 목적

- 주짓수 개인 훈련 기록 기능을 현재 `rolling-api` 구조에 맞춰 어떤 방식으로 설계할지 초안을 정리한다.
- 범위는 개인용 BJJ Log & Tracker MVP에 한정한다.
- 이번 문서는 이번 작업에서 실제로 포함할 범위만 다룬다.
- 후속 범위와 제외 범위는 별도 문서 `training-log-next-phase.md`로 분리한다.

## 2. 제품 목표

### 2.1 사용자 문제

- 주짓수는 기술이 많고 성장 속도가 느려서, 수련 내용을 남기지 않으면 복기와 축적이 어렵다.
- 지금 프로젝트는 오픈매트, 세미나, 커뮤니티 중심이라 "매일 다시 들어오게 만드는 개인 루틴"이 약하다.
- 사용자는 커뮤니티 글보다 더 자주, 더 가볍게 입력할 수 있는 개인 기록 공간이 필요하다.

### 2.2 성공 신호

- 주간 기준 훈련 일지 작성 사용자 수가 증가한다.
- 기록 작성 후 7일 내 재방문 비율이 기존 대비 상승한다.
- 기록 사용자 중 월간/연간 누적 시간 화면 재조회 비율이 높게 나온다.
- 카테고리별 기록 작성 비율과 재방문율이 상승한다.

## 3. 설계 원칙

### 3.1 단일 모델 원칙

- `일일 기록`과 `성장 이벤트`를 분리하지 않고, 카테고리 기반 단일 기록 모델로 통합한다.
- 사용자는 날짜를 고르고, 카테고리를 선택한 뒤, 같은 폼에서 제목/내용/체크리스트/해시태그를 작성한다.
- 기록의 기본 단위는 "특정 날짜에 남기는 하나의 훈련 기록"이다.

### 3.2 기존 자산 재사용

- 인증/권한은 기존 JWT `UserPrincipal` 기반 구조를 그대로 사용한다.
- 공통 시간 필드는 `BaseTimeEntity`를 그대로 사용한다.
- 이미지 업로드는 커뮤니티/대회 포스터와 동일한 presigned URL 패턴을 재사용한다.
- 외부 링크는 명시적 필드로 저장하고, 이번 범위에서는 인스타그램/유튜브만 허용한다.
- 현재 `User.beltColor`는 유지하되, `PROMOTION(승급)` 카테고리 기록의 최신 기록과 정합성을 맞춘다.

### 3.3 MVP 원칙

- MVP는 "캘린더에서 날짜를 고르고, 바로 기록을 남긴다"는 흐름에 집중한다.
- 공통 기록 구조는 `카테고리 -> 제목 -> 내용 -> 체크리스트 -> 해시태그`를 중심으로 한다.
- 이미지와 외부 링크는 선택 입력이다.
- 월간/연간 누적 집계를 위해 기록에 시간 정보는 유지한다.
- 공유, 공개 프로필 노출, 좋아요, 댓글, 관리자 운영 화면은 이번 범위에서 제외한다.

## 4. 핵심 사용자 흐름

### 4.1 기록 작성 진입

1. 사용자가 앱내의 캘린더에서 특정 날짜를 선택한다.
2. 사용자가 플로팅 버튼을 누른다.
3. 시스템은 작성 가능한 카테고리 목록을 보여준다.
4. 사용자가 카테고리를 선택하면 해당 날짜 기준의 작성 화면으로 진입한다.

### 4.2 단일 기록 작성

1. 사용자가 카테고리를 선택한다.
2. 사용자는 제목과 내용을 입력한다.
3. 사용자는 진행해야 할 내용 또는 수행한 항목을 체크리스트로 작성한다.
4. 사용자는 해시태그를 입력한다.
5. 필요하면 이미지를 추가한다.
6. 필요하면 인스타그램 또는 유튜브 링크를 추가한다.
7. 카테고리가 `PROMOTION(승급)`이면 벨트와 그랄 정보를 함께 입력한다.
8. 저장 후 해당 날짜 기록은 수정 가능 상태로 유지된다.

### 4.3 월간, 연간 누적 확인

1. 사용자가 "이번달, 올해 매트 위 시간" 화면에 진입한다.
2. 백엔드는 연도 기준 일자별 합계와 총합, 월별 합계를 반환한다.
3. 프론트는 이를 잔디 심기, 월별 합계, 총 시간 카드로 시각화한다.

## 5. 이번 작업 범위

### 5.1 이번 작업에 포함할 MVP 범위

| 영역 | 기능 | 포함 기준 |
| --- | --- | --- |
| 진입 | 캘린더 날짜 선택 | 날짜 기준 작성/조회 진입점 |
| 진입 | 플로팅 버튼 기반 작성 진입 | 작성 액션의 대표 진입점 |
| 진입 | 카테고리 선택 | 7개 카테고리 지원 |
| 기록 | 단일 훈련 기록 생성/수정 | 날짜 기준 저장, 수정 가능 |
| 기록 | 단일 훈련 기록 조회 | 날짜별 목록 또는 상세 조회 |
| 기록 | 단일 훈련 기록 삭제 | 본인 기록만 삭제 |
| 기록 | 제목/내용 저장 | 공통 필수 입력 |
| 기록 | 체크리스트 저장 | 공통 입력 |
| 기록 | 해시태그 저장 | 공통 입력 |
| 기록 | 이미지 업로드 URL 발급 및 조회 | 대표 이미지 1장 기준 |
| 기록 | 외부 링크 저장 및 조회 | `INSTAGRAM`, `YOUTUBE` 지원 |
| 집계 | 월간/연간 누적 시간 조회 | 일자별 총합, 연간 총합, 월별 총합 |
| 탐색 | 최근 기록 조회 | 기술훈련 페이지 케밥 버튼 진입용 |
| 프로필 | 현재 벨트 동기화 | 최신 `PROMOTION(승급)` 기록 기준 `User.beltColor` 동기화 |

### 5.2 카테고리 정의

- `TECHNIQUE (기술)`: 배운 기술, 성공한 기술, 복기할 기술 기록
- `SPARRING (스파링)`: 스파링 중심 회차나 스파링 복기 기록
- `TOURNAMENT (대회)`: 대회 출전, 결과, 준비 과정 기록
- `PROMOTION (승급)`: 벨트나 그랄이 올라간 기록
- `OPEN_MAT (오픈매트)`: 오픈매트 참여 및 회고 기록
- `DRILL (드릴)`: 반복 드릴 훈련 중심 기록
- `PERSONAL_TRAINING (개인훈련)`: 개인적으로 진행한 보강 훈련, 운동, 솔로 훈련 기록

### 5.3 이번 작업에서 제외한 후속 범위

- 후속 범위와 백로그는 별도 문서 [training-log-next-phase.md](C:/rolling/rolling-spring-backend/rolling-api/docs/work_diary/20260517/training-log-next-phase.md:1)에서 관리한다.

## 6. 도메인 모델 초안

### 6.1 `TrainingLogEntry`

카테고리 기반 단일 훈련 기록 모델이다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 기록 ID |
| `user` | `User` | 소유 사용자 |
| `trainingDate` | `LocalDate` | 기록 대상 날짜 |
| `category` | `TrainingLogCategory` | 기록 카테고리 |
| `title` | `String` | 기록 제목 |
| `content` | `String` | 기록 본문 |
| `checklistJson` | `String?` | 체크리스트 항목과 완료 여부 저장 |
| `hashtagsJson` | `String?` | 해시태그 목록 |
| `imageUrl` | `String?` | 대표 이미지 URL |
| `externalLinksJson` | `String?` | 인스타그램/유튜브 링크 목록 |
| `trainingMinutes` | `Integer?` | 누적 집계용 시간(분) |
| `beltColor` | `BeltColor?` | `PROMOTION(승급)` 전용 |
| `stripeCount` | `Integer?` | `PROMOTION(승급)` 전용 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

권장 enum:

```java
public enum TrainingLogCategory {
    TECHNIQUE,
    SPARRING,
    TOURNAMENT,
    PROMOTION,
    OPEN_MAT,
    DRILL,
    PERSONAL_TRAINING
}
```

설계 원칙:

- 이번 구조에서는 별도 `GrowthEvent` 테이블을 두지 않는다.
- 모든 기록은 하나의 `TrainingLogEntry`에 저장한다.
- 카테고리에 따라 일부 필드만 선택적으로 사용한다.
- `PROMOTION(승급)`만 `beltColor`, `stripeCount`를 사용한다.
- 누적 집계를 위해 `trainingMinutes`는 유지한다.

권장 외부 링크 enum:

```java
public enum TrainingLogLinkType {
    INSTAGRAM,
    YOUTUBE
}
```

권장 외부 링크 JSON 구조:

```json
[
  {
    "type": "INSTAGRAM",
    "url": "https://www.instagram.com/p/xxxx/"
  },
  {
    "type": "YOUTUBE",
    "url": "https://youtu.be/xxxx"
  }
]
```

## 7. API 설계 방향

### 7.1 컨트롤러 구조 권장

- 새 컨트롤러 `TrainingLogController`
- 경로는 개인 기능임을 명확히 드러내는 `/api/v1/training-logs/me/*` 형태를 권장

### 7.2 권장 API 목록

| 목적 | Method | Path |
| --- | --- | --- |
| 특정 날짜 기록 목록 조회 | `GET` | `/api/v1/training-logs/me/entries/{date}` |
| 특정 날짜 기록 생성 | `POST` | `/api/v1/training-logs/me/entries/{date}` |
| 특정 기록 수정 | `PATCH` | `/api/v1/training-logs/me/entries/{id}` |
| 특정 기록 삭제 | `DELETE` | `/api/v1/training-logs/me/entries/{id}` |
| 월간/연간 캘린더 집계 조회 | `GET` | `/api/v1/training-logs/me/calendar?year=2026` |
| 최근 기록 목록 조회 | `GET` | `/api/v1/training-logs/me/recent` |
| 태그 자동완성 조회 | `GET` | `/api/v1/training-logs/me/tags?q=triangle` |
| 이미지 업로드 URL 발급 | `POST` | `/api/v1/training-logs/me/upload-url` |

### 7.3 핵심 응답 방향

월간/연간 집계 응답은 프론트가 바로 잔디 심기를 그릴 수 있게 일자별 총합과 월별 요약을 포함한다.

예시 필드:

- `year`
- `totalTrainingMinutes`
- `activeDays`
- `monthlySummaries[]`
  - `month`
  - `totalMinutes`
  - `activeDays`
- `dailySummaries[]`
  - `date`
  - `totalMinutes`
  - `recordCount`

기록 응답은 공통 필드와 카테고리별 선택 필드를 함께 반환한다.

## 8. 정책 및 검증

### 8.1 권한

- 모든 API는 로그인 필수다.
- 기록은 본인 데이터만 접근 가능하다.

### 8.2 기록 저장 규칙

- `date`는 미래 날짜 저장 불가를 권장한다.
- `category`, `title`, `content`는 필수다.
- `title`, `content`는 trim 후 저장한다.
- 체크리스트는 최대 20개를 권장한다.
- 해시태그는 최대 10개를 권장한다.
- `trainingMinutes`는 0 이상 600 이하를 권장한다.

### 8.3 해시태그 규칙

- 해시태그는 trim, 소문자 정규화, 중복 제거 후 저장한다.
- 특수문자 남용은 막고 한글/영문/숫자/하이픈 정도만 허용하는 편이 안전하다.
- 본인 데이터 기준으로만 검색한다.

### 8.4 외부 링크 규칙

- 이번 범위에서 허용하는 링크 타입은 `INSTAGRAM`, `YOUTUBE` 두 가지다.
- 허용 도메인:
  - `instagram.com`
  - `www.instagram.com`
  - `youtube.com`
  - `www.youtube.com`
  - `youtu.be`
- 저장 전 `https://` 기준으로 정규화한다.
- 기록당 외부 링크는 최대 3개를 권장한다.
- 미리보기 썸네일, 제목 수집, oEmbed 연동은 이번 범위에서 제외한다.

### 8.5 카테고리별 규칙

- `PROMOTION(승급)`
  - `beltColor` 필수
  - `stripeCount` 선택 또는 필수
  - 최신 기록이면 `User.beltColor`와 동기화
- `TECHNIQUE(기술)`
  - 일반 공통 필드만 사용
- `SPARRING(스파링)`
  - 일반 공통 필드만 사용
- `TOURNAMENT(대회)`
  - 일반 공통 필드만 사용
- `OPEN_MAT(오픈매트)`
  - 일반 공통 필드만 사용
- `DRILL(드릴)`
  - 일반 공통 필드만 사용
- `PERSONAL_TRAINING(개인훈련)`
  - 일반 공통 필드만 사용

추가 원칙:

- 전 카테고리 공통으로 `title`, `content`는 필수다.
- 전 카테고리 공통으로 체크리스트, 해시태그, 이미지, 외부 링크는 선택 가능하다.
- `PROMOTION(승급)` 외 카테고리는 `User.beltColor`를 변경하지 않는다.

## 9. 현재 프로젝트와의 연결 포인트

### 9.1 `User`와의 관계

- 현재 `User`는 `beltColor`만 가진다.
- 최신 `PROMOTION(승급)` 기록의 `beltColor`를 `User.beltColor`에 반영한다.
- `stripeCount`는 `User`에 직접 넣지 않고 기록에서 관리한다.

### 9.2 이미지 업로드

- 새 서비스 `TrainingLogImageUploadService`
- S3 key prefix 예시: `training/logs/images/`
- 구현 방식은 `CommunityPostImageUploadService`와 동일한 presigned URL 패턴 재사용

### 9.3 외부 링크 저장

- 초기 구현은 `TrainingLogEntry.externalLinksJson`으로 시작하는 편이 빠르다.
- 링크 타입과 URL만 저장하고, 외부 메타데이터 수집은 하지 않는다.
- 링크 클릭 이동은 프론트 책임으로 두고, 백엔드는 검증된 URL만 반환한다.

### 9.4 DB 마이그레이션

포함 권장 테이블:

- `training_log_entries`
- 필요 시 `training_log_tags`를 별도 테이블로 유지하거나 `hashtagsJson`으로 통합 검토

## 10. 구현 순서 권장

1. Flyway 테이블 추가와 JPA 엔티티/리포지토리 정의
2. `TrainingLogCategory` enum 계약 확정
3. 특정 날짜 기록 생성/조회/수정/삭제 API 구현
4. 체크리스트 JSON 직렬화/역직렬화 구현
5. 해시태그 정규화와 자동완성 API 구현
6. 월간/연간 집계 API 구현
7. 최근 기록 조회 API 구현
8. 이미지 업로드 URL 발급 구현
9. 외부 링크 검증 및 저장 구조 구현
10. `PROMOTION(승급)` 기준 `User.beltColor` 동기화 처리

## 11. Phase별 진행 체크리스트

### Phase 0. 기획 확정 및 계약 정리

- [x] 단일 기록 모델로 간다는 방향 확정
- [x] `TrainingLogCategory` raw value 확정 - "TECHNIQUE" 이런식으로 지정 
- [x] 공통 입력 구조를 `카테고리 -> 제목 -> 내용 -> 체크리스트 -> 해시태그`로 확정"카테고리 필수 선택"
- [x] `PROMOTION(승급)`에서만 `User.beltColor`를 동기화한다는 정책 확정 
- [x] 외부 링크 허용 도메인 정책 확정
- [x] 체크리스트/해시태그/외부 링크 JSON 구조 확정
- [x] Flutter와 주고받을 request/response 필드 목록 확정

### Phase 1. DB 마이그레이션 및 엔티티 기초

- [x] `training_log_entries` 테이블 추가
- [x] `category` 컬럼 추가
- [x] `title`, `content` 컬럼 추가
- [x] `checklist_json` 컬럼 추가
- [x] `hashtags_json` 컬럼 추가
- [x] `image_url` 컬럼 추가
- [x] `external_links_json` 컬럼 추가
- [x] `training_minutes` 컬럼 추가
- [x] `belt_color`, `stripe_count` 컬럼 추가
- [x] `TrainingLogEntry` 엔티티 추가 또는 정비
- [x] `TrainingLogCategory` enum 추가
- [x] `TrainingLogLinkType` enum 추가
- [x] `TrainingLogEntryRepository` 추가

### Phase 2. 단일 기록 CRUD

- [x] 특정 날짜 기록 목록 조회 API 구현
- [x] 특정 날짜 기록 생성 API 구현
- [x] 특정 기록 수정 API 구현
- [x] 특정 기록 삭제 API 구현
- [x] 본인 데이터만 접근 가능하도록 권한 처리
- [x] 미래 날짜 저장 제한 적용
- [x] `title`, `content` 필수 검증 적용
- [x] `trainingMinutes` 범위 검증 적용

### Phase 3. 체크리스트 및 해시태그

- [x] 체크리스트 JSON 직렬화 로직 구현
- [x] 체크리스트 최대 개수 제한 적용
- [x] 해시태그 정규화 로직 구현
- [x] 해시태그 중복 제거 처리
- [x] 해시태그 최대 개수 제한 적용
- [x] 해시태그 자동완성 API 구현

### Phase 4. 월간/연간 집계

- [ ] 연간 기준 일자별 합계 조회 쿼리 구현
- [ ] 월별 합계 집계 쿼리 구현
- [ ] 총 훈련 시간 계산 구현
- [ ] activeDays 계산 구현
- [ ] 월간/연간 캘린더 집계 API 구현
- [ ] 최근 기록 목록 조회 API 구현

### Phase 5. 카테고리별 검증

- [ ] `PROMOTION(승급)`에서 `beltColor` 필수 검증 적용
- [ ] `PROMOTION(승급)`에서 `stripeCount` 검증 적용
- [ ] 나머지 6개 카테고리 공통 검증 적용
- [ ] 카테고리별 예외 메시지 정리

### Phase 6. 이미지 업로드

- [ ] 이미지 업로드 요청 DTO 추가
- [ ] 이미지 업로드 응답 DTO 추가
- [ ] `TrainingLogImageUploadService` 구현
- [ ] presigned URL 발급 API 구현
- [ ] 허용 파일 형식 검증 적용

### Phase 7. 외부 링크 처리

- [ ] 외부 링크 JSON 구조 구현
- [ ] `INSTAGRAM`, `YOUTUBE` 타입 검증 적용
- [ ] 허용 도메인 검증 적용
- [ ] `https://` 정규화 처리
- [ ] 기록당 외부 링크 최대 개수 제한 적용
- [ ] 응답에 외부 링크 배열 포함

### Phase 8. 승급 동기화

- [ ] 최신 `PROMOTION(승급)` 기록 판단 로직 구현
- [ ] `User.beltColor` 동기화 로직 구현
- [ ] `PROMOTION(승급)` 수정 시 동기화 재계산 처리
- [ ] `PROMOTION(승급)` 삭제 시 최신 벨트 재산정 처리

### Phase 9. API 응답 정리 및 검증

- [ ] API 응답 DTO 정리
- [ ] Swagger 스키마 정리
- [ ] validation 실패 응답 케이스 정리
- [ ] 권한 실패 응답 케이스 정리
- [ ] not found 응답 케이스 정리
- [ ] 카테고리 enum raw value 문서화

### Phase 10. 테스트 및 연동 마무리

- [ ] 단일 기록 CRUD 서비스 테스트
- [ ] 해시태그 정규화 테스트
- [ ] 월간/연간 집계 서비스 테스트
- [ ] 최근 기록 조회 테스트
- [ ] 외부 링크 검증 테스트
- [ ] `PROMOTION(승급)` 벨트 동기화 테스트
- [ ] 컨트롤러 레이어 API 테스트
- [ ] Flutter 연동용 필드 최종 점검

## 12. 수용 기준 초안

### 12.1 기록

- 사용자는 오늘 날짜 또는 과거 날짜의 훈련 기록을 생성할 수 있다.
- 사용자는 기록에 카테고리, 제목, 내용, 체크리스트, 해시태그를 저장할 수 있다.
- 사용자는 이미지와 외부 링크를 선택적으로 저장할 수 있다.
- 사용자는 월간/연간 총 훈련 시간과 일자별 훈련 분포를 조회할 수 있다.
- 사용자는 최근 기록과 올해 누적 시간을 확인할 수 있다.

### 12.2 승급

- 사용자가 `PROMOTION(승급)` 카테고리 기록을 저장하면 벨트/그랄 정보를 함께 저장할 수 있다.
- 최신 `PROMOTION(승급)` 기록이 존재하면 프로필의 현재 벨트가 일치해야 한다.

## 13. 리스크와 완화

| 리스크 | 영향 | 완화 |
| --- | --- | --- |
| 단일 모델에 필드가 많아질 수 있음 | 유지보수 비용 증가 | 공통 필드 중심으로 유지하고 카테고리별 필수 필드만 최소화 |
| 프로필 `beltColor`와 `PROMOTION(승급)` 기록 불일치 | 사용자 혼란 | 최신 기록 우선 정책과 저장 시 동기화 적용 |
| 해시태그 자유 입력으로 데이터 품질 저하 | 검색 품질 저하 | 정규화, 길이 제한, 개수 제한 적용 |
| 체크리스트 구조가 빨리 바뀔 수 있음 | API/DB 변경 비용 증가 | MVP는 JSON 직렬화로 유연성 확보 |
| 이미지 업로드 실패 | 기록 완성도 저하 | 이미지 없이도 저장 가능하게 설계 |
| 외부 링크에 잘못된 URL이 저장될 수 있음 | 이동 실패, UX 저하 | 허용 도메인 검증과 `https://` 정규화 적용 |

## 14. 미해결 의사결정

| 항목 | 선택지 | 권장안 |
| --- | --- | --- |
| 미래 날짜 기록 | 허용 / 비허용 | 비허용 |
| 하루 기록 개수 | 날짜당 1건 / 날짜당 다건 | 날짜당 다건 권장 |
| 체크리스트 저장 방식 | JSON 직렬화 / 별도 테이블 | MVP는 JSON 직렬화 |
| 해시태그 저장 방식 | JSON 직렬화 / 별도 태그 테이블 | MVP는 JSON 직렬화 우선 검토 |
| 이미지 개수 | 1장 / 다중 | MVP는 1장 |
| 외부 링크 저장 방식 | `externalLinksJson` / 별도 링크 테이블 | MVP는 `externalLinksJson` |
| 벨트 변경 진입점 | 프로필 수정 유지 / `PROMOTION(승급)` 기록 전용 | 신규 입력은 `PROMOTION(승급)` 기록 전용 권장 |

## 15. 권장 결론

- 이번 구조는 사용자가 생각한 입력 방식과 가장 잘 맞는다.
- 핵심은 `카테고리 선택 후 하나의 기록 폼에 모든 내용을 작성`하는 방식이다.
- 구현은 `TrainingLogEntry` 단일 모델을 중심으로 하고, `PROMOTION(승급)`만 예외적으로 벨트 동기화를 담당하게 두는 것이 가장 현실적이다.

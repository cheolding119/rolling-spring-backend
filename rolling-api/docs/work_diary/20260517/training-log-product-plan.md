# 훈련 기록 기능 기획서

## 1. 문서 목적

- 주짓수 개인 훈련 기록 기능을 `rolling-api` 구조에 맞게 설계하기 위한 기준 문서다.
- 범위는 개인용 BJJ Log & Tracker MVP로 제한한다.
- 이 문서는 현재 작업의 기준이며, 후속 확장은 별도 문서로 분리한다.

## 2. 달성 목표

### 2.1 해결하려는 문제

- 주짓수는 기술 수가 많고 성장 속도가 느려서, 훈련 내용을 그냥 지나치기 쉽다.
- 오픈매트, 커뮤니티 글, 노트앱으로는 날짜별 훈련 흐름을 한 곳에 모으기 어렵다.
- 사용자는 짧고 가볍게 누적되는 개인 기록 공간이 필요하다.

### 2.2 성공 기준

- 주간/월간 훈련 기록 생성 수가 증가한다.
- 기록 작성 후 재방문 비율이 올라간다.
- 기록에서 카테고리별 분포와 반복 패턴을 확인할 수 있다.
- 사용자가 “오늘 무엇을 했는지”를 빠르게 복기할 수 있다.

## 3. 설계 원칙

### 3.1 단일 기록 모델

- `일일 기록`과 `장기 이벤트`를 분리하지 않고, 카테고리 기반 단일 기록 모델을 사용한다.
- 사용자는 날짜와 카테고리를 선택한 뒤, 제목/내용/체크리스트/해시태그/외부 링크를 입력한다.
- 기록의 기본 단위는 “특정 날짜의 특정 훈련 기록 1건”이다.

### 3.2 기존 자산 재사용

- 인증과 권한은 기존 JWT `UserPrincipal` 구조를 그대로 사용한다.
- 공통 시간 필드는 `BaseTimeEntity`를 사용한다.
- 이미지 업로드는 커뮤니티/토너먼트에서 사용 중인 presigned URL 패턴을 따른다.
- 외부 링크는 Instagram/YouTube만 허용한다.
- 현재 `User.beltColor`가 존재하므로, `PROMOTION` 카테고리 기록과 연동해 최신 상태를 맞춘다.

### 3.3 MVP 원칙

- MVP는 “캘린더에서 날짜를 고르고 바로 기록을 남기는 흐름”에 집중한다.
- 핵심 구조는 `카테고리 -> 항목 -> 내용 -> 체크리스트 -> 해시태그 -> 외부 링크`다.
- 이미지는 복수 입력을 허용하고, 외부 링크는 선택 입력으로 둔다.
- 월간 캘린더, 날짜별 카드 목록, 상세 조회는 기록 복기를 돕는 보조 기능으로 제공한다.
- 공유, 공개 피드, 좋아요, 관리자 운영 화면은 이번 범위에서 제외한다.

## 4. 핵심 사용 흐름

### 4.1 기록 진입

1. 사용자는 내 페이지의 캘린더에서 특정 날짜를 선택한다.
2. 사용자는 기록 작성 버튼을 누른다.
3. 작성 가능한 카테고리 목록이 표시된다.
4. 사용자가 카테고리를 선택하면 해당 날짜의 기록 작성 화면으로 이동한다.

### 4.2 일일 기록 작성

1. 사용자는 카테고리를 선택한다.
2. 사용자는 제목과 내용을 입력한다.
3. 사용자는 진행한 내용을 체크리스트로 기록한다.
4. 사용자는 해시태그를 입력한다.
5. 필요하면 이미지를 첨부한다.
6. 필요하면 Instagram 또는 YouTube 링크를 추가한다.
7. 카테고리가 `PROMOTION`이면 벨트 색상과 그랄 수를 함께 입력한다.
8. 저장 시 해당 날짜의 기록이 정상 상태로 반영된다.

### 4.3 기간별 확인

1. 사용자는 “이번 달 훈련량” 화면에 진입한다.
2. 백엔드는 월간 캘린더 요약, 날짜별 카드 목록, 상세 조회 데이터를 반환한다.
3. 프론트는 월간 캘린더, 날짜 선택 후 카드 목록, 최근 기록 목록을 함께 보여준다.

## 5. 이번 범위

### 5.1 포함 기능

| 영역 | 기능 | 포함 기준 |
| --- | --- | --- |
| 진입 | 캘린더 날짜 선택 | 날짜 기준 기록 조회/작성 진입 |
| 진입 | 작성 버튼 기반 생성 진입 | 기록 작성 화면 진입 |
| 진입 | 카테고리 선택 | 7개 카테고리 사용 |
| 기록 | 일일 훈련 기록 생성/수정 | 날짜 기준 1건 또는 다건 기록 |
| 기록 | 일일 훈련 기록 조회 | 날짜 기준 요약 카드 목록 또는 상세 조회 |
| 기록 | 일일 훈련 기록 삭제 | 본인 기록만 삭제 |
| 기록 | 제목/내용 입력 | 공통 필수 입력 |
| 기록 | 체크리스트 입력 | 공통 입력 |
| 기록 | 해시태그 입력 | 공통 입력 |
| 기록 | 외부 링크 입력 | `INSTAGRAM`, `YOUTUBE` |
| 기록 | 이미지 업로드 URL 발급 및 조회 | 이미지 복수 기준 |
| 집계 | 월간 캘린더 조회 | 날짜별 색상, 기록 수, 총 훈련 시간 |
| 탐색 | 최근 기록 조회 | 최근 작성한 기록 목록 |
| 벨트 | 현재 벨트 동기화 | 최신 `PROMOTION` 기록 기준 `User.beltColor` 반영 |

### 5.2 카테고리 정의

- `TECHNIQUE` - 배운 기술, 성공한 기술, 복기한 기술 기록
- `SPARRING` - 스파링 중심의 분석과 복기 기록
- `TOURNAMENT` - 대회 출전, 결과, 준비 과정 기록
- `PROMOTION` - 승급, 벨트, 그랄 관련 기록
- `OPEN_MAT` - 오픈매트 참여 내용 기록
- `DRILL` - 반복 드릴과 숙련 중심 기록
- `PERSONAL_TRAINING` - 개인 보강 훈련, 운동, 솔로 훈련 기록

### 5.3 후속 영역

- 외부 링크 확장
- 링크 미리보기나 oEmbed
- 공개 피드, 공유, 좋아요
- 관리자 운영 도구

### 5.4 기록 색상 팔레트

- 훈련 기록 표시용 색상은 `TrainingLogColor` enum으로 분리한다.
- enum raw value는 `RED`, `ORANGE`, `YELLOW`, `GREEN`, `BLUE`, `NAVY`, `PURPLE`, `PINK`, `TEAL`, `BROWN`, `GRAY`, `BLACK`을 사용한다.
- 실제 hex 또는 디자인 토큰은 프론트 UI 레이어에서 결정한다.
- 기록 응답의 `color`는 `TrainingLogColor` 독립 필드다.

## 6. 도메인 모델

### 6.1 `TrainingLogEntry`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 기록 ID |
| `user` | `User` | 소유 사용자 |
| `trainingDate` | `LocalDate` | 기록 날짜 |
| `category` | `TrainingLogCategory` | 기록 카테고리 |
| `title` | `String` | 기록 제목 |
| `content` | `String` | 기록 본문 |
| `checklistJson` | `String?` | 체크리스트 JSON |
| `hashtagsJson` | `String?` | 해시태그 목록 JSON |
| `externalLinksJson` | `String?` | 외부 링크 목록 JSON |
| `imageUrl` | `String?` | 대표 이미지 URL |
| `imageUrlsJson` | `String?` | 이미지 목록 JSON |
| `color` | `TrainingLogColor?` | 기록 색상 |
| `beltColor` | `BeltColor?` | `PROMOTION` 전용 |
| `stripeCount` | `Integer?` | `PROMOTION` 전용 |
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

- 별도 `GrowthEvent` 테이블은 두지 않는다.
- 모든 기록은 하나의 `TrainingLogEntry`로 처리한다.
- 카테고리에 따라 일부 필드는 선택적으로 사용한다.
- `PROMOTION` 카테고리에서만 `beltColor`, `stripeCount`를 사용한다.

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

### 7.1 컨트롤러 구조

- 단일 컨트롤러는 `TrainingLogController`로 둔다.
- 개인 기능 중심 경로는 `/api/v1/training-logs/me/*` 형식을 사용한다.

### 7.2 권장 API 목록

| 목적 | Method | Path |
| --- | --- | --- |
| 특정 날짜 요약 카드 목록 조회 | `GET` | `/api/v1/training-logs/me/entries?date=2026-05-18` |
| 특정 기록 상세 조회 | `GET` | `/api/v1/training-logs/me/entries/{id}` |
| 특정 날짜 기록 생성 | `POST` | `/api/v1/training-logs/me/entries/{date}` |
| 특정 기록 수정 | `PATCH` | `/api/v1/training-logs/me/entries/{id}` |
| 특정 기록 삭제 | `DELETE` | `/api/v1/training-logs/me/entries/{id}` |
| 월간 캘린더 요약 조회 | `GET` | `/api/v1/training-logs/me/calendar?year=2026&month=5` |
| 최근 기록 목록 조회 | `GET` | `/api/v1/training-logs/me/recent` |
| 태그 자동완성 조회 | `GET` | `/api/v1/training-logs/me/tags?q=triangle` |
| 이미지 업로드 URL 발급 | `POST` | `/api/v1/training-logs/me/upload-url` |

### 7.3 응답 방향

- 월간 캘린더 응답에는 날짜별 `colors[]`, `recordCount`, `totalMinutes`를 포함한다.
- 날짜 선택 카드 응답에는 `id`, `title`, `content`, `color`, `createdAt`를 포함한다.
- 상세 응답에는 카테고리, 제목, 내용, 체크리스트, 해시태그, 외부 링크, 이미지 목록, 색상, 승급 정보, `trainingDate`가 포함된다.
- 체크리스트 항목은 `text`, `checked`, `favorite`, `emoji` 필드를 가진다.
- 엔티티는 직접 노출하지 않고 별도 DTO로 반환한다.

## 8. 정책 및 검증

### 8.1 권한

- 모든 API는 로그인 사용자를 기준으로 처리한다.
- 기록은 본인 데이터만 접근 가능하다.

### 8.2 입력 규칙

- `date`는 미래 날짜를 허용하지 않는다.
- `category`, `title`, `content`는 필수다.
- `title`, `content`는 trim 처리 후 검증한다.
- 체크리스트는 최대 20개를 권장한다.
- 해시태그는 최대 10개를 권장한다.
- 외부 링크는 최대 3개를 권장한다.

### 8.3 해시태그 규칙

- 해시태그는 trim과 정규화를 거친다.
- 중복 해시태그는 제거한다.
- 앞의 `#`는 제거한다.
- 허용 문자는 한글, 영문 소문자, 숫자, 하이픈(`-`)이다.
- 해시태그 검증은 본인 데이터 기준으로만 수행한다.

### 8.4 외부 링크 규칙

- 이번 범위에서 허용하는 링크 타입은 `INSTAGRAM`, `YOUTUBE` 두 가지다.
- 허용 도메인은 다음과 같다.
  - `instagram.com`
  - `www.instagram.com`
  - `youtube.com`
  - `www.youtube.com`
  - `youtu.be`
- URL은 `https://` 기준으로 정규화한다.
- 기록당 외부 링크는 최대 3개를 권장한다.
- 미리보기 메타 태그 수집과 oEmbed는 이번 범위 밖이다.

### 8.5 카테고리별 규칙

- `PROMOTION`
  - `beltColor` 필수
  - `stripeCount`는 0 이상
  - 최신 기록 기준으로 `User.beltColor`를 갱신한다.
- 나머지 6개 카테고리
  - 공통 필드만 사용한다.

추가 규칙:

- 모든 카테고리에서 `title`, `content`는 필수다.
- 모든 카테고리에서 체크리스트, 해시태그, 이미지, 외부 링크는 선택 입력이다.
- `PROMOTION` 카테고리 기록이 `User.beltColor`를 직접 변경하지는 않는다.

## 9. 현재 프로젝트와의 연결

### 9.1 `User`와의 관계

- 현재 `User`는 `beltColor`를 가진다.
- 최신 `PROMOTION` 기록을 기준으로 `User.beltColor`를 반영한다.
- `stripeCount`는 사용자 도메인에 직접 넣지 않고 기록에서 관리한다.

### 9.2 이미지 업로드

- 이미지 업로드 전용 서비스는 `TrainingLogImageUploadService`로 분리한다.
- S3 key prefix 예시는 `training/logs/images/`를 사용한다.
- 구현 방식은 기존 커뮤니티 이미지 업로드 서비스의 presigned URL 패턴을 따른다.

### 9.3 외부 링크 처리

- 외부 링크는 `TrainingLogEntry.externalLinksJson`으로 저장한다.
- 서비스 계층에서 타입과 URL을 검증하고 `https://`로 정규화한다.
- 응답에는 정규화된 외부 링크 배열을 그대로 반환한다.

### 9.4 DB 마이그레이션

권장 순서:

1. `training_log_entries` 테이블 추가
2. 필요한 경우 태그/링크 별도 테이블을 검토
3. MVP는 JSON 직렬화 구조를 유지한다

## 10. 구현 순서 권장

1. Flyway 마이그레이션 추가 및 JPA 엔티티/리포지토리 정의
2. `TrainingLogCategory` enum 계약 확정
3. 특정 날짜 기록 생성/조회/수정/삭제 API 구현
4. 체크리스트 JSON 직렬화 및 정규화 구현
5. 해시태그 정규화 및 자동완성 API 구현
6. 월간 캘린더 및 조회 API 구현
7. 최근 기록 조회 API 구현
8. 이미지 업로드 URL 발급 구현
9. 외부 링크 검증 및 저장 구조 구현
10. `PROMOTION` 기준 `User.beltColor` 갱신 처리

## 11. Phase 진행 체크리스트

### Phase 0. 기획 확정 및 계약 정리

- [x] 일일 기록 모델로 간다는 방향 확정
- [x] `TrainingLogCategory` raw value 확정 - `TECHNIQUE` 방식 사용
- [x] 공통 입력 구조를 `카테고리 -> 제목/내용 -> 체크리스트 -> 해시태그 -> 외부 링크`로 확정
- [x] `PROMOTION`에서 `User.beltColor`를 갱신하는 정책 확정
- [x] 외부 링크 허용 도메인 확정
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
- [x] `belt_color`, `stripe_count` 컬럼 추가
- [x] `TrainingLogEntry` 엔티티 추가 또는 정비
- [x] `TrainingLogCategory` enum 추가
- [x] `TrainingLogLinkType` enum 추가
- [x] `TrainingLogEntryRepository` 추가

### Phase 2. 일일 기록 CRUD

- [x] 특정 날짜 기록 목록 조회 API 구현
- [x] 특정 날짜 기록 생성 API 구현
- [x] 특정 기록 수정 API 구현
- [x] 특정 기록 삭제 API 구현
- [x] 본인 데이터만 접근 가능하도록 권한 처리
- [x] 미래 날짜 저장 제한 적용
- [x] `title`, `content` 필수 검증 적용

### Phase 3. 체크리스트 및 해시태그

- [x] 체크리스트 JSON 직렬화 로직 구현
- [x] 체크리스트 최대 개수 제한 적용
- [x] 해시태그 정규화 로직 구현
- [x] 해시태그 중복 제거 처리
- [x] 해시태그 최대 개수 제한 적용
- [x] 해시태그 자동완성 API 구현

### Phase 4. 월간 캘린더 / 날짜 요약 / 상세 조회

- [x] 월간 기준 날짜 집계 조회 쿼리 구현
- [x] 날짜별 색상 목록 계산 구현
- [x] 날짜별 기록 수 계산 구현
- [x] 날짜별 총 훈련 시간 계산 구현
- [x] 월간 캘린더 집계 API 구현
- [x] 선택 날짜 요약 카드 조회 API 구현
- [x] 기록 상세 조회 API 구현
- [x] 최근 기록 목록 조회 API 구현

### Phase 5. 카테고리별 검증

- [x] `PROMOTION`에서 `beltColor` 필수 검증 적용
- [x] `PROMOTION`에서 `stripeCount` 검증 적용
- [x] 나머지 6개 카테고리 공통 검증 적용
- [x] 카테고리별 예외 메시지 정리

### Phase 6. 이미지 업로드

- [x] 이미지 업로드 요청 DTO 추가
- [x] 이미지 업로드 응답 DTO 추가
- [x] `TrainingLogImageUploadService` 구현
- [x] presigned URL 발급 API 구현
- [x] 사용 일일 용량 검증 적용

### Phase 7. 외부 링크 처리

- [x] 외부 링크 JSON 구조 구현
- [x] `INSTAGRAM`, `YOUTUBE` 타입 검증 적용
- [x] 허용 도메인 검증 적용
- [x] `https://` 정규화 처리
- [x] 기록당 외부 링크 최대 개수 제한 적용
- [x] 응답에 외부 링크 배열 포함

### Phase 8. 승급 벨트 동기화

- [x] 최신 `PROMOTION` 기록 판단 로직 구현
- [x] `User.beltColor` 갱신 로직 구현
- [x] `PROMOTION` 기록 수정 시 벨트 갱신 처리
- [x] `PROMOTION` 기록 삭제 시 최신 벨트 재계산 처리

### Phase 9. API 응답 정리 및 문서화

- [x] API 응답 DTO 정리
- [x] Swagger 어노테이션 정리
- [x] validation 실패 응답 케이스 정리
- [x] 권한 실패 응답 케이스 정리
- [x] not found 응답 케이스 정리
- [x] 카테고리 enum raw value 문서화

### Phase 10. 테스트 및 운영 마무리

- [x] 일일 기록 CRUD 통합 테스트
- [x] 해시태그 정규화 통합 테스트
- [x] 월간 캘린더 및 날짜 조회 통합 테스트
- [x] 최근 기록 조회 통합 테스트
- [x] 외부 링크 검증 통합 테스트
- [x] `PROMOTION` 벨트 갱신 통합 테스트
- [x] 컨트롤러 계층 API 테스트
- [x] Flutter 연동용 필드 최종 점검

## 12. 사용자 시나리오 초안

### 12.1 기록

- 사용자는 오늘 날짜 또는 과거 날짜의 훈련 기록을 남길 수 있다.
- 사용자는 기록에 카테고리, 제목, 내용, 체크리스트, 해시태그, 외부 링크를 입력할 수 있다.
- 사용자는 필요하면 이미지를 첨부할 수 있다.
- 사용자는 최근 기록을 통해 반복 패턴을 확인할 수 있다.

### 12.2 승급

- 사용자는 `PROMOTION` 카테고리 기록으로 벨트/그랄 정보를 남길 수 있다.
- 최신 `PROMOTION` 기록이 있으면 프로필의 현재 벨트와 일치해야 한다.

## 13. 리스크 완화

| 리스크 | 영향 | 완화 |
| --- | --- | --- |
| 단일 모델에 필드가 많아짐 | 유지보수 비용 증가 | 공통 필드를 중심으로 두고 카테고리별 특수 필드는 최소화 |
| 프로필 벨트와 `PROMOTION` 기록 불일치 | 사용자 혼란 | 최신 기록 우선 정책으로 동기화 |
| 해시태그 입력이 자유로움 | 검증 품질 저하 | 정규화, 길이 제한, 개수 제한 적용 |
| 체크리스트 구조 변경 | API/DB 변경 비용 증가 | MVP는 JSON 직렬화로 유연성 확보 |
| 이미지 업로드 실패 | 기록 작성 중단 | 이미지 없이도 저장 가능하게 설계 |
| 외부 링크가 잘못된 URL일 수 있음 | 연동 실패, UX 저하 | 도메인 검증과 `https://` 정규화 적용 |

## 14. 미해결 의사결정

| 질문 | 선택지 | 권장 |
| --- | --- | --- |
| 미래 날짜 기록 | 허용 / 비허용 | 비허용 |
| 하루 기록 개수 | 1개 / 여러 개 | 여러 개 권장 |
| 체크리스트 저장 방식 | JSON / 별도 테이블 | MVP는 JSON |
| 해시태그 저장 방식 | JSON / 별도 테이블 | MVP는 JSON |
| 이미지 개수 | 1장 / 다중 | MVP는 다중 허용 |
| 색상 지정 | 카테고리 연동 / 독립 필드 | MVP는 독립 필드 |
| 외부 링크 저장 방식 | `externalLinksJson` / 별도 테이블 | MVP는 `externalLinksJson` |
| 벨트 변경 진입점 | 프로필 수정 / `PROMOTION` 기록 | 신규 입력은 `PROMOTION` 기록 우선 |

## 15. 권장 결론

- 이번 구조는 사용자가 날짜 기준으로 기록을 쌓는 방식에 적합하다.
- 핵심은 `카테고리를 선택하고 그날의 기록을 하나의 화면에서 완성`하는 흐름이다.
- 구현은 `TrainingLogEntry` 단일 모델을 중심으로 하고, `PROMOTION`만 벨트 갱신 예외로 둔다.

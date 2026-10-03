# 대회 성적 도메인 및 API 명세

> 상태: 백엔드 구현 완료, 운영 배포 전. 제품 범위는 [대회 성적 기획서](../product_plans/tournament-achievement-product-plan.md)를 따른다.
>
> DB 변경: `V46__add_tournament_achievements.sql`은 성적 테이블·제약·조회 인덱스를 추가하고, `V47__grant_tournament_achievements_permissions.sql`은 `rolling_admin` 테이블/시퀀스 권한을 부여한다. 운영 DB 적용은 아직 수행하지 않았다.

## 1. 도메인 경계

- `Tournament`는 대회 일정·접수 정보를 제공한다. 이 문서의 `TournamentAchievement`는 사용자가 직접 기록한 **개인 출전 부문별 성적**이다.
- 한 대회에서 체급전과 오픈급, Gi와 No-Gi 등 여러 부문에 참가했다면 부문마다 별도 성적 기록을 저장할 수 있다.
- 대회명은 직접 입력하며 기존 `Tournament.id`와의 연결은 필수가 아니다. 롤링 대회 목록에 없는 과거 대회도 기록할 수 있다.
- `TrainingLogCategory.TOURNAMENT`는 훈련 기록이므로 성적 집계에 자동 포함하지 않는다.
- MVP의 성적은 모두 사용자 자가 입력이며 공식 인증 결과로 표시하지 않는다.

## 2. 도메인 모델

### 2.1 `TournamentAchievement`

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | `Long` | 서버 생성 | 성적 기록 ID |
| `userId` | `Long` | 인증에서 결정 | 기록 소유자. 요청 body로 받지 않음 |
| `tournamentName` | `String` | O | 대회명. 1~255자, 앞뒤 공백 제거 |
| `competitionDate` | `LocalDate` | O | 대회 개최일, `YYYY-MM-DD` |
| `result` | `AchievementResult` | O | 금·은·동 또는 참가 |
| `uniformType` | `UniformType?` | X | `GI`, `NO_GI`. 모르면 `null` |
| `beltColor` | `BeltColor?` | X | **대회 당시** 벨트. 현재 `User.beltColor` 변경과 무관한 스냅샷 |
| `ageDivision` | `String?` | X | 대회에 쓰인 연령부 명칭. 예: `Adult`, `Master 1`; 최대 100자 |
| `divisionType` | `DivisionType?` | X | 체급전/오픈급/그 외 부문 구분 |
| `weightClass` | `String?` | X | 대회에 쓰인 체급 표기. 예: `-76kg`; 최대 100자 |
| `organizer` | `String?` | X | 주최사, 최대 255자 |
| `resultUrl` | `String?` | X | 사용자가 입력한 결과 확인 링크, HTTPS URL, 최대 1000자 |
| `memo` | `String?` | X | 개인 메모, 최대 500자 |
| `createdAt` | `LocalDateTime` | 서버 생성 | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 서버 생성 | 마지막 수정 시각 |

`ageDivision`과 `weightClass`는 운영사마다 표현과 기준이 달라 별도 텍스트로 보존한다. 따라서 서로 다른 대회의 동일 체급을 자동으로 같은 범주로 간주하지 않는다. `divisionType=ABSOLUTE`라면 `weightClass`는 비워 둔다. 현재 사용자 벨트는 입력 화면의 초기 제안값으로 사용할 수 있지만, 저장된 과거 기록을 현재 벨트 변경에 따라 수정하지 않는다.

### 2.2 Enum raw value

| Enum | 허용 값 | 의미 |
| --- | --- | --- |
| `AchievementResult` | `GOLD`, `SILVER`, `BRONZE`, `PARTICIPATION` | `PARTICIPATION`은 입상하지 않은 참가 기록이며 메달로 세지 않음 |
| `UniformType` | `GI`, `NO_GI` | 도복 / 노기 |
| `DivisionType` | `WEIGHT`, `ABSOLUTE`, `OTHER` | 체급전 / 오픈급 / 기타 부문 |
| `BeltColor` | 기존 사용자 도메인 `BeltColor` raw value 재사용 | 대회 당시 벨트. [공통 모델](shared/common-models.md) 참조 |

### 2.3 집계 규칙

- `goldCount`, `silverCount`, `bronzeCount`는 **각 결과값을 가진 기록 수**다. 메달 한 개당 성적 기록 한 건을 만든다.
- `medalCount = goldCount + silverCount + bronzeCount`다. `PARTICIPATION`은 `participationCount`에만 포함한다.
- `recordCount`는 메달 기록과 참가 기록을 합친 **부문별 기록 건수**다. 고유 대회 참가 횟수로 표시하지 않는다.
- 같은 대회명과 날짜에 여러 성적이 있어도 허용한다. 서버는 대회명만으로 중복 또는 동일 대회를 확정하지 않는다.
- 집계의 기준은 저장된 본인 성적 기록이다. 수정·삭제 후 집계는 같은 기록 데이터를 기준으로 즉시 재계산된다.

## 3. 공통 정책

- 모든 API는 `Authorization: Bearer {accessToken}`이 필요한 **본인 전용** API다. 비회원과 다른 사용자는 접근할 수 없다.
- 조회·수정·삭제 대상 ID가 존재하지 않거나 다른 사용자 소유이면 동일하게 `404 NOT_FOUND`를 반환한다.
- 개최일은 `Asia/Seoul` 기준 오늘까지 허용하고 미래 날짜는 `400 VALIDATION_ERROR`로 거부한다. 과거 연도 하한은 두지 않는다.
- `tournamentName`은 공백만 입력할 수 없다. 선택 텍스트 필드는 빈 문자열을 `null`로 정규화한다.
- 알 수 없는 enum, 잘못된 날짜·URL 형식, 글자 수 초과는 `400 VALIDATION_ERROR`다.
- 목록은 `competitionDate DESC, id DESC`로 정렬해 같은 날짜에도 페이지 순서를 안정적으로 유지한다.
- 유사 기록 확인은 클라이언트가 목록에서 같은 대회명·날짜를 비교해 안내한다. 서버는 같은 대회에서 여러 부문 성적 입력을 막는 유니크 제약을 두지 않는다.
- 회원 탈퇴 예약이 실행되는 시점에 본인 성적 기록을 삭제한다. 탈퇴 예약/취소만으로는 성적 기록을 삭제하지 않는다.
- 이 데이터는 공식 성적·인증 배지·공개 랭킹의 근거로 사용하지 않는다.

## 4. 공통 API 응답

기존 `ApiResponse<T>`를 사용한다. 성공 시 `success=true`, `data`를 반환한다. 오류 시 `success=false`, `error.code`, `error.message`를 반환한다. 값이 없는 필드는 JSON에서 생략할 수 있다.

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "대회 개최일은 미래일 수 없습니다"
  }
}
```

| HTTP 상태 | 오류 코드 | 발생 조건 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 필수값 누락, 잘못된 값·날짜·URL·enum, 길이 초과 |
| `401` | `UNAUTHORIZED` | 인증 토큰 없음/유효하지 않음 |
| `403` | `FORBIDDEN` | 기존 계정 제재 정책에 의해 접근 제한 |
| `404` | `NOT_FOUND` | 없는 기록 또는 다른 사용자의 기록 ID |

## 5. API 명세

기본 경로: `/api/v1/tournament-achievements/me`

### 5.1 본인 성적 목록

`GET /api/v1/tournament-achievements/me?page=0&size=20`

- 인증: 필요
- Query: `page` 기본 0, `size` 기본 20. 음수 page, 1 미만 또는 100 초과 size는 `400`.
- Response data: 기존 `VIA_DTO` 페이징 설정을 따르는 `Page<TournamentAchievementResponse>`. 기록 배열은 `data.content`, 페이지 정보는 `data.page`에 포함된다. 빈 기록이면 `content=[]`.
- 정렬: 개최일 내림차순, 동일 날짜는 ID 내림차순.

빈 목록 응답 예시:

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": { "size": 20, "number": 0, "totalElements": 0, "totalPages": 0 }
  }
}
```

### 5.2 본인 메달 요약

`GET /api/v1/tournament-achievements/me/summary`

- 인증: 필요
- Response data: `TournamentAchievementSummaryResponse`.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `recordCount` | `Long` | 성적 기록 전체 건수 |
| `goldCount` | `Long` | 금메달 기록 수 |
| `silverCount` | `Long` | 은메달 기록 수 |
| `bronzeCount` | `Long` | 동메달 기록 수 |
| `medalCount` | `Long` | 금·은·동 합계 |
| `participationCount` | `Long` | 입상 없는 참가 기록 수 |

기록이 없으면 모든 값은 `0`이다.

### 5.3 성적 단건 조회

`GET /api/v1/tournament-achievements/me/{id}`

- 인증: 필요
- Response data: `TournamentAchievementResponse`.
- 타인 소유 또는 존재하지 않는 ID: `404 NOT_FOUND`.

### 5.4 성적 등록

`POST /api/v1/tournament-achievements/me`

- 인증: 필요
- Request body: `TournamentAchievementSaveRequest`.
- Response data: 저장된 `TournamentAchievementResponse`. 기존 API의 생성 응답 관례에 맞춰 HTTP `200`.
- `userId`, `id`, 집계값은 요청 body에서 받지 않는다.

### 5.5 성적 수정

`PUT /api/v1/tournament-achievements/me/{id}`

- 인증: 필요
- Request body: 등록과 동일한 `TournamentAchievementSaveRequest`. 전체 교체이므로 필수 3개 필드를 전송해야 한다. 선택 필드는 `null` 또는 생략 시 기존 값이 삭제된다.
- Response data: 수정된 `TournamentAchievementResponse`, HTTP `200`.
- 타인 소유 또는 존재하지 않는 ID: `404 NOT_FOUND`.

### 5.6 성적 삭제

`DELETE /api/v1/tournament-achievements/me/{id}`

- 인증: 필요
- Response: HTTP `200`, `{ "success": true }` (`data=null`은 JSON에서 생략 가능).
- 삭제 후 요약 집계에도 반영된다.
- 타인 소유 또는 존재하지 않는 ID: `404 NOT_FOUND`.

## 6. DTO 및 예시

### 6.1 `TournamentAchievementSaveRequest`

| 필드 | 타입 | 필수 | 예시 |
| --- | --- | --- | --- |
| `tournamentName` | `String` | O | `서울 주짓수 오픈 2026` |
| `competitionDate` | `Date (YYYY-MM-DD)` | O | `2026-09-12` |
| `result` | `AchievementResult` | O | `GOLD` |
| `uniformType` | `UniformType?` | X | `GI` |
| `beltColor` | `BeltColor?` | X | `BLUE` |
| `ageDivision` | `String?` | X | `Adult` |
| `divisionType` | `DivisionType?` | X | `WEIGHT` |
| `weightClass` | `String?` | X | `-76kg` |
| `organizer` | `String?` | X | `대회 주최사` |
| `resultUrl` | `String?` | X | `https://example.com/results/123` |
| `memo` | `String?` | X | `첫 블루벨트 대회` |

```json
{
  "tournamentName": "서울 주짓수 오픈 2026",
  "competitionDate": "2026-09-12",
  "result": "GOLD",
  "uniformType": "GI",
  "beltColor": "BLUE",
  "ageDivision": "Adult",
  "divisionType": "WEIGHT",
  "weightClass": "-76kg",
  "organizer": "대회 주최사",
  "resultUrl": "https://example.com/results/123",
  "memo": "첫 블루벨트 대회"
}
```

### 6.2 `TournamentAchievementResponse`

등록/단건/수정 응답과 목록의 `content` 항목은 요청 필드에 더해 `id`, `createdAt`, `updatedAt`을 포함한다. 다른 사용자의 ID나 개인정보는 반환하지 않는다.

```json
{
  "success": true,
  "data": {
    "id": 42,
    "tournamentName": "서울 주짓수 오픈 2026",
    "competitionDate": "2026-09-12",
    "result": "GOLD",
    "uniformType": "GI",
    "beltColor": "BLUE",
    "ageDivision": "Adult",
    "divisionType": "WEIGHT",
    "weightClass": "-76kg",
    "organizer": "대회 주최사",
    "resultUrl": "https://example.com/results/123",
    "memo": "첫 블루벨트 대회",
    "createdAt": "2026-10-03T11:00:00",
    "updatedAt": "2026-10-03T11:00:00"
  }
}
```

## 7. 범위 및 배포 참고

- 체급/연령부는 우선 대회 원문을 별도 텍스트 필드로 저장한다. 플랫폼 공통 분류나 체급별 통계가 필요해지면 정규화 기준을 별도로 정의한다.
- 메인 홈 요약 카드, 다른 사용자 공개, 공식 결과 자동 연결, 증빙/검증, 고유 대회 참가 횟수는 이 API의 MVP 계약에 포함하지 않는다.
- `resultUrl`은 사용자가 제공한 링크이며 공식 검증 상태를 뜻하지 않는다.
- API 통합 테스트는 실제 보안 필터·서비스·JPA를 H2에서 연결해 본인 CRUD, 집계, 날짜/enum/URL 검증 및 타인 접근 거부를 확인한다. V46 SQL의 생성·제약·외래키 삭제 동작도 H2 PostgreSQL 모드에서 검증한다.
- PostgreSQL V46/V47 실행과 운영 마이그레이션 이력은 배포 전에 확인해야 한다. 기존 저장소에 V2와 V3 버전 파일이 각각 두 개 있어 전체 Flyway 탐색/적용 경로는 별도 확인이 필요하다. 이 기능에서 기존 마이그레이션 번호나 이력을 변경하지 않는다.

# 오픈매트 / 대회 신고 기능 안내

이 문서는 현재 백엔드에 구현된 `오픈매트`와 `대회` 신고 기능을 쉽게 이해할 수 있도록 정리한 문서다.

## 0. 차단 종류 먼저 구분하기

이 문서에서 말하는 `차단`은 두 종류로 나눠서 봐야 한다.

- `신고 누적 차단`
  - 오픈매트가 신고 3건 이상 누적되면 신청이 막히는 상태
  - 관리자 `report-block` 해제로 풀 수 있다
- `사용자 차단`
  - 내가 싫은 다른 사용자를 차단해서 그 사람을 기준으로 보지 않으려는 기능
  - 현재 코드에는 `POST /api/v1/users/{id}/block`, `DELETE /api/v1/users/{id}/block`가 있다
  - 다만 현재 확인된 코드 범위에서는 이 차단이 오픈매트/대회 목록 조회에서 자동으로 게시물을 숨기는 필터와 연결된 흔적은 보이지 않는다

## 1. 한 줄 요약

- 오픈매트와 대회는 모두 신고할 수 있다.
- 신고는 로그인한 사용자만 가능하다.
- 같은 대상은 같은 사용자가 한 번만 신고할 수 있다.
- 자기 자신이 작성한 대상은 신고할 수 없다.
- `OTHER` 사유를 선택하면 추가 설명(`customReason`)을 꼭 넣어야 한다.
- 오픈매트는 신고가 3건 이상 쌓이면 신청이 막힌다.
- 대회는 신고를 저장하지만, 오픈매트처럼 자동 차단되지는 않는다.

## 2. 공통 신고 규칙

두 기능 모두 공통 신고 저장 기능을 사용한다.

### 신고 요청 바디

```json
{
  "reason": "SPAM",
  "customReason": "광고성 게시물입니다"
}
```

### 신고 사유

- `FALSE_INFO`
- `INAPPROPRIATE`
- `SPAM`
- `OTHER`

### 공통 제약

- `reason`은 필수다.
- `customReason`은 `OTHER`일 때만 필수다.
- 동일한 사용자가 같은 대상에 중복 신고하면 `ALREADY_REPORTED` 에러가 난다.
- 대상이 없으면 `404`가 난다.
- 자기 글/자기 오픈매트는 신고할 수 없다.

## 3. 오픈매트 신고

### API

`POST /api/v1/open-mats/{id}/report`

### 동작

1. 로그인한 사용자가 오픈매트를 신고한다.
2. 백엔드는 공통 `Report` 데이터로 저장한다.
3. 오픈매트 자체의 신고 횟수(`reportCount`)도 1 증가한다.
4. 신고가 3건 이상이면 해당 오픈매트는 `reported=true`로 보인다.
5. 신고 누적으로 막힌 오픈매트는 신청이 차단된다.

### 응답에서 보이는 값

오픈매트 상세 응답에는 `reported` 값이 있다.

- `false`: 신고 누적이 아직 3건 미만
- `true`: 신고 누적 3건 이상

### 관리자 기능

오픈매트는 관리자용 신고 차단 해제 API가 따로 있다.

`PATCH /api/v1/admin/open-mats/{id}/report-block`

이 API를 호출하면:

- 오픈매트의 신고 누적 수가 0으로 초기화된다.
- 다시 신청 가능한 상태로 되돌릴 수 있다.

## 4. 대회 신고

### API

`POST /api/v1/tournaments/{id}/report`

### 동작

1. 로그인한 사용자가 대회를 신고한다.
2. 백엔드는 공통 `Report` 데이터로 저장한다.
3. 대회 신고는 저장과 관리자 확인 용도에 초점이 있다.
4. 오픈매트처럼 별도의 신청 차단 상태는 없다.

## 5. 관리자 신고 관리

관리자는 오픈매트와 대회 신고를 같은 관리 화면에서 볼 수 있다.

### 목록 조회

`GET /api/v1/admin/reports`

### 상세 조회

`GET /api/v1/admin/reports/{id}`

### 상태 변경

`PATCH /api/v1/admin/reports/{id}/status`

신고 상태는 아래 값을 가진다.

- `RECEIVED`
- `IN_REVIEW`
- `RESOLVED`
- `REJECTED`

---

## 6. 차단 상세

이 문서에서 말하는 차단은 신고 저장 단계의 실패와, 오픈매트 서비스 차단을 따로 구분해서 봐야 한다.

### 6.1 신고 자체가 막히는 경우

아래 경우에는 신고가 저장되지 않고 바로 실패한다.

- 로그인하지 않은 경우
- 같은 사용자가 같은 대상을 이미 신고한 경우
- 자기 자신이 작성한 대상인 경우
- `reason`이 없는 경우
- `OTHER`인데 `customReason`이 비어 있는 경우

### 6.2 오픈매트 신청이 막히는 경우

오픈매트는 신고가 누적되면 실제 서비스 동작이 바뀐다.

- 신고가 3건 이상이면 오픈매트의 `reported` 값이 `true`가 된다.
- 이 상태의 오픈매트는 신청이 차단된다.
- 관리자가 `PATCH /api/v1/admin/open-mats/{id}/report-block`를 호출하면 차단을 해제할 수 있다.
- 차단 해제 시 내부 신고 누적 수는 0으로 초기화된다.

### 6.3 대회는 자동 차단이 없는 경우

대회는 신고를 저장하고 관리자가 확인하는 구조다.

- 신고가 쌓여도 대회 자체가 자동으로 막히지는 않는다.
- 관리자는 `GET /api/v1/admin/reports`에서 목록을 보고, `PATCH /api/v1/admin/reports/{id}/status`로 처리 상태를 바꾼다.

## 7. 실제 코드 위치

- 오픈매트 신고 API: `src/main/java/com/rolling/api/domain/openmat/controller/OpenMatController.java`
- 오픈매트 신고 처리: `src/main/java/com/rolling/api/domain/openmat/service/OpenMatService.java`
- 오픈매트 신고 차단 해제: `src/main/java/com/rolling/api/domain/openmat/controller/OpenMatAdminController.java`
- 대회 신고 API: `src/main/java/com/rolling/api/domain/tournament/controller/TournamentController.java`
- 대회 신고 처리: `src/main/java/com/rolling/api/domain/tournament/service/TournamentService.java`
- 공통 신고 처리: `src/main/java/com/rolling/api/domain/report/service/ReportService.java`
- 신고 요청 DTO: `src/main/java/com/rolling/api/domain/report/dto/ReportCreateRequest.java`
- 사용자 차단 API: `src/main/java/com/rolling/api/domain/user/controller/UserController.java`
- 사용자 차단 처리: `src/main/java/com/rolling/api/domain/user/service/UserService.java`

## 8. 이해하기 쉬운 비유

- 오픈매트 신고는 `민원 접수 + 누적 3회면 임시 차단` 구조다.
- 대회 신고는 `민원 접수 + 관리자 검토` 구조에 가깝다.

즉:

- 오픈매트는 신고가 쌓이면 바로 서비스 동작에 영향이 간다.
- 대회는 신고를 모아서 관리자가 확인하고 처리한다.

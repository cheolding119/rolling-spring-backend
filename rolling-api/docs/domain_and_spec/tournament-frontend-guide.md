# Tournament Frontend Guide

- 이 문서는 대회 도메인 변경사항을 프론트에서 바로 반영할 수 있게 정리한 작업 가이드다.
- 상세 source of truth는 [tournament.md](tournament.md)를 따른다.

## 1. 이번 변경의 목적

이번 변경은 대회 조회만 가능하던 현재 구조에 아래 3가지를 추가하기 위한 것이다.

1. 지역 기준 탐색
2. 관심 대회 저장
3. 접수 마감 전 리마인드 설정

프론트 관점에서 핵심은 아래다.

- 목록/상세에 `region`이 추가됐다.
- 사용자가 대회를 `찜`할 수 있다.
- 사용자가 찜한 대회에만 알림 on/off와 날짜/시간을 설정할 수 있다.
- 알림은 자유 문장 저장이 아니라 `notificationEnabled + remindDate + remindTime` 구조다.

## 2. 왜 추가됐는가

### 2.1 `region`

기존 `location`은 자유 텍스트라서 `경기`, `경남`, `대구` 같은 넓은 지역 단위 탐색이 어려웠다.

그래서:

- 목록 필터용 `region`이 추가됐다.
- 상세/목록 응답에도 `region`이 포함된다.
- `region`은 nullable이다.

프론트 의미:

- 지역 필터 UI를 만들 수 있다.
- `region == null`인 대회는 “지역 미설정”으로 처리하면 된다.

### 2.2 찜

사용자가 나중에 다시 볼 대회를 저장할 수 있도록 추가됐다.

그래서:

- 대회 상세에서 찜 버튼을 둘 수 있다.
- 별도 `찜한 대회 목록` 화면을 만들 수 있다.

주의:

- 일반 대회 목록/상세 응답에는 아직 `favorited` 필드가 없다.
- 현재는 `찜 목록 API` 기준으로 UX를 구성해야 한다.

### 2.3 리마인드

사용자가 접수 마감을 놓치지 않도록 추가됐다.

그래서:

- 찜한 대회에만 리마인드를 설정할 수 있다.
- 알림 스위치 `on/off`
- 알림 날짜 `remindDate`
- 알림 시간 `remindTime`

를 저장한다.

주의:

- 현재 마감 시각은 DB에 없고 `registrationDeadline` 날짜만 있다.
- 서버는 내부적으로 `registrationDeadline 23:59:59 KST`를 기준 마감 시각으로 본다.

즉 프론트는:

- `D-1`, `D-2`, `D-30` 같은 빠른 선택 UX를 줄 수 있고
- 최종 저장은 `날짜 + 시간`으로 보내면 된다.

## 3. 프론트에서 바로 써야 하는 API

### 3.1 대회 목록 조회

`GET /api/v1/tournaments`

새로 추가된 점:

- query param `region`
- response field `region`

예시:

```http
GET /api/v1/tournaments?region=GYEONGGI&page=0&size=20
```

프론트 사용처:

- 대회 목록 지역 필터

### 3.2 대회 상세 조회

`GET /api/v1/tournaments/{id}`

새로 추가된 점:

- response field `region`

프론트 사용처:

- 상세 상단 정보
- 지역 뱃지/라벨 표시

### 3.3 대회 찜 추가

`POST /api/v1/tournaments/{id}/favorite`

- 인증 필요
- response: `TournamentFavoriteResponse`

프론트 사용처:

- 상세의 찜 버튼 on

주의:

- 멱등적으로 동작한다.
- 이미 찜한 상태에서 다시 호출해도 현재 상태 응답을 받을 수 있다.

### 3.4 대회 찜 해제

`DELETE /api/v1/tournaments/{id}/favorite`

- 인증 필요

프론트 사용처:

- 상세의 찜 버튼 off
- 찜 목록에서 제거

### 3.5 찜한 대회 목록 조회

`GET /api/v1/tournaments/favorites`

- 인증 필요
- response: `Page<TournamentFavoriteResponse>`

프론트 사용처:

- 마이페이지 > 찜한 대회
- 리마인드 관리 진입 리스트

### 3.6 찜한 대회 리마인드 설정

`PATCH /api/v1/tournaments/{id}/favorite-reminder`

- 인증 필요
- request: `notificationEnabled`, `remindDate`, `remindTime`
- response: `TournamentFavoriteResponse`

예시:

```json
{
  "notificationEnabled": true,
  "remindDate": "2026-06-13",
  "remindTime": "09:00"
}
```

프론트 사용처:

- 찜한 대회 상세 또는 찜 목록 아이템의 알림 설정

중요 규칙:

- 찜한 대회에만 호출 가능
- `notificationEnabled=true`면 날짜/시간 둘 다 보내야 함
- `notificationEnabled=false`면 날짜/시간 없이 보내도 됨

## 4. 응답에서 추가로 봐야 하는 필드

### 4.1 `TournamentResponse`

이번에 프론트가 새로 써야 하는 필드:

- `region: Region?`

예시:

```json
{
  "id": 10,
  "source": "MANUAL",
  "title": "제5회 롤링컵",
  "competitionDate": "2026-04-15",
  "registrationDeadline": "2026-04-01",
  "location": "서울 올림픽공원 체조경기장",
  "region": "SEOUL",
  "applyLink": "https://forms.google.com/...",
  "registrationClosed": false
}
```

### 4.2 `TournamentFavoriteResponse`

프론트가 찜/리마인드 화면에서 써야 하는 핵심 필드:

- `tournamentId`
- `title`
- `posterUrl`
- `competitionDate`
- `registrationDeadline`
- `location`
- `region`
- `applyLink`
- `registrationClosed`
- `notificationEnabled`
- `remindDate`
- `remindTime`
- `favoritedAt`

예시:

```json
{
  "tournamentId": 10,
  "source": "MANUAL",
  "title": "제5회 롤링컵",
  "posterUrl": "https://cdn.rolling.com/posters/10.jpg",
  "competitionDate": "2026-04-15",
  "registrationDeadline": "2026-04-01",
  "location": "서울 올림픽공원 체조경기장",
  "region": "SEOUL",
  "applyLink": "https://forms.google.com/...",
  "registrationClosed": false,
  "notificationEnabled": true,
  "remindDate": "2026-04-01",
  "remindTime": "09:00:00",
  "favoritedAt": "2026-03-20T10:00:00"
}
```

## 5. 프론트 구현 포인트

### 5.1 목록 화면

- 지역 필터를 추가한다.
- `region`이 없으면 “전체” 또는 “지역 미설정” 정책으로 처리한다.
- 현재 목록 응답에는 `favorited`가 없으므로 찜 여부를 목록 카드에 기본 표시하는 구조는 아직 맞지 않다.

### 5.2 상세 화면

- `region` 표시 가능
- 로그인 사용자에게만 찜 버튼 노출
- 찜 후 바로 리마인드 설정 진입 가능

### 5.3 찜 목록 화면

- 찜 목록은 `GET /favorites`를 source of truth로 사용한다.
- 여기서 알림 on/off 상태와 예약된 날짜/시간을 함께 보여줄 수 있다.

### 5.4 리마인드 UX

추천 흐름:

1. 사용자가 `알림 받기` 스위치를 켠다.
2. 빠른 선택 버튼으로 `1일 전`, `2일 전`, `30일 전` 중 하나를 누른다.
3. 프론트가 `remindDate`를 계산해 채운다.
4. 시간은 time picker로 받는다.
5. 서버에 `notificationEnabled`, `remindDate`, `remindTime`를 보낸다.

예:

- 마감일 `2026-06-14`
- `1일 전` 선택
- `remindDate = 2026-06-13`
- 시간 `09:00`

최종 요청:

```json
{
  "notificationEnabled": true,
  "remindDate": "2026-06-13",
  "remindTime": "09:00"
}
```

### 5.5 알림 off

알림 끄기는 아래처럼 단순하게 처리하면 된다.

```json
{
  "notificationEnabled": false
}
```

서버는 기존 날짜/시간과 pending 상태를 제거한다.

## 6. 프론트가 알아야 하는 제약

### 6.1 아직 없는 것

- 일반 대회 목록 응답의 `favorited`
- 대회 1건당 다중 리마인드
- 실제 마감 시각(`registrationDeadlineAt`)

즉 현재 프론트는:

- 찜 여부는 별도 찜 목록 기준으로 관리
- 리마인드는 1개만 관리
- 마감 기준 문구는 “앱에 저장된 마감일 기준”으로 안내

### 6.2 전역 푸시 설정

사용자 전역 푸시를 꺼도:

- 리마인드 설정 저장은 가능
- `Notification` 알림함 저장은 유지
- 다만 실제 FCM 푸시는 발송 대상에서 제외될 수 있음

프론트 의미:

- 리마인드 스위치와 사용자 전역 푸시 스위치는 다른 개념이다.
- 가능하면 리마인드 화면에서 “기기/계정 푸시 설정이 꺼져 있으면 실제 푸시 수신은 안 될 수 있음” 정도의 안내를 둘 수 있다.

## 7. 프론트 작업 순서 권장

1. 목록/상세 모델에 `region` 추가
2. 지역 필터 UI 추가
3. 찜 버튼 + 찜 해제 연결
4. 찜 목록 화면 연결
5. 리마인드 on/off + 날짜/시간 선택 연결
6. 전역 푸시 설정 UX와 문구 점검

## 8. 참고 문서

- 상세 계약: [tournament.md](tournament.md)
- 공통 모델/알림/푸시 정책: [shared/common-models.md](shared/common-models.md)

# 오픈매트/대회 공유 기능 기획안

작성일: 2026-05-05  
대상: Rolling Flutter 앱, Rolling API, 딥링크/App Link 설정  
범위: 오픈매트와 대회 상세 공유, Flutter 앱 상세 화면 진입, 앱 미설치 fallback

## 1. 배경

오픈매트와 대회 정보는 지인, 팀원, 체육관 단톡방, 인스타 DM 등으로 자주 공유된다. 현재 상세 페이지에 명확한 공유 버튼이 없으면 사용자가 제목, 일시, 장소, 링크를 직접 복사해야 한다.

공유 기능의 목적은 React 관리자 페이지나 웹 상세 페이지를 보여주는 것이 아니다. 사용자 앱은 Flutter이므로 공유 링크를 누른 사용자가 Flutter 앱의 해당 상세 화면으로 이동하는 것이 1차 목표다.

## 2. 목표

- 사용자가 오픈매트와 대회 상세를 1번의 행동으로 공유한다.
- 공유받은 사용자가 앱 설치 상태라면 Flutter 앱의 해당 상세 화면으로 바로 이동한다.
- 앱이 설치되어 있지 않다면 앱 설치 안내 또는 Google Play fallback으로 연결한다.
- `api.rolling-app.com` API URL을 직접 공유하지 않는다.

## 3. 공유 링크 정책

공유 링크는 사람이 누르는 HTTPS 링크이며, 백엔드 JSON API 주소가 아니다.

사용자에게 공유되는 링크:

- `https://rolling-app.com/open-mats/{id}`
- `https://rolling-app.com/tournaments/{id}`

Flutter 앱이 내부에서 호출하는 API:

- `GET https://api.rolling-app.com/api/v1/open-mats/{id}`
- `GET https://api.rolling-app.com/api/v1/tournaments/{id}`

`https://api.rolling-app.com/api/v1/tournaments/{id}` 같은 API URL은 JSON 응답을 돌려주는 주소이므로 카카오톡/문자 공유 링크로 직접 쓰지 않는다. 직접 공유하면 Flutter 앱 라우팅, 앱 설치 fallback, Open Graph 미리보기를 제공하기 어렵다.

## 4. 사용자 흐름

### 앱 설치 사용자

```text
[공유받은 사용자]
        |
        | 공유 링크 클릭
        v
https://rolling-app.com/open-mats/123
        |
        v
Android App Link / iOS Universal Link
        |
        v
Flutter 앱 실행
        |
        v
딥링크 id = 123 파싱
        |
        v
GET https://api.rolling-app.com/api/v1/open-mats/123
        |
        v
Flutter 오픈매트 상세 화면 렌더링
```

### 앱 미설치 사용자

```text
[공유받은 사용자]
        |
        | 공유 링크 클릭
        v
https://rolling-app.com/open-mats/123
        |
        v
앱 미설치로 App Link / Universal Link 미동작
        |
        v
앱 설치 안내 또는 Google Play fallback
```

MVP에서는 웹에서 상세 전체를 구현하지 않는다. 핵심은 Flutter 앱 상세 진입이다.

현재 Android 앱은 Google Play에 등록되어 있다.

- Google Play: `https://play.google.com/store/apps/details?id=com.rolling.jiujits`

iOS App Store 주소는 출시 후 별도로 추가한다.

## 5. 공유 문구

오픈매트:

```text
[Rolling] 주말 오픈매트
일시: 2026-05-10 10:00
장소: 롤링 주짓수 아카데미
주소: 서울시 강남구 역삼동 123-45
상세 보기: https://rolling-app.com/open-mats/123
```

대회:

```text
[Rolling] 제5회 롤링컵
대회일: 2026-06-20
접수마감: 2026-06-01
장소: 서울 올림픽공원 체조경기장
상세 보기: https://rolling-app.com/tournaments/77
```

## 6. 구현 방향

### Flutter 앱

- 오픈매트 상세 화면에 공유 버튼을 추가한다.
- 대회 상세 화면에 공유 버튼을 추가한다.
- OS 기본 share sheet를 사용한다.
- 공유 텍스트는 앱에서 생성한다.
- `https://rolling-app.com/open-mats/{id}` 딥링크를 수신한다.
- `https://rolling-app.com/tournaments/{id}` 딥링크를 수신한다.
- 딥링크의 `{id}`를 파싱해 Flutter 상세 화면으로 라우팅한다.
- 상세 화면 진입 후 `api.rolling-app.com/api/v1` API로 데이터를 조회한다.

### 백엔드/API

- 기존 공개 상세 API를 Flutter 딥링크 진입 후에도 사용할 수 있게 유지한다.
- 오픈매트 상세 API: `GET /api/v1/open-mats/{id}`
- 대회 상세 API: `GET /api/v1/tournaments/{id}`
- 숨김/삭제 오픈매트 응답 정책을 Flutter 상세 화면과 맞춘다.
- 공유 문구 생성에 필요한 필드가 응답에 포함되어 있는지 확인한다.

### App Link / Universal Link

- Android App Link 설정을 추가한다.
- iOS Universal Link 설정을 추가한다.
- `rolling-app.com/open-mats/{id}`와 `rolling-app.com/tournaments/{id}`가 앱으로 연결되도록 설정한다.
- Android 앱 미설치 사용자는 Google Play `https://play.google.com/store/apps/details?id=com.rolling.jiujits`로 연결한다.
- iOS 앱 미설치 사용자는 App Store 출시 전까지 별도 fallback 정책을 결정한다.
- 가능하면 공유 링크의 Open Graph title, description, image를 제공한다.

## 7. Phase별 체크리스트

### Phase 1. 공유 기능 출시

공통 결정:

- [x] 앱 화면 문구를 `공유`로 확정한다.
- [x] 오픈매트 공유 문구 형식을 확정한다.
- [x] 대회 공유 문구 형식을 확정한다.
- [x] 공유 가능한 상태를 확정한다.
- [x] 삭제/숨김 오픈매트 공유 링크 접근 시 표시 정책을 확정한다.
- [x] 공유 링크는 `https://rolling-app.com/open-mats/{id}` 형식으로 확정한다.
- [x] 공유 링크는 `https://rolling-app.com/tournaments/{id}` 형식으로 확정한다.
- [x] 공유 링크의 1차 목적은 Flutter 앱 상세 화면 진입으로 확정한다.
- [x] React 관리자 페이지는 사용자 공유 흐름에서 제외한다.

프론트가 할 일:

- [ ] Flutter 오픈매트 상세 화면에 공유 버튼을 추가한다.
- [ ] Flutter 대회 상세 화면에 공유 버튼을 추가한다.
- [ ] Flutter에서 OS 기본 share sheet를 연동한다.
- [ ] Flutter 앱에서 `https://rolling-app.com/open-mats/{id}` 딥링크를 수신한다.
- [ ] Flutter 앱에서 `https://rolling-app.com/tournaments/{id}` 딥링크를 수신한다.
- [ ] Flutter 앱이 딥링크의 `{id}`를 파싱해 오픈매트 상세 화면으로 라우팅한다.
- [ ] Flutter 앱이 딥링크의 `{id}`를 파싱해 대회 상세 화면으로 라우팅한다.
- [ ] Android App Link 설정을 추가한다.
- [ ] iOS Universal Link 설정을 추가한다.
- [ ] Android 앱 미설치 사용자는 `https://play.google.com/store/apps/details?id=com.rolling.jiujits`로 이동하게 한다.
- [ ] iOS 앱 미설치 사용자는 App Store 출시 전까지 fallback 정책을 결정한다.

백엔드가 할 일:

- [x] 오픈매트 상세 API 접근 정책을 확인한다.
- [x] 대회 상세 API 접근 정책을 확인한다.
- [x] 현재 오픈매트 상세 API 경로가 `GET https://api.rolling-app.com/api/v1/open-mats/{id}`임을 문서화한다.
- [x] 현재 대회 상세 API 경로가 `GET https://api.rolling-app.com/api/v1/tournaments/{id}`임을 문서화한다.
- [x] 숨김/삭제 오픈매트 응답 정책을 Flutter 상세 화면과 맞춘다.
- [x] 공유 문구 생성에 필요한 오픈매트 필드가 응답에 모두 포함되어 있는지 확인한다.
- [x] 공유 문구 생성에 필요한 대회 필드가 응답에 모두 포함되어 있는지 확인한다.

백엔드 진행 메모:

- `GET /api/v1/open-mats/{id}`와 `GET /api/v1/tournaments/{id}`는 Spring Security 설정상 비로그인 접근 가능 공개 상세 API다.
- 로그인 사용자가 `Authorization` 헤더를 함께 보내면 차단한 작성자의 오픈매트/대회 상세는 `NOT_FOUND`로 처리된다.
- 삭제된 오픈매트는 비로그인 공유 링크 진입 사용자에게 `NOT_FOUND`이며, 호스트/신청자/삭제 알림 수신자/관리자에게만 `deleted=true`, `deletedAt` 포함 상세 응답을 반환한다.
- 오픈매트 공유 문구 필드인 `title`, `startDateTime`, `locationName`, `address`는 `OpenMatResponse` 상세 응답에 포함되어 있다.
- 대회 공유 문구 필드인 `title`, `competitionDate`, `registrationDeadline`, `location`은 `TournamentResponse` 상세 응답에 포함되어 있다.
- 공유 링크 정책은 `AGENTS.md`와 `C:\rolling\.codex-shared\domain-models.md`에 동기화했다.

검증:

- [ ] 앱에서 오픈매트 공유 버튼을 누르면 공유창이 열린다.
- [ ] 앱에서 대회 공유 버튼을 누르면 공유창이 열린다.
- [ ] 앱 설치 사용자가 공유받은 오픈매트 링크를 누르면 Flutter 오픈매트 상세로 열린다.
- [ ] 앱 설치 사용자가 공유받은 대회 링크를 누르면 Flutter 대회 상세로 열린다.
- [ ] 공유 링크가 React 관리자 페이지로 이동하지 않는다.
- [ ] Android 앱 미설치 사용자는 Google Play `https://play.google.com/store/apps/details?id=com.rolling.jiujits`로 이동한다.
- [ ] iOS 앱 미설치 사용자는 정해진 fallback 정책대로 이동한다.
- [ ] 카카오톡/문자에서 공유 미리보기가 깨지지 않는다.

## 8. 후속 검토

- 공유 클릭 이벤트 수집 여부
- 오픈매트와 대회 공유 전환 지표 분리
- Open Graph 동적 미리보기 제공 방식
- 앱 미설치 사용자의 상세 미리보기 제공 여부

# 오픈매트 지도 기능 기획안

작성일: 2026-05-05
대상: Rolling Flutter 앱, Rolling API, 관리자 웹
범위: 오픈매트 상세 지도 미리보기, 오픈매트 생성/수정 시 우편번호 WebView 주소 선택 및 좌표 저장

## 1. 배경

오픈매트는 실제 장소로 이동해야 하는 오프라인 이벤트다. 현재 오픈매트 장소 정보는 문자열 장소명과 주소에 의존하므로, 사용자는 주소를 복사해서 별도 지도 앱에서 검색해야 한다.

MVP에서는 대회 지도는 제외하고 오픈매트에만 지도 기능을 적용한다. 대회는 크롤링 또는 포스터 기반 장소 문자열 품질이 일정하지 않으므로, 장소 데이터 정규화 이후 후속으로 검토한다.

## 2. 목표

- 사용자가 오픈매트 상세 화면에서 위치를 즉시 이해한다.
- 오픈매트 생성/수정 시 사용자가 카카오/다음 우편번호 WebView에서 주소를 선택한다.
- 선택한 주소를 좌표로 변환하고 위도/경도를 서버에 저장한다.
- 상세 화면은 저장된 좌표로 카카오맵 WebView 지도 미리보기를 표시한다.
- 좌표가 없는 기존 오픈매트도 화면이 깨지지 않는다.

## 3. 범위

### 지금 출시할 범위

1. Flutter 오픈매트 생성/수정 화면에 주소 검색 버튼 추가
2. 주소 검색 버튼 클릭 시 카카오/다음 우편번호 WebView 표시
3. 사용자가 WebView에서 주소 선택
4. 선택한 주소를 백엔드 geocoding API로 위도/경도 변환
5. 사용자가 장소명을 별도 입력
6. Flutter가 장소명, 주소, 위도, 경도를 저장 요청에 포함
7. 오픈매트 상세 화면에서 저장 좌표로 카카오맵 WebView 지도 미리보기 표시
8. 카카오맵 앱 또는 카카오맵 웹 열기
9. 좌표 없는 오픈매트 fallback 처리

### 제외 범위

1. 대회 상세 지도 표시
2. 대회 좌표 저장
3. 목록 화면 지도 탐색
4. 내 위치 기준 거리순 정렬
5. 반경 검색
6. 자체 길찾기
7. 실시간 교통/대중교통 경로 계산
8. 사용자 위치 상시 수집
9. MVP의 수동 핀 이동

## 4. 사용자 흐름

### 오픈매트 생성/수정

```text
오픈매트 생성/수정 화면
        |
        | 주소 검색 버튼 클릭
        v
카카오/다음 우편번호 WebView
        |
        | 사용자가 주소 선택
        v
주소 문자열 확보
        |
        | 선택 주소로 백엔드 geocoding API 호출
        v
주소 + 위도 + 경도 확보
        |
        | 장소명 별도 입력
        v
지도 미리보기 확인
        |
        v
POST/PUT API로 서버 저장
```

좌표 변환이 실패해도 주소와 장소명은 유지한다. 이 경우 사용자는 좌표 없이 오픈매트를 생성/수정할 수 있다.

### 오픈매트 상세

```text
오픈매트 상세 화면
        |
        | 서버 응답에 latitude/longitude 있음
        v
카카오맵 WebView 지도 미리보기 + 마커 표시
        |
        | 지도에서 보기 클릭
        v
카카오맵 앱 또는 카카오맵 웹 fallback
```

좌표가 없으면 기존처럼 장소명과 주소만 표시한다.

## 5. UI 요구사항

### 생성/수정 화면

- 주소 검색 버튼
- 카카오/다음 우편번호 WebView
- 선택한 주소 표시
- 장소명 입력 필드
- 좌표 변환 성공 시 지도 미리보기
- 좌표 변환 실패 시 주소는 유지하고 좌표 없이 저장 가능하다는 안내
- 선택한 장소명, 주소, 위도, 경도 저장
- MVP에서는 수동 핀 이동 제외

중요: 사용자가 직접 입력한 자유 텍스트를 즉시 좌표 변환하지 않는다. 우편번호 WebView에서 선택되어 반환된 주소를 기준으로 좌표 변환을 수행한다.

### 상세 화면

- 장소명
- 주소
- 좌표가 있으면 카카오맵 WebView 지도 미리보기
- 좌표가 없으면 지도 없이 장소명/주소만 표시
- 지도에서 보기
- 주소 복사

## 6. 백엔드 변경 방향

### 데이터 모델

`open_mats`에 좌표 필드를 추가한다.

MVP 필수:

- `latitude DECIMAL(10, 7)` nullable
- `longitude DECIMAL(10, 7)` nullable

후속 검토:

- `map_provider VARCHAR(30)`
- `map_place_id VARCHAR(100)`
- `geocoded_at TIMESTAMP(6)`
- `geocode_status VARCHAR(30)`

### 요청 DTO

`OpenMatCreateRequest`, `OpenMatUpdateRequest`에 추가:

```json
{
  "latitude": 37.5012345,
  "longitude": 127.0398765
}
```

Flutter는 사용자가 우편번호 WebView에서 주소를 선택하고 좌표 변환이 성공하면 `locationName`, `address`, `latitude`, `longitude`를 함께 보낸다.

좌표 변환이 실패하거나 사용자가 좌표 없이 저장하는 경우에는 `locationName`, `address`만 보낼 수 있다.

### 응답 DTO

`OpenMatResponse`에 추가:

```json
{
  "latitude": 37.5012345,
  "longitude": 127.0398765
}
```

### Geocoding API

카카오 REST API Key는 Flutter 앱에 직접 포함하지 않는다. 좌표 변환은 백엔드 geocoding endpoint가 카카오 Local API를 호출하는 방식으로 처리한다.

예상 API:

```text
GET /api/v1/maps/kakao/geocode?address={선택주소}
```

예상 응답:

```json
{
  "address": "경남 창녕군 창녕읍 종로 2",
  "latitude": 35.5412345,
  "longitude": 128.4912345
}
```

카카오 Local API 응답 기준:

- `x`는 경도(`longitude`)
- `y`는 위도(`latitude`)

#### 인증 정책

Geocoding API는 인증이 필요하다.

이유:

- 카카오 REST API Key는 백엔드가 보호해야 하는 값이다.
- 인증 없는 공개 endpoint로 열면 외부 호출자가 주소 좌표 변환 API를 남용할 수 있다.
- 오픈매트 생성/수정 자체가 로그인 사용자 흐름이므로, geocoding도 동일한 인증 경계 안에 두는 것이 자연스럽다.

정책:

- `GET /api/v1/maps/kakao/geocode`는 Bearer access token을 요구한다.
- 토큰이 없거나 유효하지 않으면 `401 UNAUTHORIZED`를 반환한다.
- 계정 이용 제한 정책이 적용 중인 사용자는 서버의 기존 제한 정책을 따른다.

#### Geocoding 실패 응답 정책

Geocoding 실패는 오픈매트 생성/수정 자체를 막지 않는다. Flutter는 실패 메시지를 보여주고, 사용자가 좌표 없이 저장할 수 있게 한다.

백엔드는 실패 원인을 아래 코드로 구분한다.

```json
{
  "success": false,
  "error": {
    "code": "GEOCODE_NOT_FOUND",
    "message": "선택한 주소의 좌표를 찾지 못했습니다."
  }
}
```

권장 에러 코드:

- `VALIDATION_ERROR`: `address`가 비어 있거나 너무 짧은 경우, HTTP 400
- `GEOCODE_NOT_FOUND`: 카카오 Local API 결과가 없는 경우, HTTP 404
- `KAKAO_GEOCODE_FAILED`: 카카오 API가 4xx/5xx를 반환한 경우, HTTP 502
- `KAKAO_GEOCODE_TIMEOUT`: 카카오 API timeout, HTTP 504
- `KAKAO_GEOCODE_RATE_LIMITED`: 카카오 API quota/rate limit, HTTP 429

Flutter 처리:

- 성공하면 `latitude`, `longitude`를 화면 상태에 보관한다.
- 실패하면 주소는 유지하고 `latitude`, `longitude`는 비운다.
- 실패해도 사용자는 오픈매트를 좌표 없이 생성/수정할 수 있다.

#### 오픈매트 수정 시 좌표 null 처리 정책

`OpenMatUpdateRequest`에서 좌표는 세 가지 상태를 구분한다.

1. `latitude`, `longitude`가 모두 숫자로 전달됨
  - 서버는 기존 좌표를 새 좌표로 교체한다.

2. `latitude`, `longitude`가 둘 다 `null`로 전달됨
  - 서버는 기존 좌표를 제거한다.
  - 사용자가 주소를 새로 선택했지만 geocoding에 실패했거나, 좌표 없는 주소 상태로 저장하는 경우에 사용한다.

3. `latitude`, `longitude` 필드가 요청 body에 없음
  - 서버는 기존 좌표를 변경하지 않는다.
  - 주소/장소명 외 다른 필드만 수정하는 경우에 사용한다.

검증 정책:

- `latitude`만 있고 `longitude`가 없거나, 반대로 `longitude`만 있으면 `400 VALIDATION_ERROR`를 반환한다.
- 좌표를 저장하는 경우 `latitude`는 -90부터 90, `longitude`는 -180부터 180 범위를 검증한다.
- 생성 요청에서는 좌표가 둘 다 없으면 좌표 없이 생성한다.
- 생성 요청에서 둘 중 하나만 있으면 `400 VALIDATION_ERROR`를 반환한다.

### 검증

- `latitude`는 -90부터 90 사이여야 한다.
- `longitude`는 -180부터 180 사이여야 한다.
- 기존 데이터 호환을 위해 좌표는 nullable로 시작한다.
- 서버는 선택된 주소 기반 geocoding endpoint를 제공한다.
- 서버는 카카오 Local API 호출을 위해 `KAKAO_REST_API_KEY`를 환경변수로 관리한다.

## 7. 프론트엔드 변경 방향

### Flutter 앱

- 오픈매트 생성 화면에 주소 검색 버튼을 추가한다.
- 주소 검색 버튼 클릭 시 카카오/다음 우편번호 WebView를 표시한다.
- 사용자가 WebView에서 주소를 선택할 수 있게 한다.
- WebView에서 반환된 주소를 화면에 표시한다.
- 장소명은 체육관/도장 이름으로 사용자가 별도 입력한다.
- 선택된 주소를 백엔드 geocoding endpoint로 전달해 위도/경도를 받는다.
- 좌표 변환 성공 시 지도 미리보기를 표시한다.
- 좌표 변환 실패 시 주소는 유지하고 좌표 없이 저장 가능하다는 안내를 표시한다.
- 오픈매트 수정 화면에서도 동일한 주소 선택 및 좌표 변환 흐름을 제공한다.
- 오픈매트 상세 화면에서 저장된 위도/경도로 카카오맵 WebView 지도 미리보기를 표시한다.
- 좌표가 없으면 기존 장소명/주소만 표시한다.
- `지도에서 보기` 버튼을 추가한다.
- 카카오맵 앱이 없을 때 카카오맵 웹 fallback이 동작하도록 한다.

현재 Flutter 구현 상태:

- 우편번호 WebView는 앱 내부 HTML에서 카카오/다음 우편번호 스크립트를 로드한다.
- 우편번호 WebView는 `RollingPostcode` JavaScriptChannel로 선택 주소를 Flutter에 반환한다.
- Flutter는 반환된 주소로 `GET /api/v1/maps/kakao/geocode`를 호출한다.
- 백엔드 geocoding endpoint가 없거나 실패하면 좌표 없이 저장할 수 있도록 안내한다.
- 상세 지도 미리보기는 `KAKAO_MAP_WEBVIEW_BASE_URL`을 사용한다.

### React 관리자 웹

React 관리자 웹은 운영자 전용이다. 사용자 공유 흐름에는 포함하지 않는다.

관리자 기능으로는 후속 또는 선택적으로 아래를 검토한다.

- 오픈매트 관리 상세에서 좌표 유무 표시
- 운영자가 오픈매트 장소를 검색하고 좌표를 보정하는 UI

## 8. 지도 연동 참고

MVP 지도 provider는 카카오맵으로 확정한다.

카카오맵 적용 원칙:

- 생성/수정 화면의 주소 검색은 Flutter WebView에서 카카오/다음 우편번호 서비스를 사용한다.
- Flutter는 우편번호 WebView에서 반환된 주소 문자열을 받는다.
- Flutter는 반환된 주소를 백엔드 geocoding endpoint에 전달한다.
- 백엔드는 카카오 Local API 주소 검색 결과의 `x`를 경도(`longitude`), `y`를 위도(`latitude`)로 변환해 Flutter에 응답한다.
- 카카오 REST API Key는 백엔드 환경변수로만 관리하고 Flutter 앱에는 포함하지 않는다.
- 상세 화면 지도 미리보기는 저장된 위도/경도를 사용해 카카오맵을 WebView로 표시한다.
- `지도에서 보기`는 카카오맵 앱 딥링크를 우선 시도하고, 앱이 없으면 카카오맵 웹 URL로 fallback한다.

공통 원칙:

- 상세 화면은 저장된 좌표를 사용한다.
- 매번 상세 조회 때 주소를 좌표로 변환하지 않는다.
- 좌표 변환은 생성/수정 시점에 사용자가 우편번호 WebView에서 주소를 선택한 뒤 1회 수행한다.
- Flutter 네이티브 지도 SDK 대신 WebView 기반 카카오맵 미리보기를 우선 적용한다.

## 9. Phase별 체크리스트

### Phase 1. 오픈매트 지도 MVP

#### 1-A. 외부 설정

- [ ] Kakao Developers 앱에서 JavaScript Key를 확인한다.
- [ ] Kakao Developers 앱에서 REST API Key를 확인한다.
- [ ] JavaScript SDK 도메인에 `https://rolling-app.com`을 등록한다.
- [ ] 개발 테스트가 필요하면 JavaScript SDK 도메인에 로컬 도메인을 추가한다.
  예: `http://localhost:3000`, `http://localhost:8080`
- [ ] 백엔드 운영 환경변수에 `KAKAO_REST_API_KEY`를 등록한다.
- [ ] 백엔드 개발/스테이징 환경변수에도 `KAKAO_REST_API_KEY`를 등록한다.
- [ ] 랜딩 도메인에 카카오맵 WebView 페이지를 배포할 경로를 확정한다.
- [x] 카카오맵 WebView 페이지 경로를 `https://rolling-app.com/maps/kakao/openmat.html`로 확정한다.
- [ ] 카카오맵 WebView 페이지에서 JavaScript Key를 사용해 좌표 기준 마커를 표시할 수 있게 준비한다.
- [x] Flutter 내 WebView에서 JavaScriptChannel로 주소를 반환하는 방식으로 확정한다.

#### 1-B. 공통 정책 결정

- [x] 오픈매트 지도 기능은 대회와 분리해 오픈매트에만 적용한다.
- [x] 주소 선택은 자유 텍스트 자동 변환이 아니라 우편번호 WebView 선택 방식으로 확정한다.
- [x] 좌표가 없는 기존 오픈매트의 표시 정책을 확정한다.
- [x] 좌표가 없어도 오픈매트 생성은 가능하게 한다.
- [x] 좌표가 없으면 상세 화면에서 WebView 지도를 띄우지 않는다.
- [x] 좌표가 있으면 WebView 지도에 좌표 기준 마커를 표시한다.
- [x] 지도 provider를 카카오맵으로 확정한다.
- [x] 좌표 변환 실패 시 사용자 안내 문구를 확정한다.
- [x] 우편번호 WebView에서 받은 주소와 사용자가 입력한 장소명을 함께 저장하는 정책을 확정한다.

#### 1-C. 백엔드

- [x] Kakao Developers에서 REST API Key를 확인한다.
- [x] 로컬 개발 환경에 `KAKAO_REST_API_KEY`를 등록한다.
- [x] 운영 서버 환경변수에 `KAKAO_REST_API_KEY`를 등록한다.
- [x] 스테이징 환경이 있으면 스테이징 환경변수에도 `KAKAO_REST_API_KEY`를 등록한다.
- [x] `open_mats` 테이블에 `latitude` 컬럼을 추가한다.
- [x] `open_mats` 테이블에 `longitude` 컬럼을 추가한다.
- [x] `OpenMatCreateRequest`에 `latitude`, `longitude`를 추가한다.
- [x] `OpenMatUpdateRequest`에 `latitude`, `longitude`를 추가한다.
- [x] `OpenMatResponse`에 `latitude`, `longitude`를 추가한다.
- [x] `KAKAO_REST_API_KEY` 환경변수를 읽는 설정 클래스를 추가한다.
- [x] 카카오 Local API 호출용 HTTP client 또는 service를 추가한다.
- [x] 주소 기반 geocoding endpoint `GET /api/v1/maps/kakao/geocode?address={address}`를 추가한다.
- [x] geocoding endpoint는 인증이 필요한 것으로 결정한다.
- [x] 수정 요청에서 좌표는 숫자/둘 다 null/필드 없음의 3가지 상태로 처리하는 것으로 결정한다.
- [x] geocoding 실패 응답 코드를 정의한다.
- [x] geocoding endpoint 요청 파라미터 `address`가 비어 있으면 `400 VALIDATION_ERROR`를 반환한다.
- [x] geocoding endpoint에서 카카오 Local API 주소 검색을 호출한다.
  요청 예: `GET https://dapi.kakao.com/v2/local/search/address.json?query={address}`
- [x] 카카오 Local API 요청 헤더에 `Authorization: KakaoAK {KAKAO_REST_API_KEY}`를 포함한다.
- [x] 카카오 Local API 응답의 첫 번째 문서에서 `x`를 `longitude`, `y`를 `latitude`로 매핑한다.
- [x] 카카오 Local API 결과가 없으면 `404 GEOCODE_NOT_FOUND`를 반환한다.
- [x] 카카오 Local API 호출 실패는 `502 KAKAO_GEOCODE_FAILED`로 변환한다.
- [x] 카카오 Local API timeout은 `504 KAKAO_GEOCODE_TIMEOUT`으로 변환한다.
- [x] 카카오 Local API quota/rate limit은 `KAKAO_GEOCODE_RATE_LIMITED`로 변환한다.
- [x] geocoding 응답 DTO를 정의한다.
  예: `address`, `latitude`, `longitude`
- [x] geocoding endpoint에 Bearer 인증을 적용한다.
- [x] 서버에서 `latitude` 범위가 -90부터 90 사이인지 검증한다.
- [x] 서버에서 `longitude` 범위가 -180부터 180 사이인지 검증한다.
- [x] 생성/수정 요청에서 `latitude`, `longitude` 중 하나만 있으면 `400 VALIDATION_ERROR`를 반환한다.
- [x] 수정 요청에서 `latitude`, `longitude`가 둘 다 `null`이면 기존 좌표를 제거한다.
- [x] 수정 요청에서 좌표 필드가 없으면 기존 좌표를 유지한다.
- [x] 좌표가 누락된 기존 데이터가 조회 시 오류를 만들지 않도록 nullable 정책을 반영한다.
- [x] 오픈매트 생성 service 테스트에 좌표 저장 케이스를 추가한다.
- [x] 오픈매트 수정 service 테스트에 좌표 갱신 케이스를 추가한다.
- [x] 오픈매트 조회 controller/service 테스트에 좌표 응답 케이스를 추가한다.
- [x] geocoding endpoint 성공 테스트를 추가한다.
- [x] geocoding endpoint 결과 없음 테스트를 추가한다.
- [x] geocoding endpoint 카카오 API 실패 테스트를 추가한다.
- [ ] 실제 주소 예시로 geocoding endpoint를 수동 검증한다.
  예: `창녕군 창녕읍 종로 2`
- [ ] Flutter에서 주소 선택 후 geocoding endpoint가 호출되는지 서버 로그로 확인한다.
- [ ] 오픈매트 등록 요청 body에 `latitude`, `longitude`가 포함되는지 서버 로그 또는 테스트로 확인한다.

#### 1-D. Flutter 생성/수정

- [x] Flutter 오픈매트 생성 화면에 주소 검색 버튼을 추가한다.
- [x] 주소 검색 버튼 클릭 시 카카오/다음 우편번호 WebView를 표시한다.
- [x] WebView에서 사용자가 주소를 선택하면 Flutter로 주소 문자열을 반환받는다.
- [x] 반환된 주소를 화면의 주소 필드에 표시한다.
- [x] 장소명은 체육관/도장 이름으로 별도 입력할 수 있게 유지한다.
- [x] 반환된 주소로 백엔드 geocoding endpoint를 호출해 위도/경도를 받는다.
- [x] 좌표 변환 성공 시 지도 미리보기를 표시한다.
- [x] 좌표 변환 실패 시 주소는 유지하고 좌표 없이 저장 가능하다는 안내를 표시한다.
- [x] 선택 주소와 좌표를 저장 요청에 포함한다.
- [x] Flutter 오픈매트 수정 화면에서도 동일한 주소 선택 및 좌표 변환 흐름을 제공한다.
- [x] 수정 화면에서 기존 좌표가 있는 오픈매트를 열면 지도 미리보기를 표시한다.
- [x] 수정 화면에서 주소를 새로 선택하면 기존 좌표를 새 좌표로 갱신한다.
- [x] 수정 화면에서 좌표 변환 실패 시 좌표 없이 저장할 수 있게 처리한다.

#### 1-E. Flutter 상세

- [x] Flutter 오픈매트 상세 화면에서 저장된 위도/경도로 카카오맵 WebView 지도 미리보기를 표시한다.
- [x] 좌표가 없으면 기존 장소명/주소만 표시한다.
- [x] `지도에서 보기` 버튼을 추가한다.
- [x] 카카오맵 앱이 없을 때 카카오맵 웹 fallback이 동작하도록 한다.
- [x] 주소 복사 버튼을 추가한다.

#### 1-F. 검증

- [ ] 신규 오픈매트 생성 시 우편번호 WebView에서 주소를 선택할 수 있다.
- [ ] 선택한 주소가 Flutter 화면에 표시된다.
- [ ] 선택한 주소로 geocoding endpoint가 호출된다.
- [ ] geocoding 성공 시 위도/경도가 저장 요청에 포함된다.
- [ ] geocoding 실패 시 좌표 없이 오픈매트를 생성할 수 있다.
- [ ] 오픈매트 수정 시 주소와 좌표가 함께 갱신된다.
- [ ] 오픈매트 상세에서 지도 마커가 저장된 좌표에 표시된다.
- [ ] 좌표 없는 오픈매트 상세가 깨지지 않는다.
- [ ] 카카오맵 앱/웹 열기가 정상 동작한다.
- [ ] 잘못된 좌표 범위는 서버에서 거부된다.
- [ ] Android 실기기에서 우편번호 WebView 주소 반환이 동작한다.
- [ ] iOS 실기기에서 우편번호 WebView 주소 반환이 동작한다.
- [ ] Android 실기기에서 카카오맵 앱 fallback 또는 웹 fallback이 동작한다.
- [ ] iOS 실기기에서 카카오맵 앱 fallback 또는 웹 fallback이 동작한다.

#### 1-G. 관리자 웹

- [ ] React 관리자 웹에서 좌표 유무 표시 또는 보정 UI 필요 여부를 결정한다.

## 10. 후속 검토

- 대회 장소 정규화
- 대회 지도 도입 여부
- 지도 목록 탐색
- 거리순 정렬
- 반경 검색
- 관리자 좌표 보정 UI

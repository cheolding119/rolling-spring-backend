# 프로필 벨트/그랄 확장 기획서

## 1. 문서 목적

- 이 문서는 2026-06-01 기준으로 운영 중인 `rolling-api`에 사용자 프로필의 `그랄` 개념을 안전하게 추가하기 위한 제품/백엔드 기준 문서다.
- 목표는 현재 운영 중인 앱과 백엔드 동작을 깨지 않으면서, 다음 앱 버전에서 `벨트 + 그랄` 선택 경험을 지원하는 것이다.
- 이 문서는 우선 `내 정보`, `오픈매트 참가자 조회`, `훈련 기록 PROMOTION` 범위를 중심으로 정리하고, 후속 노출 범위는 단계적으로 확장한다.

## 2. 현재 상태

### 2.1 현재 사용자 프로필 계약

- 현재 사용자 프로필은 `beltColor`만 가진다.
- `GET /api/v1/users/me` 응답에는 `beltColor`가 포함된다.
- `PUT /api/v1/users/me` 요청은 `nickname`, `affiliation`, `beltColor`만 수정할 수 있다.
- 로그인, 친구 검색, 친구 상세, 세미나 신청자, 오픈매트 참가자, 관리자 사용자 상세 등 여러 응답이 현재 `beltColor` 값에 의존한다.

### 2.2 현재 훈련 기록 계약

- `PROMOTION` 카테고리 기록은 이미 `beltColor`와 `stripeCount`를 가진다.
- 현재 구현은 최신 `PROMOTION` 기록을 기준으로 `User.beltColor`만 동기화한다.
- `PROMOTION.stripeCount`는 훈련일지 기록용 값이며, 이 문서 기준 사용자 프로필의 현재 `stripeCount`와는 별개로 다룬다.

### 2.3 현재 오픈매트 참가자 계약

- 오픈매트 참가자 조회 응답은 현재 참가자의 `beltColor`를 노출한다.
- 다음 앱 버전 요구사항에는 오픈매트 참가자 조회 시 `stripeCount`도 함께 보이는 범위가 포함된다.

## 3. 해결하려는 문제

- 다음 앱 버전에서는 사용자가 현재 벨트뿐 아니라 현재 그랄 수도 함께 관리하고 싶다.
- 하지만 운영 중인 구버전 앱은 `beltColor`만 알고 있으므로, 백엔드 계약을 파괴적으로 바꾸면 안 된다.
- `beltColor` enum 자체에 그랄 정보를 섞어 넣으면 기존 응답, 저장값, 클라이언트 enum 매핑, 검색/정렬/표시 로직이 모두 흔들릴 수 있다.

## 4. 제품 목표 

### 4.1 사용자 목표

- 사용자는 현재 등급 상태를 `벨트 + 그랄`로 표현할 수 있어야 한다.
- 구버전 앱 사용자는 기존과 동일하게 `벨트`만 보고 수정할 수 있어야 한다.
- 새 앱 사용자는 같은 사용자 정보를 `벨트 + 그랄`로 저장하고 다시 조회할 수 있어야 한다.

### 4.2 비즈니스/운영 목표

- 백엔드를 먼저 배포해도 운영 장애가 없어야 한다.
- 구버전 앱이 계속 정상 동작해야 한다.
- 새 필드 도입 후에도 기존 API raw value와 DB 저장값의 호환성이 유지돼야 한다.

## 5. 설계 원칙

### 5.1 파괴적 변경 금지

- 기존 `beltColor` enum raw value는 유지한다.
- 기존 request 필드는 제거하거나 의미를 바꾸지 않는다.
- 기존 response 필드는 이름, 타입, 의미를 유지한다.

### 5.2 그랄은 독립 필드로 추가

- `그랄`은 `beltColor`의 enum 확장이 아니라 별도 숫자 필드로 모델링한다.
- 백엔드/DB/API 명칭은 `stripeCount`로 통일한다.
- 앱 UI 문구는 사용자 친화적으로 `그랄`을 사용하더라도, 저장 계약은 `stripeCount`를 기준으로 유지한다.
- 즉 새 개념 이름을 `gral` 같은 새 필드로 만들지 않고, 이미 훈련 기록 문서와 구현에서 사용 중인 `stripeCount`를 공용 계약 명칭으로 사용한다.

### 5.3 운영 무중단 우선

- 새 컬럼은 nullable 추가를 기본으로 한다.
- 기존 데이터 일괄 변환이나 강제 backfill 없이 배포 가능해야 한다.
- 새 앱만 새 필드를 사용하도록 하고, 구버전 앱은 새 필드를 몰라도 동작해야 한다.

## 6. 권장안

### 6.1 지금 반영할 범위

- `users` 테이블에 `stripe_count` nullable 컬럼을 추가한다.
- `User` 엔티티에 `stripeCount` 필드를 추가한다.
- `GET /api/v1/users/me` 응답에 `stripeCount`를 추가한다.
- `PUT /api/v1/users/me` 요청에 optional `stripeCount`를 추가한다.
- `GET /api/v1/open-mats/{id}/participants` 응답에 참가자 `stripeCount`를 추가한다.
- `PROMOTION` 기록 request/response의 `stripeCount`는 기존 계약을 유지한다.
- 오픈매트 참가자 조회에서는 `beltColor + stripeCount`를 함께 노출한다.
- 그 외 `beltColor` 기반 기존 응답들은 우선 그대로 두고, 필요한 화면만 점진 확장한다.

### 6.2 지금 하지 않을 범위

- `beltColor` enum에 `WHITE_1`, `WHITE_2` 같은 값을 추가하지 않는다.
- 기존 사용자 데이터를 임의로 `0그랄`로 일괄 채우지 않는다.
- 친구 목록, 세미나, 관리자 화면 전부에 동시에 `stripeCount`를 노출하지 않는다.
- 벨트별 허용 그랄 최대치 같은 복잡한 정책 검증은 이번 1차 범위에 넣지 않는다.

## 7. 권장 데이터 모델

### 7.1 사용자 프로필

- 현재 등급 상태는 `beltColor + stripeCount` 조합으로 표현한다.
- `beltColor`는 필수다.
- `stripeCount`는 선택값이다.
- 초기 의미는 아래처럼 둔다.

| 값 | 의미 |
| --- | --- |
| `null` | 아직 입력하지 않음 |
| `0` | 입력했으며 현재 그랄이 0개 |
| `1 이상` | 입력한 그랄 수 |

### 7.2 훈련 기록 PROMOTION

- `PROMOTION` 기록은 계속 `beltColor`, `stripeCount`를 사용한다.
- 현재 구조를 재사용하므로, 새 앱의 승급 기록 작성 경험과 사용자 프로필 모델의 필드 명칭을 맞출 수 있다.
- 다만 `PROMOTION.stripeCount`는 훈련일지 기록용 값이고, 사용자 프로필의 현재 `stripeCount`와 자동 동기화하지 않는다.

### 7.3 오픈매트 참가자 표시

- 오픈매트 참가자 조회 응답의 현재 등급 표시는 `beltColor + stripeCount` 조합을 사용한다.
- 이때 `stripeCount` source field는 `User.stripeCount`다.
- 훈련일지 `PROMOTION.stripeCount`를 직접 읽어 참가자 응답에 노출하지 않는다.

## 8. Source Of Truth 권장안

### 권장 결정

- 사용자 프로필의 현재 `stripeCount` source of truth는 `User.stripeCount`다.
- 오픈매트 참가자 조회를 포함해 현재 사용자 상태를 보여주는 응답은 `User.stripeCount`를 사용한다.
- `PROMOTION.stripeCount`는 과거 승급/훈련일지 기록으로 유지하며, 사용자 프로필 `stripeCount`와 자동 동기화하지 않는다.

### 이유

- 현재 사용자 상태와 훈련일지 기록은 의미가 다르다.
- 사용자는 현재 그랄 수를 프로필에서 직접 관리할 수 있어야 하고, 훈련일지는 과거 특정 시점의 승급 기록으로 남길 수 있어야 한다.
- 따라서 같은 `stripeCount`라는 이름을 쓰더라도 두 값은 같은 source of truth를 공유하지 않는다.

## 9. API 확장 권장안

### 9.1 내 정보 조회

`GET /api/v1/users/me`

- 기존 필드는 유지한다.
- `stripeCount`를 response data에 추가한다.

예시:

```json
{
  "id": 1,
  "nickname": "rolling_user",
  "affiliation": "롤링짐 강남",
  "beltColor": "BLUE",
  "stripeCount": 2
}
```

### 9.2 내 정보 수정

`PUT /api/v1/users/me`

- 기존 필드는 유지한다.
- `stripeCount`를 optional request field로 추가한다.

예시:

```json
{
  "nickname": "rolling_user",
  "affiliation": "롤링짐 강남",
  "beltColor": "BLUE",
  "stripeCount": 2
}
```

검증 권장안:

- `stripeCount`는 `null` 또는 `0 이상`만 허용한다.
- `stripeCount`를 보내지 않으면 기존 값을 유지한다.
- `stripeCount`만 단독 수정하는 것도 허용한다.

### 9.3 오픈매트 참가자 조회

`GET /api/v1/open-mats/{id}/participants`

- 기존 참가자 응답 필드는 유지한다.
- 참가자 현재 상태 표시를 위해 `stripeCount`를 response data에 추가한다.

예시:

```json
{
  "id": 13,
  "nickname": "rolling_user",
  "beltColor": "BLUE",
  "stripeCount": 2
}
```

- 이 응답의 `stripeCount`는 `User.stripeCount`를 사용한다.
- `PROMOTION` 기록의 최신 `stripeCount`를 조회해서 계산하지 않는다.

### 9.4 하위 호환성

- 구버전 앱은 `stripeCount`를 보내지 않아도 된다.
- 구버전 앱은 응답에 `stripeCount`가 추가돼도 무시하면 된다.
- 새 앱은 `stripeCount`가 `null`이면 “미설정” 상태로 표시한다.

## 10. 단계별 출시 권장안

### 10.1 1단계 - 백엔드 선배포

- DB migration으로 `users.stripe_count` nullable 컬럼 추가
- 엔티티/DTO/API 문서 확장
- 오픈매트 참가자 조회 응답 확장
- 기본 동작은 기존과 동일
- 운영 배포 후 구버전 앱 영향이 없는지 확인

### 10.2 2단계 - 새 앱 배포

- 앱에서 `벨트 + 그랄` 입력 UI 추가
- `GET /users/me`의 `stripeCount`를 읽고 표시
- `PUT /users/me`에 `stripeCount`를 포함해 저장
- `null` 처리 UX와 0개 표시 UX를 구분

### 10.3 3단계 - 후속 확장

- 친구 검색/친구 상세에 `stripeCount` 노출 여부 검토
- 세미나 신청자 프로필에 `stripeCount` 노출 여부 검토
- 관리자 사용자 상세에 `stripeCount` 노출 여부 검토
- `PROMOTION` 기록과 프로필의 `beltColor` 연동 정책 재정리 여부 검토

## 11. 리스크와 완화

### 11.1 가장 큰 리스크

- `beltColor` 의미를 바꾸거나 enum을 확장하는 경우 기존 앱 파싱, 기존 저장값, 운영 응답 호환성이 깨질 수 있다.

완화:

- `beltColor`는 절대 건드리지 않고 `stripeCount`만 추가한다.

### 11.2 source of truth 충돌

- 프로필 `stripeCount`와 `PROMOTION.stripeCount`를 같은 값처럼 취급하면 현재 상태와 과거 기록의 의미가 충돌한다.

완화:

- 프로필 `stripeCount`와 훈련일지 `PROMOTION.stripeCount`를 별도 필드 의미로 문서와 구현에서 명시한다.

### 11.3 null 해석 불일치

- `null`과 `0`을 앱/백엔드/운영자가 다르게 해석하면 상태 표시가 흔들릴 수 있다.

완화:

- `null = 미입력`, `0 = 입력된 0개`를 문서와 테스트에 명시한다.

### 11.4 구버전 앱 파서 이슈

- 일부 클라이언트 구현이 응답 추가 필드에 민감하면 예기치 않은 파싱 오류가 생길 수 있다.

완화:

- 앱 네트워크 계층에서 unknown field 무시 여부를 출시 전 확인한다.
- 필요하면 스테이징/내부 QA에서 구버전 앱으로 `GET /users/me` 회귀 확인을 수행한다.

## 12. 수용 기준

- 운영 배포 후 구버전 앱의 로그인, 내 정보 조회, 내 정보 수정이 그대로 동작한다.
- `GET /api/v1/users/me`는 기존 필드를 유지한 채 `stripeCount`를 추가 반환할 수 있다.
- `PUT /api/v1/users/me`는 `stripeCount`가 없어도 기존처럼 동작한다.
- `GET /api/v1/open-mats/{id}/participants`는 기존 필드를 유지한 채 `stripeCount`를 추가 반환할 수 있다.
- 새 앱은 `beltColor + stripeCount`를 저장하고 재조회할 수 있다.
- `stripeCount`에 음수를 보내면 validation error를 반환한다.
- 기존 `PROMOTION` 기록 API 계약은 깨지지 않으며, 프로필 `stripeCount`와 자동 동기화하지 않는다.

## 13. 성공 신호

- 새 앱 버전 사용자는 현재 등급을 `벨트 + 그랄`로 입력할 수 있다.
- 오픈매트 참가자 조회에서 참가자의 현재 등급을 `벨트 + 그랄`로 확인할 수 있다.
- 관련 CS 없이 구버전 앱 사용자 흐름이 유지된다.
- 운영 배포 직후 사용자 프로필 수정 실패율 증가가 없다.
- `users/me`와 `users/me` 수정 API의 오류율이 기존 수준을 유지한다.

## 14. 미해결 의사결정 항목

### 제품 책임자 결정 필요

1. 앱에서 `그랄`을 모든 벨트에 노출할지, 일부 벨트에서만 노출할지
2. `null` 상태를 UI에서 숨길지, “미설정”으로 보여줄지
3. 오픈매트 참가자 목록에서 `stripeCount=null`을 어떻게 표시할지

### 백엔드 책임자 결정 필요

1. `stripeCount`를 `GET /users/me`, `PUT /users/me`에만 우선 넣을지
2. 친구/세미나/관리자 응답까지 같이 확장할지
3. 1차에서 과거 `PROMOTION` 기록 기반 backfill을 할지, 하지 않을지

## 15. 최종 권장 결론

- 이번 변경은 `벨트 enum 확장`이 아니라 `프로필 stripeCount 추가`로 가는 것이 맞다.
- 2026-06-01 기준 1차 출시 범위는 `users` 프로필 API 확장, 오픈매트 참가자 조회 확장, 운영 무중단 배포에 집중한다.
- `PROMOTION.stripeCount`는 훈련일지 기록용 값으로 유지하고, 프로필 `stripeCount`와 자동 동기화하지 않는다.
- 친구/세미나/관리자 등 다른 화면 노출 확장은 2차 이후로 미루는 것이 리스크 대비 효율이 가장 좋다.

## 16. Codex 진행 체크리스트

### Phase 1. 문서/계약 정리

- [x] `docs/domain_and_spec/shared/common-models.md`에 `GET /api/v1/users/me`, `PUT /api/v1/users/me`의 `stripeCount` 계약 추가
- [x] 오픈매트 참가자 조회 스펙 문서에 `stripeCount` 응답 필드 추가
- [x] `PROMOTION.stripeCount`와 프로필 `stripeCount`가 자동 동기화되지 않는다는 정책 문서 반영

### Phase 2. DB 마이그레이션

- [x] `users` 테이블에 nullable `stripe_count` 컬럼 추가 플라이웨이 작성
- [x] 기존 운영 데이터에 destructive change가 없는지 검토
- [x] 기본값 없이 nullable 추가로 배포 가능한지 확인

### Phase 3. 도메인 모델 반영

- [x] `User` 엔티티에 `stripeCount` 필드 추가
- [x] 사용자 생성/수정 시 `stripeCount` 기본 동작 정리
- [x] `PROMOTION` 기록 로직이 프로필 `stripeCount`를 건드리지 않도록 보장

### Phase 4. API/DTO 반영

- [x] `UserResponse`에 `stripeCount` 추가
- [x] `UserUpdateRequest`에 optional `stripeCount` 추가
- [x] `UserService.updateMe()`에 `stripeCount` 반영 로직 추가
- [x] `OpenMatParticipantResponse`에 `stripeCount` 추가
- [x] 오픈매트 참가자 조회가 `User.stripeCount`를 사용하도록 반영

### Phase 5. 검증/예외 처리

- [x] `stripeCount`가 `null` 또는 `0 이상`만 허용되도록 검증 추가
- [x] 음수 입력 시 `VALIDATION_ERROR` 또는 현재 예외 규약에 맞는 bad request 처리 확인
- [x] 미전달 시 기존 값 유지 동작 확인

### Phase 6. 테스트

- [x] 사용자 프로필 조회 응답에 `stripeCount`가 포함되는 테스트 추가
- [x] 사용자 프로필 수정 시 `stripeCount` 저장 테스트 추가
- [x] 사용자 프로필 수정 시 `stripeCount` 음수 거절 테스트 추가
- [x] 오픈매트 참가자 조회 응답에 `stripeCount`가 포함되는 테스트 추가
- [ ] `PROMOTION` 기록 생성/수정/삭제가 프로필 `stripeCount`를 바꾸지 않는 회귀 테스트 추가

### Phase 7. 최종 검증

- [ ] 구버전 앱 관점에서 `stripeCount` 미전달 시 기존 흐름이 유지되는지 확인
- [ ] `GET /api/v1/users/me`와 `GET /api/v1/open-mats/{id}/participants` 응답 하위 호환성 확인
- [ ] 문서와 구현의 필드명(`stripeCount`, `stripe_count`) 일치 여부 확인
- [ ] 작업 결과를 `FAILURE_LOG.md` 반영 대상인지 검토

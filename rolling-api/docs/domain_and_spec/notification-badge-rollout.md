# Notification Badge Rollout Guide

## 1. 목적

- 운영 중인 모바일 앱에서 구버전 앱을 깨뜨리지 않고 notification badge 기능을 추가한다.
- 이번 기능은 `새로운 API version을 올리는 문제`가 아니라, `/api/v1` 안에서 하위 호환을 유지하며 확장하는 문제로 다룬다.

## 2. 기본 원칙

- 기존 앱이 사용 중인 endpoint, response field, field type은 깨지지 않게 유지한다.
- 변경은 가능한 한 `additive change`로만 진행한다.
- `백엔드 선배포`, `앱 후배포` 순서를 지킨다.
- unread count의 source of truth는 프론트 로컬 상태가 아니라 백엔드 `Notification` 데이터다.

## 3. 비호환 변경으로 보는 항목

- 기존 endpoint 경로 변경
- 기존 응답 필드 삭제
- 기존 응답 필드 이름 변경
- 기존 응답 필드 타입 변경
- 기존 요청 필수값 추가
- 기존 enum raw value 제거 또는 의미 변경
- 구도메인 즉시 종료

## 4. 확정안

### 4.1 API versioning

- 이번 기능은 `/api/v1` 안에서 처리한다.
- `v2`는 기존 클라이언트가 그대로 해석할 수 없는 비호환 변경이 생길 때만 검토한다.

### 4.2 구현 방식

- `옵션 B`를 선택한다.
- badge는 별도 endpoint `GET /api/v1/notifications/badge`로 제공한다.

응답 예시:

```json
{
  "unreadCount": 3
}
```

### 4.3 unread count 정의

- unread count는 `현재 로그인한 사용자의 알림 중 readAt 이 null 인 알림 개수`로 정의한다.
- 현재 알림함에서 숨김, 삭제, 만료 개념을 별도로 운영하지 않으므로 추가 제외 조건은 두지 않는다.

### 4.4 읽음 처리 후 반영 정책

- `PATCH /api/v1/notifications/{id}/read` 성공 후 클라이언트는 `GET /api/v1/notifications/badge`를 다시 호출해 최신 unread count를 반영한다.

### 4.5 badge API 실패 fallback

- badge API 실패는 홈 화면 전체 실패로 전파하지 않는다.
- 마지막 성공값이 있으면 유지하고, 없으면 배지를 미표시한다.
- 실패는 로깅만 수행하고 다음 홈 진입, 새로고침, 앱 foreground 복귀 시 재조회한다.

## 5. Phase Checklist

### Phase 0. 범위 확정

목표:

- 구현 전에 제품 정책과 API 방향을 고정한다.

Checklist:

- [x] badge 제공 방식을 `옵션 B`로 확정했다.
- [x] `/api/v1` 안에서 additive change로 진행하기로 확정했다.
- [x] unread count를 `readAt == null` 기준으로 정의했다.
- [x] 읽음 처리 성공 후 badge API 재조회 정책을 정의했다.
- [x] badge API 실패 시 fallback 정책을 정의했다.

### Phase 1. 백엔드 계약 설계

목표:

- 구현 전에 API 계약과 문서 반영 위치를 확정한다.

Checklist:

- [x] `/api/v1/notifications/badge` endpoint 계약을 확정했다.
- [x] 인증 필요 endpoint로 정의했다.
- [x] response field를 `unreadCount: Long`으로 확정했다.
- [x] 기존 알림 목록 API 계약을 유지하기로 확정했다.
- [x] shared contract 반영 위치를 `docs/domain_and_spec/shared/common-models.md`로 확정했다.

### Phase 2. 백엔드 구현

목표:

- 구버전 앱을 깨지 않는 방식으로 unread count 조회 endpoint를 추가한다.

Checklist:

- [x] controller에 `GET /api/v1/notifications/badge`를 추가했다.
- [x] service에 unread count 조회 로직을 추가했다.
- [x] repository에 unread count 조회 메서드를 추가했다.
- [x] 현재 로그인 사용자 기준으로만 count 하도록 구현했다.
- [x] 읽음 처리 API와 동일한 `readAt` 기준을 사용하도록 맞췄다.
- [x] unread count 조회 최적화를 위해 `(user_id, read_at)` 인덱스 migration을 추가했다.

### Phase 3. 백엔드 테스트

목표:

- 새 badge 계약과 기존 알림 계약이 함께 유지되는지 검증한다.

Checklist:

- [x] 기존 알림 목록 조회 서비스 테스트가 유지되는지 확인했다.
- [x] unread count 응답 서비스 테스트를 추가했다.
- [x] unread count가 `0`인 경우 테스트를 추가했다.
- [x] 본인 사용자만 badge 조회 가능한 흐름을 서비스 테스트로 확인했다.
- [x] badge endpoint 인증 성공/실패 컨트롤러 테스트를 추가했다.

### Phase 4. 문서 동기화

목표:

- 구현과 문서가 어긋나지 않도록 shared contract를 갱신한다.

Checklist:

- [x] `shared/common-models.md`에 최종 badge 계약을 반영했다.
- [x] controller, dto, repository, migration과 문서가 일치하는지 확인했다.




# FCM Token Lifecycle Policy

## 1. 목적

- `POST /api/v1/auth/logout`의 선택적 `fcmToken` 제거와 `DELETE /api/v1/users/me/fcm`의 역할을 같은 기준으로 해석한다.
- 로그아웃, 탈퇴 예약, 최종 탈퇴, 기기 변경, 무효 토큰 정리 시점의 실제 동작을 한 문서에 모은다.
- 운영 확인 포인트를 코드 기준으로 고정한다.

## 2. Source Of Truth

- 사용자 디바이스 토큰 저장의 source of truth는 `user_devices` 테이블(`UserDevice`)이다.
- `fcmToken`은 unique 값으로 관리한다.
- 한 사용자는 여러 디바이스 토큰을 가질 수 있다.
- 같은 토큰이 다시 등록되면 기존 `UserDevice` 레코드를 재사용한다.

## 3. API 역할 정의

### 3.1 `POST /api/v1/users/me/fcm`

- 현재 로그인한 사용자의 디바이스 토큰을 등록하거나 갱신한다.
- 동일 `fcmToken`이 이미 존재하면 기존 `UserDevice`를 재사용한다.
- 동일 `fcmToken`이 다른 사용자에 연결돼 있으면 현재 사용자에게 재연결한다.
- `platform`, `deviceId`, `appVersion`, `updatedAt`은 최신값으로 갱신한다.
- 로그인 직후, 앱 재설치 후, OS가 토큰을 새로 발급한 경우, 다른 계정으로 기기를 넘겨받은 경우에 사용한다.

### 3.2 `DELETE /api/v1/users/me/fcm`

- 현재 로그인한 사용자가 현재 디바이스 토큰을 명시적으로 제거할 때 사용 한다.
- 앱 세션은 유지하되 특정 디바이스 토큰만 해제하려는 경우의 API다.
- 현재 로그인한 사용자에게 연결된 토큰만 삭제한다.
- 토큰이 이미 없거나 다른 사용자 토큰이면 성공 응답으로 끝낸다. 동작은 idempotent다.

### 3.3 `POST /api/v1/auth/logout`

- 현재 사용자의 Refresh Token을 무효화한다.
- 요청 본문에 `fcmToken`을 함께 보내면 현재 디바이스 토큰도 같이 제거한다.
- body 없이 호출하거나 `fcmToken`을 생략하면 Refresh Token만 무효화한다.
- 현재 디바이스에서 로그아웃하면서 같은 디바이스 토큰 정리까지 한 번에 끝내고 싶을 때 쓰는 편의 API다.

정리:

- `DELETE /api/v1/users/me/fcm`은 명시적 토큰 해제용 API다.
- `POST /api/v1/auth/logout`의 `fcmToken`은 로그아웃 흐름 안에서 같은 일을 같이 처리하는 선택 옵션이다.
- 두 API 모두 "현재 로그인한 사용자에게 연결된 해당 토큰만 제거한다"는 규칙을 공유한다.

## 4. 라이프사이클 시나리오

### 4.1 로그인 / 토큰 갱신

- 로그인과 Refresh Token 갱신만으로는 `UserDevice`를 자동 생성하거나 삭제하지 않는다.
- 디바이스 푸시 수신을 원하면 앱이 별도로 `POST /api/v1/users/me/fcm`를 호출한다.

### 4.2 기기 변경 / 토큰 재발급

- 새 디바이스 또는 새 FCM 토큰이 생기면 `POST /api/v1/users/me/fcm`로 등록한다.
- 기존과 다른 토큰이면 같은 사용자 아래 디바이스가 추가된다.
- 같은 토큰이면 기존 디바이스 레코드가 최신 메타데이터로 갱신된다.
- 같은 토큰이 다른 사용자에 묶여 있으면 현재 사용자에게 소유권이 이동한다.

### 4.3 토큰 명시 해제

- 사용자가 현재 디바이스 알림만 끄고 로그인은 유지하려면 `DELETE /api/v1/users/me/fcm`를 호출한다.
- 성공 후에도 Access Token / Refresh Token은 유지된다.

### 4.4 로그아웃

- `POST /api/v1/auth/logout`는 항상 Refresh Token을 삭제한다.
- `fcmToken`을 함께 전달하면 현재 디바이스 `UserDevice`도 같이 삭제한다.
- `fcmToken`이 없으면 디바이스 토큰은 남는다.

### 4.5 탈퇴 예약

- `DELETE /api/v1/auth/withdraw`는 즉시 토큰을 삭제하지 않는다.
- 탈퇴 예약 중(`withdrawalPending=true`) 사용자는 푸시 발송 대상 조회에서 제외된다.
- 탈퇴 취소 시 별도 토큰 복구는 필요 없다. 기존 디바이스가 남아 있으면 다시 발송 대상이 된다.

### 4.6 최종 탈퇴

- 예약 시각이 지나 배치가 실행되면 Refresh Token을 삭제한다.
- 같은 시점에 해당 사용자의 `UserDevice` 전체를 삭제한다.
- 최종 탈퇴 후에는 남아 있는 FCM 토큰이 없어야 한다.

## 5. 무효 토큰 정리 정책

- 푸시 발송은 `Notification` 저장 후 FCM 전송을 시도한다.
- FCM 응답에서 `UNREGISTERED`, `INVALID_ARGUMENT`가 나온 토큰은 무효 토큰으로 보고 삭제한다.
- 위 두 케이스 외의 토큰 단위 실패는 로그만 남기고 토큰을 유지한다.
- 배치 전체 전송에서 `FirebaseMessagingException`이 발생하면 자동 재시도 없이 `IllegalStateException`으로 올린다.

## 6. 운영 확인 포인트

푸시가 오지 않는다고 보고되면 아래 순서로 본다.

1. 알림함 데이터 확인
- `Notification` 레코드가 생성됐는지 먼저 확인한다.
- 알림함에 레코드가 있으면 "이벤트 생성"까지는 성공한 것이다.

2. 발송 대상 제외 조건 확인
- 사용자 `withdrawalPending=true` 여부를 확인한다.
- 탈퇴 예약 상태면 푸시 대상에서 제외되는 것이 정상이다.

3. 디바이스 연결 상태 확인
- `user_devices`에 해당 `userId` 또는 `fcmToken` row가 남아 있는지 확인한다.
- 토큰이 다른 사용자에게 재연결됐는지도 확인한다.

4. 로그 확인
- `Resolved FCM target devices`
- `Deleted ... invalid FCM tokens`
- `FCM token send failed`
- `FCM batch send failed`

5. 클라이언트 호출 이력 확인
- 로그아웃 직전 `POST /api/v1/auth/logout`에 `fcmToken`을 보냈는지 확인한다.
- 로그인 상태 유지 중 토큰만 지웠다면 `DELETE /api/v1/users/me/fcm` 호출 여부를 확인한다.
- 기기 변경 또는 토큰 재발급 뒤 `POST /api/v1/users/me/fcm` 재등록이 있었는지 확인한다.

# FCM Integration Checklist

기준일: 2026-03-16

이 문서는 Flutter 프론트엔드와 Java Spring 백엔드가 FCM(Firebase Cloud Messaging)을 끝까지 연동하기 위해 필요한 작업을 한 곳에 정리한 체크리스트다. Codex가 이 파일만 읽어도 현재 상태와 다음 작업을 이해할 수 있도록 작성한다.

## Phase 0. 목표와 현재 상태

## 목표

- Flutter 앱에서 알림 권한을 요청한다.
- 기기별 FCM 토큰을 발급받는다.
- 로그인된 사용자 기준으로 FCM 토큰을 백엔드에 등록한다.
- Spring 서버가 Firebase Admin SDK로 특정 사용자에게 푸시를 발송한다.
- MVP 범위에서는 오픈매트 수정/삭제 알림만 우선 붙인다.
- 앱 내 알림함은 백엔드 저장/조회/읽음 처리 API까지 구현하고, 프론트 화면 연동은 별도 진행한다.

## 현재 프론트 상태

- [x] `firebase_core`, `firebase_messaging` 의존성 추가 완료
- [x] `lib/firebase_options.dart` 생성 완료
- [x] `android/app/google-services.json` 반영 완료
- [x] `lib/main.dart`에서 `Firebase.initializeApp()` 호출 완료
- [x] `lib/core/services/fcm_service.dart` 생성 완료
- [x] 알림 권한 요청(`requestPermission`) 구현 완료
- [x] 초기 FCM 토큰 발급 및 `debugPrint` 출력 구현 완료
- [x] `onTokenRefresh.listen` 토큰 갱신 리스너 구현 완료
- [x] 로그인 상태일 때 `POST /api/v1/users/me/fcm`로 토큰 업로드 구현 완료
- [x] Android 13+용 `POST_NOTIFICATIONS` 권한 선언 완료
- [ ] iOS `GoogleService-Info.plist` 반영 필요
- [ ] iOS APNs / Xcode Push 설정 필요
- [x] 포그라운드 알림 표시 처리 필요
- [x] 백그라운드/종료 상태 알림 탭 라우팅 처리 필요
- [x] 알림 목록 API 연동 필요

## 현재 프론트 관련 파일

- `lib/main.dart`
- `lib/core/services/fcm_service.dart`
- `lib/core/services/api_service.dart`
- `android/app/src/main/AndroidManifest.xml`
- `lib/firebase_options.dart`
- `docs/AGENTS.md`의 `POST/DELETE /api/v1/users/me/fcm` 명세

## 지금 바로 내가 해야 할 것

- [x] Firebase Console에서 서버용 서비스 계정 JSON 발급
- [x] 백엔드 실행 환경에 `FIREBASE_ENABLED=true` 설정
- [x] 백엔드 실행 환경에 `FIREBASE_PROJECT_ID` 설정
- [x] `FIREBASE_CREDENTIALS_PATH`를 쓸지, `GOOGLE_APPLICATION_CREDENTIALS`를 쓸지 결정
- [ ] Android 실기기 2대 이상 또는 테스트용 사용자 2명 이상 준비
- [ ] 오픈매트 수정 알림을 `일정/장소 변경`일 때만 보낼지 최종 확인

## Phase 1. 즉시 착수 항목

## 프론트엔드 체크리스트

### 1. Firebase / 플랫폼 설정

- [x] Firebase 프로젝트 생성
- [x] Android 앱 Firebase 등록
- [x] iOS 앱 Firebase 등록
- [x] FlutterFire CLI로 Dart 설정 파일 생성
- [ ] macOS에서 `ios/Runner/GoogleService-Info.plist` 생성 또는 Firebase Console에서 직접 다운로드 후 반영
- [ ] Xcode에서 `Push Notifications` Capability 활성화
- [ ] Xcode에서 `Background Modes > Remote notifications` 활성화
- [ ] Firebase Console `Project settings > Cloud Messaging`에 APNs Auth Key 업로드
- [ ] iOS 실기기에서 권한 요청 및 수신 테스트

### 2. Flutter 앱 코드

- [x] 앱 시작 시 Firebase 초기화
- [x] `Get.putAsync(() => FcmService().init())` 등록
- [x] 권한 요청 후 FCM 토큰 발급
- [x] 토큰 갱신 시 재등록 처리
- [x] 로그인 완료 후 토큰을 백엔드로 업로드
- [ ] `FirebaseMessaging.onMessage` 처리 추가
- [ ] `FirebaseMessaging.onMessageOpenedApp` 처리 추가
- [ ] `FirebaseMessaging.getInitialMessage()` 처리 추가
- [ ] `FirebaseMessaging.onBackgroundMessage(...)` 백그라운드 핸들러 추가
- [ ] 포그라운드 알림 노출 방식 결정
- [ ] 알림 payload에 따라 특정 화면으로 이동하는 라우팅 규칙 정의
- [ ] 로그아웃 시 토큰 비활성화가 필요하면 백엔드 API와 연동

### 3. 프론트 검증

- [ ] Android 실기기에서 권한 팝업 확인
- [ ] Android 실기기에서 콘솔에 FCM 토큰 출력 확인
- [x] 로그인 직후 백엔드에 토큰 등록 요청이 가는지 확인
- [ ] Firebase Console 테스트 메시지 수신 확인
- [ ] 포그라운드 상태 수신 확인
- [ ] 백그라운드 상태 수신 확인
- [ ] 앱 종료 상태에서 알림 탭 진입 확인
- [ ] 앱 재설치 또는 토큰 갱신 시 백엔드 토큰 업데이트 확인

## Phase 2. 백엔드 구현

## Java Spring 백엔드 체크리스트

### 1. Firebase Admin SDK 설정

- [x] Firebase Admin SDK 의존성 추가
- [x] 서비스 계정 키 또는 서버 실행 환경 인증 방식 결정
- [x] `FirebaseApp` 초기화 설정 클래스 추가
- [x] 운영/개발 환경별 Firebase 인증 전략 분리

### 2. FCM 토큰 저장 API

- [x] `POST /api/v1/users/me/fcm` 구현
- [x] 요청 본문 `fcmToken` 검증
- [x] 사용자-디바이스 1:N 기준 토큰 저장 처리
- [x] 동일 토큰 중복 저장 방지
- [x] 동일 토큰 재등록 시 기존 디바이스 레코드 재사용 및 현재 사용자에게 재연결
- [x] `platform`, `deviceId`, `appVersion`, `updatedAt` 함께 저장
- [x] 로그아웃 시 현재 디바이스 토큰 삭제 정책 결정
- [x] `DELETE /api/v1/users/me/fcm` API 추가

### 3. 추천 서버 구조

- [x] `FirebaseAdminConfig` 추가
- [x] `UserDevice` 엔티티 또는 테이블 추가
- [x] `UserDeviceRepository` 추가
- [x] 사용자 토큰 등록은 기존 `UserController`에서 처리
- [ ] `FcmTokenService` 또는 `UserDeviceService` 분리 여부 결정
- [x] `PushNotificationService` 추가
- [x] 도메인 이벤트 발생 시 푸시 발송하는 서비스 연결

### 4. 푸시 발송 로직

- [x] 특정 사용자에게 푸시 발송 메서드 구현
- [x] 다중 토큰 발송 지원 여부 결정
- [x] 사용자별 복수 디바이스 토큰 발송 구조 사용
- [x] 메시지 구조 표준화
- [x] `notification` + `data` payload 규칙 정의
- [x] 실패 응답 중 `unregistered`, `invalid-argument` 등은 토큰 정리 처리
- [ ] 재시도 정책이 필요한지 결정
- [x] 오픈매트 수정/삭제 이벤트를 참가자 대상 푸시 발송으로 연결

### 5. 알림함이 필요한 경우

- [x] Notification 테이블 설계
- [x] 사용자용 알림함 레코드 저장 구현
- [x] 읽음 처리 API 추가
- [x] 알림 목록 조회 API 추가
- [x] 알림 목록/읽음 처리 응답 스펙을 `docs/AGENTS.md`에 반영
- [x] 프론트 알림 화면 연동

### 6. 백엔드 검증

- [x] 토큰 등록 서비스 테스트 작성
- [x] Firebase Admin 초기화 테스트 또는 스모크 테스트 작성
- [x] 잘못된 토큰 정리 테스트 작성
- [x] 오픈매트 수정/삭제 이벤트 테스트 작성
- [x] 실제 Android 디바이스 대상 테스트 발송 확인
- [ ] 실제 iOS 디바이스 대상 테스트 발송 확인

## Phase 3. 공통 계약 정리

## 공통 계약 체크리스트

- [x] 프론트가 호출할 토큰 등록 엔드포인트 정의
- [x] 토큰 등록/알림함 요청·응답 스펙을 `docs/AGENTS.md`에 반영
- [x] 알림 `type` 값 목록 정의
- [x] 알림 payload 예시 정의
- [x] 어떤 이벤트에서 어떤 푸시를 보낼지 MVP 도메인 정책 1차 정리
- [x] 로그아웃/탈퇴 시 토큰 처리 정책 정리
- [x] 토큰 저장 단위를 `device 기준`으로 확정

## 권장 payload 규칙

- [x] `type`: `OPEN_MAT_UPDATED`, `OPEN_MAT_DELETED`
- [x] `targetId`: 오픈매트 ID
- [x] `title`: 사용자 노출 제목
- [x] `body`: 사용자 노출 내용
- [x] `route`: 앱 진입 경로 (`/openmat/detail`, `/openmat`)

### 현재 payload 예시

#### 오픈매트 수정

```json
{
  "notification": {
    "title": "오픈매트 일정이 변경되었습니다",
    "body": "주말 오픈매트 오픈매트의 일정 또는 장소가 변경되었습니다."
  },
  "data": {
    "type": "OPEN_MAT_UPDATED",
    "targetId": "123",
    "route": "/openmat/detail",
    "title": "오픈매트 일정이 변경되었습니다",
    "body": "주말 오픈매트 오픈매트의 일정 또는 장소가 변경되었습니다."
  }
}
```

#### 오픈매트 삭제

```json
{
  "notification": {
    "title": "오픈매트가 취소되었습니다",
    "body": "주말 오픈매트 오픈매트가 삭제되었습니다."
  },
  "data": {
    "type": "OPEN_MAT_DELETED",
    "targetId": "123",
    "route": "/openmat",
    "title": "오픈매트가 취소되었습니다",
    "body": "주말 오픈매트 오픈매트가 삭제되었습니다."
  }
}
```

## Phase 4. 완료 기준과 최종 점검

## 완료 기준

- [x] Android 실기기에서 로그인 후 토큰이 서버에 저장된다.
- [ ] iOS 실기기에서 로그인 후 토큰이 서버에 저장된다.
- [x] Spring 서버가 특정 사용자에게 FCM 푸시를 정상 발송한다.
- [x] 잘못된 토큰은 서버에서 정리된다.
- [x] 앱이 포그라운드/백그라운드/종료 상태에서 모두 의도한 동작을 한다.
- [x] 필요 시 앱 내 알림 목록과 푸시 알림이 일관되게 동작한다.

## 릴리스 검증 매트릭스

| 플랫폼 | 앱 상태 | 권한 상태 | 푸시 수신 | 알림 탭 라우팅 | 비고 |
| --- | --- | --- | --- | --- | --- |
| Android | foreground | 허용 | pending | pending | 수동 검증 필요 |
| Android | background | 허용 | pending | pending | 수동 검증 필요 |
| Android | terminated | 허용 | pending | pending | 수동 검증 필요 |
| Android | foreground/background/terminated | 거부 | pending | n/a | 핵심 기능 사용 가능 여부 수동 확인 필요 |
| iOS | foreground | 허용 | pending | pending | APNs/Xcode 설정 후 실기기 검증 필요 |
| iOS | background | 허용 | pending | pending | APNs/Xcode 설정 후 실기기 검증 필요 |
| iOS | terminated | 허용 | pending | pending | APNs/Xcode 설정 후 실기기 검증 필요 |

## 운영 메모

- 현재 FCM 발송 실패 시 서버는 자동 재시도하지 않는다.
- `UNREGISTERED`, `INVALID_ARGUMENT`는 토큰 정리 대상으로 처리한다.
- 그 외 FCM 오류는 `errorCode`, `retryPolicy`, `tokenPrefix`를 포함한 로그로 남기고 예외로 종료한다.
- 알림 payload 계약 회귀 테스트는 `route`, `targetId` 필수 검증 기준으로 유지한다.

## 비고

- 현재 백엔드는 `POST /api/v1/users/me/fcm`와 `UserDevice` 1:N 저장 구조를 구현했다.
- 현재 백엔드는 `DELETE /api/v1/users/me/fcm`를 구현했고, 로그아웃 시 `POST /api/v1/auth/logout` 본문에 `fcmToken`을 함께 보내면 현재 디바이스 토큰도 제거할 수 있다.
- 현재 백엔드는 `Notification` 저장 구조와 `GET /api/v1/notifications`, `PATCH /api/v1/notifications/{id}/read`를 구현했다.
- Firebase Admin SDK, `FirebaseAdminConfig`, `PushNotificationService`, 오픈매트 수정/삭제 푸시 이벤트 연결이 구현되어 있다.
- 토큰은 `users` 단일 컬럼이 아니라 `user_devices` 기준으로 저장된다.
- 같은 사용자가 여러 휴대폰을 쓰면 여러 FCM 토큰을 각각 연결할 수 있다.
- `withdrawalPending = true` 사용자는 FCM 발송 대상에서 제외되고, 최종 탈퇴 실행 시 `user_devices`가 삭제된다.
- 서버는 `FIREBASE_ENABLED`, `FIREBASE_PROJECT_ID`, `FIREBASE_CREDENTIALS_PATH` 또는 ADC(`GOOGLE_APPLICATION_CREDENTIALS`)로 Firebase 인증을 구성한다.
- iOS 쪽은 Windows에서 FlutterFire 설정만으로 완결되지 않으므로 macOS에서 최종 반영이 필요하다.
- FCM은 전달 채널이고, 사용자용 알림함은 `Notification` 저장 구조로 별도 관리된다.


# Store Review Checklist By Phase (기준일: 2026-03-18)

<생각>
이 문서는 내부 추론 전체가 아니라, 공개 가능한 실행 정리 문서다.

- 지금 목표는 `Google Play 선출시`이므로, 플레이스토어 통과에 직접 필요한 항목을 먼저 Phase로 묶고 Apple 전용 이슈는 뒤로 분리하는 편이 효율적이다.
- 이번 정리는 단순 체크리스트가 아니라 `현재 상태`, `프론트엔드`, `백엔드`, `외부 준비물`로 나눠서 실제 작업 분담이 바로 가능하도록 재구성한다.
- 현재 저장소 기준 핵심 리스크는 `약관/개인정보처리방침 외부 페이지 공개 상태 확인 필요`, `Play Console 심사용 입력 준비`, `Apple 로그인 서버 미구현`, `iOS GoogleService-Info.plist 미반영`이다.
  </생각>

<답>

## 전체 전략

- `Phase 0`: 현재 상태 정리와 공통 마감
- `Phase 1`: Google Play 심사 통과 및 출시
- `Phase 2`: Apple App Store 심사 준비
- `Phase 3`: 출시 후 운영 안정화

## Phase 0. 현재 상태 정리와 공통 마감

### 현재 상태

- [x] 프론트 주요 기능은 거의 구현 완료
  소셜 로그인, JWT 복원/재발급, 오픈매트, 대회, 알림함, 공지사항, 회원 탈퇴, FCM 토큰 등록
- [x] Android Firebase 설정 파일은 반영 완료
  `android/app/google-services.json`
- [x] 앱 내부 회원 탈퇴 기능은 구현 완료
  `DELETE /api/v1/auth/withdraw`
- [x] 사용자 생성 콘텐츠 대응 기능이 앱에 존재
  신고, 차단, 공지사항 조회, 외부 링크 처리
- [x] 앱 내부 약관/개인정보처리방침 링크 상수는 반영 완료
  `lib/core/constants/app_links.dart`
- [x] 앱 내부 정책 링크가 가리키는 외부 페이지는 아직 정상 공개 상태 확인 필요
  현재 Notion 링크 기준 접근 시 404 확인
- [ ] iOS `GoogleService-Info.plist`는 아직 미반영
- [ ] Apple 로그인은 문서 기준으로 아직 서버 미구현

### 프론트엔드에서 해야 할 것

- [x] [app_links.dart](/c:/rolling/rolling-flutter-frontend/rolling_flutter/lib/core/constants/app_links.dart)의 약관 URL이 실제 공개 페이지를 가리키도록 수정
- [x] [app_links.dart](/c:/rolling/rolling-flutter-frontend/rolling_flutter/lib/core/constants/app_links.dart)의 개인정보처리방침 URL이 실제 공개 페이지를 가리키도록 수정
- [ ] Android 릴리스 빌드로 전체 핵심 흐름 스모크 테스트 
- [ ] 푸시 권한 요청과 알림함 진입 흐름 재확인
- [ ] 회원 탈퇴 후 세션 정리와 재로그인 흐름 재확인
- [ ] 심사자가 볼 핵심 화면 기준으로 캡처용 데이터 정리

### 백엔드에서 해야 할 것

- [x] 운영 환경에서 로그인, 토큰 갱신, 로그아웃, 회원 탈퇴 API 정상 동작 확인
- [x] 운영 환경에서 FCM 토큰 등록 및 푸시 발송 흐름 확인
- [ ] 신고, 차단, 외부 링크 차단 조건이 운영 데이터에서도 정상 동작하는지 확인
- [ ] 심사 기간 동안 사용할 테스트 계정과 테스트 데이터 준비

### 내가 외부에서 해야 할 것

- [x] 실제 서비스 약관 페이지 공개
- [x] 실제 개인정보처리방침 페이지 공개
- [ ] 지원 이메일 또는 지원 페이지 준비
- [ ] 심사용 테스트 계정 정보 정리
- [ ] 스토어 설명, 아이콘, 스크린샷, 카테고리 문안 정리

## Phase 1. Google Play 심사 통과 및 출시

이 단계가 현재 최우선이다.

### 현재 상태

- [x] Android 쪽 Firebase 기본 연결 완료
- [x] 앱 내 회원 탈퇴 기능 존재
- [x] Google 로그인, Kakao 로그인 UI 및 연동 존재
- [x] 알림함과 FCM 토큰 등록 연동 존재
- [x] Google Play 심사에 필요한 웹 계정 삭제 경로 준비
  Google Form 기반 삭제 요청 폼 존재
- [ ] Google Play Console 정책 입력은 아직 별도 정리 필요

### 프론트엔드에서 해야 할 것

- [ ] Play Store 제출용 Android 릴리스 빌드 최종 점검
- [ ] 실제 URL 반영 후 로그인 화면 링크 동작 확인
- [ ] 오픈매트 작성/신청/취소/신고/삭제 동작 재검증
- [ ] 대회 상세, 외부 링크, 신고 상태 동작 재검증
- [ ] 공지사항 홈/목록/상세 동작 재검증
- [ ] 알림 수신 후 알림함 이동과 상세 이동 재검증
- [ ] 스토어 스크린샷 촬영용 빌드/데이터 상태 정리

### 백엔드에서 해야 할 것

- [ ] 심사용 계정으로 전체 기능 접근 가능하도록 운영 데이터 준비
- [ ] 회원 탈퇴 API가 실운영 환경에서도 안정적으로 동작하는지 확인
- [x] Google Play 계정 삭제 요구사항 대응 방식 확정
  현재안: Google Form 제출 후 운영 처리
- [ ] 공지사항 조회 API를 실운영에서 계속 열어둘지 확인
- [ ] 심사 중 백엔드가 꺼지지 않도록 배포 상태와 로그 모니터링 준비

### 내가 외부에서 해야 할 것

- [x] 웹 계정 삭제 URL 준비
  Google Play는 앱 내 삭제와 별개로 웹 삭제 경로를 요구할 수 있음
- [ ] Google Play Console `Data safety` 작성
- [ ] Google Play Console `App access` 작성
- [ ] Google Play Console `Account deletion` 항목 작성
- [ ] 개인정보처리방침 URL을 Play Console에 등록
- [ ] 심사팀에게 줄 로그인 절차와 테스트 방법 문서화
- [ ] Play Store 등록 정보 작성
  앱 설명, 짧은 설명, 스크린샷, 아이콘, 카테고리, 연락처

### Phase 1 완료 기준

- [ ] 앱 내부 정책 링크 정상 공개 확인 완료
- [x] 웹 계정 삭제 URL 준비 완료
- [ ] Play Console 정책 입력 완료
- [ ] Android 릴리스 실기기 검증 완료
- [ ] 심사용 계정과 리뷰 메모 준비 완료

## Phase 2. Apple App Store 심사 준비

Play Store 제출 이후 진행하면 된다.

### 현재 상태

- [ ] Apple 로그인은 문서 기준으로 아직 서버 미구현
- [ ] iOS `GoogleService-Info.plist` 미반영
- [ ] iOS 심사용 App Review Notes 미작성
- [ ] Apple Developer / App Store Connect 제출 자료는 별도 준비 필요

### 프론트엔드에서 해야 할 것

- [ ] Apple 로그인 지원이 필요하다고 확정되면 iOS 로그인 버튼과 클라이언트 플로우 추가
- [ ] iOS에서 푸시 권한 요청과 알림 동작 확인
- [ ] iOS 실기기에서 로그인, 회원 탈퇴, 공지사항, 외부 링크 테스트
- [ ] iOS 릴리스 빌드와 아카이브 검증
- [ ] iOS 전용 안내 문구나 권한 설명이 필요하면 반영

### 백엔드에서 해야 할 것

- [ ] Apple 로그인 필요 여부를 정책 기준으로 최종 판단
- [ ] 필요하면 Apple 로그인 서버 지원 추가
  Apple identity token 검증, 사용자 생성/로그인, 토큰 발급, 탈퇴 연동
- [ ] iOS 심사용 테스트 계정 또는 데모 데이터 준비
- [ ] 심사 중 백엔드 응답 안정성 재확인

### 내가 외부에서 해야 할 것

- [ ] macOS 환경에서 `GoogleService-Info.plist` 생성 및 Xcode 반영
- [ ] Apple Developer 설정 확인
  Bundle ID, Sign in with Apple capability, Push capability
- [ ] App Store Connect 개인정보처리방침 URL 등록
- [ ] App Review Notes 작성
- [ ] 심사용 활성 계정 또는 데모 모드 설명 준비
- [ ] App Store 스크린샷과 설명 문구 준비

### Phase 2 완료 기준

- [ ] Apple 로그인 정책 대응 완료
- [ ] iOS Firebase 설정 완료
- [ ] iPhone 실기기 검증 완료
- [ ] App Store Connect 제출 정보 입력 완료

## Phase 3. 출시 후 운영 안정화

### 현재 상태

- [x] 앱에는 신고, 차단, 탈퇴, 알림함 같은 운영 대응 장치가 이미 들어가 있음
- [ ] 스토어 반려 대응용 운영 문서와 템플릿은 아직 별도 정리 필요

### 프론트엔드에서 해야 할 것

- [ ] 반려나 정책 수정 시 빠르게 수정할 수 있도록 화면별 수정 포인트 정리
- [ ] 버전별 변경사항 노트 정리
- [ ] 스토어 설명과 앱 실제 기능 차이가 생기지 않도록 관리

### 백엔드에서 해야 할 것

- [ ] 심사 기간 로그 모니터링
- [ ] 장애 시 즉시 복구 가능한 운영 절차 준비
- [ ] 공지사항 운영 API와 콘텐츠 운영 프로세스 정리
- [ ] 계정 삭제 요청 처리 이력과 문의 대응 흐름 정리

### 내가 외부에서 해야 할 것

- [ ] 심사 중 문의 메일 확인 담당자 지정
- [ ] 반려 사유 정리용 문서 템플릿 준비
- [ ] 개인정보처리방침, 약관, 지원 페이지 지속 관리
- [ ] 운영 공지 문안 템플릿 준비

## 지금 바로 해야 하는 순서

1. 프론트 정책 링크가 가리키는 약관/개인정보처리방침 외부 페이지를 실제 공개 상태로 정리
2. 외부 준비물로 개인정보처리방침, 약관, 웹 계정 삭제 페이지 상태를 최종 점검
3. 백엔드에서 운영 환경 기준 로그인, 탈퇴, 공지사항, FCM 흐름을 점검
4. 외부 작업으로 Play Console `Data safety`, `App access`, `Account deletion`을 작성
5. 프론트에서 Android 릴리스 실기기 검증을 마무리
6. Google Play 제출
7. 이후 Apple 로그인과 iOS 심사 준비로 이동

## 공식 기준 링크

- Apple App Review Guidelines
  https://developer.apple.com/app-store/review/guidelines/
- Google Play App Access
  https://support.google.com/googleplay/android-developer/answer/15748846?hl=en
- Google Play Data Safety
  https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
- Google Play App Account Deletion
  https://support.google.com/googleplay/android-developer/answer/13327111?hl=en

</답>

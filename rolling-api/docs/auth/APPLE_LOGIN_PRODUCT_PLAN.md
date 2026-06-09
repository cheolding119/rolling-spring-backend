# Apple 로그인 구현 기획안

작성일: 2026-04-29  
대상: Rolling API 소셜 로그인 확장



## 1. 배경

현재 Rolling API의 소셜 로그인은 `POST /api/v1/auth/login` 단일 엔드포인트에서 `provider`와 `accessToken`을 받아 처리한다.

현재 서버 구현 기준:

- 지원 provider: `GOOGLE`, `KAKAO`
- 미지원 provider: `APPLE`
- 로그인 성공 시 Rolling 자체 JWT `accessToken`, `refreshToken` 발급
- 사용자 식별 기준: `users.social_id + users.social_provider`
- 기존 사용자면 닉네임/이메일 갱신, 신규 사용자면 기본 벨트 `WHITE`로 생성
- 탈퇴, 제재, 관리자 여부, refresh token rotation 흐름은 provider와 무관하게 공통 처리

Apple 로그인은 이 구조를 유지하면서 `APPLE` provider만 추가하는 방식으로 진행한다.

## 2. 목표

Apple 계정으로 로그인한 사용자가 Google/Kakao 로그인 사용자와 동일한 방식으로 Rolling 계정에 진입할 수 있게 한다.

성공 상태:

- 앱 클라이언트가 Apple 로그인 후 받은 토큰을 기존 로그인 API로 전달한다.
- 서버가 Apple 토큰을 검증하고 Apple 고유 사용자 식별자 `sub`를 `socialId`로 저장한다.
- 기존 JWT 발급, refresh, logout, withdraw, 제재 정책은 변경 없이 재사용한다.
- 기존 Google/Kakao 로그인 동작은 깨지지 않는다.

## 3. 제공받아야 할 정보

### 3.1 Apple Developer 계정/앱 정보

필수:

- Apple Developer Team ID
- iOS Bundle ID
- Sign in with Apple capability 활성화 여부
- Apple 로그인용 Key ID
- Apple 로그인용 `.p8` private key
- Apple `client_id`로 사용할 값
  - 네이티브 iOS 앱만 대상으로 하면 보통 Bundle ID(안드로이드 , 웹은 애플 로그인 제공 x )


선택 또는 후속 필요:

- Return URL / Redirect URI
- 운영 도메인
- 개발/스테이징/운영 환경별 client_id 분리 여부

### 3.2 클라이언트 구현 정보

필수:

- Flutter Apple 로그인 라이브러리 또는 네이티브 구현 방식
- 서버로 전달할 토큰 종류
  - 권장: Apple `identityToken`
  - 필요 시: `authorizationCode`
- 클라이언트에서 생성/전달할 `nonce` 사용 여부
- 최초 동의 시 받은 사용자 이름을 서버로 보낼지 여부
- 이메일 미공개/비공개 릴레이 이메일을 허용할지 여부

결정 필요:

- 기존 요청 필드 `accessToken`에 Apple `identityToken`을 넣어 보낼지
- Apple 전용 명확성을 위해 `identityToken` 필드를 새로 추가할지

현재 코드베이스 유지 관점의 권장안은 기존 `accessToken` 필드에 Apple `identityToken`을 전달하는 것이다. 단, API 문서에는 provider가 `APPLE`일 때 `accessToken`의 의미가 Apple `identityToken`임을 명시해야 한다.

### 3.3 제품 정책 정보

필수:

- Apple 로그인 출시 대상
  - iOS 앱만
  - iOS + 관리자 웹
  - iOS + Android + 웹
- Apple 로그인 사용자가 프로필 이름을 수정하지 않은 경우 기본 닉네임
- Apple이 이름을 다시 제공하지 않는 상황에서 기존 닉네임 유지 정책
- Apple 이메일이 없거나 null일 때 가입 허용 여부
- 기존 Google/Kakao 계정과 Apple 계정의 이메일이 같을 때 자동 계정 병합을 할지 여부

권장 정책:

- 이메일 기준 자동 병합은 이번 범위에서 제외한다.
- Apple `sub + APPLE` 조합을 별도 계정으로 취급한다.
- 이름이 없으면 `Unknown` 또는 앱의 기본 닉네임 설정 흐름으로 보낸다.

### 3.4 운영/보안 정보

필수:

- `.p8` private key 보관 방식
  - 환경 변수
  - Secret Manager
  - 파일 경로
- 운영 서버에 주입할 환경 변수 이름
- 키 교체 담당자와 교체 절차
- Apple 공개키/JWKS 조회 실패 시 알림 정책

MVP 권장 환경 변수:

- `APPLE_CLIENT_ID`
- `APPLE_JWKS_URL=https://appleid.apple.com/auth/keys`
- `APPLE_ISSUER=https://appleid.apple.com`

후속 Apple server-to-server 연동을 구현할 때 추가할 환경 변수:

- `APPLE_TEAM_ID`
- `APPLE_KEY_ID`
- `APPLE_PRIVATE_KEY_PATH`

이번 MVP는 iOS 네이티브 앱이 받은 Apple `identityToken`을 서버가 검증하는 방식이므로 `.p8` private key와 Key ID를 로그인 검증에 사용하지 않는다. 해당 값들은 token revoke 또는 authorization code 교환이 필요해질 때 사용한다.

## 4. 권장 범위

### 지금 구현

- `SocialProvider.APPLE` 추가
- `SocialLoginRequest` 문서 허용값에 `APPLE` 추가
- `AppleTokenVerifier` 추가
- Apple `identityToken` 검증
  - 서명 검증
  - `iss` 검증
  - `aud` 검증
  - 만료 시간 검증
  - `sub` 추출
  - nonce를 쓰기로 결정하면 nonce 검증
- Apple 응답 DTO 추가
  - `socialId = sub`
  - `email = email claim`
  - `nickname = 최초 전달 이름 또는 기본값`
- `AuthService.login`의 provider 분기 확장
- `AuthException.appleApiError(...)` 추가
- `/actuator/health`의 `socialLogin` detail에 Apple 준비 상태 추가
- Spring Security JOSE/Nimbus 기반 `JwtDecoder` 사용
- 로그인 검증 테스트 추가
- `docs/AGENTS.md`와 Swagger 설명 업데이트

### 다음으로 미룰 항목

- 이메일 기준 계정 병합
- Apple 토큰 revoke API 연동
- Apple authorization code를 서버가 직접 교환하는 별도 OAuth callback API
- 웹 관리자용 Sign in with Apple JS 연동
- Apple private email relay 발송 도메인 설정
- Apple `.p8` private key를 사용하는 client secret 생성

### 제외 범위

- 기존 로그인 API 경로 변경
- Google/Kakao 로그인 계약 변경
- Rolling JWT 구조 변경
- 사용자 테이블의 식별 모델 변경

## 5. API 계약

기존 엔드포인트를 유지한다.

```http
POST /api/v1/auth/login
Content-Type: application/json
```

Apple 로그인 요청 예시:

```json
{
  "provider": "APPLE",
  "accessToken": "apple-identity-token"
}
```

응답은 기존 `AuthResponse`와 동일하다.

```json
{
  "accessToken": "rolling-jwt-access-token",
  "refreshToken": "rolling-jwt-refresh-token",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "newUser": true,
  "userId": 1,
  "email": "user@example.com",
  "name": "Unknown",
  "isAdmin": false,
  "accountStatus": "ACTIVE"
}
```

## 6. 사용자 흐름

1. 사용자가 앱에서 Apple 로그인 버튼을 누른다.
2. Apple 인증 UI에서 사용자가 이름/이메일 공유 여부를 선택한다.
3. 앱은 Apple `identityToken`을 받는다.
4. 앱은 `POST /api/v1/auth/login`에 `provider=APPLE`, `accessToken={identityToken}`을 보낸다.
5. 서버는 Apple 공개키로 `identityToken`을 검증한다.
6. 서버는 `sub`를 `socialId`로 사용해 기존 사용자 조회 또는 신규 가입을 처리한다.
7. 서버는 Rolling JWT를 발급한다.
8. 앱은 기존 Google/Kakao와 같은 로그인 완료 흐름으로 진입한다.

## 7. 데이터 정책

- `users.social_provider`: `APPLE`
- `users.social_id`: Apple `sub`
- `users.email`: Apple token의 `email` claim이 있으면 저장
- `users.nickname`: 클라이언트가 최초 로그인 때 전달한 이름 또는 기본값
- Apple은 이름을 최초 승인 시점에만 제공할 수 있으므로, 서버는 이후 로그인에서 null 이름으로 기존 닉네임을 덮어쓰지 않아야 한다.
- Apple 이메일은 사용자가 비공개 릴레이를 선택할 수 있으므로 계정의 안정 식별자로 사용하지 않는다.

## 8. 수용 기준

- `provider=APPLE` 요청이 200 OK로 Rolling JWT를 반환한다.
- 유효하지 않은 Apple token은 `APPLE_API_ERROR` 또는 인증 실패 코드로 실패한다.
- `sub`가 없거나 비어 있으면 사용자를 생성하지 않는다.
- 동일한 Apple `sub`로 재로그인하면 기존 사용자를 반환하고 `newUser=false`가 된다.
- Apple 이메일이 null이어도 정책상 가입 허용이면 로그인에 성공한다.
- 기존 `GOOGLE`, `KAKAO` 로그인 테스트가 통과한다.
- Swagger/API 문서에 `APPLE` 허용값과 Apple token 의미가 반영된다.
- 운영 health detail에서 Apple 로그인 검증 구성 누락 여부를 확인할 수 있다.

## 9. 성공 지표

- iOS Apple 로그인 성공률
- Apple 로그인 실패 코드별 비율
- Apple 신규 가입자 수
- Apple 로그인 후 첫 세션 이탈률
- Apple 로그인 사용자 CS/문의 건수
- Google/Kakao 로그인 오류율 변화 없음

## 10. 리스크와 완화

| 리스크 | 영향 | 완화 |
| --- | --- | --- |
| Apple 이름은 최초 승인 때만 제공될 수 있음 | 닉네임이 `Unknown`으로 생성될 수 있음 | 최초 로그인 시 클라이언트가 name을 함께 전달하거나 가입 후 프로필 보완 유도 |
| 이메일 비공개 릴레이 사용 | 이메일 기반 사용자 식별/병합 불가 | `sub`를 유일 식별자로 사용하고 이메일 병합 제외 |
| `identityToken` 검증 누락 | 위조 로그인 위험 | Apple JWKS 서명, issuer, audience, exp, nonce 검증 |
| Apple JWKS 조회 장애 | 로그인 실패 | 키 캐시, 짧은 재시도, 장애 알림 |
| client_id 혼선 | 운영 로그인 실패 | iOS Bundle ID/Services ID 사용 범위를 출시 전에 확정 |
| `.p8` key 노출 | 보안 사고 | secret 저장소 사용, 로그 마스킹, 키 교체 절차 문서화 |

## 11. 미해결 의사결정

| 항목 | 권장안 | 결정자 |
| --- | --- | --- |
| Apple token 전달 필드 | 기존 `accessToken` 재사용 | 백엔드/앱 담당자 |
| nonce 필수 여부 | iOS 앱에서 nonce 생성 후 서버 검증 | 백엔드/앱 담당자 |
| 이름 전달 방식 | 최초 로그인 때 앱이 별도 필드로 전달하거나 기본값 사용 | 제품/앱 담당자 |
| 이메일 null 허용 | 허용 | 제품 책임자 |
| 이메일 동일 계정 병합 | 이번 릴리스 제외 | 제품 책임자 |
| 웹/관리자 Apple 로그인 포함 여부 | iOS 먼저 출시 | 제품 책임자 |
| Apple revoke 연동 | 후속 과제 | 제품/백엔드 담당자 |

## 12. 구현 파일 영향 범위

예상 수정 파일:

- `src/main/java/com/rolling/api/domain/user/entity/SocialProvider.java`
- `src/main/java/com/rolling/api/domain/auth/dto/SocialLoginRequest.java`
- `src/main/java/com/rolling/api/domain/auth/service/AuthService.java`
- `src/main/java/com/rolling/api/global/exception/AuthException.java`
- `src/main/java/com/rolling/api/infra/apple/AppleTokenVerifier.java`
- `src/main/java/com/rolling/api/infra/apple/AppleAuthProperties.java`
- `src/main/java/com/rolling/api/infra/apple/dto/AppleUserResponse.java`
- `src/main/java/com/rolling/api/global/monitoring/ExternalDependenciesHealthIndicator.java`
- `src/test/java/com/rolling/api/domain/auth/service/AuthServiceLoginValidationTest.java`
- `docs/AGENTS.md`

DB 마이그레이션은 기본적으로 필요하지 않다. 현재 `social_provider`는 문자열 enum 저장이며 별도 CHECK 제약이 확인되지 않았다. 다만 운영 DB에 수동 CHECK 제약이 있으면 `APPLE` 허용 마이그레이션이 필요하다.

## 13. 공식 참고 문서

- [Apple - Authenticating users with Sign in with Apple](https://developer.apple.com/documentation/signinwithapple/authenticating-users-with-sign-in-with-apple)
- [Apple - Verifying a user](https://developer.apple.com/documentation/signinwithapple/verifying-a-user)
- [Apple - Configuring your environment for Sign in with Apple](https://developer.apple.com/documentation/signinwithapple/configuring-your-environment-for-sign-in-with-apple)
- [Apple - Sign in with Apple REST API authorization](https://developer.apple.com/documentation/signinwithapplerestapi/request-an-authorization-to-the-sign-in-with-apple-server.)
- [Apple - Token revocation](https://developer.apple.com/documentation/signinwithapplerestapi/revoke-tokens)

## 14. 실행 체크리스트

### 14.1 Codex가 해야 할 일

#### Phase 1. 백엔드 계약 고정

- [x] 기존 `POST /api/v1/auth/login` 엔드포인트를 유지한다.
- [x] `provider=APPLE`일 때 `accessToken` 필드에 Apple `identityToken`을 받는 것으로 계약을 확정한다.
- [x] `SocialLoginRequest` Swagger 설명과 허용값에 `APPLE`을 추가한다.
- [x] `docs/AGENTS.md`의 Apple 로그인 미구현 메모를 구현 완료 상태에 맞게 갱신한다.

#### Phase 2. Apple provider 추가

- [x] `SocialProvider` enum에 `APPLE`을 추가한다.
- [x] `AuthException.appleApiError(...)`를 추가한다.
- [x] `AuthService.providerUserInfoError(...)`가 `APPLE`을 처리하도록 확장한다.
- [x] 기존 `GOOGLE`, `KAKAO` 분기 동작이 바뀌지 않도록 변경 범위를 제한한다.

#### Phase 3. Apple identityToken 검증 구현

- [x] `infra/apple` 패키지를 추가한다.
- [x] `spring-security-oauth2-jose` 의존성을 추가한다.
- [x] Apple JWKS URL 기반 `JwtDecoder`를 구성한다.
- [x] Apple `identityToken` 서명을 검증한다.
- [x] token claim의 `iss`가 `https://appleid.apple.com`인지 검증한다.
- [x] token claim의 `aud`가 설정된 `APPLE_CLIENT_ID`와 일치하는지 검증한다.
- [x] token 만료 시간 `exp`를 검증한다.
- [x] `sub`를 Rolling의 `socialId`로 추출한다.
- [x] `email` claim이 있으면 사용자 이메일로 사용한다.
- [x] 이름이 없을 경우 기본값 `Unknown`을 사용하도록 처리한다.

#### Phase 4. 설정과 운영 준비

- [x] `application.yml`에 Apple 로그인 설정 키를 추가한다.
- [x] MVP 필수 환경 변수는 `APPLE_CLIENT_ID`만 사용한다.
- [x] `APPLE_JWKS_URL`, `APPLE_ISSUER`는 코드 기본값으로 둔다.
- [x] `.p8` private key는 이번 MVP 코드에서 사용하지 않는다.
- [x] `ExternalDependenciesHealthIndicator`에 Apple 로그인 설정 준비 상태를 추가한다.
- [x] 운영 배포 환경에서 `APPLE_CLIENT_ID` 주입 방식을 반영한다.

#### Phase 5. 테스트

- [x] Apple `sub`가 없으면 사용자를 생성하지 않고 `APPLE_API_ERROR`를 반환하는 테스트를 추가한다.
- [x] Apple token 검증 실패 시 로그인 실패 테스트를 추가한다.
- [x] Apple 신규 사용자 생성 테스트를 추가한다.
- [x] 동일 Apple `sub` 재로그인 시 기존 사용자 반환은 기존 `findOrCreateUser` 공통 경로로 유지한다.
- [x] 기존 Google/Kakao 로그인 검증 테스트가 계속 통과하는지 확인한다.
- [x] refresh, logout, withdraw 흐름이 provider와 무관하게 유지되는지 인증 서비스 테스트로 확인한다.

#### Phase 6. 통합 검증 지원

- [ ] 프론트가 전달한 실제 Apple `identityToken`으로 개발 환경 로그인을 검증한다.
- [ ] iOS 실기기에서 Apple 신규 가입, 재로그인, 이메일 비공개 선택 케이스를 확인한다.
- [x] Apple 로그인 실패 로그에 민감 토큰 값이 남지 않는지 확인한다.
- [x] 배포 전 Swagger/API 문서와 앱 요청 형식이 일치하는지 확인한다.

### 14.2 사용자가 해야 할 일

#### Phase 1. Apple Developer 정보 확정

- [x] Apple Developer Team ID 존재 여부를 확인한다.
- [x] iOS Bundle ID를 확인한다. 현재 값: `com.rolling.jiujits`
- [x] Sign in with Apple capability 활성화 여부를 확인한다.
- [x] Apple 로그인용 Key ID 존재 여부를 확인한다.
- [x] Apple 로그인용 `.p8` private key 보유 여부를 확인한다.
- [x] iOS 네이티브 앱만 Apple 로그인을 제공하기로 결정한다.
- [x] `APPLE_CLIENT_ID`는 Bundle ID인 `com.rolling.jiujits`를 사용한다.

#### Phase 2. Secret 전달 방식 결정

- [x] `APPLE_TEAM_ID` 실제 값을 백엔드 배포 담당자에게 전달한다.
- [x] `APPLE_KEY_ID` 실제 값을 백엔드 배포 담당자에게 전달한다.
- [x] `.p8` private key를 안전한 방식으로 전달하거나 배포 secret에 등록한다.
- [x] 개발/운영 서버별 secret 등록 담당자를 정한다.
- [x] private key 원본 파일을 저장할 안전한 위치와 접근 권한을 정한다.

#### Phase 3. 앱 구현 준비

- [ ] Flutter/iOS에서 사용할 Apple 로그인 패키지 또는 네이티브 구현 방식을 정한다.
- [ ] Apple 로그인 성공 후 `identityToken`을 받을 수 있는지 확인한다.
- [ ] `identityToken` 전체 값을 로그에 남기지 않도록 앱 로그 정책을 정한다.
- [ ] 서버 요청 형식을 아래 형태로 맞춘다.

```json
{
  "provider": "APPLE",
  "accessToken": "{Apple identityToken}"
}
```

#### Phase 4. 제품 정책 결정

- [ ] Apple 이메일이 null이어도 가입을 허용할지 최종 결정한다.
- [ ] Apple이 이름을 제공하지 않을 때 기본 닉네임 정책을 결정한다.
- [ ] Google/Kakao 계정과 Apple 계정의 이메일이 같아도 자동 병합하지 않는 정책을 확정한다.
- [ ] Apple 로그인은 iOS 앱에만 노출하고 Android/웹에는 노출하지 않는 정책을 확정한다.

#### Phase 5. 실기기 검수

- [ ] iOS 실기기에서 Apple 로그인 버튼이 정상 노출되는지 확인한다.
- [ ] 신규 Apple 계정으로 가입이 되는지 확인한다.
- [ ] 같은 Apple 계정으로 재로그인 시 기존 계정으로 진입하는지 확인한다.
- [ ] 이메일 공개 선택 케이스를 확인한다.
- [ ] 이메일 비공개 선택 케이스를 확인한다.
- [ ] 로그아웃 후 재로그인 흐름을 확인한다.
- [ ] 앱 심사 제출 전 Apple 로그인 관련 스크린샷/리뷰어 안내가 필요한지 확인한다.

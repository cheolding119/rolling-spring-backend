# Apple 로그인 코드 리뷰

작성일: 2026-04-29

## 1. 리뷰 범위

검토 대상:

- `POST /api/v1/auth/login` Apple provider 처리
- Apple `identityToken` 검증 경로
- Apple 로그인 설정/운영 환경변수 주입
- 외부 의존성 health detail
- 인증 서비스 단위 테스트
- API/기획 문서 갱신

주요 변경 파일:

- `build.gradle`
- `docker-compose.yml`
- `.github/workflows/deploy.yml`
- `src/main/resources/application.yml`
- `src/main/java/com/rolling/api/domain/auth/service/AuthService.java`
- `src/main/java/com/rolling/api/infra/apple/AppleTokenVerifier.java`
- `src/main/java/com/rolling/api/infra/apple/AppleAuthProperties.java`
- `src/main/java/com/rolling/api/infra/apple/dto/AppleUserResponse.java`
- `src/main/java/com/rolling/api/global/monitoring/ExternalDependenciesHealthIndicator.java`
- `src/test/java/com/rolling/api/domain/auth/service/AuthServiceLifecycleTest.java`
- `src/test/java/com/rolling/api/domain/auth/service/AuthServiceLoginValidationTest.java`

## 2. 결론

현재 변경은 기존 Google/Kakao 로그인 계약을 유지하면서 Apple provider만 추가하는 방향으로 작성되어 있다. 코드 변경 지역성이 좁고, Rolling JWT 발급/refresh/logout/withdraw 공통 흐름을 재사용하므로 기존 인증 구조와 잘 맞는다.

리뷰 결과, 배포를 막아야 할 코드 결함은 발견하지 못했다.

## 3. 확인된 강점

- 기존 API 경로 `POST /api/v1/auth/login`을 변경하지 않았다.
- `provider=APPLE`일 때만 Apple token verifier를 사용하므로 기존 Google/Kakao 호출 경로의 외부 의존성이 늘어나지 않는다.
- Apple `identityToken` 검증은 직접 JWKS/서명 검증을 수작업으로 구현하지 않고 Spring Security JOSE/Nimbus `JwtDecoder`를 사용한다.
- `iss`, `aud`, `exp` 검증이 명시적으로 구성되어 있다.
- Apple 이름이 없을 때 기존 사용자의 닉네임을 `Unknown`으로 덮어쓰지 않는다.
- 신규 Apple 사용자는 닉네임 기본값 `Unknown`을 사용해 `users.nickname NOT NULL` 조건을 지킨다.
- 운영 배포에는 MVP 필수값인 `APPLE_CLIENT_ID`만 주입한다.
- `.p8` private key는 이번 MVP 코드 경로에서 사용하지 않으므로 불필요한 secret 사용면이 늘어나지 않는다.

## 4. 발견 사항

현재 코드 기준 blocking finding 없음.

## 5. 검증 결과

실행한 테스트:

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.auth.service.*"
.\gradlew.bat test
```

결과:

- 인증 서비스 테스트 통과
- 전체 테스트 통과

검증된 경로:

- Apple 신규 사용자 로그인 시 Rolling JWT 발급
- 동일 Apple `sub` 재로그인 시 기존 사용자 반환
- Apple `sub` 누락 시 `APPLE_API_ERROR`
- Apple token 검증 실패 시 `APPLE_API_ERROR`
- 기존 Google/Kakao 로그인 검증 테스트 유지
- refresh/logout/withdraw 인증 생명주기 테스트 유지

## 6. 잔여 리스크

- 실제 Apple `identityToken`을 사용한 개발/운영 환경 통합 검증은 아직 필요하다.
- iOS 앱에서 전달하는 `identityToken`의 `aud`가 `APPLE_CLIENT_ID=com.rolling.jiujits`와 정확히 일치하는지 실기기에서 확인해야 한다.
- Apple JWKS 네트워크 장애 시 로그인 실패가 발생할 수 있다. 현재 `NimbusJwtDecoder`의 기본 JWKS 조회/캐시 동작에 의존한다.
- `APPLE_CLIENT_ID`가 운영 GitHub Secret 또는 서버 `.env`에 누락되면 `socialLogin` health가 `DOWN`이 된다.
- nonce 검증은 이번 MVP에서 구현하지 않았다. 클라이언트가 nonce를 도입하면 서버 요청/검증 계약을 추가해야 한다.

## 7. 후속 조치

- iOS 앱에서 Apple 로그인 후 `identityToken`을 `accessToken` 필드로 전달하도록 구현한다.
- 실제 iOS 실기기에서 신규 가입, 재로그인, 이메일 공개/비공개 케이스를 검증한다.
- 운영 배포 전 GitHub Actions Secret `APPLE_CLIENT_ID`가 `com.rolling.jiujits`로 등록되어 있는지 확인한다.
- Apple token revoke 또는 authorization code 교환이 필요해질 때 `APPLE_TEAM_ID`, `APPLE_KEY_ID`, `.p8` private key 사용 범위를 다시 연다.

## 8. Flutter 구현 메모

이 섹션은 Flutter 앱 담당자가 Apple 로그인을 붙일 때 필요한 최소 계약만 정리한다. 전체 API 원문은 `docs/AGENTS.md`와 `C:\rolling\.codex-shared\api-spec.md`를 기준으로 본다.

### 8.1 백엔드 요청 계약

기존 소셜 로그인 API를 그대로 사용한다.

```http
POST /api/v1/auth/login
Content-Type: application/json
```

Apple 로그인 요청 body:

```json
{
  "provider": "APPLE",
  "accessToken": "{Apple identityToken}"
}
```

주의:

- `accessToken` 필드 이름은 기존 Google/Kakao 계약을 유지하기 위한 이름이다.
- `provider=APPLE`일 때 `accessToken`에는 Apple access token이 아니라 Apple `identityToken`을 넣는다.
- `authorizationCode`, `.p8`, `Key ID`, `Team ID`는 이번 MVP 앱 요청에 보내지 않는다.
- nonce는 이번 MVP 백엔드에서 검증하지 않는다.

### 8.2 Flutter에서 확보해야 하는 값

Apple 로그인 성공 후 앱이 백엔드에 전달해야 하는 값은 하나다.

```dart
final identityToken = credential.identityToken;
```

`identityToken`이 `null`이거나 빈 문자열이면 백엔드 로그인 API를 호출하지 말고 앱에서 Apple 로그인 실패로 처리한다.

로그 정책:

- `identityToken` 전체를 로그에 남기지 않는다.
- 디버깅이 필요하면 null 여부만 확인한다.
- 토큰 앞자리 일부도 운영 로그에는 남기지 않는다.

### 8.3 응답 처리

응답 형식은 기존 Google/Kakao 로그인과 동일하다.

앱은 기존 소셜 로그인 성공 처리와 같은 방식으로 아래 값을 저장/반영하면 된다.

- `accessToken`
- `refreshToken`
- `tokenType`
- `expiresIn`
- `newUser`
- `userId`
- `email`
- `name`
- `isAdmin`
- `accountStatus`
- `suspensionUntil`
- `sanctionReasonSummary`

Apple 로그인 특성:

- Apple이 이메일을 주지 않으면 `email`은 null일 수 있다.
- Apple은 사용자 이름을 항상 주지 않는다.
- 이번 백엔드는 Apple 이름을 별도 요청 필드로 받지 않으므로 신규 Apple 사용자의 기본 이름은 `Unknown`일 수 있다.
- 기존 Apple 사용자가 재로그인할 때 Apple 이름이 없어도 기존 닉네임을 `Unknown`으로 덮어쓰지 않는다.

### 8.4 에러 처리

Apple 로그인에서 앱이 주로 처리해야 하는 서버 에러 code:

- `APPLE_API_ERROR`: Apple `identityToken`이 없거나, 유효하지 않거나, 서버 설정의 `APPLE_CLIENT_ID`와 토큰 audience가 맞지 않는 경우
- `UNSUPPORTED_PROVIDER`: `provider` 값 오타 또는 서버 미지원 provider
- `VALIDATION_ERROR`: `provider` 또는 `accessToken` 누락

권장 UX:

- `APPLE_API_ERROR`는 "Apple 로그인 정보를 확인할 수 없습니다. 다시 시도해 주세요." 수준의 재시도 가능한 메시지로 처리한다.
- `VALIDATION_ERROR`는 앱 요청 생성 문제이므로 배포 전 QA에서 잡아야 한다.
- 동일한 Apple 계정 재로그인은 `newUser=false`로 내려오는지 확인한다.

### 8.5 실기기 QA 체크리스트

- [ ] iOS 실기기에서 Apple 로그인 버튼이 표시된다.
- [ ] Apple 로그인 성공 후 `identityToken`이 null이 아니다.
- [ ] `provider=APPLE`, `accessToken=identityToken`으로 백엔드 로그인이 성공한다.
- [ ] 신규 Apple 계정 로그인 시 `newUser=true`가 내려온다.
- [ ] 같은 Apple 계정 재로그인 시 `newUser=false`가 내려온다.
- [ ] 이메일 공개 선택 시 응답 `email`이 내려오는지 확인한다.
- [ ] 이메일 비공개 선택 시 Apple relay email 또는 null 가능성을 앱이 허용한다.
- [ ] Apple 로그인 성공 후 Google/Kakao와 동일하게 토큰 저장, 자동 로그인, refresh, logout 흐름이 동작한다.

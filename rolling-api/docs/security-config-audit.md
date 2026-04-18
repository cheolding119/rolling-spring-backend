# 보안 및 설정 점검 보고서

점검 기준:
- 레포에 실제로 커밋된 설정과 코드만 우선 확인
- 로컬/배포 환경에 놓일 수 있는 비밀 파일은 별도로 분리해서 표기

## 결론

`.env`와 `firebase-service-key.json`는 현재 Git 추적 대상이 아니고 `rolling-api/.gitignore`에도 등록돼 있습니다.
즉, **레포 자체에 비밀값이 올라간 상태는 아닙니다**.

다만 이 프로젝트는 런타임에서 `.env`를 읽고, `FIREBASE_CREDENTIALS_PATH`가 설정되면 Firebase 서비스 계정 JSON을 읽도록 구성돼 있어서, **실행 환경에 실제 비밀파일이 존재하면 운영상 위험**이 됩니다.
`FIREBASE_CREDENTIALS_PATH`가 없을 때는 ADC(Application Default Credentials)로 폴백합니다.

레포 기준으로는 아래 항목이 가장 중요합니다.
- CORS가 admin.rolling-app.com과 로컬 개발 Origin만 허용하도록 제한됨
- Firebase가 기본적으로 활성화돼 있음
- AWS 정적 키, Slack Webhook, JWT Secret 같은 민감값을 환경변수로 주입하는 구조

## 확인 결과

### 1) `.env`와 `firebase-service-key.json`는 레포에 커밋되지 않음

확인 내용:
- `git ls-files -- rolling-api/.env rolling-api/firebase-service-key.json` 결과 없음
- [`rolling-api/.gitignore`](../.gitignore) 에 둘 다 등록돼 있음

의미:
- 현재 상태만 보면 레포 유출 사고는 아님
- 이전 문서에서 이 둘을 “레포에 올라간 문제”처럼 표현한 부분은 정정이 필요함

## 레포 기준으로 실제로 봐야 할 항목

### 2) 완료: CORS 제한 적용

근거:
- [`src/main/java/com/rolling/api/global/config/SecurityConfig.java`](../src/main/java/com/rolling/api/global/config/SecurityConfig.java#L140-L161)
- `configuration.setAllowedOrigins(List.of("https://admin.rolling-app.com", "http://localhost:5173", "http://127.0.0.1:5173"))`
- `configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id", "X-Trace-Id", "X-Correlation-Id", "traceparent"))`
- `configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"))`

영향:
- 브라우저 기반 클라이언트는 허용된 origin에서만 API를 호출할 수 있음
- 운영 origin과 로컬 개발 origin만 남겨 범위를 줄였다

권고:
- 로컬 개발 Origin이 더 필요하면 명시적으로 추가
- 운영 origin 외의 새 origin은 추가 전에 검토

### 3) 중간: Firebase가 기본적으로 활성화돼 있음

근거:
- [`src/main/resources/application.yml`](../src/main/resources/application.yml#L113-L116): `firebase.enabled: true`
- [`src/main/java/com/rolling/api/global/config/FirebaseAdminConfig.java`](../src/main/java/com/rolling/api/global/config/FirebaseAdminConfig.java#L56-L65): `credentialsPath` 가 없으면 `GoogleCredentials.getApplicationDefault()` 사용

영향:
- 실행 환경에 따라 의도치 않게 Firebase 연동이 켜질 수 있음
- 자격증명 경로가 없을 때 ADC로 폴백하므로, 배포 환경에서 승인되지 않은 인증 경로를 타는 문제가 생길 수 있음

권고:
- 기본값을 `false`로 두고 운영에서만 명시적으로 켜기
- prod 환경에서는 credentials path 미설정 시 fail-fast 하도록 변경

### 4) 중간: AWS S3 인증이 정적 Access Key / Secret Key 방식

근거:
- [`src/main/java/com/rolling/api/global/config/AwsS3Config.java`](../src/main/java/com/rolling/api/global/config/AwsS3Config.java#L16-L39)
- [`src/main/resources/application.yml`](../src/main/resources/application.yml#L133-L141)

영향:
- 장기 수명의 IAM 키를 환경변수로 주입하는 방식이라 유출 시 피해가 큼
- 키 회전과 권한 분리가 느슨해지기 쉬움

권고:
- EC2/ECS/EKS 환경이면 IAM Role 또는 Task Role 사용
- 정적 키는 불가피할 때만 사용

### 5) 중간: Slack Webhook URL이 환경변수 기반이지만 민감값으로 취급해야 함

근거:
- [`src/main/resources/application.yml`](../src/main/resources/application.yml#L69-L73)
- [`src/main/java/com/rolling/api/global/alert/SlackAlertConfig.java`](../src/main/java/com/rolling/api/global/alert/SlackAlertConfig.java#L21-L41)

영향:
- Webhook URL이 외부에 노출되면 알림 채널 오남용이 가능

권고:
- 운영 시크릿 저장소에서만 주입
- 유출 가능성이 있으면 Slack Webhook 재발급

### 6) 중간: JWT Secret, DB 비밀번호, 관리자 키는 환경변수 의존

근거:
- [`src/main/resources/application.yml`](../src/main/resources/application.yml#L8-L12)
- [`src/main/resources/application.yml`](../src/main/resources/application.yml#L88-L92)
- [`src/main/resources/application.yml`](../src/main/resources/application.yml#L100-L125)

영향:
- 레포에 직접 커밋된 것은 아니지만, 배포 파이프라인에서 안전하게 주입되지 않으면 위험
- 특히 `JWT_SECRET`이 약하거나 유출되면 토큰 위조 위험이 생김

권고:
- 환경변수 대신 Secret Manager 계열로 관리하는 편이 안전함
- 최소한 prod 값은 `.env` 파일이 아니라 배포 시크릿에서 주입

## 로컬/운영 파일 기준 주의사항

### 7) `firebase-service-key.json`는 레포에는 없지만, 실행 환경에 있으면 민감 파일임

이 파일은 Firebase Admin SDK 서비스 계정 키 JSON입니다.

근거:
- [`src/main/java/com/rolling/api/global/config/FirebaseAdminConfig.java`](../src/main/java/com/rolling/api/global/config/FirebaseAdminConfig.java#L56-L65)
- [`src/main/resources/application.yml`](../src/main/resources/application.yml#L113-L116)

의미:
- 레포 문제는 아니지만, 실제 실행 서버나 로컬에 놓여 있으면 강한 권한을 가진 인증 파일입니다.
- 파일 자체를 공유하거나 백업할 때 유출 위험이 큽니다.

권고:
- 배포 서버에서는 시크릿 마운트로만 전달
- 필요하면 서비스 계정 키를 회전

### 8) `.env`는 레포에는 없지만, 런타임 비밀 설정 파일로는 관리가 필요함

근거:
- [`src/main/resources/application.yml`](../src/main/resources/application.yml#L1-L3)

의미:
- 현재는 Git 추적 대상이 아니므로 레포 유출 문제는 아님
- 하지만 로컬 또는 서버에 실제 값이 들어 있는 `.env`가 있다면 그 머신의 보안 책임이 됨

권고:
- 로컬 개발용 샘플은 `.env.example`로 분리
- 운영 값은 배포 환경 시크릿으로 관리

## 정리

- 레포 유출 문제: `없음`
- 실제 주의해야 할 레포 설정 문제: `Firebase 기본 활성화`, `정적 AWS 키`, `환경변수 의존 민감정보`
- 로컬/운영 주의사항: `.env`, `firebase-service-key.json` 자체는 레포에 없지만, 실행 환경에 존재하면 민감 자산으로 취급해야 함
## Phase Checklist

### Phase 0. 운영 기준 확정

- [x] 운영 웹 도메인을 `admin.rolling-app.com`으로 확정한다.
- [x] API 도메인을 `api.rolling-app.com`으로 확정한다.
- [x] 브라우저 인증 방식은 `Authorization: Bearer {accessToken}` 유지로 확정한다.

### Phase 1. CORS 제한

- [x] `SecurityConfig`의 `allowedOrigins`를 운영 Origin과 로컬 개발 Origin만 허용하도록 바꾼다.
- [x] 로컬 개발용 Origin도 별도로 허용한다.
- [x] `allowedHeaders`와 `allowedMethods`는 필요한 값만 유지한다.
- [x] `admin.rolling-app.com`에서 로그인, 토큰 갱신, 일반 API 호출이 정상 동작하는지 확인한다.
- [x] 이 단계의 완료 기준: `*` 허용이 사라지고, 필요한 Origin만 통과한다.

### Phase 2. Firebase 운영값 정리

- [ ] 운영에서 Firebase를 실제로 사용할지 먼저 결정한다.
- [ ] 사용할 경우 `credentialsPath` 또는 Secret Manager 경로를 명시한다.
- [ ] 사용하지 않을 경우 `firebase.enabled=false`로 바꾼다.
- [ ] 기동 시 자격증명 누락을 조용히 넘기지 않고 fail-fast 하도록 맞춘다.
- [ ] 이 단계의 완료 기준: 운영 환경에서 Firebase가 의도대로만 켜진다.

### Phase 3. AWS 정적 키 제거

- [ ] `AwsS3Config`와 `application.yml`의 정적 Access Key / Secret Key 사용 여부를 확인한다.
- [ ] EC2 / ECS / EKS 중 실제 배포 환경에 맞는 IAM Role 또는 Task Role로 전환한다.
- [ ] 로컬 개발에서만 별도 자격증명을 쓰도록 분리한다.
- [ ] S3 업로드가 운영 자격으로 정상 동작하는지 확인한다.
- [ ] 이 단계의 완료 기준: 장기 고정 키를 코드/설정에 두지 않는다.

### Phase 4. Slack Webhook 비밀값 관리

- [ ] Slack Webhook URL이 설정 파일이나 이미지에 평문으로 남아 있지 않은지 확인한다.
- [ ] Webhook은 Secret Manager 또는 동일한 비밀값 저장소로 옮긴다.
- [ ] 이미 노출 가능성이 있었다면 Webhook을 교체한다.
- [ ] 알림이 실제 Slack 채널로 도착하는지 검증한다.
- [ ] 이 단계의 완료 기준: Webhook URL이 운영 비밀값으로만 취급된다.

### Phase 5. 환경변수 / 비밀값 정리

- [ ] `.env.example`를 만들어 필요한 키만 예시로 남긴다.
- [ ] 운영용 `.env`와 로컬용 `.env`를 분리한다.
- [ ] `.env`, `firebase-service-key.json` 같은 파일이 Git 추적 대상이 아닌지 재확인한다.
- [ ] JWT Secret, DB 비밀번호, 관리자 비밀값의 저장 위치를 표준화한다.
- [ ] 이 단계의 완료 기준: 비밀값이 코드/레포/이미지에 섞이지 않는다.

### Phase 6. 최종 점검

- [ ] 관리자 웹에서 로그인부터 주요 API 호출까지 전체 흐름을 점검한다.
- [ ] 앱 클라이언트에서 인증/갱신/차단 흐름이 깨지지 않았는지 확인한다.
- [ ] 운영 배포 전에 스테이징에서 한 번 더 전송 경로를 검증한다.
- [ ] 이 단계의 완료 기준: 배포 후 바로 장애가 날 수 있는 설정 리스크가 남아 있지 않다.

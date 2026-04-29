# Tech Stack

## 프로젝트 구성
- `rolling-react-admin-frontend`: 관리자 웹
- `rolling-react-randing-frontend`: 랜딩 웹
- `rolling-flutter-frontend/rolling_flutter`: 모바일 앱
- `rolling-spring-backend/rolling-api`: 백엔드 API

## 프론트엔드

### 관리자 웹
- React 19 + TypeScript + Vite
- Tailwind CSS v4
- Radix UI, Zustand, React Router, React Hook Form
- Recharts, date-fns, motion, lucide-react

### 랜딩 웹
- React 19 + TypeScript + Vite
- Tailwind CSS v4
- motion, lucide-react, clsx, tailwind-merge

### Flutter 앱
- Flutter 3.6.x
- 상태관리: GetX
- 저장소: `flutter_secure_storage`
- 네트워크: `http`
- 소셜 로그인: Google, Kakao
- Firebase: `firebase_core`, `firebase_messaging`
- 기타: `table_calendar`, `intl`, `image_picker`, `url_launcher`, `flutter_dotenv`
- 폰트: Pretendard

## 백엔드
- Java 17
- Spring Boot 4.0.1
- Gradle 기반 단일 애플리케이션 구조
- Spring Web, Security, Validation, Data JPA
- PostgreSQL + Flyway
- 테스트는 H2를 사용
- JWT 인증
- Swagger/OpenAPI
- Actuator + Prometheus
- Firebase Admin
- Google / Kakao 소셜 로그인 연동 코드 존재
- Apple 로그인은 iOS 네이티브 `identityToken`을 서버가 검증하는 방식으로 추가 예정
- Apple `identityToken` 검증은 Spring Security JOSE/Nimbus 기반 `JwtDecoder`를 사용하고, 별도 OAuth2 Client 플로우는 도입하지 않음
- AWS S3 SDK v2
- Jsoup
- 설정은 `application.yml` + 환경변수 + `.env` 조합
- 운영은 프록시 뒤에서 동작하도록 `server.forward-headers-strategy=framework` 사용

## 운영/배포
- `Dockerfile`은 Temurin 17 JDK로 빌드 후 JRE 런타임 이미지로 실행하는 멀티 스테이지 구조
- `docker-compose.yml`에서 `api`와 `nginx`를 함께 띄움
- `nginx`가 TLS 종료와 리버스 프록시를 담당
- `api.rolling-app.com` 요청은 `api:8080`으로 전달
- `rolling-app.com`과 `admin.rolling-app.com`은 Nginx가 정적 프론트 자산을 직접 서빙
- `docker-compose.monitoring.yml`에 Prometheus, Alertmanager, Grafana가 추가됨
- Actuator는 `9090` 포트에서 `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`를 노출
- Slack webhook으로 앱 알림과 모니터링 알림을 전송

## 작업 기준
- 각 프로젝트는 별도 저장소/애플리케이션처럼 다룸
- 백엔드가 인증, DB, 파일/외부 연동의 기준
- 프론트와 모바일은 백엔드 API 계약에 맞춰 동작

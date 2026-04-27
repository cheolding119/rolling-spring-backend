## 안드로이드 콘솔 프로덕션 준비중

## 서비스 소개 

Rolling은 흩어져 있는 주짓수 오픈매트 정보와 대회 정보를 한곳에서 모아 보여주는 서비스입니다.  
사용자는 지역과 일정 기준으로 정보를 탐색하고, 오픈매트 신청, 공지 확인, 알림 확인, 문의 등록까지 앱 안에서 처리할 수 있습니다.

주짓수 커뮤니티에서는 오픈매트 공지와 대회 정보가 주로 인스타그램 스토리, 단체 채팅방, 외부 사이트, 개별 게시물처럼 여러 채널에 흩어져 있어 필요한 정보를 제때 찾기 어렵다는 문제가 있었습니다.  
특히 지역별로 어떤 오픈매트가 열리는지, 참가 가능한 대회가 무엇인지 한눈에 비교하기 어려워 사용자는 반복적으로 여러 채널을 확인해야 했습니다.

Rolling은 이런 비효율을 줄이기 위해 오픈매트와 대회 정보를 한곳에 모아 탐색, 신청, 알림 확인까지 이어지는 흐름을 하나의 서비스로 통합했습니다.  
단순한 정보 나열이 아니라 실제 참여 행동까지 연결되는 구조를 목표로 했고, 사용자용 Flutter 앱과 운영용 React 관리자 페이지를 함께 구성해 확장 가능한 서비스 형태로 설계했습니다.

---

## 팀 / 역할

| 이름 | 역할 |
| --- | --- |
| 정원철 | 1인 개발로 서비스 기획, Flutter 앱 개발, React 랜딩/관리자 웹 개발, Spring 백엔드 API 개발, 데이터베이스 설계, 배포/모니터링 구조 설계까지 전 과정을 직접 수행 |

---

## 문제 정의

- 오픈매트 공지와 대회 정보가 SNS, 커뮤니티, 외부 사이트에 흩어져 있어 한눈에 확인하기 어렵습니다.
- 지역과 일정 기준으로 필요한 정보를 빠르게 탐색하기 어려워 사용자 탐색 비용이 큽니다.
- 정보 확인 이후 참가 신청, 변경사항 확인, 문의 처리까지의 흐름이 분리되어 있어 사용성이 떨어집니다.
- 초기에는 단일 API 서버 중심으로 시작했지만, 서비스가 커지면서 관리자 페이지, 랜딩 페이지, 운영 모니터링, 배포 자동화까지 함께 고려해야 했습니다.


---


## 해결 방식

- 오픈매트와 대회 정보를 하나의 앱 안에서 통합 조회할 수 있도록 구성했습니다.
- 지역, 일정, 상태 중심으로 정보를 탐색할 수 있게 해 사용자 탐색 비용을 줄였습니다.
- 신청, 알림, 공지, 문의 기능을 함께 제공해 정보 확인 이후의 행동까지 하나의 흐름으로 연결했습니다.
- 사용자 앱과 관리자 웹을 분리하고, `Nginx` + `Docker Compose` 기반 라우팅 및 배포 구조를 구성해 운영 확장성을 확보했습니다.
- `Prometheus`, `Grafana`, `Slack Webhook`, `Actuator` 기반 운영 관측 체계를 도입해 프로덕션 대응 기반을 마련했습니다.


---

## 핵심 기능

- 오픈매트 목록 조회 및 상세 확인
- 오픈매트 신청 및 취소
- 내가 신청한 오픈매트 / 내가 개최한 오픈매트 관리
- 대회 목록 조회 및 상세 확인
- 공지사항 조회
- 앱 내 알림 확인
- 1:1 문의 등록 및 답변 확인
- 사용자 차단 및 차단 사용자 목록 관리
- 사용자 차단 및 차단 사용자 목록 관리 
- **관리자 전용 운영 기능**: 공지 운영, 문의 답변, 신고 처리, 대회 크롤링 실행, 사용자 제재 관리 

## 사용자 흐름

1. 사용자가 Google 또는 Kakao 소셜 로그인으로 앱에 진입한다.
2. 오픈매트 / 대회 정보를 지역과 일정 기준으로 탐색한다.
3. 원하는 오픈매트에 참가 신청한다.
4. 공지사항과 알림을 통해 변경사항을 확인한다.
5. 필요하면 특정 사용자를 차단해 탐색 및 상세 조회에서 제외할 수 있다. 
6. 필요하면 문의를 남기고 답변을 확인한다.
7. 운영자는 React 관리자 페이지에서 공지, 문의, 신고, 크롤링, 사용자 제재를 관리한다. 


## 주요 화면

썸네일을 클릭하면 더 큰 이미지로 이동합니다.

| 로그인 | 메인 페이지 | 오픈매트 목록 |
| :---: | :---: | :---: |
| <a href="./docs/screenshots.md#user-login"><img src="assets/images/users/login.jpg" width="250" alt="로그인"></a> | <a href="./docs/screenshots.md#user-main-page"><img src="assets/images/users/main_page.jpg" width="250" alt="메인 페이지"></a> | <a href="./docs/screenshots.md#user-openmat-list"><img src="assets/images/users/openmat_list.jpg" width="250" alt="오픈매트 목록"></a> |

| 오픈매트 신청 | 개최한 오픈매트 | 대회 목록 |
| :---: | :---: | :---: |
| <a href="./docs/screenshots.md#user-openmat-sign-up"><img src="assets/images/users/openmat_sign_up.jpg" width="250" alt="오픈매트 신청"></a> | <a href="./docs/screenshots.md#user-subject-open-mat"><img src="assets/images/users/subject_open_mat.jpg" width="250" alt="개최한 오픈매트"></a> | <a href="./docs/screenshots.md#user-tournaments-list"><img src="assets/images/users/tournaments_list.jpg" width="250" alt="대회 목록"></a> |

### 관리자 주요 화면


| 운영 대시보드 | 신고 관리 |
| :---: | :---: |
| <a href="./docs/screenshots.md#admin-dashboard"><img src="assets/images/admin/메인%20대시보드.jpg" width="420" alt="운영 대시보드"></a> | <a href="./docs/screenshots.md#admin-reports"><img src="assets/images/admin/신고%20관리.jpg" width="420" alt="신고 관리"></a> |

| 문의 관리 | 문의 답변 |
| :---: | :---: |
| <a href="./docs/screenshots.md#admin-inquiries"><img src="assets/images/admin/문의%20관리.jpg" width="420" alt="문의 관리"></a> | <a href="./docs/screenshots.md#admin-inquiry-reply"><img src="assets/images/admin/문의%20답변.jpg" width="420" alt="문의 답변"></a> |

| 공지사항 관리 | 공지사항 작성 |
| :---: | :---: |
| <a href="./docs/screenshots.md#admin-notices"><img src="assets/images/admin/공지사항%20관리.jpg" width="420" alt="공지사항 관리"></a> | <a href="./docs/screenshots.md#admin-notice-create"><img src="assets/images/admin/공지사항%20작성.jpg" width="420" alt="공지사항 작성"></a> |

| 대회 운영 | 대회 수동 등록 |
| :---: | :---: |
| <a href="./docs/screenshots.md#admin-tournaments"><img src="assets/images/admin/대회%20운영.jpg" width="420" alt="대회 운영"></a> | <a href="./docs/screenshots.md#admin-tournament-create"><img src="assets/images/admin/대회%20수동%20등록.jpg" width="420" alt="대회 수동 등록"></a> |

| 대회 크롤링 실행 |  |
| :---: | :---: |
| <a href="./docs/screenshots.md#admin-tournament-crawling"><img src="assets/images/admin/대회%20운영%20크롤링%20실행.jpg" width="420" alt="대회 크롤링 실행"></a> |  |

## 기술 스택

### Frontend

**App Development**
![Flutter](https://img.shields.io/badge/Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white)
![Dart](https://img.shields.io/badge/Dart-0175C2?style=for-the-badge&logo=dart&logoColor=white)

**State Management / Architecture**
![GetX](https://img.shields.io/badge/GetX-8A2BE2?style=for-the-badge&logo=flutter&logoColor=white)

**Admin Web**
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white) 
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white) 
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS_v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white) 

**Landing Web** 
![React](https://img.shields.io/badge/React_19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB) 
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white) 
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white) 

**Networking / Storage**
![HTTP](https://img.shields.io/badge/HTTP-00599C?style=for-the-badge&logo=flutter&logoColor=white)
![Flutter Secure Storage](https://img.shields.io/badge/Secure_Storage-4A148C?style=for-the-badge&logo=flutter&logoColor=white)
![Dotenv](https://img.shields.io/badge/flutter_dotenv-222222?style=for-the-badge&logo=.env&logoColor=white)

**Authentication / Notification**
![Google Sign-In](https://img.shields.io/badge/Google_Sign--In-4285F4?style=for-the-badge&logo=google&logoColor=white)
![Kakao Login](https://img.shields.io/badge/Kakao-FFCD00?style=for-the-badge&logo=kakaotalk&logoColor=000000)
![Firebase](https://img.shields.io/badge/Firebase_Messaging-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)


---

### Backend

**Core**
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)

**Security / Auth**
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

**Data / API**
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-59666C?style=for-the-badge&logo=spring&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![Validation](https://img.shields.io/badge/Spring_Validation-0A7E07?style=for-the-badge)
![OpenAPI](https://img.shields.io/badge/OpenAPI-6BA539?style=for-the-badge&logo=openapiinitiative&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)

**Integration / Utility**
![Jsoup](https://img.shields.io/badge/Jsoup-2E7D32?style=for-the-badge)
![Firebase Admin](https://img.shields.io/badge/Firebase_Admin_SDK-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)

**Database / Monitoring**
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Spring Actuator](https://img.shields.io/badge/Spring_Actuator-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Micrometer](https://img.shields.io/badge/Micrometer-455A64?style=for-the-badge)

---

### Infrastructure / Operation

**Cloud / Delivery**
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![Amazon S3](https://img.shields.io/badge/Amazon_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![Lightsail](https://img.shields.io/badge/AWS_Lightsail-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

**Container / Routing**
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)

**Push Notification**
![Firebase Cloud Messaging](https://img.shields.io/badge/FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

**Monitoring / Health**
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)
![Alertmanager](https://img.shields.io/badge/Alertmanager-E6522C?style=for-the-badge&logo=prometheus&logoColor=white) 
![Spring Actuator](https://img.shields.io/badge/Health_Check-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Slack](https://img.shields.io/badge/Slack_Webhook-4A154B?style=for-the-badge&logo=slack&logoColor=white)

**Documentation**
![Swagger](https://img.shields.io/badge/Swagger/OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)


## 시스템 아키텍처

포트폴리오 관점에서 서비스의 전체 흐름과 운영 구조를 한눈에 보여주기 위해 아키텍처 다이어그램을 함께 정리했습니다.

![롤링 시스템 아키텍처](assets/images/architecture/롤링_아키텍처_다이어그램.png)


## 프로젝트 구조

### Frontend

- Flutter 앱과 React 관리자 웹을 분리해 사용자 기능과 운영 기능을 구분
- Feature-based 구조
- MVVM 기반 화면 구성
- GetX를 이용한 상태 관리, 의존성 주입, 라우팅

### Backend

- REST API 기반 서버
- 인증/인가 분리
- 도메인별 API 구성
- 관리자 기능 별도 운영 API 제공
- 스케줄러, 알림, 모니터링, 운영 정책을 포함한 구조

## API / 도메인 구성

- **인증(Auth)**: 소셜 로그인, 토큰 발급 및 갱신, 로그아웃, 회원 탈퇴 예약/자동 처리 배치와 같은 사용자 인증 흐름을 담당합니다.
- **사용자(User)**: 내 정보 조회 및 수정, FCM 토큰 등록 및 삭제, 사용자 차단 및 차단 목록 조회 기능을 제공합니다. 
- **오픈매트(OpenMat)**: 오픈매트 생성, 수정, 삭제, 목록/상세 조회, 참가 신청 및 취소, 내가 신청한 목록과 내가 개최한 목록 조회, 모집 상태 변경, 참가자 관리 기능을 담당합니다.
- **대회(Tournament)**: 대회 목록 및 상세 조회, 수동 등록/수정/삭제, 관리자 전용 크롤링 실행 기능을 제공합니다.
- **알림(Notification)**: 사용자 알림 목록 조회와 읽음 처리 기능을 담당하며, 오픈매트 변경/삭제와 문의 답변 같은 이벤트를 관리합니다.
- **공지사항(Notice)**: 일반 사용자 대상 공지사항 목록/상세 조회와 관리자 전용 공지 등록, 수정, 삭제 기능으로 구성됩니다.
- **문의(Inquiry)**: 사용자의 문의 등록, 내 문의 목록/상세 조회 기능과 관리자 전용 문의 답변 및 상태 변경 기능을 포함합니다.
- **신고(Report)**: 신고 생성, 관리자 전용 신고 목록/상세 조회, 상태 변경, 누적 신고 대상 요약 기능을 포함합니다.
- **관리자(Admin)**: 공지 운영, 대회 크롤링 실행, 문의 응답 처리, 신고 및 운영 데이터 관리, 사용자 제재 이력 조회/생성/해제 기능을 담당합니다. 


## 운영 정책 또는 비즈니스 규칙

- 오픈매트는 생성, 신청, 취소, 모집 상태 변경 흐름을 기준으로 관리되며 삭제 시에는 soft delete 정책을 사용합니다.
- 오픈매트는 정원 초과, 종료 시간, 신고 누적 상태에 따라 신청 가능 여부가 제한됩니다.
- 공지사항은 일반 사용자에게는 조회 전용으로 제공되며, 관리자만 생성, 수정, 삭제할 수 있습니다.
- 알림은 FCM 수신 여부와 별개로 백엔드에 저장된 알림 데이터를 기준으로 관리합니다.
- 문의 기능은 사용자는 본인 문의만 조회할 수 있고, 관리자는 전체 문의 조회 및 답변 처리를 수행할 수 있습니다.
- 사용자는 차단한 사용자의 오픈매트 및 수동 등록 대회를 탐색/상세 조회에서 제외할 수 있습니다. 
- 관리자는 사용자 상태와 제재 이력을 조회하고, 경고 또는 일시정지를 부여하거나 해제할 수 있습니다.
- 관리자 기능은 `Authorization: Bearer {accessToken}` 기반 인증과 `ROLE_ADMIN` 권한 검사로 보호됩니다.
- 운영 환경 DB 스키마는 `Flyway` 기반 버전 관리로 반영합니다.
- 스케줄러와 외부 의존성 상태는 `Actuator`와 커스텀 메트릭을 통해 모니터링합니다.


## 트러블슈팅

### 1. 오픈매트 신청 시 정원 초과가 발생할 수 있는 동시성 문제
- **문제:** 정원이 1명 남은 오픈매트에 여러 사용자가 동시에 신청하면 중복 승인으로 인해 정원 초과가 발생할 수 있었습니다.
- **원인:** 단순 조회 후 신청하는 구조만으로는 동시에 들어온 요청 간 정합성을 보장하기 어려웠고, 정원 경계 상황에서 동일 오픈매트에 대한 동시 접근을 안전하게 제어할 수 없었습니다.
- **해결:** 신청 API에 **`PESSIMISTIC_WRITE`** 락을 적용해 동일 오픈매트에 대한 요청을 직렬화하고, 정원 검증과 상태 변경을 하나의 트랜잭션 안에서 처리하도록 구성했습니다. 이후 멀티스레드 통합 테스트를 추가해 동시 요청 상황에서도 1건만 성공하는지 검증했습니다.
- **검증:** 동시 요청 2건이 동시에 들어오는 통합 테스트에서 1건만 성공하고, 나머지 요청은 `OPEN_MAT_CLOSED` 또는 `CAPACITY_FULL`로 실패하는 것을 확인했습니다. 
- **결과:** 오픈매트 신청 로직의 정합성을 DB 트랜잭션 수준에서 보장해 정원 초과 신청 문제를 방지했습니다.

### 2. 인증/인가 경계가 불명확한 다중 API 환경의 보안 문제
- **문제:** 공개 API, 사용자 API, 관리자 API, Actuator 엔드포인트가 함께 존재하는 구조에서 권한 검증이 일관되지 않으면 보안 누락이나 과도한 차단이 발생할 수 있었습니다.
- **원인:** 단순히 인증 여부만 확인하는 방식으로는 관리자 전용 기능, 운영 엔드포인트, 제재 계정에 대한 세밀한 접근 제어를 분리하기 어려웠습니다.
- **해결:** **Spring Security** 기반으로 보안 필터 체인을 분리하고, **JWT** 인증 이후 `UserPrincipal`에 사용자 권한과 계정 상태를 반영하도록 구성했습니다. 또한 제재된 사용자는 일부 경로만 접근할 수 있도록 별도 필터를 적용하고, 공개/인증/관리자/운영 경계를 통합 테스트로 검증했습니다.
- **검증:** 공개 엔드포인트 허용, 관리자 엔드포인트 차단, `actuator` 접근 제어, CORS preflight, request tracking header 동작까지 보안 통합 테스트로 확인했습니다. 
- **결과:** 권한 경계를 명확히 분리해 관리자 및 운영 기능을 보다 안전하게 보호하면서도 공개 API의 접근성은 유지할 수 있었습니다.

### 3. 관리자 조회 API에서의 N+1 문제와 집계 데이터 조회 성능 저하
- **문제:** 관리자 목록 조회 시 작성자, 신고자 등 연관 엔티티와 참가자 수 같은 집계 데이터를 함께 조회하면서 쿼리가 반복 실행될 수 있는 성능 이슈가 있었습니다.
- **원인:** 연관 엔티티의 LAZY 로딩으로 인해 목록 크기만큼 추가 쿼리가 발생할 수 있었고, 집계 정보도 별도 최적화 없이 조회하면 전체 응답 성능이 저하될 수 있었습니다.
- **해결:** **`@EntityGraph`**를 활용해 필요한 연관 엔티티 조회를 명시적으로 제어하고, 집계 데이터는 별도 최적화 쿼리로 분리했습니다. 또한 **Hibernate statistics 기반 테스트**를 작성해 쿼리 수와 지연 로딩 발생 여부를 수치로 검증했습니다.
- **검증:** 관리자 조회 경로별로 prepared statement 수 상한을 검증해 오픈매트 목록은 5개 이하, 문의 목록은 2개 이하, 신고 목록은 3개 이하로 통제되고 추가 lazy fetch가 발생하지 않음을 확인했습니다. 
- **결과:** 관리자 조회 API의 쿼리 수를 예측 가능한 수준으로 통제해 응답 성능과 조회 안정성을 개선했습니다.

### 4. 프로덕션 환경의 백그라운드 작업 및 운영 장애 감지 한계
- **문제:** 상태 동기화, 대회 크롤링, 탈퇴 처리 배치 같은 백그라운드 작업이 실패해도 사용자의 제보 이전에는 관리자가 즉시 인지하기 어려웠습니다.
- **원인:** 작업별 실행 상태와 실패 이력을 일관되게 수집하고 추적할 수 있는 운영 관측 체계가 부족했습니다.
- **해결:** **`ScheduledTaskTracker`**를 도입해 작업의 시작, 성공, 실패 상태를 추적하고, 이를 **Spring Actuator, Prometheus, Grafana, Slack Webhook**과 연동해 health check, 커스텀 메트릭, 운영 알림 체계를 구성했습니다.
- **검증:** scheduler health indicator와 메트릭 기록 테스트를 추가해 배치 작업 단위의 최근 실행 상태가 헬스 체크와 운영 지표에 반영되는지 확인했습니다. 
- **결과:** 배치 작업의 최근 실행 상태와 실패 이력을 빠르게 파악할 수 있게 되었고, 이상 징후를 이전보다 빠르게 감지할 수 있는 기반을 마련했습니다.

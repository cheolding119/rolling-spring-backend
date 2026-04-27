# Rolling 🥋

> 주짓수 오픈매트 & 대회 정보를 지역별로 한눈에 볼 수 있는 커뮤니티 앱





   

## 📌 프로젝트 소개

Rolling은 주짓수 수련자들을 위한 오픈매트 및 대회 정보 통합 플랫폼입니다.

### 배경
- 오픈매트 공지가 개인 SNS(인스타그램 스토리 등)에 분산되어 있어 찾기 어려움
- 대회 정보가 여러 사이트에 흩어져 있어 지역별로 확인하기 불편함
- 주짓수 커뮤니티에 특화된 정보 플랫폼 부재

### 목표
- 지역별 오픈매트 정보를 한눈에 확인
- 대회 일정을 지역 필터링으로 편리하게 조회
- 주짓수 수련자들 간 소통 공간 제공

개발 기간: 2025.01 ~ 진행 중

## 🛠 기술 스택

### Backend
- **Spring Boot 4.0.1**
- **Spring Security 6.x** (JWT/OAuth2)
- **JPA/Hibernate**
- **MySQL 8.0**

### Frontend
- **Flutter**

## 운영 시크릿 관리

이 저장소에는 운영 시크릿을 커밋하지 않는다.

- 로컬 실행 값은 `.env`로 관리하고 Git에는 포함하지 않는다.
- 운영 배포 값은 GitHub Secrets 또는 서버 secret 파일로 주입한다.
- Firebase service account, AWS key, DB credential, JWT secret, Slack webhook은 repository에 저장하지 않는다.
- 필요한 환경변수 이름은 `rolling-api/src/main/resources/application.yml`과 `.github/workflows/deploy.yml`의 placeholder를 기준으로 확인한다.

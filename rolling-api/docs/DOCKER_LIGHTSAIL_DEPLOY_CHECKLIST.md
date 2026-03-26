# Docker + Lightsail 운영 배포 체크리스트

현재 기준:
- DB는 Lightsail 호스트에 이미 설치된 PostgreSQL을 그대로 사용
- 인증서는 호스트의 certbot이 이미 관리
- Docker는 `api`와 `nginx`만 실행

## 1. 배포 전 확인

- [ ] Lightsail 인스턴스에 Docker와 Docker Compose가 설치되어 있는지 확인한다.
- [ ] 기존 jar 방식으로 실행 중인 Spring Boot 프로세스를 중지한다.
- [ ] Lightsail 방화벽/보안 그룹에서 `80`, `443` 포트가 열려 있는지 확인한다.
- [ ] PostgreSQL이 호스트에서 실행 중인지 확인한다.
- [ ] PostgreSQL이 Docker 컨테이너에서 접근 가능한 설정인지 확인한다.

## 2. 인증서 확인

- [ ] 호스트에서 `sudo certbot certificates`를 실행해서 `rolling-app.com` 인증서가 존재하는지 확인한다.
- [ ] 인증서 경로가 아래와 같이 존재하는지 확인한다.
  - [ ] `/etc/letsencrypt/live/rolling-app.com/fullchain.pem`
  - [ ] `/etc/letsencrypt/live/rolling-app.com/privkey.pem`
- [ ] certbot 갱신이 기존 호스트 방식으로 정상 동작하는지 확인한다.

## 3. 서버 파일 준비

- [ ] 레포를 Lightsail 서버로 배포한다.
- [ ] 서버 루트에 `.env` 파일을 준비한다.
- [ ] `.env`에 `SERVER_NAME=rolling-app.com`을 설정한다.
- [ ] `.env`에 `DB_URL=jdbc:postgresql://host.docker.internal:5432/rolling`을 설정한다.
- [ ] `.env`에 `DB_USERNAME`, `DB_PASSWORD`를 설정한다.
- [ ] `.env`에 `JWT_SECRET`을 설정한다.
- [ ] `.env`에 AWS 관련 값들을 설정한다.
- [ ] Firebase를 쓸 경우 `secrets/firebase-service-key.json` 파일을 서버에 둔다.
- [ ] Firebase를 쓸 경우 `.env`의 `FIREBASE_ENABLED=true`를 확인한다.

## 4. DB 연결 확인

- [ ] Docker 컨테이너에서 호스트 PostgreSQL로 접근 가능한지 확인한다.
- [ ] 필요하면 PostgreSQL의 `listen_addresses` 설정을 확인한다.
- [ ] 필요하면 `pg_hba.conf`에서 Docker 접속을 허용한다.
- [ ] DB 접속 계정과 비밀번호가 실제 운영 DB와 일치하는지 확인한다.

## 5. Docker 구성 확인

- [ ] [Dockerfile](/C:/rolling/rolling-spring-backend/rolling-api/Dockerfile)이 빌드 가능한지 확인한다.
- [ ] [docker-compose.yml](/C:/rolling/rolling-spring-backend/rolling-api/docker-compose.yml)에서 `api`와 `nginx`만 사용하도록 확인한다.
- [ ] nginx가 `/etc/letsencrypt`를 읽도록 마운트되어 있는지 확인한다.
- [ ] nginx가 `SERVER_NAME` 값을 사용하도록 설정되어 있는지 확인한다.

## 6. 최초 실행

- [ ] `docker compose up -d api nginx`를 실행한다.
- [ ] `docker compose logs -f api`로 API 기동 로그를 확인한다.
- [ ] `docker compose logs -f nginx`로 nginx 기동 로그를 확인한다.
- [ ] API가 정상적으로 8080에서 올라왔는지 확인한다.
- [ ] nginx가 80/443 포트를 정상적으로 바인딩했는지 확인한다.

## 7. HTTPS 확인

- [ ] `https://rolling-app.com`으로 접속해 본다.
- [ ] 브라우저에서 인증서가 유효한지 확인한다.
- [ ] HTTP 접속이 HTTPS로 리다이렉트되는지 확인한다.
- [ ] API 응답이 nginx를 통해 정상 전달되는지 확인한다.

## 8. 기능 확인

- [ ] 로그인/API 주요 엔드포인트가 정상 동작하는지 확인한다.
- [ ] DB 조회/저장이 정상 동작하는지 확인한다.
- [ ] 이미지 업로드/S3 연동이 정상 동작하는지 확인한다.
- [ ] Firebase 푸시가 필요한 기능이 있으면 실제 알림이 동작하는지 확인한다.
- [ ] 스케줄러가 중복 없이 정상 실행되는지 확인한다.

## 9. 운영 전환

- [ ] 기존 jar 기반 서비스 자동 실행 설정을 제거한다.
- [ ] Docker Compose만 재부팅 후에도 자동으로 올라오게 설정한다.
- [ ] 장애 대응을 위해 `docker compose logs`, `docker compose restart` 절차를 정리한다.
- [ ] 도메인과 운영 포트가 외부에서 정상 접근되는지 최종 확인한다.

## 10. 참고 명령어

```bash
sudo certbot certificates
docker compose up -d api nginx
docker compose logs -f api
docker compose logs -f nginx
docker compose restart nginx
```


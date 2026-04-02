# Rolling 랜딩 페이지 Nginx 전환 기획서

## 목적

- `landing.rolling-app.com` 같은 별도 랜딩 서브도메인을 만들지 않는다.
- 메인 루트 도메인인 `rolling-app.com`으로 접속하면 React 랜딩 페이지가 노출되도록 구조를 재정리한다.
- `admin.rolling-app.com`은 관리자 React, `api.rolling-app.com`은 Spring API로 유지한다.
- 이 문서는 구현 예시 코드가 아니라, 어떤 파일을 어떤 방향으로 정리해야 하는지 정리한 기획 문서다.

---

## 1. 현재 상태

현재 저장소 기준 구조는 아래와 같다.

- `admin.rolling-app.com` -> 관리자 React
- `rolling-app.com` -> Spring API 프록시
- `api.rolling-app.com` -> Spring API 프록시

현재 확인 기준 파일:

- 운영 반영 기준: [nginx/default.conf](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default.conf)
- 배포 마운트 기준: [docker-compose.yml](/C:/rolling/rolling-spring-backend/rolling-api/docker-compose.yml)
- HTTP 템플릿: [nginx/default-http.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-http.conf.template)
- HTTPS 템플릿: [nginx/default-https.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-https.conf.template)

현재 구조에서 봐야 할 포인트:

- 지금 배포는 `nginx/default.conf`를 컨테이너에 직접 마운트하고 있다.
- 따라서 실제 런타임 반영 기준은 `default.conf`다.
- 다만 `default-http.conf.template`, `default-https.conf.template`도 별도로 존재한다.
- 현재 `default-https.conf.template`는 `admin`용 블록은 분리돼 있지만, `rolling-app.com`과 `api.rolling-app.com`은 아직 같은 HTTPS 블록으로 묶여 있다.
- 현재 `default.conf`도 `rolling-app.com`과 `api.rolling-app.com`이 같은 블록으로 묶여 있어, 메인 루트 도메인으로 들어오면 API 프록시로 처리된다.

정리:

- 실제 운영 반영은 `default.conf`가 담당한다.
- 하지만 설정 정합성 유지를 위해 `default-http.conf.template`, `default-https.conf.template`도 함께 수정 대상으로 본다.

---

## 2. 목표 구조

이번 전환의 목표 구조는 아래와 같다.

- `rolling-app.com` -> 랜딩 React
- `www.rolling-app.com` -> `rolling-app.com`으로 301 리다이렉트
- `admin.rolling-app.com` -> 관리자 React
- `api.rolling-app.com` -> Spring API

이번 기획에서 같이 고정되는 정책:

- 랜딩 전용 서브도메인은 따로 만들지 않는다.
- 메인 루트 도메인 `rolling-app.com` 자체를 랜딩 페이지 주소로 사용한다.
- API는 계속 `api.rolling-app.com`으로 분리한다.
- 관리자 웹은 계속 `admin.rolling-app.com`으로 유지한다.

현재 랜딩 페이지 기준:

- 랜딩 페이지는 정적 파일이다.
- 현재는 API 연동이 필요 없다.

이 구조의 의미:

- 메인 도메인의 역할이 명확해진다.
- 사용자 입장에서 가장 자연스러운 진입 주소를 유지할 수 있다.
- 랜딩과 API 책임이 도메인 단위로 분리된다.
- 이후 랜딩이 API를 사용하게 되더라도 `api.rolling-app.com`을 그대로 붙이면 된다.

이 방향은 충분히 괜찮은 방식이다.

- 브랜딩 관점에서 자연스럽다.
- 운영 복잡도도 낮다.
- 랜딩이 정적이라면 메인 루트에 두는 쪽이 가장 단순하다.

---

## 3. 이번 변경의 핵심 판단

이번 변경에서 중요한 판단은 아래와 같다.

### 3.1 메인 루트 도메인에 랜딩을 두는 방향이 기본안이다

- `rolling-app.com` 자체를 랜딩 주소로 쓰는 것은 일반적인 구조다.
- 별도 랜딩 서브도메인을 두는 것보다 사용자 경험과 브랜딩 측면에서 더 자연스럽다.
- 현재 랜딩이 정적 페이지라면 굳이 추가 서브도메인을 만들 이유가 크지 않다.

### 3.2 `rolling-app.com`과 `api.rolling-app.com`은 같은 HTTPS 블록에 두면 안 된다

- 지금 구조처럼 메인 도메인과 API 도메인을 같은 `server_name` 블록으로 묶으면 둘 다 같은 업스트림으로 처리된다.
- 메인 루트 도메인에서 랜딩 페이지를 띄우려면 `rolling-app.com` 전용 HTTPS 블록이 반드시 필요하다.
- `api.rolling-app.com`은 별도 HTTPS 블록으로 분리해야 한다.

### 3.3 `default.conf`만 수정하고 끝내지 않는다

- 현재 운영 반영은 `default.conf`가 맞다.
- 하지만 템플릿 파일을 그대로 두면 나중에 템플릿 기반 기동으로 돌아갔을 때 예전 설정이 다시 살아날 수 있다.
- 따라서 `default.conf`와 템플릿 파일을 같이 맞춰 두는 것이 안전하다.

### 3.4 HTTP와 HTTPS 모두 목표 구조와 일치해야 한다

- HTTP는 인증서 발급용 `.well-known` 처리와 HTTPS 리다이렉트 정책이 일관돼야 한다.
- HTTPS는 실제 서비스 도메인 역할 분리가 정확해야 한다.
- 즉 `default-http.conf.template`와 `default-https.conf.template`를 같이 정리해야 한다.

### 3.5 랜딩과 관리자 정적 파일은 반드시 분리한다

- 현재 관리자 React는 단일 정적 경로를 사용 중이다.
- 랜딩 React를 추가할 때 같은 Nginx root를 재사용하면 운영 중 충돌 가능성이 높다.
- 랜딩과 관리자 정적 파일은 별도 경로, 별도 root로 관리해야 한다.

### 3.6 SPA라면 `try_files` 처리가 필요하다

- 랜딩이나 관리자 페이지가 React SPA라면 새로고침 시 정적 파일 경로가 직접 존재하지 않을 수 있다.
- 따라서 랜딩과 관리자 쪽 모두 `index.html`로 fallback 되는 구성이 필요하다.

---

## 4. 수정해야 하는 파일

이번 기획 기준 수정 대상은 아래처럼 나눈다.

### 4.1 실제 운영 반영 파일

- [nginx/default.conf](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default.conf)
- [docker-compose.yml](/C:/rolling/rolling-spring-backend/rolling-api/docker-compose.yml)

역할:

- `default.conf`는 현재 컨테이너에 직접 마운트되는 실제 운영 설정이다.
- `docker-compose.yml`은 Nginx가 어떤 설정 파일과 어떤 정적 파일 경로를 마운트할지 결정한다.

### 4.2 정합성 유지용 템플릿 파일

- [nginx/default-http.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-http.conf.template)
- [nginx/default-https.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-https.conf.template)

역할:

- 현재 즉시 반영 대상은 아니더라도, 향후 템플릿 기반 기동 시 동일한 도메인 정책을 유지하기 위해 함께 수정해야 한다.

### 4.3 이번 범위에서 검토만 필요한 파일

- [nginx/10-select-template.sh](/C:/rolling/rolling-spring-backend/rolling-api/nginx/10-select-template.sh)

판단:

- 현재 스크립트 자체는 템플릿 선택 역할이라 도메인 정책을 직접 결정하지 않는다.
- 따라서 우선순위는 낮다.
- 다만 템플릿 운용을 다시 살릴 계획이면 동작 정합성 차원에서 같이 검토해야 한다.

---

## 5. 작업 방향

### 5.1 Nginx 도메인 역할 재정의

아래 역할로 완전히 분리한다.

- `rolling-app.com`과 `www.rolling-app.com`은 랜딩 React 전용
- `admin.rolling-app.com`은 관리자 React 전용
- `api.rolling-app.com`은 Spring API 전용

핵심은 메인 도메인과 API 도메인을 같은 HTTPS `server_name` 블록으로 묶지 않는 것이다.

### 5.2 HTTP 설정 정리

HTTP 설정은 아래 기준으로 정리한다.

- `rolling-app.com`
- `www.rolling-app.com`
- `admin.rolling-app.com`
- `api.rolling-app.com`

위 도메인에 대해:

- `/.well-known/acme-challenge/`는 정상적으로 열어둔다.
- 나머지 요청은 HTTPS로 일관되게 리다이렉트한다.

즉 `default-http.conf.template`도 목표 도메인 구조를 반영하도록 수정해야 한다.

### 5.3 HTTPS 설정 정리

HTTPS 설정은 아래 기준으로 정리한다.

- 메인 도메인 block은 랜딩 React를 서빙한다.
- `www`는 `rolling-app.com`으로 301 리다이렉트한다.
- 관리자 도메인 block은 관리자 React를 서빙한다.
- API 도메인 block은 Spring API만 프록시한다.
- 메인 도메인 block에는 `/api` 프록시를 두지 않는다.

즉 `default-https.conf.template`도 최종 구조와 동일한 역할 분리 상태가 되도록 수정해야 한다.

### 5.4 Docker Compose 정리

`docker-compose.yml`은 아래 방향으로 정리한다.

- 랜딩 React 정적 파일 마운트 경로를 추가한다.
- 관리자 React 정적 파일 마운트 경로와 분리한다.
- Nginx 내부 root 경로도 랜딩용과 관리자용을 분리한다.

결론적으로 아래 두 경로는 서로 독립적으로 운영되어야 한다.

- 랜딩 `dist`: `/home/ubuntu/ROLLING-REACT-LANDING/dist`
- 관리자 `dist`: `/home/ubuntu/ROLLING-REACT-ADMIN-FRONTEND/dist`

권장 Nginx 내부 마운트 경로는 아래와 같다.

- 랜딩 root: `/usr/share/nginx/landing`
- 관리자 root: `/usr/share/nginx/admin`

### 5.5 운영 사전 점검 항목

구현 전에 아래 항목을 먼저 확인해야 한다.

- `rolling-app.com`, `www.rolling-app.com`, `admin.rolling-app.com`, `api.rolling-app.com`의 DNS 연결 상태
- 인증서가 위 도메인 범위를 모두 포함하는지 여부
- 랜딩과 관리자 React의 빌드 산출물 경로를 서버에 분리 배치할 수 있는지 여부
- 랜딩과 관리자 페이지가 React SPA인지 여부
- SPA라면 `index.html` fallback 구조를 Nginx에 반영할 수 있는지 여부

현재 랜딩 페이지는 API 연동이 없으므로, 이번 전환의 우선 검토 항목에서 랜딩 기준 CORS는 필수 이슈가 아니다.

---

이 문서는 여기까지를 이번 전환의 기획 범위로 본다.

다음 단계에서는 이 기획을 기준으로 실제 설정 파일 변경안과 반영 순서를 따로 정리하면 된다.

---

## 6. Codex 진행 체크리스트

### PHASE 1. 현재 설정 정합성 재확인

- [x] [nginx/default.conf](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default.conf)에서 `rolling-app.com`과 `api.rolling-app.com`이 같은 블록으로 묶여 있는 현재 상태를 다시 확인한다.
- [x] [nginx/default-http.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-http.conf.template)와 [nginx/default-https.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-https.conf.template)의 현재 역할 분리 상태를 다시 확인한다.
- [x] [docker-compose.yml](/C:/rolling/rolling-spring-backend/rolling-api/docker-compose.yml)에서 Nginx 설정 파일 마운트 방식과 정적 파일 마운트 경로를 다시 확인한다.
- [x] 현재 문서의 목표 구조와 실제 설정 간 차이를 최종 정리한다.

PHASE 1 확인 결과:

- 현재 운영 반영 파일은 [nginx/default.conf](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default.conf)이며, [docker-compose.yml](/C:/rolling/rolling-spring-backend/rolling-api/docker-compose.yml)에서 직접 `/etc/nginx/conf.d/default.conf`로 마운트되고 있다.
- 현재 [nginx/default.conf](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default.conf)는 `rolling-app.com`과 `api.rolling-app.com`을 같은 HTTP/HTTPS 블록에 두고 있어, 메인 루트 도메인으로 들어오는 요청도 API 프록시로 처리된다.
- 현재 [nginx/default-https.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-https.conf.template)도 `admin.${SERVER_NAME}`은 분리되어 있지만, `${SERVER_NAME}`과 `api.${SERVER_NAME}`은 같은 HTTPS 블록에 묶여 있다.
- 현재 [nginx/default-http.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-http.conf.template)는 `${SERVER_NAME}`, `api.${SERVER_NAME}`, `admin.${SERVER_NAME}`만 처리하며 `www.${SERVER_NAME}`은 포함하지 않는다.
- 현재 [docker-compose.yml](/C:/rolling/rolling-spring-backend/rolling-api/docker-compose.yml)는 관리자 React 정적 파일만 `/usr/share/nginx/html`로 마운트하고 있으며, 랜딩 React용 별도 정적 파일 마운트는 아직 없다.
- 현재 템플릿 파일과 실제 운영 파일 모두 목표 구조인 `rolling-app.com -> 랜딩`, `api.rolling-app.com -> API` 분리 상태와는 차이가 있다.

### PHASE 2. Nginx 도메인 분리 변경안 작성

- [x] `rolling-app.com` 전용 랜딩 블록 구성 방향을 정리한다.
- [x] `www.rolling-app.com` 처리 방식을 `rolling-app.com` 리다이렉트 또는 동일 랜딩 중 하나로 확정한다.
- [x] `admin.rolling-app.com` 관리자 React 블록 유지 방향을 정리한다.
- [x] `api.rolling-app.com` Spring API 전용 블록 분리 방향을 정리한다.
- [x] 메인 도메인과 API 도메인이 같은 HTTPS 블록에 남지 않도록 변경안을 작성한다.

PHASE 2 확인 결과:

- `rolling-app.com`은 랜딩 React 전용 HTTPS 블록으로 분리한다.
- `rolling-app.com` 블록은 정적 랜딩 파일만 서빙하며, 현재 랜딩이 API 연동이 없으므로 `/api` 프록시 location은 두지 않는다.
- `www.rolling-app.com`은 별도 콘텐츠를 두지 않고 `https://rolling-app.com$request_uri`로 301 리다이렉트하는 방향으로 확정한다.
- `admin.rolling-app.com`은 기존 관리자 React 전용 블록 구조를 유지하되, 랜딩과는 별도 root를 사용하도록 정리한다.
- `api.rolling-app.com`은 Spring API 전용 HTTPS 블록으로 분리하고, `proxy_pass http://api:8080;`만 담당하도록 정리한다.
- HTTP는 `rolling-app.com`, `www.rolling-app.com`, `admin.rolling-app.com`, `api.rolling-app.com`을 모두 수용하되, `/.well-known/acme-challenge/`를 제외한 요청은 각 호스트 기준 HTTPS로 리다이렉트하는 방향으로 정리한다.
- 최종 변경안 기준으로 HTTPS 역할은 최소 4개로 분리된다.
- `rolling-app.com` 랜딩 블록
- `www.rolling-app.com` 리다이렉트 블록
- `admin.rolling-app.com` 관리자 블록
- `api.rolling-app.com` API 프록시 블록

### PHASE 3. 정적 파일 배치 및 Compose 변경안 작성

- [x] 랜딩 React 정적 파일 경로와 관리자 React 정적 파일 경로를 분리하는 방향을 정리한다.
- [x] [docker-compose.yml](/C:/rolling/rolling-spring-backend/rolling-api/docker-compose.yml)에서 랜딩용 정적 파일 마운트를 추가하는 변경안을 작성한다.
- [x] 관리자 React 정적 파일 마운트가 기존과 충돌하지 않도록 변경안을 정리한다.
- [x] Nginx 내부 root 경로를 랜딩용과 관리자용으로 분리하는 방향을 문서화한다.

PHASE 3 확인 결과:

- 랜딩 React 정적 파일 경로는 `/home/ubuntu/ROLLING-REACT-LANDING/dist`로 분리하는 방향으로 확정한다.
- 관리자 React 정적 파일 경로는 기존 `/home/ubuntu/ROLLING-REACT-ADMIN-FRONTEND/dist`를 유지한다.
- [docker-compose.yml](/C:/rolling/rolling-spring-backend/rolling-api/docker-compose.yml)에는 랜딩 정적 파일 마운트를 추가하고, 관리자 정적 파일 마운트는 기존 단일 root 구조에서 분리한다.
- Nginx 내부 마운트 경로는 `/usr/share/nginx/landing`, `/usr/share/nginx/admin`으로 분리하는 방향으로 정리한다.
- 기존 `/usr/share/nginx/html` 단일 root 구조는 랜딩과 관리자 페이지를 함께 운영하기에 적합하지 않으므로 분리 대상이다.
- Compose 변경안 기준으로 Nginx 볼륨 구성은 아래 방향을 기준으로 한다.
- `./nginx/default.conf:/etc/nginx/conf.d/default.conf`
- `/etc/letsencrypt:/etc/letsencrypt:ro`
- `/var/www/certbot:/var/www/certbot`
- `/home/ubuntu/ROLLING-REACT-LANDING/dist:/usr/share/nginx/landing:ro`
- `/home/ubuntu/ROLLING-REACT-ADMIN-FRONTEND/dist:/usr/share/nginx/admin:ro`

### PHASE 4. 템플릿 파일 정합성 동기화안 작성

- [x] [nginx/default-http.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-http.conf.template)에 목표 도메인 구조가 반영되도록 변경 방향을 정리한다.
- [x] [nginx/default-https.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-https.conf.template)에 목표 도메인 구조가 반영되도록 변경 방향을 정리한다.
- [x] 현재 운영 반영 파일인 `default.conf`와 템플릿 파일 간 설정 불일치가 없도록 동기화 기준을 정리한다.
- [x] [nginx/10-select-template.sh](/C:/rolling/rolling-spring-backend/rolling-api/nginx/10-select-template.sh)는 수정 필요 여부만 검토 결과로 남긴다.

PHASE 4 확인 결과:

- [nginx/default-http.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-http.conf.template)는 `${SERVER_NAME}`, `www.${SERVER_NAME}`, `admin.${SERVER_NAME}`, `api.${SERVER_NAME}`을 모두 수용하는 HTTP 진입점으로 정리한다.
- HTTP 템플릿은 `/.well-known/acme-challenge/`만 예외 처리하고, 나머지 요청은 `https://$host$request_uri`로 리다이렉트하는 구조를 유지한다.
- [nginx/default-https.conf.template](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default-https.conf.template)는 아래 4개 역할로 분리하는 방향으로 정리한다.
- `${SERVER_NAME}` 랜딩 React 서빙 블록
- `www.${SERVER_NAME}` -> `${SERVER_NAME}` 301 리다이렉트 블록
- `admin.${SERVER_NAME}` 관리자 React 서빙 블록
- `api.${SERVER_NAME}` Spring API 프록시 블록
- 랜딩 템플릿 root는 `/usr/share/nginx/landing`, 관리자 템플릿 root는 `/usr/share/nginx/admin`으로 맞춘다.
- [nginx/default.conf](/C:/rolling/rolling-spring-backend/rolling-api/nginx/default.conf)는 위 템플릿 구조와 동일한 역할 분리 상태를 갖도록 concrete domain 기준으로 동기화한다.
- 즉 실제 운영 파일과 템플릿 파일 모두에서 `rolling-app.com`과 `api.rolling-app.com`이 더 이상 같은 블록에 남아 있지 않도록 맞춘다.
- [nginx/10-select-template.sh](/C:/rolling/rolling-spring-backend/rolling-api/nginx/10-select-template.sh)는 템플릿 선택만 담당하므로 이번 변경 범위에서는 수정하지 않는 방향으로 정리한다.

### PHASE 5. 반영 전 검증 항목 정리

- [x] `rolling-app.com`, `www.rolling-app.com`, `admin.rolling-app.com`, `api.rolling-app.com` DNS 상태 점검 항목을 정리한다.
- [x] SSL 인증서에 필요한 도메인 범위 점검 항목을 정리한다.
- [x] 랜딩과 관리자 React 빌드 산출물 경로 점검 항목을 정리한다.
- [x] React SPA 여부와 `index.html` fallback 필요 여부를 점검 항목으로 정리한다.
- [x] 설정 반영 후 어떤 주소에서 어떤 화면이 보여야 하는지 최종 검증 기준을 정리한다.

PHASE 5 확인 결과:

- DNS 점검 기준은 아래 4개 도메인이 모두 같은 Nginx 서버 IP를 바라보는지 확인하는 것이다.
- `rolling-app.com`
- `www.rolling-app.com`
- `admin.rolling-app.com`
- `api.rolling-app.com`
- SSL 인증서 점검 기준은 현재 사용 중인 인증서가 아래 도메인 범위를 모두 포함하는지 확인하는 것이다.
- `rolling-app.com`
- `www.rolling-app.com`
- `admin.rolling-app.com`
- `api.rolling-app.com`
- 정적 파일 점검 기준은 서버에 아래 두 경로가 실제 존재하고, 최신 빌드 결과가 배치되어 있는지 확인하는 것이다.
- `/home/ubuntu/ROLLING-REACT-LANDING/dist`
- `/home/ubuntu/ROLLING-REACT-ADMIN-FRONTEND/dist`
- SPA fallback 점검 기준은 랜딩과 관리자 페이지가 React SPA인 경우 `try_files ... /index.html` 구성이 각 도메인 블록에 반영되어 있는지 확인하는 것이다.
- 주소별 최종 기대 결과는 아래와 같다.
- `https://rolling-app.com` -> 랜딩 페이지 노출
- `https://www.rolling-app.com` -> `https://rolling-app.com`으로 301 리다이렉트
- `https://admin.rolling-app.com` -> 관리자 페이지 노출
- `https://api.rolling-app.com` -> Spring API 응답
- 랜딩과 관리자 페이지가 SPA라면 내부 라우트 직접 접근과 새로고침도 함께 확인 대상이다.
- 예시:
- `https://rolling-app.com/about`
- `https://admin.rolling-app.com/login`
- 위 주소들이 404가 아니라 각 앱의 `index.html` 기준으로 정상 진입해야 한다.
- 이번 전환에서는 랜딩 페이지 API 연동이 없으므로, 랜딩 기준 CORS는 최우선 검증 항목에서 제외한다.

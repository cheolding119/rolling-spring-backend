# GitHub Public 전환 보안 점검 보고서

## 1. 결론

현재 상태에서 바로 GitHub repository를 public으로 전환하는 것은 권장하지 않는다.

운영 DB 비밀번호, AWS 키, JWT secret, Slack webhook URL 같은 핵심 시크릿은 코드에 직접 하드코딩되어 있지 않고 GitHub Secrets 또는 환경변수 참조로 분리되어 있다. 하지만 repository에 이미 추적 중인 로컬 설정 파일과 임시 파일이 있으며, 로컬 작업트리에는 실제 Firebase service account 키 파일도 존재한다.

따라서 public 전환 전에는 최소한 아래 조치를 먼저 해야 한다.

- 추적 중인 `.idea`, `.claude`, `.ai`, 임시 HTML, 네트워크 캡처성 파일 제거
- 루트 `.gitignore` 추가
- 로컬 Firebase service account 키가 절대 커밋되지 않도록 확인
- Git 히스토리의 민감 정보 검색 결과를 한 번 더 확인
- public 전환 직후 GitHub secret scanning과 push protection 활성화

## 2. 점검 범위

점검일: 2026-04-27

점검 대상:

- `.gitignore`, `.dockerignore`
- 현재 Git 추적 파일 목록
- 현재 작업트리의 untracked/ignored 민감 파일
- GitHub Actions 배포 워크플로
- Spring Boot 설정 파일
- Docker Compose 및 monitoring 설정
- 과거 Git history의 주요 시크릿 패턴
- AI/IDE/로컬 작업 설정 파일

## 3. 현재 안전한 부분

### 3.1 운영 시크릿은 대부분 환경변수로 분리되어 있음

아래 값들은 코드에 실제 값이 아니라 placeholder 또는 GitHub Secrets 참조로 들어가 있다.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `AWS_S3_BUCKET_NAME`
- `AWS_S3_PUBLIC_BASE_URL`
- `SLACK_ALERT_WEBHOOK_URL`
- `SLACK_METRICS_ALERT_WEBHOOK_URL`
- `LIGHTSAIL_SSH_KEY`
- `DOCKER_PASSWORD`
- `FIREBASE_CREDENTIALS_PATH`

관련 파일:

- `.github/workflows/deploy.yml`
- `rolling-api/src/main/resources/application.yml`
- `rolling-api/docker-compose.yml`
- `rolling-api/docker-compose.monitoring.yml`

현재 구조 자체는 public repository에서 사용할 수 있는 방식이다.

### 3.2 `rolling-api/.gitignore`에는 핵심 로컬 시크릿 제외 규칙이 있음

현재 `rolling-api/.gitignore`는 아래 파일을 제외한다.

- `.env`
- `firebase-service-key.json`
- `.gradle/`
- `build/`
- `.idea/`
- `out/`

다만 이 규칙은 `rolling-api` 하위에만 적용된다. repository root에는 `.gitignore`가 없으므로 루트의 `.idea`, `.claude`, `.ai` 같은 파일은 별도 보호가 약하다.

### 3.3 Firebase 키 파일은 현재 Git 추적 대상은 아님

로컬에 `rolling-api/firebase-service-key.json`이 존재하지만, 현재 `.gitignore`에 의해 제외된다.

이 파일은 public 전환 전 반드시 로컬에만 남아 있어야 하며, 절대 `git add -f`로 추가하면 안 된다.

## 4. 공개 전 반드시 정리해야 하는 항목

### 4.1 루트 `.idea` 파일이 이미 Git에 추적 중

현재 아래 IDE 파일들이 Git 추적 대상이다.

- `.idea/misc.xml`
- `.idea/modules.xml`
- `.idea/rolling-spring-backend.iml`
- `.idea/vcs.xml`
- `.idea/workspace.xml`

위험도: P1

이 파일들은 보통 직접적인 운영 시크릿은 아니지만, 로컬 경로, IDE 상태, workspace 정보가 포함될 수 있다. public repository에는 올리지 않는 것이 일반적이다.

권장 조치:

- Git 추적에서 제거한다.
- 루트 `.gitignore`에 `.idea/`를 추가한다.

### 4.2 Claude/Codex 로컬 권한 설정이 추적 중

현재 아래 파일들이 Git 추적 대상이다.

- `.claude/settings.local.json`
- `.ai/mcp/mcp.json`
- `rolling-api/.claude/settings.local.json`

위험도: P1

현재 확인된 내용은 주로 로컬 명령 허용 목록이지만, public repository에 공개하면 로컬 자동화 정책과 작업 습관이 그대로 노출된다. 특히 `rm`, `curl`, `git checkout`, `git stash` 같은 허용 명령 정보는 공개할 이유가 없다.

권장 조치:

- Git 추적에서 제거한다.
- 루트 `.gitignore`와 `rolling-api/.gitignore`에 `.claude/`, `.ai/`를 추가한다.
- 공유가 필요한 agent 설정은 `.codex/agents/*.toml`처럼 의도된 파일만 남긴다.

### 4.3 임시 크롤링 HTML 파일이 추적 중

현재 아래 파일이 Git 추적 대상이다.

- `rolling-api/.codex_tmp_street_detail.html`

위험도: P1

외부 사이트 HTML 덤프는 코드가 아니고, public repository에 둘 이유가 없다. 외부 사이트의 스크립트, 토큰성 URL, 세션 관련 구조, 저작권성 페이지 내용을 그대로 포함할 수 있다.

권장 조치:

- Git 추적에서 제거한다.
- `.gitignore`에 `.codex_tmp_*`, `*.html` 임시 파일 제외 규칙을 둔다.

### 4.4 네트워크 캡처성 파일이 추적 중

현재 아래 파일이 Git 추적 대상이다.

- `rolling-api/heroes-network.txt`

위험도: P2

현재 내용은 Heroes 사이트 요청 URL과 status code 수준이지만, 이런 파일은 향후 쿠키, 세션, Authorization 헤더가 섞일 가능성이 있다. public repository에는 제거하는 편이 안전하다.

권장 조치:

- Git 추적에서 제거한다.
- 필요하면 `docs/tournaments`에 정책 문서로 요약만 남긴다.

### 4.5 repository root `.gitignore`가 없음

위험도: P1

현재 ignore 규칙이 `rolling-api/.gitignore`에만 있어 root-level 파일은 보호되지 않는다. 실제로 root `.idea`, `.claude`, `.ai`가 추적되고 있다.

권장 root `.gitignore` 예시:

```gitignore
# Environment and secrets
.env
.env.*
secrets/
firebase-service-key.json
*.pem
*.key
*.p12
*.jks
*.keystore

# IDE and local agent settings
.idea/
.vscode/
.claude/
.ai/
*.iml

# Build outputs
.gradle/
.gradle-local/
.gradle-test/
build/
out/

# Codex/local worktrees and temporary files
.wt-heap/
.codex_tmp_*
*.tmp
*.log

# OS
.DS_Store
Thumbs.db
```

## 5. 로컬 시크릿 관련 판단

### 5.1 Firebase service account 키

로컬 파일:

- `rolling-api/firebase-service-key.json`

현재 판단:

- Git 추적 대상은 아니다.
- `.gitignore`에 포함되어 있다.
- 다만 로컬에 실제 private key가 존재한다.

권장 조치:

- public 전환 전 `git status --ignored` 또는 `git check-ignore`로 계속 제외되는지 확인한다.
- 실수로 노출됐거나 노출 여부가 불확실하면 Firebase service account key를 폐기하고 재발급한다.
- 운영 서버에는 파일을 repository가 아니라 `/run/secrets` 또는 외부 secret store로 주입한다.

### 5.2 `.env`

현재 판단:

- `rolling-api/.env`는 `.gitignore` 대상이다.
- Git 추적 이력에서는 `.env` 직접 추적 흔적이 확인되지 않았다.

권장 조치:

- public 전환 전 `.env.example`만 제공하고 실제 `.env`는 절대 커밋하지 않는다.
- README에는 환경변수 이름만 문서화한다.

## 6. Git history 점검 결과

확인한 항목:

- `firebase-service-key.json` 또는 `.env`가 Git에 직접 추적된 이력
- `BEGIN PRIVATE KEY` 문자열 이력
- Firebase 프로젝트 ID 문자열 이력
- `DB_PASSWORD` 문자열 이력

현재 확인 결과:

- `firebase-service-key.json`과 `.env` 직접 추적 이력은 발견되지 않았다.
- `BEGIN PRIVATE KEY`는 테스트 코드의 더미 private key 생성 테스트에서 발견됐다.
- `DB_PASSWORD`는 GitHub Actions와 compose 환경변수 참조로 발견됐다.

주의:

- 이 검사는 주요 패턴 기반 점검이다. 완전한 보장을 위해서는 `gitleaks`, `trufflehog` 같은 전용 secret scanning 도구를 추가로 실행하는 것이 좋다.

## 7. GitHub Actions 공개 위험

파일:

- `.github/workflows/deploy.yml`

현재 구조:

- `main` push 시 Docker image build/push
- Lightsail 서버 SSH 접속 후 배포
- 운영 `.env`를 GitHub Secrets 값으로 생성

위험도: P2

public repository 자체에서는 GitHub Secrets 값이 노출되지 않는다. 다만 public repository에서는 외부 기여자의 PR, workflow permission, branch protection 설정이 중요해진다.

권장 조치:

- `main` branch protection 활성화
- pull request에서 deployment workflow가 실행되지 않도록 유지
- GitHub Actions 권한을 기본 read-only로 낮추고 필요한 job만 권한 부여
- GitHub Environments의 production protection rule 사용
- secret scanning과 push protection 활성화

## 8. 공개 전 권장 작업 순서

1. root `.gitignore`를 추가한다.
2. 추적 중인 로컬 파일을 Git index에서 제거한다.
3. `rolling-api/.gitignore`에 `.claude/`, `.ai/`, `.wt-heap/`, `.gradle-local/`, `.gradle-test/`, `.codex_tmp_*`를 보강한다.
4. `gitleaks` 또는 `trufflehog`로 전체 history를 스캔한다.
5. Firebase service account key가 노출된 적 있는지 확신이 없으면 폐기 후 재발급한다.
6. GitHub repository settings에서 secret scanning, push protection, branch protection을 켠다.
7. public 전환 후 README에 운영 시크릿이 repository에 포함되지 않는다는 환경변수 기준을 문서화한다.

## 9. 제거 권장 파일 목록

아래 파일은 public 전환 전 Git 추적에서 제거하는 것을 권장한다.

```text
.ai/mcp/mcp.json
.claude/settings.local.json
.idea/misc.xml
.idea/modules.xml
.idea/rolling-spring-backend.iml
.idea/vcs.xml
.idea/workspace.xml
rolling-api/.claude/settings.local.json
rolling-api/.codex_tmp_street_detail.html
rolling-api/heroes-network.txt
```

제거 방식 예시:

```powershell
git rm --cached .ai/mcp/mcp.json
git rm --cached .claude/settings.local.json
git rm --cached -r .idea
git rm --cached rolling-api/.claude/settings.local.json
git rm --cached rolling-api/.codex_tmp_street_detail.html
git rm --cached rolling-api/heroes-network.txt
```

파일을 로컬에서도 삭제할지, Git 추적만 제거할지는 용도에 따라 결정한다.

## 10. 최종 판단

현재 repository는 public 전환이 불가능한 수준은 아니지만, 바로 전환하기에는 준비가 부족하다.

최소 조건:

- P1 항목 제거 완료
- root `.gitignore` 추가 완료
- secret scanning 통과
- Firebase key 노출 이력 확인 또는 key rotation 완료
- GitHub branch protection과 secret scanning 활성화

위 조건을 만족한 뒤 public으로 전환하는 것이 안전하다.

## 11. Phase별 실행 체크리스트

### Phase 0. 현재 상태 확인

- [x] 현재 Git 추적 파일 목록 확인
- [x] `rolling-api/.gitignore` 확인
- [x] root `.gitignore` 부재 확인
- [x] GitHub Actions 배포 워크플로 확인
- [x] Spring Boot 운영 설정이 환경변수 기반인지 확인
- [x] 로컬 `firebase-service-key.json`이 Git ignore 대상인지 확인
- [x] 주요 시크릿 패턴 기반 Git history 1차 검색
- [x] public 전환 전 제거 권장 파일 목록 작성

### Phase 1. Ignore 규칙 보강

- [x] repository root `.gitignore` 추가
- [x] `rolling-api/.gitignore`에 `.claude/`, `.ai/`, `.wt-heap/`, `.gradle-local/`, `.gradle-test/`, `.codex_tmp_*`, `secrets/`, `*.pem`, `*.key` 보강
- [x] `git check-ignore`로 `.env`, `firebase-service-key.json`, `.wt-heap`, `.gradle-local`, `.claude`, `.ai` 제외 여부 확인

### Phase 2. 추적 중인 로컬/임시 파일 제거

- [x] `.ai/mcp/mcp.json` Git 추적 제거
- [x] `.claude/settings.local.json` Git 추적 제거
- [x] `.idea/` Git 추적 제거
- [x] `rolling-api/.claude/settings.local.json` Git 추적 제거
- [x] `rolling-api/.codex_tmp_street_detail.html` Git 추적 제거
- [x] `rolling-api/heroes-network.txt` Git 추적 제거 또는 문서 요약으로 대체
- [x] 제거 후 `git status --short`로 의도한 파일만 변경됐는지 확인

### Phase 3. Secret scanning 정밀 점검

- [x] `gitleaks` 또는 `trufflehog` 설치/실행 방식 결정
- [x] 전체 Git history secret scan 실행
- [x] scan 결과에서 실제 시크릿과 테스트 더미 값을 분류
- [x] 실제 시크릿 발견 시 해당 키 폐기 및 재발급 필요 여부 판단
- [x] 필요하면 Git history rewrite 여부 결정

Phase 3 실행 결과:

- Docker 기반 실행은 Docker Desktop 엔진 미실행으로 실패했다.
- GitHub release에서 `gitleaks 8.30.1` Windows x64 바이너리를 임시 다운로드해 실행했다.
- 전체 Git history 101 commits를 스캔했고 총 33건이 탐지됐다.
- 탐지 유형은 `generic-api-key` 32건, `private-key` 1건이다.
- 1차 분류 결과 운영 DB, AWS, Slack, Firebase service account 실제 키 노출은 확인되지 않았다.
- 탐지 대부분은 테스트용 `jwt.secret`, 문서 예시 `accessToken`/`refreshToken`, 테스트 코드에서 런타임 생성하는 private key 문자열이다.
- 현재 결과만 기준으로는 key rotation 또는 Git history rewrite를 즉시 진행할 필요는 낮다.
- 다만 public 전환 전 GitHub secret scanning alert가 같은 더미 값에 반응할 수 있으므로, Phase 6에서 current tree의 테스트/문서 예시 secret 표현을 더미 표기로 정리할지 최종 판단한다.

### Phase 4. 운영 키/토큰 회전 판단

- [x] Firebase service account key 노출 여부 최종 판단
- [x] Firebase key 노출 가능성이 있으면 폐기 후 재발급
- [x] Slack webhook URL 노출 여부 확인
- [x] AWS access key 노출 여부 확인
- [x] DB credential 노출 여부 확인
- [x] GitHub Secrets 최신값 재등록 필요 여부 확인

### Phase 5. GitHub repository 보호 설정

- [ ] GitHub secret scanning 활성화
- [ ] GitHub push protection 활성화
- [ ] `main` branch protection 활성화
- [ ] PR required review 또는 required status check 설정
- [ ] GitHub Actions 기본 권한을 read-only로 제한
- [x] production environment protection rule 적용 여부 결정

Phase 5 실행 결과:

- 로컬에 `gh` CLI가 없고 `GITHUB_TOKEN`/`GH_TOKEN` 환경변수도 없어 GitHub repository settings 자체는 직접 변경하지 못했다.
- `.github/workflows/deploy.yml`에 `permissions: contents: read`를 추가해 deploy workflow 권한을 최소화했다.
- `.github/workflows/pr-checks.yml`에도 `permissions: contents: read`를 추가했다.
- repository settings의 GitHub Actions 기본 권한 read-only 설정은 GitHub UI에서 수동으로 적용해야 한다.
- `.github/workflows/deploy.yml`의 deploy job에 `environment: production`을 연결했다.
- `.github/workflows/pr-checks.yml`을 추가해 PR과 작업 브랜치에서 `./gradlew test`가 실행되도록 했다.
- GitHub UI에서 `production` environment protection rule을 켜면 deploy job이 해당 rule을 따를 수 있는 상태가 됐다.
- GitHub UI에서 `main` branch protection의 required status check로 `PR Checks / test`를 선택해야 최종 완료된다.
- GitHub secret scanning, push protection, branch protection, required review/status check는 repository settings 권한이 필요한 수동 작업으로 남아 있다.

### Phase 6. Public 전환 전 최종 검증

- [x] `git status --short`에서 공개 전 정리 커밋 범위 확인
- [x] `git ls-files`에 제거 권장 파일이 남아 있지 않은지 확인
- [x] `.env`, `firebase-service-key.json`, private key 파일이 추적되지 않는지 확인
- [x] 전체 테스트 또는 최소 빌드 검증 실행
- [x] README에 환경변수/시크릿 주입 방식만 문서화
- [ ] public 전환 가능 여부 최종 판단

Phase 6 실행 결과:

- `git ls-files` 기준 `.idea`, `.claude`, `.ai`, `.codex_tmp`, `heroes-network`, `firebase-service-key`, `.env`, `.pem`, `.key` 추적 파일은 남아 있지 않다.
- `git check-ignore` 기준 `.env`, `firebase-service-key.json`, `.wt-heap`, `.gradle-local`, `.gradle-test`, `.claude`, `.ai`, `.codex_tmp_*`, `heroes-network.txt`가 ignore 처리된다.
- `.\gradlew test`를 실행했고 성공했다.
- `README.md`에 운영 시크릿을 repository에 저장하지 않고 환경변수/GitHub Secrets/서버 secret 파일로 주입한다는 기준을 추가했다.
- 최종 public 전환 판단은 GitHub UI 보호 설정 완료 후 닫는다.

### Phase 7. Public 전환 후 모니터링

- [ ] GitHub secret scanning alert 확인
- [ ] Actions가 의도치 않게 배포되지 않는지 확인
- [ ] public repository에서 민감 문서나 로컬 파일이 보이지 않는지 수동 확인
- [ ] Firebase, AWS, Slack, DB 접근 로그에서 이상 징후 확인
- [ ] 문제 발견 시 즉시 repository private 전환 또는 노출 키 폐기

Phase 7 상태:

- 아직 repository를 public으로 전환하지 않았으므로 Phase 7은 실행 전이다.
- public 전환 직후 GitHub security alerts, Actions 실행 이력, public 파일 노출 여부, 외부 서비스 접근 로그를 순서대로 확인한다.

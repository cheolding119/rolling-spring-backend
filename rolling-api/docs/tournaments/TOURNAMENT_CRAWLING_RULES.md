# Tournament Crawling Rules

## 1) 문서 목적
- 스트릿 주짓수 크롤링에서 확정된 패턴을 표준화한다.
- 남은 5개 리그 크롤러를 동일한 구조와 품질로 구현한다.
- 대상: `리그로얄`, `예거스 챔피언십`, `코리아 주짓수 챔피언십(코주챔)`, `주짓수코리아 챔피언십(JJKC)`, `히어로즈 오브 주짓수`

## 2) 현재 기준 상태 (2026-03-06)
- 완료: `StreetJiuJitsuCrawler`, `StreetJiuJitsuCrawlerService`
- 매니저: `TournamentManagerService`가 `List<TournamentCrawler>`를 순회하며 실패 크롤러를 건너뛰고 계속 진행
- 모델: `TournamentModel` 필드 사용
- 필드 목록: `title`, `organizer`, `posterUrl`, `competitionDate`, `registrationDeadline`, `location`, `applyLink`
- Spotlite는 접수중 목록 카드에서 최소 사실 정보만 수집하며 `posterUrl`은 저장하지 않는다.

## 3) 리그별 우선순위
1. 리그로얄 (League Royale): 신뢰도/인지도 핵심 데이터
2. 코리아 주짓수 챔피언십 (코주챔): 수도권 고정 수요
3. 히어로즈 오브 주짓수: 초보자 유입 기대

## 4) 공통 구현 규칙
1. 클래스 구조
- 각 리그마다 `...crawler` 패키지에 `XXCrawler implements TournamentCrawler` 생성
- 각 리그마다 `...service` 패키지에 `XXCrawlerService` 생성
- 책임 분리: `XXCrawler`는 목록/URL 수집, `XXCrawlerService`는 상세 파싱

2. HTTP 요청 규칙
- Jsoup 연결에 공통 설정 적용
- `timeout`: `10_000ms`
- `userAgent`: `"Mozilla/5.0 (compatible; RollingCrawler/1.0)"`
- 목록/상세 요청 모두 동일 규칙 사용

3. 목록 페이지(URL 수집) 규칙
- 기본 셀렉터: `a[href]`
- URL 정규화 순서 고정
- `abs:href` 우선, 없으면 `href`
- fragment(`#...`) 제거
- blank 제거
- 스킴 허용: `http`, `https`
- 호스트 화이트리스트: 해당 리그 공식 도메인만 허용
- 루트(`/`) 링크 제외
- 중복 제거: `LinkedHashSet` 유지

4. 상세 페이지 파싱 규칙
- 파싱 실패 시 예외를 던지지 않고 `null` 반환 (로그 남김)
- `applyLink`는 상세 URL을 기본값으로 저장
- 텍스트 정규화
- NBSP 제거
- zero-width 문자 제거
- 연속 공백 1칸으로 축약
- 앞뒤 공백 trim
- 날짜 필드는 `yyyy년 M월 d일` 형태로 추출

5. 실패 허용 규칙
- 목록 페이지 실패: 해당 목록만 스킵
- 상세 페이지 실패: 해당 상세만 스킵
- 필드 일부 누락: 객체는 반환하되 누락 필드는 `null`
- 매니저 단계에서 개별 크롤러 실패가 전체 배치 실패로 전파되지 않게 유지

6. 최소 저장 기준
- `applyLink`가 비어 있으면 저장 대상에서 제외
- `title`, `competitionDate`, `location`은 가능한 한 추출하되, 1차 단계에서는 일부 `null` 허용

7. "착한 크롤러" 규칙 (Rate Limiting)
- `TournamentManagerService`가 크롤러를 순회할 때, 각 크롤러 실행 사이에 랜덤 지연을 둔다.
- 지연 범위: `1,000ms ~ 2,000ms` (`Thread.sleep`)
- 목적: 대상 서버 부하 완화 및 IP 차단 위험 감소
- `InterruptedException` 발생 시 현재 스레드 interrupt 상태를 복구하고 로그를 남긴다.

8. 중복 방지 전략 (Upsert)
- 크롤링 저장은 Insert-only가 아니라 Upsert를 기본 전략으로 한다.
- 판별 우선순위
- 1순위: `applyLink` 일치
- 2순위: `title + competitionDate` 조합 일치
- 기존 데이터가 있으면 `Update`, 없으면 `Insert`
- 권장: 저장소 레벨에서 중복 방지를 위해 유니크 인덱스 또는 유니크 제약 검토

## 5) 리그별 파싱 규칙
### 5.1 리그로얄
- 목표: 공식 공지/대회 정보의 신뢰도 높은 데이터 확보
- 우선 수집 필드: `title`, `competitionDate`, `registrationDeadline`, `location`, `organizer`, `applyLink`
- 링크 수집 시 공지/이벤트 상세 URL 패턴을 우선 사용

### 5.2 예거스 챔피언십
- 목표: 지역 필터링에 쓸 수 있는 `location` 품질 강화
- 위치 파싱 시 시/도, 구/군 문자열 보존
- 대회명이 동일해도 `competitionDate + location`이 다르면 별도 대회로 처리

### 5.3 코주챔
- 목표: 수도권 반복 개최 데이터 안정 수집
- 반복 회차 데이터에서 회차(예: N회)와 날짜 혼동 금지
- 회차 정보는 `title`에 보존
- Wix 카드에서 `참가신청란` 링크가 여러 대회에 공통으로 재사용되면, 저장 식별 충돌을 피하기 위해 카드별 상세 URL을 `applyLink`로 사용한다
- 상세 파싱은 `wixui-rich-text` 통짜 텍스트 전체가 아니라 줄(`p`, `h1` 등) 단위로 읽고 `대회일정`, `마감`, `시합장소`를 파싱한다

### 5.4 JJKC
- 목표: 시각 리소스 연계 가능한 데이터 기반 확보
- 목록 페이지 1-depth에서 `title`, `posterUrl`, `applyLink`를 우선 수집
- 상세 페이지 2-depth에서는 `wixui-rich-text` 통짜 텍스트를 정규식 또는 문자열 파싱으로 처리
- `organizer`는 `"코리아 주짓수 챔피언십"` 하드코딩

### 5.5 히어로즈 오브 주짓수
- 목표: 초보자 유입용 대회 정보 노출
- beginner/novice 등 초보자 문구는 우선 탐지
- 1차 스코프에서는 태그 저장 필드가 없으므로 파싱 규칙만 유지

### 5.6 Spotlite
- 목표: Spotlite에서 실제 접수중인 주짓수 대회를 Rolling 탐색 목록에 연결한다.
- 수집 범위는 `https://spotlite.co.kr/jiujitsu/` 목록 페이지로 제한한다.
- `data-status="registration_open"` 카드만 저장 후보로 본다.
- 우선 수집 필드: `title`, `organizer`, `competitionDate`, `registrationDeadline`, `location`, `applyLink`
- `competitionDate`는 카드의 `data-end`, `registrationDeadline`은 카드의 `data-reg-end`를 사용한다.
- `applyLink`는 Spotlite 상세 URL(`/jiujitsu/{id}`)을 사용한다.
- 포스터, 상세 본문, 환불 규정, 참가자 명단, 대진표, 결과 데이터는 수집하지 않는다.
- `posterUrl`은 `null`로 두고 프론트 기본 이미지를 사용한다.

## 6) 구현 체크리스트 (리그 1개당)
1. `XXCrawler`, `XXCrawlerService` 클래스 생성
2. 리스트 URL 상수 정의
3. 도메인 화이트리스트 정의
4. 상세 페이지 파싱 정규식/셀렉터 정의
5. `TournamentManagerService`에서 빈 주입으로 자동 포함되는지 확인
6. 테스트 2종 작성
- 정상 HTML에서 필드 추출 성공 케이스
- 키워드 누락/구조 변경 시 `null` 또는 스킵 처리 케이스
7. 매니저 순회 시 크롤러 간 `1~2초` 랜덤 지연 적용
8. 저장 로직이 `applyLink` 또는 `title + competitionDate` 기준 Upsert인지 확인

## 7) 테스트 기준
- `./gradlew test` 통과 필수
- 리그별 서비스 단위 테스트 파일 권장 네이밍
- `XXCrawlerServiceTest`
- 매니저 실패 격리 규칙 회귀 금지
- 매니저 지연 규칙(1~2초)이 제거/누락되지 않도록 회귀 테스트 또는 코드 리뷰 체크
- Upsert 규칙(`applyLink` 우선, `title + competitionDate` 보조) 회귀 금지

## 8) 확장 규칙 (2차)
- 필요 시 모델 확장 후보
- `region`
- `levelTag` (초보/중급/오픈 등)
- 확장 전까지는 현재 `TournamentModel` 7필드 기준으로 크롤러를 완성한다.

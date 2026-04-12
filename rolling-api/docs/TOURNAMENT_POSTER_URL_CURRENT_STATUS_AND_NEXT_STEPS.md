# 대회 포스터 URL 현재 상태와 앞으로 할 일

기준일: 2026-04-12

## 1. 한 줄 결론

- 지금 백엔드는 `posterUrl`을 내려주는 구조로 맞춰졌지만, 운영에서 실제로 이미지를 보이게 하려면 `cdn.rolling-app.com`이 실제 이미지 제공처로 연결되어 있어야 한다.
- 즉, **백엔드 설정만으로 끝나지 않고 DNS와 이미지 호스팅 경로까지 함께 맞아야 한다.**

## 2. 현재 상태

### 2.1 백엔드

- 대회 생성 시 `posterKey`를 저장하고, 응답에서는 `posterUrl`을 내려주도록 정리돼 있다.
- 리스트 조회와 상세 조회는 `posterUrl`만 사용한다.
- 운영 환경에서는 `AWS_S3_PUBLIC_BASE_URL`이 없으면 서버가 시작되지 않도록 막아두었다.
- 따라서 운영에서는 `posterUrl`이 반드시 브라우저에서 직접 열 수 있는 공개 URL이어야 한다.

### 2.2 프론트엔드

- 리스트와 상세 화면은 계속 `posterUrl`을 그대로 사용하면 된다.
- 대회 생성은 먼저 업로드용 URL을 받아서 S3에 업로드한 뒤, 생성 요청에는 `posterKey`만 보내는 방식이다.
- 프론트가 `uploadUrl`이나 `posterKey`를 이미지 표시용으로 쓰면 안 된다.

### 2.3 AWS / DNS

- 현재 설정값은 `AWS_S3_PUBLIC_BASE_URL=https://cdn.rolling-app.com` 으로 잡혀 있다.
- 하지만 이 값만 넣는다고 `cdn.rolling-app.com`이 자동으로 연결되지는 않는다.
- `cdn.rolling-app.com`이 실제로 어떤 이미지 서비스로 연결될지 DNS와 CDN 설정이 따로 있어야 한다.

## 3. 지금 막힌 지점

- 운영 재배포 시 `cloud.aws.s3.public-base-url must be set in prod` 에러가 발생했다.
- 이 에러는 백엔드가 의도적으로 시작을 막은 것이다.
- 의미는 간단하다.
  - 운영에서는 공개 이미지 기준 주소가 반드시 있어야 한다.
  - 그 주소가 없으면 나중에 앱에서 `AccessDenied`가 다시 발생할 수 있다.

## 4. 중요한 오해 정리

- 환경변수만 넣는다고 DNS가 생기지 않는다.
- `cdn.rolling-app.com`이라는 이름은 DNS에서 실제로 생성해야 한다.
- `Add assignment`는 Lightsail 리소스 연결용이고, 서브도메인 연결은 보통 `DNS records`에서 처리한다.
- `cdn.rolling-app.com`은 **이름**이고, 그 이름이 실제로 어디로 갈지는 별도 설정이 필요하다.

## 5. 앞으로 해야 할 일

### 5.1 먼저 정해야 할 것

- `cdn.rolling-app.com`이 실제로 무엇을 가리킬지 결정해야 한다.
- 추천 순서는 다음과 같다.
  1. CloudFront를 새로 만든다.
  2. S3를 public으로 잠시 연다.
  3. GET presigned URL 방식을 쓴다.

### 5.2 추천 방식

- 운영 기준으로는 **CloudFront + private S3**가 가장 안전하다.
- 이 방식이면:
  - S3 버킷은 private로 유지할 수 있다.
  - 앱은 항상 공개 URL만 받는다.
  - 나중에 캐시 정책이나 도메인 변경이 쉬워진다.

### 5.3 CloudFront를 쓴다면 해야 할 일

- CloudFront 배포를 생성한다.
- S3 private 버킷을 오리진으로 연결한다.
- CloudFront 배포 도메인 `dxxxxx.cloudfront.net`을 확인한다.
- `cdn.rolling-app.com` DNS를 그 CloudFront 도메인으로 연결한다.
- CloudFront에 `cdn.rolling-app.com` 같은 대체 도메인과 인증서를 설정한다.

### 5.4 임시로 S3 public을 쓴다면 해야 할 일

- 버킷 또는 특정 prefix를 public read 가능하게 설정한다.
- `AWS_S3_PUBLIC_BASE_URL`을 S3 공개 도메인으로 맞춘다.
- 단, 이 방식은 운영 리스크가 커서 장기 운영용으로는 추천하지 않는다.

## 6. 배포 전 체크리스트

- [ ] `AWS_S3_PUBLIC_BASE_URL`이 배포 환경에 들어가 있다.
- [ ] `cdn.rolling-app.com` DNS가 실제 이미지 제공처로 연결되어 있다.
- [ ] 브라우저에서 `https://cdn.rolling-app.com/{posterKey}`가 열린다.
- [ ] 대회 생성 시 `posterKey`가 저장된다.
- [ ] 리스트/상세 응답에 `posterUrl`이 내려온다.
- [ ] 앱에서 `Image.network(posterUrl)`로 이미지를 볼 수 있다.
- [ ] 운영 로그에 `AccessDenied`가 더 이상 나오지 않는다.

## 7. 지금 바로 확인할 것

1. `cdn.rolling-app.com`이 어떤 대상에 연결될지 결정한다.
2. CloudFront를 만들지, S3 public으로 갈지 결정한다.
3. 결정한 방식에 맞게 DNS를 연결한다.
4. 브라우저에서 이미지 URL이 실제로 열리는지 확인한다.
5. 확인되면 다시 배포한다.

## 8. 현재 구조 요약

- 수동 대회 추가
  - 프론트가 업로드용 URL로 S3에 직접 업로드
  - 백엔드는 `posterKey`를 저장
  - 앱은 `posterUrl`만 사용

- 크롤링 대회 저장
  - 백엔드가 원본 이미지 URL을 가져옴
  - 백엔드가 S3에 다시 업로드
  - 최종적으로 앱은 `posterUrl`만 사용

- 리스트 / 상세 조회
  - `posterKey`는 노출하지 않음
  - 공개된 `posterUrl`만 내려줌


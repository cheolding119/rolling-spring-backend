# Slack Webhook Phase 2-6 Code Review (기준일: 2026-04-09)

## 리뷰 범위

- 앱 내부 Slack 메시지 포맷터의 한국어 가독성 개선
- startup health down 전용 요약 포맷
- Alertmanager Slack 템플릿의 한국어 메시지 구조
- Prometheus alert rule annotation의 한국어 운영 문구
- 관련 테스트 보강

## 코드 리뷰 결과

- Blocking findings 없음

## 직접 검증한 내용

- `SlackMessageFormatterTest`에서 스케줄러 실패, startup health down, 예상하지 못한 서버 오류 메시지가 한국어 중심 구조로 변환되는지 확인
- startup health down 메시지가 정상 항목을 숨기고 비정상 항목만 요약하는지 확인
- startup health down 메시지 줄 수가 7줄 이하인지 확인
- `PrometheusMonitoringConfigTest`에서 Alertmanager 템플릿과 alert rule annotation에 한국어 문구가 반영됐는지 확인

## 확인된 설계 장점

- startup health down처럼 detail이 큰 알림도 운영자가 원인과 조치만 먼저 읽을 수 있게 됐다.
- 앱 알림과 메트릭 알림 모두 `무슨 문제인가 -> 확인된 원인 -> 지금 확인할 것` 흐름으로 맞춰져 읽는 방식이 일관적이다.
- Alertmanager 템플릿에서 `발생/FIRING`, `복구/RESOLVED`, `치명/CRITICAL` 병기를 넣어 상태 해석이 쉬워졌다.

## 런타임에서 추가 확인이 필요한 내용

- 실제 Slack 모바일 화면에서 Alertmanager 메시지가 줄바꿈과 강조 표시를 과도하게 깨뜨리지 않는지 확인
- 여러 원인이 동시에 내려간 startup health down 상황에서 2개 원인 제한이 운영상 충분한지 확인
- 운영 환경에서 복구 메시지 `복구/RESOLVED`가 기대한 빈도로 오는지 확인

## 잔존 위험

- startup health down 요약 규칙은 현재 알려진 health detail 구조를 기준으로 작성돼 있어, 새로운 외부 의존성 항목이 추가되면 별도 한국어 요약 규칙 보강이 필요하다.
- Alertmanager 메시지 길이는 템플릿 수준에서 줄였지만, 긴 dashboard URL은 Slack 클라이언트 표시 폭에 따라 여전히 길게 보일 수 있다.

## 권장 후속 조치

- 배포 직후 startup health down 테스트 메시지를 한 번 더 발송해 실제 모바일 가독성을 확인
- Alertmanager의 firing/resolved 메시지를 각각 1회씩 확인
- 모바일 확인 후 필요하면 title 길이와 dashboard 링크 표기를 한 번 더 축약

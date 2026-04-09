# Slack Webhook Phase 2: Prometheus/Grafana 메트릭 알림 (기준일: 2026-04-09)

## 목표

이 phase의 목표는 이미 노출된 `/actuator/prometheus` 기반 메트릭을 사용해 운영 지표 이상 징후를 Prometheus/Grafana 쪽에서 감지하고, 그 결과를 Slack으로 보내는 것이다.

이 단계는 앱 내부 예외 기반 Slack 알림을 대체하는 것이 아니라 보완하는 단계다.

- Phase 1은 앱 내부 장애 이벤트 push
- Phase 2는 메트릭 기반 이상 징후 탐지와 추세 관측

## 왜 분리하는가

- 앱 예외 알림과 메트릭 알림은 신호의 성격이 다르다.
- 처음부터 둘을 한 번에 구현하면 범위가 커지고 잡음이 늘어난다.
- 메트릭 알림은 threshold, 재알림, dashboard 운영 기준까지 함께 정리돼야 실효성이 있다.

## 전제 조건

- `spring-boot-starter-actuator`와 Prometheus registry가 이미 추가돼 있다.
- `/actuator/prometheus`가 노출돼 있다.
- Grafana/Prometheus 대시보드와 alert rule 문서는 아직 별도 정리가 더 필요하다.

## 권장 운영 모델

알림 주체는 다음 둘 중 하나로 결정한다.

1. Grafana Alerting
2. Prometheus Alertmanager

현재 단계에서는 둘 다 구현 대상으로 열어두되, 실제 운영에서는 한 곳만 Slack 발송 주체로 선택하는 것이 좋다.

## 우선 알림 후보

- 인스턴스 down 또는 scrape 실패
- 5xx 비율 급증
- 응답 시간 p95 또는 p99 급증
- JVM heap 사용률 과다
- 컨테이너 CPU 사용률 과다
- 스케줄러 관련 커스텀 메트릭 이상

다음 항목은 나중에 검토한다.

- 너무 세분화된 endpoint별 경고
- 짧은 순간 스파이크에 과민한 경고
- 대시보드 없이 설명하기 어려운 복합 조건 경고

## Slack 메시지 원칙

- 메트릭명, 현재값, 임계치, 관찰 기간을 같이 보낸다.
- dashboard 또는 runbook 링크를 포함할 수 있으면 포함한다.
- 앱 예외 알림과 구별되도록 제목 prefix를 분리한다.

예시:

```text
[ROLLING API][PROD][WARN] High 5xx rate detected
- metric: http_server_requests_5xx_ratio
- currentValue: 6.2%
- threshold: 3.0%
- window: 5m
- action: check Grafana dashboard and request logs
```

## Phase 2 체크리스트

### Phase 2-1. 메트릭 기준 확정

- [ ] Slack으로 보낼 메트릭 후보 목록 확정
- [ ] 각 메트릭별 threshold 초안 작성
- [ ] severity 기준 정의
- [ ] false positive 가능성 높은 항목 제거

### Phase 2-2. 알림 주체 결정

- [ ] Grafana Alerting 사용 여부 결정
- [ ] Prometheus Alertmanager 사용 여부 결정
- [ ] 실제 Slack 발송 주체를 하나로 확정
- [ ] Slack 채널 분리 여부 결정

### Phase 2-3. 알림 정책

- [ ] alert rule window 확정
- [ ] cooldown 및 재알림 주기 확정
- [ ] 복구 알림 발송 여부 결정
- [ ] 앱 예외 알림과 중복되는 시나리오 정리

### Phase 2-4. 운영 문서화

- [ ] dashboard 링크 목록 정리
- [ ] runbook 연결
- [ ] 메트릭별 1차 확인 절차 문서화
- [ ] 운영자 대응 우선순위 문서화

### Phase 2-5. 검증

- [ ] 테스트용 threshold로 알림 발생 확인
- [ ] Slack 메시지 포맷 가독성 검증
- [ ] 반복 알림 폭주 여부 검증
- [ ] 복구 알림 동작 검증

## 완료 기준

- 운영자가 앱 예외뿐 아니라 지표 이상 징후도 Slack에서 먼저 인지할 수 있다.
- 각 알림은 dashboard 또는 runbook으로 바로 이어질 수 있다.
- 메트릭 알림이 운영 채널을 과도한 잡음으로 오염시키지 않는다.

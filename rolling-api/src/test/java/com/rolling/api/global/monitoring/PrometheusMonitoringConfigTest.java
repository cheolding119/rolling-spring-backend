package com.rolling.api.global.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PrometheusMonitoringConfigTest {

    private static final Path PROMETHEUS_CONFIG = Path.of("monitoring", "prometheus", "prometheus.yml");
    private static final Path ALERT_RULES_CONFIG = Path.of("monitoring", "prometheus", "alerts", "rolling-alerts.yml");
    private static final Path APPLICATION_CONFIG = Path.of("src", "main", "resources", "application.yml");

    private final Yaml yaml = new Yaml();

    @Test
    @DisplayName("Prometheus 설정은 rolling alert rule 파일과 rolling-api scrape target을 포함한다")
    @SuppressWarnings("unchecked")
    void prometheusConfig_includesRuleFileAndApiTarget() throws IOException {
        Map<String, Object> prometheusConfig = yaml.load(Files.readString(PROMETHEUS_CONFIG));

        assertThat((List<String>) prometheusConfig.get("rule_files"))
                .contains("/etc/prometheus/alerts/rolling-alerts.yml");

        List<Map<String, Object>> scrapeConfigs = (List<Map<String, Object>>) prometheusConfig.get("scrape_configs");
        Map<String, Object> rollingApiJob = scrapeConfigs.stream()
                .filter(config -> "rolling-api".equals(config.get("job_name")))
                .findFirst()
                .orElseThrow();

        List<Map<String, Object>> staticConfigs = (List<Map<String, Object>>) rollingApiJob.get("static_configs");
        assertThat((List<String>) staticConfigs.get(0).get("targets"))
                .containsExactly("api:9090");
    }

    @Test
    @DisplayName("Phase 3 alert rules는 계획 문서의 핵심 알람을 모두 포함한다")
    @SuppressWarnings("unchecked")
    void alertRules_coverPhase3Alerts() throws IOException {
        Map<String, Object> alertConfig = yaml.load(Files.readString(ALERT_RULES_CONFIG));

        List<Map<String, Object>> groups = (List<Map<String, Object>>) alertConfig.get("groups");
        List<Map<String, Object>> rules = (List<Map<String, Object>>) groups.get(0).get("rules");

        assertThat(rules)
                .extracting(rule -> rule.get("alert"))
                .containsExactly(
                        "RollingApiScrapeFailed",
                        "RollingApiHigh5xxErrorRate",
                        "RollingApiHighP95Latency",
                        "RollingTournamentCrawlerDidNotSucceed",
                        "RollingOpenMatStatusSyncFailure",
                        "RollingFcmHighFailureRate"
                );

        String alertFile = Files.readString(ALERT_RULES_CONFIG);
        assertThat(alertFile).contains("http_server_requests_seconds_bucket");
        assertThat(alertFile).contains("process_uptime_seconds > 93600");
        assertThat(alertFile).contains("rolling_scheduler_last_success_unixtime{task=\"tournamentCrawler\"}");
        assertThat(alertFile).contains("rolling_fcm_send_total{result=\"failure\"}");
    }

    @Test
    @DisplayName("HTTP p95 알람에 필요한 server request histogram 설정이 활성화되어 있다")
    @SuppressWarnings("unchecked")
    void applicationConfig_enablesHttpServerRequestHistogram() throws IOException {
        Map<String, Object> applicationConfig = yaml.load(Files.readString(APPLICATION_CONFIG));

        Map<String, Object> management = (Map<String, Object>) applicationConfig.get("management");
        Map<String, Object> metrics = (Map<String, Object>) management.get("metrics");
        Map<String, Object> distribution = (Map<String, Object>) metrics.get("distribution");
        Map<String, Object> percentilesHistogram = (Map<String, Object>) distribution.get("percentiles-histogram");
        Map<String, Object> http = (Map<String, Object>) percentilesHistogram.get("http");
        Map<String, Object> server = (Map<String, Object>) http.get("server");

        assertThat(server.get("requests")).isEqualTo(true);
    }
}

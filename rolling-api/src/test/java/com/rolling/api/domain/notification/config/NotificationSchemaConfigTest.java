package com.rolling.api.domain.notification.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSchemaConfigTest {

    private final NotificationSchemaConfig config = new NotificationSchemaConfig();

    @Test
    @DisplayName("notifications type check SQL은 현재 enum 값을 모두 포함한다")
    void buildAddConstraintSql_containsAllNotificationTypes() {
        String sql = config.buildAddConstraintSql();

        assertThat(sql).contains("OPEN_MAT_UPDATED");
        assertThat(sql).contains("OPEN_MAT_DELETED");
        assertThat(sql).contains("SEMINAR_APPLIED");
        assertThat(sql).contains("SEMINAR_APPLICATION_CANCELED");
        assertThat(sql).contains("SEMINAR_APPLICATION_CANCELED_BY_HOST");
        assertThat(sql).contains("SEMINAR_UPDATED");
        assertThat(sql).contains("SEMINAR_DELETED");
        assertThat(sql).contains("SEMINAR_CANCELED");
        assertThat(sql).contains("INQUIRY_ANSWERED");
        assertThat(sql).contains("COMMUNITY_COMMENT_CREATED");
        assertThat(sql).contains("notifications_type_check");
    }

    @Test
    @DisplayName("현재 constraint 정의에 enum 값이 모두 있으면 동기화가 필요 없다고 판단한다")
    void containsAllAllowedTypes_whenDefinitionContainsAllValues_returnsTrue() {
        String definition = "CHECK (((type)::text = ANY ((ARRAY['OPEN_MAT_UPDATED'::character varying, 'OPEN_MAT_DELETED'::character varying, 'SEMINAR_APPLIED'::character varying, 'SEMINAR_APPLICATION_CANCELED'::character varying, 'SEMINAR_APPLICATION_CANCELED_BY_HOST'::character varying, 'SEMINAR_UPDATED'::character varying, 'SEMINAR_DELETED'::character varying, 'SEMINAR_CANCELED'::character varying, 'INQUIRY_ANSWERED'::character varying, 'COMMUNITY_COMMENT_CREATED'::character varying])::text[])))";

        assertThat(config.containsAllAllowedTypes(definition)).isTrue();
    }

    @Test
    @DisplayName("현재 constraint 정의에 새 enum 값이 빠져 있으면 동기화가 필요하다고 판단한다")
    void containsAllAllowedTypes_whenDefinitionMissesInquiryAnswered_returnsFalse() {
        String definition = "CHECK (((type)::text = ANY ((ARRAY['OPEN_MAT_UPDATED'::character varying, 'OPEN_MAT_DELETED'::character varying])::text[])))";

        assertThat(config.containsAllAllowedTypes(definition)).isFalse();
    }
}

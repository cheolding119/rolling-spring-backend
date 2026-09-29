package com.rolling.api.domain.notification.config;

import com.rolling.api.domain.notification.model.PushNotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

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
        assertThat(sql).contains("FRIEND_REQUEST_RECEIVED");
        assertThat(sql).contains("TRAINING_LOG_COMMENT_CREATED");
        assertThat(sql).contains("TRAINING_LOG_COMMENT_REPLY_CREATED");
        assertThat(sql).contains("TOURNAMENT_FAVORITE_REMINDER");
        assertThat(sql).contains("notifications_type_check");
    }

    @Test
    @DisplayName("현재 constraint 정의에 enum 값이 모두 있으면 동기화가 필요 없다고 판단한다")
    void containsAllAllowedTypes_whenDefinitionContainsAllValues_returnsTrue() {
        String allowedTypes = Arrays.stream(PushNotificationType.values())
                .map(type -> "'" + type.name() + "'::character varying")
                .collect(Collectors.joining(", "));
        String definition = "CHECK (((type)::text = ANY ((ARRAY[" + allowedTypes + "])::text[])))";

        assertThat(config.containsAllAllowedTypes(definition)).isTrue();
    }

    @Test
    @DisplayName("현재 constraint 정의에 새 enum 값이 빠져 있으면 동기화가 필요하다고 판단한다")
    void containsAllAllowedTypes_whenDefinitionMissesInquiryAnswered_returnsFalse() {
        String definition = "CHECK (((type)::text = ANY ((ARRAY['OPEN_MAT_UPDATED'::character varying, 'OPEN_MAT_DELETED'::character varying])::text[])))";

        assertThat(config.containsAllAllowedTypes(definition)).isFalse();
    }
}

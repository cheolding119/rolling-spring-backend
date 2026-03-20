package com.rolling.api.domain.notification.config;

import com.rolling.api.domain.notification.model.PushNotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class NotificationSchemaConfig {

    private static final String NOTIFICATIONS_TABLE = "notifications";
    private static final String TYPE_CONSTRAINT_NAME = "notifications_type_check";

    @Bean
    public ApplicationRunner notificationTypeConstraintSynchronizer(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            if (!isPostgreSql(dataSource)) {
                return;
            }
            if (!notificationsTableExists(jdbcTemplate)) {
                return;
            }

            String currentDefinition = findCurrentConstraintDefinition(jdbcTemplate);
            if (containsAllAllowedTypes(currentDefinition)) {
                return;
            }

            jdbcTemplate.execute("ALTER TABLE " + NOTIFICATIONS_TABLE + " DROP CONSTRAINT IF EXISTS " + TYPE_CONSTRAINT_NAME);
            jdbcTemplate.execute(buildAddConstraintSql());

            log.info(
                    "Synchronized {} constraint for notifications. allowedTypes={}",
                    TYPE_CONSTRAINT_NAME,
                    Arrays.toString(PushNotificationType.values())
            );
        };
    }

    boolean containsAllAllowedTypes(String currentDefinition) {
        if (currentDefinition == null || currentDefinition.isBlank()) {
            return false;
        }

        return Arrays.stream(PushNotificationType.values())
                .map(Enum::name)
                .allMatch(type -> currentDefinition.contains("'" + type + "'"));
    }

    String buildAddConstraintSql() {
        String allowedTypes = Arrays.stream(PushNotificationType.values())
                .map(Enum::name)
                .map(type -> "'" + type + "'")
                .collect(Collectors.joining(", "));

        return "ALTER TABLE " + NOTIFICATIONS_TABLE
                + " ADD CONSTRAINT " + TYPE_CONSTRAINT_NAME
                + " CHECK (type IN (" + allowedTypes + "))";
    }

    private boolean isPostgreSql(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException e) {
            log.warn("Failed to inspect database metadata for notification schema synchronization", e);
            return false;
        }
    }

    private boolean notificationsTableExists(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = current_schema()
                  AND table_name = ?
                """,
                Integer.class,
                NOTIFICATIONS_TABLE
        );
        return count != null && count > 0;
    }

    private String findCurrentConstraintDefinition(JdbcTemplate jdbcTemplate) {
        List<String> definitions = jdbcTemplate.query(
                """
                SELECT pg_get_constraintdef(oid)
                FROM pg_constraint
                WHERE conname = ?
                """,
                (rs, rowNum) -> rs.getString(1),
                TYPE_CONSTRAINT_NAME
        );

        return definitions.isEmpty() ? null : definitions.get(0);
    }
}

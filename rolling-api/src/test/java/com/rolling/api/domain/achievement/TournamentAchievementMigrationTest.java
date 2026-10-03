package com.rolling.api.domain.achievement;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TournamentAchievementMigrationTest {
    @Test
    void schemaSupportsMedalsAndUserCleanupAndRejectsInvalidValues() throws Exception {
        var dataSource = new SingleConnectionDataSource("jdbc:h2:mem:achievement-migration;MODE=PostgreSQL", "sa", "", true);
        try {
            var jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("create table users (id bigint primary key)");
            new ResourceDatabasePopulator(new ClassPathResource("db/migration/V46__add_tournament_achievements.sql"))
                    .execute(dataSource);
            jdbc.update("insert into users values (1)");
            String insert = "insert into tournament_achievements (user_id, tournament_name, competition_date, result, created_at, updated_at) "
                    + "values (1, 'Open', DATE '2026-10-03', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
            jdbc.update(insert, "GOLD");
            jdbc.update(insert, "PARTICIPATION");
            assertThatThrownBy(() -> jdbc.update(insert, "INVALID"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(jdbc.queryForObject("select count(*) from tournament_achievements", Long.class)).isEqualTo(2L);
            jdbc.update("delete from users where id = 1");
            assertThat(jdbc.queryForObject("select count(*) from tournament_achievements", Long.class)).isZero();
        } finally {
            dataSource.destroy();
        }
    }
}

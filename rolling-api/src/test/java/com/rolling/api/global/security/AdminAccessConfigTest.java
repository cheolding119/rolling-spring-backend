package com.rolling.api.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAccessConfigTest {

    @Test
    @DisplayName("쉼표로 구분된 관리자 userId 목록을 파싱한다")
    void parsesAdminUserIds() {
        AdminAccessConfig config = new AdminAccessConfig("1, 2,3");

        assertThat(config.isAdmin(1L)).isTrue();
        assertThat(config.isAdmin(2L)).isTrue();
        assertThat(config.isAdmin(3L)).isTrue();
        assertThat(config.isAdmin(4L)).isFalse();
    }

    @Test
    @DisplayName("빈 설정이면 관리자 권한 사용자가 없다")
    void emptyConfigHasNoAdmins() {
        AdminAccessConfig config = new AdminAccessConfig("");

        assertThat(config.isAdmin(1L)).isFalse();
    }

    @Test
    @DisplayName("숫자가 아닌 userId가 포함되면 예외를 던진다")
    void invalidUserIdThrows() {
        assertThatThrownBy(() -> new AdminAccessConfig("1,admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("admin.user-ids");
    }
}

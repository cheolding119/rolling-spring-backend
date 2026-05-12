package com.rolling.api.domain.seminar.controller;

import com.rolling.api.domain.seminar.dto.SeminarResponse;
import com.rolling.api.domain.seminar.entity.SeminarStatus;
import com.rolling.api.domain.seminar.service.SeminarService;
import com.rolling.api.global.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeminarControllerTest {

    @Mock
    private SeminarService seminarService;

    @Test
    @DisplayName("세미나 목록 조회는 인증 사용자의 viewer id를 서비스에 전달한다")
    void list_withUserToken_passesViewerId() {
        SeminarController controller = new SeminarController(seminarService);
        PageRequest pageable = PageRequest.of(0, 20);
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 3, 31, 23, 59);
        when(seminarService.findAll(null, SeminarStatus.RECRUITING, null, from, to, pageable, 2L))
                .thenReturn(new PageImpl<>(List.of(SeminarResponse.builder().id(10L).title("세미나 10").build())));

        controller.list(new UserPrincipal(2L), null, SeminarStatus.RECRUITING, null, from, to, pageable);

        verify(seminarService).findAll(null, SeminarStatus.RECRUITING, null, from, to, pageable, 2L);
    }

    @Test
    @DisplayName("비로그인 세미나 상세 조회는 viewer id 없이 서비스에 전달한다")
    void findById_withoutUserToken_passesNullViewerId() {
        SeminarController controller = new SeminarController(seminarService);
        when(seminarService.findById(eq(10L), isNull()))
                .thenReturn(SeminarResponse.builder().id(10L).title("세미나 10").build());

        controller.findById(null, 10L);

        verify(seminarService).findById(10L, null);
    }
}

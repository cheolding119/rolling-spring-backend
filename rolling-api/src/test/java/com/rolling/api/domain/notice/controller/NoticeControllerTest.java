package com.rolling.api.domain.notice.controller;

import com.rolling.api.domain.notice.dto.NoticeListItemResponse;
import com.rolling.api.domain.notice.dto.NoticeResponse;
import com.rolling.api.domain.notice.service.NoticeService;
import com.rolling.api.domain.openmat.config.OpenMatTestingAccessConfig;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.config.SecurityConfig;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.exception.GlobalExceptionHandler;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

class NoticeControllerTest {

    private MockMvc mockMvc;

    private NoticeService noticeService;

    private AnnotationConfigWebApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertyValues.of(
                "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
                "jwt.access-token-expiry=1800000",
                "jwt.refresh-token-expiry=1209600000"
        ).applyTo(context);
        context.register(TestConfig.class);
        context.refresh();

        noticeService = context.getBean(NoticeService.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("공지사항 목록 조회는 인증 없이 접근할 수 있고 공통 응답 형식으로 반환한다")
    void list_isAccessibleWithoutAuthentication() throws Exception {
        NoticeListItemResponse response = NoticeListItemResponse.builder()
                .id(1L)
                .title("3월 점검 안내")
                .content("3월 정기 점검이 예정되어 있습니다.")
                .authorName("Rolling Admin")
                .createdAt(LocalDateTime.of(2026, 3, 18, 9, 0))
                .build();

        given(noticeService.findAll(any())).willReturn(
                new PageImpl<>(
                        List.of(response),
                        PageRequest.of(0, 20, Sort.by("createdAt").descending()),
                        1
                )
        );

        mockMvc.perform(get("/api/v1/notices").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("3월 점검 안내"))
                .andExpect(jsonPath("$.data.content[0].authorName").value("Rolling Admin"))
                .andExpect(jsonPath("$.data.content[0].content").value("3월 정기 점검이 예정되어 있습니다."))
                .andExpect(jsonPath("$.data.content[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    @DisplayName("공지사항 상세 조회는 인증 없이 접근할 수 있고 존재하지 않으면 NOT_FOUND를 반환한다")
    void findById_whenMissing_returnsNotFound() throws Exception {
        given(noticeService.findById(99L)).willThrow(BusinessException.notFound("공지사항을 찾을 수 없습니다"));

        mockMvc.perform(get("/api/v1/notices/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("공지사항을 찾을 수 없습니다"));
    }

    @Configuration
    @EnableWebMvc
    @EnableSpringDataWebSupport
    @Import({NoticeController.class, GlobalExceptionHandler.class, SecurityConfig.class})
    static class TestConfig {

        @Bean
        NoticeService noticeService() {
            return mock(NoticeService.class);
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        AdminAccessConfig adminAccessConfig() {
            return mock(AdminAccessConfig.class);
        }

        @Bean
        OpenMatTestingAccessConfig openMatTestingAccessConfig() {
            return mock(OpenMatTestingAccessConfig.class);
        }
    }
}

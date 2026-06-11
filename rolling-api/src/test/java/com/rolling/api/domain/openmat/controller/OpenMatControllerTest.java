package com.rolling.api.domain.openmat.controller;

import com.rolling.api.domain.openmat.dto.OpenMatImageUploadUrlRequest;
import com.rolling.api.domain.openmat.dto.OpenMatImageUploadUrlResponse;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.service.OpenMatImageUploadService;
import com.rolling.api.domain.openmat.service.OpenMatService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenMatControllerTest {

    @Mock
    private OpenMatService openMatService;

    @Mock
    private OpenMatImageUploadService openMatImageUploadService;

    @Test
    @DisplayName("오픈매트 목록 조회는 인증 사용자의 viewer id를 서비스에 전달한다")
    void list_withUserToken_passesViewerId() {
        OpenMatController controller = new OpenMatController(openMatService, openMatImageUploadService);
        PageRequest pageable = PageRequest.of(0, 20);
        when(openMatService.findAll(null, OpenMatStatus.RECRUITING, null, pageable, 2L))
                .thenReturn(new PageImpl<>(List.of(OpenMatResponse.builder().id(10L).title("오픈매트 10").build())));

        controller.list(new UserPrincipal(2L), null, OpenMatStatus.RECRUITING, null, pageable);

        verify(openMatService).findAll(null, OpenMatStatus.RECRUITING, null, pageable, 2L);
    }

    @Test
    @DisplayName("오픈매트 이미지 업로드 URL 발급은 업로드 서비스 응답을 그대로 반환한다")
    void createImageUploadUrl_returnsUploadMetadata() {
        OpenMatController controller = new OpenMatController(openMatService, openMatImageUploadService);
        OpenMatImageUploadUrlRequest request = new OpenMatImageUploadUrlRequest();
        when(openMatImageUploadService.createUploadUrl(any()))
                .thenReturn(OpenMatImageUploadUrlResponse.builder()
                        .uploadUrl("https://upload.test/presigned")
                        .imageKey("openmats/images/sample.jpg")
                        .imageUrl("https://cdn.test.com/openmats/images/sample.jpg")
                        .expiresAt(LocalDateTime.of(2026, 6, 11, 16, 40))
                        .build());

        var response = controller.createImageUploadUrl(new UserPrincipal(2L), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getImageKey()).isEqualTo("openmats/images/sample.jpg");
        verify(openMatImageUploadService).createUploadUrl(request);
    }
}

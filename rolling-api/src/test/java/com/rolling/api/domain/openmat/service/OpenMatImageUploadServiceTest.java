package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.openmat.dto.OpenMatImageUploadUrlRequest;
import com.rolling.api.domain.openmat.dto.OpenMatImageUploadUrlResponse;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OpenMatImageUploadServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private OpenMatImageUploadService openMatImageUploadService;

    @BeforeEach
    void setUp() {
        openMatImageUploadService = new OpenMatImageUploadService(
                s3Presigner,
                Clock.fixed(Instant.parse("2026-06-11T07:25:00Z"), ZoneId.of("Asia/Seoul"))
        );
        ReflectionTestUtils.setField(openMatImageUploadService, "bucket", "rolling-jiujitsu-bucket");
        ReflectionTestUtils.setField(openMatImageUploadService, "region", "ap-northeast-2");
    }

    @Test
    @DisplayName("오픈매트 이미지 업로드 URL 발급은 지원하는 이미지 형식의 presigned 메타데이터를 반환한다")
    void createUploadUrl_returnsPresignedMetadata() throws MalformedURLException {
        OpenMatImageUploadUrlRequest request = new OpenMatImageUploadUrlRequest();
        ReflectionTestUtils.setField(request, "fileName", "openmat.jpg");
        ReflectionTestUtils.setField(request, "contentType", "image/jpeg");
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedPutObjectRequest);
        given(presignedPutObjectRequest.url()).willReturn(new URL("https://upload.test/presigned"));

        OpenMatImageUploadUrlResponse response = openMatImageUploadService.createUploadUrl(request);

        assertThat(response.getUploadUrl()).isEqualTo("https://upload.test/presigned");
        assertThat(response.getImageKey()).startsWith("openmats/images/");
        assertThat(response.getImageKey()).endsWith(".jpg");
        assertThat(response.getImageUrl())
                .startsWith("https://rolling-jiujitsu-bucket.s3.ap-northeast-2.amazonaws.com/openmats/images/");
        assertThat(response.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 6, 11, 16, 40));
    }

    @Test
    @DisplayName("오픈매트 이미지 업로드 URL 발급은 지원하지 않는 파일 형식을 거부한다")
    void createUploadUrl_rejectsUnsupportedFileTypes() {
        OpenMatImageUploadUrlRequest request = new OpenMatImageUploadUrlRequest();
        ReflectionTestUtils.setField(request, "fileName", "openmat.txt");
        ReflectionTestUtils.setField(request, "contentType", "text/plain");

        assertThatThrownBy(() -> openMatImageUploadService.createUploadUrl(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("지원하지 않는 이미지 형식입니다");
    }

    @Test
    @DisplayName("오픈매트 이미지 public URL은 configured public base URL을 우선 사용한다")
    void buildPublicUrl_usesConfiguredPublicBaseUrl() {
        ReflectionTestUtils.setField(openMatImageUploadService, "publicBaseUrl", "https://cdn.rolling.com/");

        assertThat(openMatImageUploadService.buildPublicUrl("openmats/images/poster.jpg"))
                .isEqualTo("https://cdn.rolling.com/openmats/images/poster.jpg");
    }
}

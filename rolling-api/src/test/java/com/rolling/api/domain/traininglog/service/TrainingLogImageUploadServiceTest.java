package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.traininglog.dto.TrainingLogImageUploadUrlRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogImageUploadUrlResponse;
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
class TrainingLogImageUploadServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private TrainingLogImageUploadService trainingLogImageUploadService;

    @BeforeEach
    void setUp() {
        trainingLogImageUploadService = new TrainingLogImageUploadService(
                s3Presigner,
                Clock.fixed(Instant.parse("2026-05-17T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
        ReflectionTestUtils.setField(trainingLogImageUploadService, "bucket", "rolling-jiujitsu-bucket");
        ReflectionTestUtils.setField(trainingLogImageUploadService, "region", "ap-northeast-2");
    }

    @Test
    @DisplayName("create upload URL returns presigned metadata for supported images")
    void createUploadUrl_returnsPresignedMetadata() throws MalformedURLException {
        TrainingLogImageUploadUrlRequest request = new TrainingLogImageUploadUrlRequest();
        ReflectionTestUtils.setField(request, "fileName", "training-log.jpg");
        ReflectionTestUtils.setField(request, "contentType", "image/jpeg");
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedPutObjectRequest);
        given(presignedPutObjectRequest.url()).willReturn(new URL("https://upload.test/presigned"));

        TrainingLogImageUploadUrlResponse response = trainingLogImageUploadService.createUploadUrl(request);

        assertThat(response.getUploadUrl()).isEqualTo("https://upload.test/presigned");
        assertThat(response.getImageKey()).startsWith("training/logs/images/");
        assertThat(response.getImageKey()).endsWith(".jpg");
        assertThat(response.getImageUrl())
                .startsWith("https://rolling-jiujitsu-bucket.s3.ap-northeast-2.amazonaws.com/training/logs/images/");
        assertThat(response.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 5, 17, 12, 15));
    }

    @Test
    @DisplayName("create upload URL rejects unsupported file types")
    void createUploadUrl_rejectsUnsupportedFileTypes() {
        TrainingLogImageUploadUrlRequest request = new TrainingLogImageUploadUrlRequest();
        ReflectionTestUtils.setField(request, "fileName", "training-log.txt");
        ReflectionTestUtils.setField(request, "contentType", "text/plain");

        assertThatThrownBy(() -> trainingLogImageUploadService.createUploadUrl(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("지원하지 않는 이미지 형식입니다");
    }

    @Test
    @DisplayName("build public URL uses configured public base URL when present")
    void buildPublicUrl_usesConfiguredPublicBaseUrl() {
        ReflectionTestUtils.setField(trainingLogImageUploadService, "publicBaseUrl", "https://cdn.rolling.com/");

        assertThat(trainingLogImageUploadService.buildPublicUrl("training/logs/images/poster.jpg"))
                .isEqualTo("https://cdn.rolling.com/training/logs/images/poster.jpg");
    }
}

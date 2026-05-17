package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.traininglog.dto.TrainingLogImageUploadUrlRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogImageUploadUrlResponse;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingLogImageUploadService {

    private static final String IMAGE_PREFIX = "training/logs/images/";
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(15);
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png"
    );
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png"
    );

    private final S3Presigner s3Presigner;
    private final Clock clock;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.public-base-url:}")
    private String publicBaseUrl;

    public TrainingLogImageUploadUrlResponse createUploadUrl(TrainingLogImageUploadUrlRequest request) {
        String extension = resolveExtension(request.getFileName(), request.getContentType());
        if (!StringUtils.hasText(extension)) {
            throw BusinessException.badRequest("지원하지 않는 이미지 형식입니다");
        }

        String contentType = resolveContentType(request.getContentType(), extension);
        String imageKey = IMAGE_PREFIX + UUID.randomUUID() + "." + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_DURATION)
                .putObjectRequest(putObjectRequest)
                .build();

        try {
            String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
            return TrainingLogImageUploadUrlResponse.builder()
                    .uploadUrl(uploadUrl)
                    .imageKey(imageKey)
                    .imageUrl(buildPublicUrl(imageKey))
                    .expiresAt(LocalDateTime.now(clock).plus(PRESIGN_DURATION))
                    .build();
        } catch (SdkException e) {
            throw BusinessException.badRequest("이미지 업로드 URL을 생성할 수 없습니다");
        }
    }

    public String buildPublicUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }
        return joinUrl(resolvePublicBaseUrl(), imageKey);
    }

    private String resolvePublicBaseUrl() {
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl.trim();
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com";
    }

    private String joinUrl(String baseUrl, String path) {
        if (!StringUtils.hasText(baseUrl)) {
            return path;
        }

        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return normalizedBaseUrl + "/" + normalizedPath;
    }

    private String resolveExtension(String fileName, String contentType) {
        String fromContentType = resolveExtensionFromContentType(contentType);
        if (StringUtils.hasText(fromContentType)) {
            return fromContentType;
        }
        return resolveExtensionFromFileName(fileName);
    }

    private String resolveExtensionFromContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return CONTENT_TYPE_TO_EXTENSION.get(mediaType.getType() + "/" + mediaType.getSubtype());
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveExtensionFromFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }

        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return null;
        }

        String extension = fileName.substring(extensionIndex + 1).toLowerCase();
        if ("jpeg".equals(extension)) {
            extension = "jpg";
        }
        return EXTENSION_TO_CONTENT_TYPE.containsKey(extension) ? extension : null;
    }

    private String resolveContentType(String contentType, String extension) {
        if (StringUtils.hasText(contentType)) {
            try {
                MediaType mediaType = MediaType.parseMediaType(contentType);
                String resolved = CONTENT_TYPE_TO_EXTENSION.get(mediaType.getType() + "/" + mediaType.getSubtype());
                if (resolved != null) {
                    return mediaType.toString();
                }
            } catch (Exception ignored) {
                // fallback to extension
            }
        }

        return EXTENSION_TO_CONTENT_TYPE.getOrDefault(extension, MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }
}

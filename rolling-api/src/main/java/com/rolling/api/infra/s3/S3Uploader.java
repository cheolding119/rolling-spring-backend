package com.rolling.api.infra.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class S3Uploader {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RollingPosterUploader/1.0)";
    private static final String POSTER_PREFIX = "posters/";
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/bmp", "bmp",
            "image/svg+xml", "svg",
            "image/avif", "avif",
            "image/x-icon", "ico",
            "image/vnd.microsoft.icon", "ico"
    );
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif",
            "bmp", "image/bmp",
            "svg", "image/svg+xml",
            "avif", "image/avif",
            "ico", "image/x-icon"
    );

    private final RestClient restClient;
    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public S3Uploader(RestClient restClient,
                      S3Client s3Client,
                      @Value("${cloud.aws.s3.bucket}") String bucket,
                      @Value("${cloud.aws.region.static}") String region) {
        this.restClient = restClient;
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
    }

    public String uploadImageFromUrl(String sourceImageUrl) {
        if (!StringUtils.hasText(sourceImageUrl)) {
            return sourceImageUrl;
        }

        try {
            DownloadedImage downloadedImage = downloadImage(sourceImageUrl);
            if (downloadedImage == null || downloadedImage.bytes().length == 0) {
                log.warn("Skip S3 upload because image download returned empty body. sourceImageUrl={}", sourceImageUrl);
                return sourceImageUrl;
            }

            String extension = resolveExtension(downloadedImage.contentType(), sourceImageUrl);
            if (!StringUtils.hasText(extension)) {
                log.warn("Skip S3 upload because image extension could not be resolved. sourceImageUrl={}, contentType={}",
                        sourceImageUrl,
                        downloadedImage.contentType());
                return sourceImageUrl;
            }

            String key = POSTER_PREFIX + UUID.randomUUID() + "." + extension;
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(resolveContentType(downloadedImage.contentType(), extension))
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(downloadedImage.bytes()));
            return buildPublicUrl(key);
        } catch (Exception e) {
            log.warn("Failed to upload tournament poster to S3. sourceImageUrl={}", sourceImageUrl, e);
            return sourceImageUrl;
        }
    }

    String resolveExtension(String contentType, String sourceImageUrl) {
        String fromContentType = extractExtensionFromContentType(contentType);
        if (StringUtils.hasText(fromContentType)) {
            return fromContentType;
        }
        return extractExtensionFromUrl(sourceImageUrl);
    }

    private DownloadedImage downloadImage(String sourceImageUrl) {
        ResponseEntity<byte[]> response = restClient.get()
                .uri(sourceImageUrl)
                .header(HttpHeaders.ACCEPT, "image/*")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .retrieve()
                .toEntity(byte[].class);

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            return null;
        }

        MediaType mediaType = response.getHeaders().getContentType();
        String contentType = mediaType != null ? mediaType.toString() : response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return new DownloadedImage(body, contentType);
    }

    private String extractExtensionFromContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }

        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return CONTENT_TYPE_TO_EXTENSION.get(mediaType.getType() + "/" + mediaType.getSubtype());
        } catch (InvalidMediaTypeException e) {
            log.warn("Invalid content type while resolving extension. contentType={}", contentType);
            return null;
        }
    }

    private String extractExtensionFromUrl(String sourceImageUrl) {
        if (!StringUtils.hasText(sourceImageUrl)) {
            return null;
        }

        try {
            String path = URI.create(sourceImageUrl).getPath();
            if (!StringUtils.hasText(path)) {
                return null;
            }

            int extensionIndex = path.lastIndexOf('.');
            if (extensionIndex < 0 || extensionIndex == path.length() - 1) {
                return null;
            }

            String extension = path.substring(extensionIndex + 1).toLowerCase();
            if ("jpeg".equals(extension)) {
                return "jpg";
            }

            return EXTENSION_TO_CONTENT_TYPE.containsKey(extension) ? extension : null;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid image URL while resolving extension. sourceImageUrl={}", sourceImageUrl);
            return null;
        }
    }

    private String resolveContentType(String contentType, String extension) {
        if (StringUtils.hasText(contentType)) {
            try {
                MediaType mediaType = MediaType.parseMediaType(contentType);
                String resolved = CONTENT_TYPE_TO_EXTENSION.get(mediaType.getType() + "/" + mediaType.getSubtype());
                if (resolved != null) {
                    return mediaType.toString();
                }
            } catch (InvalidMediaTypeException e) {
                log.warn("Invalid content type while resolving upload content type. contentType={}", contentType);
            }
        }

        return EXTENSION_TO_CONTENT_TYPE.getOrDefault(extension, MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private String buildPublicUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private record DownloadedImage(byte[] bytes, String contentType) {
    }
}

package com.rolling.api.infra.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class S3UploaderTest {

    @Mock
    private S3Client s3Client;

    private MockRestServiceServer mockServer;
    private RestClient restClient;
    private S3Uploader s3Uploader;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        s3Uploader = new S3Uploader(restClient, s3Client, "rolling-jiujitsu-bucket", "ap-northeast-2", "");
    }

    @Test
    @DisplayName("이미지 Content-Type이 있으면 해당 확장자로 업로드한다")
    void uploadImageFromUrl_usesContentTypeExtension() {
        String sourceImageUrl = "https://origin.example.com/poster.png";
        MediaType webp = MediaType.parseMediaType("image/webp");

        mockServer.expect(requestTo(sourceImageUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] {1, 2, 3}, webp));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());

        String uploadedUrl = s3Uploader.uploadImageFromUrl(sourceImageUrl);
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().key()).startsWith("posters/").endsWith(".webp");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/webp");
        assertThat(uploadedUrl)
                .startsWith("https://rolling-jiujitsu-bucket.s3.ap-northeast-2.amazonaws.com/posters/")
                .endsWith(".webp");
        mockServer.verify();
    }

    @Test
    @DisplayName("공개 base URL이 있으면 그 도메인으로 업로드 결과를 반환한다")
    void uploadImageFromUrl_usesConfiguredPublicBaseUrl() {
        String sourceImageUrl = "https://origin.example.com/poster.jpg";
        String publicBaseUrl = "https://cdn.rolling.com/";
        S3Uploader publicUrlUploader = createUploader(publicBaseUrl);

        mockServer.expect(requestTo(sourceImageUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] {1, 2, 3}, MediaType.IMAGE_JPEG));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());

        String uploadedUrl = publicUrlUploader.uploadImageFromUrl(sourceImageUrl);

        assertThat(uploadedUrl).startsWith("https://cdn.rolling.com/posters/");
        mockServer.verify();
    }

    @Test
    @DisplayName("Content-Type을 신뢰할 수 없으면 원본 URL 확장자를 사용한다")
    void uploadImageFromUrl_fallsBackToSourceUrlExtension() {
        String sourceImageUrl = "https://origin.example.com/poster.png?version=2";

        mockServer.expect(requestTo(sourceImageUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] {9, 8, 7}, MediaType.APPLICATION_OCTET_STREAM));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());

        s3Uploader.uploadImageFromUrl(sourceImageUrl);
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().key()).startsWith("posters/").endsWith(".png");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        mockServer.verify();
    }

    @Test
    @DisplayName("S3 업로드가 실패하면 원본 URL을 그대로 반환한다")
    void uploadImageFromUrl_returnsSourceUrlWhenUploadFails() {
        String sourceImageUrl = "https://origin.example.com/poster.jpg";

        mockServer.expect(requestTo(sourceImageUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] {4, 5, 6}, MediaType.IMAGE_JPEG));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("upload failed").build());

        String uploadedUrl = s3Uploader.uploadImageFromUrl(sourceImageUrl);

        assertThat(uploadedUrl).isEqualTo(sourceImageUrl);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        mockServer.verify();
    }

    @Test
    @DisplayName("확장자를 알 수 없으면 업로드하지 않고 원본 URL을 반환한다")
    void uploadImageFromUrl_returnsSourceUrlWhenExtensionCannotBeResolved() {
        String sourceImageUrl = "https://origin.example.com/poster";

        mockServer.expect(requestTo(sourceImageUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] {4, 5, 6}, MediaType.APPLICATION_OCTET_STREAM));

        String uploadedUrl = s3Uploader.uploadImageFromUrl(sourceImageUrl);

        assertThat(uploadedUrl).isEqualTo(sourceImageUrl);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        mockServer.verify();
    }

    private S3Uploader createUploader(String publicBaseUrl) {
        return new S3Uploader(restClient, s3Client, "rolling-jiujitsu-bucket", "ap-northeast-2", publicBaseUrl);
    }
}

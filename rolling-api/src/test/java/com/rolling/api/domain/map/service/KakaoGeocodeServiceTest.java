package com.rolling.api.domain.map.service;

import com.rolling.api.domain.map.config.KakaoMapProperties;
import com.rolling.api.domain.map.dto.GeocodeResponse;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoGeocodeServiceTest {

    private KakaoMapProperties properties;
    private MockRestServiceServer server;
    private KakaoGeocodeService service;

    @BeforeEach
    void setUp() {
        properties = new KakaoMapProperties();
        properties.setRestApiKey("test-rest-api-key");

        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://dapi.kakao.com");
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        service = new KakaoGeocodeService(restClientBuilder.build(), properties);
    }

    @Test
    @DisplayName("카카오 주소 검색 첫 번째 결과의 y를 위도, x를 경도로 변환한다")
    void geocode_mapsKakaoXYToLatitudeLongitude() {
        server.expect(once(), requestToUriTemplate(
                        "https://dapi.kakao.com/v2/local/search/address.json?query={address}",
                        "경남 창녕군 창녕읍 종로 2"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "KakaoAK test-rest-api-key"))
                .andRespond(withSuccess("""
                        {
                          "documents": [
                            {
                              "address_name": "경남 창녕군 창녕읍 종로 2",
                              "x": "128.4912345",
                              "y": "35.5412345"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        GeocodeResponse response = service.geocode("  경남   창녕군 창녕읍 종로 2  ");

        assertThat(response.getAddress()).isEqualTo("경남 창녕군 창녕읍 종로 2");
        assertThat(response.getLatitude()).isEqualByComparingTo("35.5412345");
        assertThat(response.getLongitude()).isEqualByComparingTo("128.4912345");
        server.verify();
    }

    @Test
    @DisplayName("카카오 주소 검색 결과가 없으면 GEOCODE_NOT_FOUND를 반환한다")
    void geocode_whenNoDocument_throwsNotFound() {
        server.expect(once(), requestToUriTemplate(
                        "https://dapi.kakao.com/v2/local/search/address.json?query={address}",
                        "없는 주소"
                ))
                .andRespond(withSuccess("{\"documents\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.geocode("없는 주소"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("GEOCODE_NOT_FOUND");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        server.verify();
    }

    @Test
    @DisplayName("카카오 API 오류는 KAKAO_GEOCODE_FAILED로 변환한다")
    void geocode_whenKakaoApiFails_throwsBadGateway() {
        server.expect(once(), requestToUriTemplate(
                        "https://dapi.kakao.com/v2/local/search/address.json?query={address}",
                        "경남 창녕군 창녕읍 종로 2"
                ))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.geocode("경남 창녕군 창녕읍 종로 2"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("KAKAO_GEOCODE_FAILED");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
        server.verify();
    }

    @Test
    @DisplayName("주소가 비어 있으면 VALIDATION_ERROR를 반환한다")
    void geocode_whenAddressBlank_throwsValidationError() {
        assertThatThrownBy(() -> service.geocode(" "))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}

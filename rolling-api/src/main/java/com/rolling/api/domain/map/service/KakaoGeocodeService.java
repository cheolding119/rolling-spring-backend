package com.rolling.api.domain.map.service;

import com.rolling.api.domain.map.config.KakaoMapProperties;
import com.rolling.api.domain.map.dto.GeocodeResponse;
import com.rolling.api.domain.map.dto.KakaoAddressSearchResponse;
import com.rolling.api.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class KakaoGeocodeService {

    private static final int MIN_ADDRESS_LENGTH = 2;

    private final RestClient kakaoMapRestClient;
    private final KakaoMapProperties properties;

    public KakaoGeocodeService(
            @Qualifier("kakaoMapRestClient") RestClient kakaoMapRestClient,
            KakaoMapProperties properties
    ) {
        this.kakaoMapRestClient = kakaoMapRestClient;
        this.properties = properties;
    }

    public GeocodeResponse geocode(String address) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.length() < MIN_ADDRESS_LENGTH) {
            throw BusinessException.badRequest("주소는 2자 이상이어야 합니다");
        }
        if (!StringUtils.hasText(properties.getRestApiKey())) {
            throw new BusinessException(
                    "KAKAO_GEOCODE_FAILED",
                    "카카오 지도 REST API 키가 설정되지 않았습니다",
                    HttpStatus.BAD_GATEWAY
            );
        }

        KakaoAddressSearchResponse response = requestKakaoAddressSearch(normalizedAddress);
        if (response == null) {
            throw new BusinessException(
                    "KAKAO_GEOCODE_FAILED",
                    "카카오 주소 좌표 변환 응답이 비어 있습니다",
                    HttpStatus.BAD_GATEWAY
            );
        }
        List<KakaoAddressSearchResponse.Document> documents = response.getDocuments();
        if (documents == null || documents.isEmpty()) {
            throw new BusinessException(
                    "GEOCODE_NOT_FOUND",
                    "선택한 주소의 좌표를 찾지 못했습니다.",
                    HttpStatus.NOT_FOUND
            );
        }

        KakaoAddressSearchResponse.Document document = documents.get(0);
        return GeocodeResponse.builder()
                .address(StringUtils.hasText(document.getAddressName()) ? document.getAddressName() : normalizedAddress)
                .latitude(parseCoordinate(document.getY(), "latitude"))
                .longitude(parseCoordinate(document.getX(), "longitude"))
                .build();
    }

    private KakaoAddressSearchResponse requestKakaoAddressSearch(String address) {
        try {
            return kakaoMapRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", address)
                            .build())
                    .header("Authorization", "KakaoAK " + properties.getRestApiKey())
                    .retrieve()
                    .body(KakaoAddressSearchResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new BusinessException(
                        "KAKAO_GEOCODE_RATE_LIMITED",
                        "카카오 주소 좌표 변환 요청 한도를 초과했습니다",
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
            log.warn("Kakao geocode request failed. status={}, body={}",
                    exception.getStatusCode().value(), exception.getResponseBodyAsString());
            throw new BusinessException(
                    "KAKAO_GEOCODE_FAILED",
                    "카카오 주소 좌표 변환 API 호출에 실패했습니다",
                    HttpStatus.BAD_GATEWAY
            );
        } catch (ResourceAccessException exception) {
            log.warn("Kakao geocode request timed out or failed to connect. message={}", exception.getMessage());
            throw new BusinessException(
                    "KAKAO_GEOCODE_TIMEOUT",
                    "카카오 주소 좌표 변환 API 응답 시간이 초과되었습니다",
                    HttpStatus.GATEWAY_TIMEOUT
            );
        }
    }

    private String normalizeAddress(String address) {
        if (address == null) {
            throw BusinessException.badRequest("주소는 필수입니다");
        }
        String normalized = address.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            throw BusinessException.badRequest("주소는 필수입니다");
        }
        return normalized;
    }

    private BigDecimal parseCoordinate(String value, String fieldName) {
        try {
            return new BigDecimal(value);
        } catch (RuntimeException exception) {
            log.warn("Kakao geocode response has invalid {}. value={}", fieldName, value);
            throw new BusinessException(
                    "KAKAO_GEOCODE_FAILED",
                    "카카오 주소 좌표 변환 응답이 올바르지 않습니다",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}

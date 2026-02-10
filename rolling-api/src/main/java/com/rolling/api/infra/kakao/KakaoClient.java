package com.rolling.api.infra.kakao;

import com.rolling.api.infra.kakao.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoClient {

    private final RestClient restClient;

    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    public KakaoUserResponse getUserInfo(String accessToken) {
        log.debug("Requesting Kakao user info");

        return restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserResponse.class);
    }
}

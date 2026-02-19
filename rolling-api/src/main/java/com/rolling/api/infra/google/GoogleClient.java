package com.rolling.api.infra.google;

import com.rolling.api.infra.google.dto.GoogleUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleClient {

    private final RestClient restClient;

    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    public GoogleUserResponse getUserInfo(String accessToken) {
        log.debug("Requesting Google user info");

        return restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserResponse.class);
    }
}
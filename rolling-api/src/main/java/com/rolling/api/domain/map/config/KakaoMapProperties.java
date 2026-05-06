package com.rolling.api.domain.map.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kakao.map")
public class KakaoMapProperties {

    private String restApiKey = "";
    private String localApiBaseUrl = "https://dapi.kakao.com";
    private int connectTimeoutMs = 1000;
    private int readTimeoutMs = 3000;
}

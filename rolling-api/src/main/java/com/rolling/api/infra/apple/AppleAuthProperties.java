package com.rolling.api.infra.apple;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "apple.auth")
public class AppleAuthProperties {

    private String clientId;
    private String issuer = "https://appleid.apple.com";
    private String jwksUrl = "https://appleid.apple.com/auth/keys";
}

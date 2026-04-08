package com.necronet.registerapi.dto;

import lombok.Data;

@Data
public class AuthConfigDTO {
    private String username;
    private String password;
    private String token;
    private String apiKeyName;
    private String apiKeyValue;
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String scope;
    private String validationEndpoint;
    private String additionalConfig;
}
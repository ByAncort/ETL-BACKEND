package com.necronet.registerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "auth_configs")
public class AuthConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false)
    private AuthType authType;

    // Campos para Basic Auth
    @Column(length = 100)
    private String username;

    @Column(length = 255)
    private String password;

    // Campos para Bearer/API Key
    @Column(name = "token", columnDefinition = "TEXT")
    private String token;

    @Column(name = "api_key_name", length = 50)
    private String apiKeyName;

    @Column(name = "api_key_value", length = 255)
    private String apiKeyValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "api_key_location")
    private ApiKeyLocation apiKeyLocation;

    // Campos para OAuth2
    @Column(name = "oauth2_client_id", length = 100)
    private String oauth2ClientId;

    @Column(name = "oauth2_client_secret", length = 255)
    private String oauth2ClientSecret;

    @Column(name = "oauth2_auth_url", length = 500)
    private String oauth2AuthUrl;

    @Column(name = "oauth2_token_url", length = 500)
    private String oauth2TokenUrl;

    @Column(name = "oauth2_refresh_url", length = 500)
    private String oauth2RefreshUrl;

    @Column(name = "oauth2_scope", length = 500)
    private String oauth2Scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth2_grant_type")
    private OAuth2GrantType oauth2GrantType;

    @Column(name = "oauth2_redirect_uri", length = 500)
    private String oauth2RedirectUri;

    // Token actual y refresh token
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "last_token_refresh")
    private LocalDateTime lastTokenRefresh;

    // Campos adicionales
    @Column(name = "auth_payload", columnDefinition = "TEXT")
    private String authPayload;

    @Column(name = "custom_auth_headers", columnDefinition = "TEXT")
    private String customAuthHeaders;

    @OneToOne(mappedBy = "authConfig")
    private IntegrationApis integrationApi;

    public enum AuthType {
        NONE, BASIC, BEARER, API_KEY, OAUTH2, OAUTH2_CLIENT_CREDENTIALS,
        OAUTH2_PASSWORD, OAUTH2_AUTHORIZATION_CODE, DIGEST, HMAC, CUSTOM
    }

    public enum ApiKeyLocation {
        HEADER, QUERY_PARAM, COOKIE
    }

    public enum OAuth2GrantType {
        CLIENT_CREDENTIALS, PASSWORD, AUTHORIZATION_CODE, REFRESH_TOKEN
    }
}
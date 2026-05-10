package com.necronet.identityservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String message;
    private String accessToken;
    private String refreshToken;
    private Set<String> roles;

    public AuthResponse(String message, String accessToken, String refreshToken, Set<String> roles) {
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.roles = roles;
    }

    public AuthResponse(String message, String accessToken) {
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = null;
        this.roles = null;
    }

    public AuthResponse(String message, String accessToken, String refreshToken) {
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.roles = null;
    }
}
package com.necronet.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String message;
    private String accessToken;
    private String refreshToken;

    public AuthResponse(String message, String accessToken) {
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = null;
    }
}
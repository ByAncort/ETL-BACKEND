package com.necronet.apiregisterms.dto;

import com.necronet.apiregisterms.entity.AuthType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiRegisterRequest {
    @NotBlank(message = "method is required")
    private String method;

    @NotBlank(message = "url is required")
    private String url;
    private String description;
    private String pathParams;
    private String queryParams;

    private AuthType authType;
    private String authHeader;
    private String authValue;
    private String username;
    private String password;
    private String tokenEndpoint;

    private Map<String, String> headers;
    private String body;

    private ApiRegisterRequest apiAuth;
}
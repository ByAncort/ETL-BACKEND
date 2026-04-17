package com.necronet.apiregisterms.dto;

import com.necronet.apiregisterms.entity.AuthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {
    private Long id;
    private String method;
    private String url;
    private String description;
    private String pathParams;
    private String queryParams;
    private String body;
    private LocalDateTime createdAt;
    private AuthType authType;
    private String authHeader;
    private String authHeaderValue;
    private Long authApiId;
    private String authApiUrl;
    private String authValue;
}
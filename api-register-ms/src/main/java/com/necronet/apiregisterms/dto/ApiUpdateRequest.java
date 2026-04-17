package com.necronet.apiregisterms.dto;

import com.necronet.apiregisterms.entity.AuthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiUpdateRequest {
    private String method;
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
}
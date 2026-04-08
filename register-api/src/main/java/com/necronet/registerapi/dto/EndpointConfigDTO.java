package com.necronet.registerapi.dto;

import com.necronet.registerapi.entity.enums.AuthType;
import com.necronet.registerapi.entity.enums.TypeExample;
import lombok.Data;
import java.util.Map;

@Data
public class EndpointConfigDTO {
    private String url;
    private String method;
    private AuthType authType;
    private AuthConfigDTO authConfig;
    private Integer timeout;
    private Integer retryCount;
    private TypeExample typeExample;
    private String example;
    private Map<String, String> customHeaders;
}
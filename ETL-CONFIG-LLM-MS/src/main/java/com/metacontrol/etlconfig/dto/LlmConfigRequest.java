package com.metacontrol.etlconfig.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LlmConfigRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "API key is required")
    private String apiKey;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    private String modelName;

    private Boolean isDefault = false;
}

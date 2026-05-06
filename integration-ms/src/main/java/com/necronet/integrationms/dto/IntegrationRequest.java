package com.necronet.integrationms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IntegrationRequest {
    @NotBlank(message = "apiA is required")
    private String apiA;

    @NotBlank(message = "apiB is required")
    private String apiB;

    private String description;
}

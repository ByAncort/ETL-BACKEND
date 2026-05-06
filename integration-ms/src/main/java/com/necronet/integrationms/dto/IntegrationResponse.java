package com.necronet.integrationms.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IntegrationResponse {
    private Long id;
    private String apiA;
    private String apiB;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

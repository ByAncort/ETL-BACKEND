package com.necronet.integrationms.dto;

import lombok.Data;

import com.necronet.integrationms.entity.IntegrationStatus;
import java.time.LocalDateTime;

@Data
public class IntegrationResponse {
    private Long id;
    private String apiA;
    private String apiB;
    private String description;
    private IntegrationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

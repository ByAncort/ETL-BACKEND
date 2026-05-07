package com.necronet.schemamatchingms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class ConnectionResponseDTO {

    private Long id;

    @JsonProperty("apiA")
    private String apiA;

    @JsonProperty("apiB")
    private String apiB;

    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApiA() { return apiA; }
    public void setApiA(String apiA) { this.apiA = apiA; }

    public String getApiB() { return apiB; }
    public void setApiB(String apiB) { this.apiB = apiB; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
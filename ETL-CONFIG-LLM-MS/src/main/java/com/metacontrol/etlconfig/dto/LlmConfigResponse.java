package com.metacontrol.etlconfig.dto;

import com.metacontrol.etlconfig.entity.LlmStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmConfigResponse {

    private Long id;
    private String name;
    private String provider;
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private Boolean isDefault;
    private LlmStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

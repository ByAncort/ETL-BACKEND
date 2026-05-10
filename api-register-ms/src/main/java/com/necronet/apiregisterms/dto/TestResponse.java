package com.necronet.apiregisterms.dto;

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
public class TestResponse {
    private int statusCode;
    private String body;
    private Map<String, String> headers;
    private long responseTimeMs;
    private LocalDateTime timestamp;
    private String error;
}

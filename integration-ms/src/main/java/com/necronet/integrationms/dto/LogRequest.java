package com.necronet.integrationms.dto;

import lombok.Data;

@Data
public class LogRequest {
    private String parentId;
    private String executionId;
    private String serviceName;
    private String className;
    private String methodName;
    private String logLevel;
    private String message;
    private String detail;
    private Long durationMs;
    private String integrationId;
}

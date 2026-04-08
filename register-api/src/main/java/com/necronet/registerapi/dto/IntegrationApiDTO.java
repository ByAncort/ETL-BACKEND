package com.necronet.registerapi.dto;

import com.necronet.registerapi.entity.enums.ExecutionMode;
import com.necronet.registerapi.entity.enums.ScheduleFrequency;
import lombok.Data;

@Data
public class IntegrationApiDTO {
    private String name;
    private String description;
    private ExecutionMode executionMode;
    private ScheduleFrequency scheduleFrequency;
    private String cronExpression;
    private Boolean active;
    private EndpointConfigDTO inputEndpoint;
    private EndpointConfigDTO outputEndpoint;
}
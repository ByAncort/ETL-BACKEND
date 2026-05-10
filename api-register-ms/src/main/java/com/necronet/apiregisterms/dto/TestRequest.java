package com.necronet.apiregisterms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TestRequest {
    private String pathParams;
    private String queryParams;
    private String body;
}

package com.necronet.registerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "api_inputs")
public class ApiInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "endpoint_path", length = 200)
    private String endpointPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false)
    private HttpMethod httpMethod;

    @Column(name = "headers_json", columnDefinition = "TEXT")
    private String headersJson;

    @Column(name = "request_body_template", columnDefinition = "TEXT")
    private String requestBodyTemplate;

    @Column(name = "query_params_template", columnDefinition = "TEXT")
    private String queryParamsTemplate;

    @Column(name = "path_params_template", columnDefinition = "TEXT")
    private String pathParamsTemplate;

    @Column(name = "pagination_type", length = 20)
    private String paginationType; // OFFSET, PAGE_NUMBER, CURSOR, LINK_HEADER

    @Column(name = "pagination_config", columnDefinition = "TEXT")
    private String paginationConfig;

    @Column(name = "timeout_ms")
    private Integer timeoutMs = 30000;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "retry_delay_ms")
    private Integer retryDelayMs = 1000;

    @OneToOne(mappedBy = "apiInput")
    private IntegrationApis integrationApi;

    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    }
}
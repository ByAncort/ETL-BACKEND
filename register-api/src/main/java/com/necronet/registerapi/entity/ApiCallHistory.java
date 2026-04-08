package com.necronet.registerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "api_call_history")
public class ApiCallHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_api_id", nullable = false)
    private IntegrationApis integrationApi;

    @Column(name = "call_timestamp", nullable = false)
    private LocalDateTime callTimestamp;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "is_success")
    private Boolean isSuccess;

    @Column(name = "records_extracted")
    private Integer recordsExtracted;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_sample", columnDefinition = "TEXT")
    private String responseSample;

    @Column(name = "schema_drift_detected")
    private Boolean schemaDriftDetected = false;

    @Column(name = "schema_drift_details", columnDefinition = "TEXT")
    private String schemaDriftDetails;

    @Column(name = "ml_mapping_confidence")
    private Double mlMappingConfidence;

    @PrePersist
    protected void onCreate() {
        callTimestamp = LocalDateTime.now();
    }
}
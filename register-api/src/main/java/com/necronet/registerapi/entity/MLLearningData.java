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
@Table(name = "ml_learning_data")
public class MLLearningData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_api_id", nullable = false)
    private IntegrationApis integrationApi;

    @Column(name = "source_field", length = 200)
    private String sourceField;

    @Column(name = "source_field_type", length = 50)
    private String sourceFieldType;

    @Column(name = "target_field", length = 200)
    private String targetField;

    @Column(name = "target_field_type", length = 50)
    private String targetFieldType;

    @Column(name = "mapping_confidence")
    private Double mappingConfidence;

    @Column(name = "validation_status")
    private String validationStatus; // PENDING, APPROVED, REJECTED

    @Column(name = "sample_values", columnDefinition = "TEXT")
    private String sampleValues;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "validated_by", length = 100)
    private String validatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
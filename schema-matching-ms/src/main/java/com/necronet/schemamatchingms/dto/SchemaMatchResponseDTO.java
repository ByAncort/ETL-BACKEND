package com.necronet.schemamatchingms.dto;

import com.necronet.schemamatchingms.entity.MatchStatus;
import com.necronet.schemamatchingms.entity.SchemaMatch;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SchemaMatchResponseDTO {

    private Long id;
    private Long integrationId;
    private String sourceField;
    private String targetField;
    private BigDecimal confidence;
    private MatchStatus status;
    private String transformation;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public static SchemaMatchResponseDTO fromEntity(SchemaMatch entity) {
        SchemaMatchResponseDTO dto = new SchemaMatchResponseDTO();
        dto.setId(entity.getId());
        dto.setIntegrationId(entity.getIntegrationId());
        dto.setSourceField(entity.getSourceField());
        dto.setTargetField(entity.getTargetField());
        dto.setConfidence(entity.getConfidence());
        dto.setStatus(entity.getStatus());
        dto.setTransformation(entity.getTransformation());
        dto.setReviewedBy(entity.getReviewedBy());
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIntegrationId() { return integrationId; }
    public void setIntegrationId(Long integrationId) { this.integrationId = integrationId; }

    public String getSourceField() { return sourceField; }
    public void setSourceField(String sourceField) { this.sourceField = sourceField; }

    public String getTargetField() { return targetField; }
    public void setTargetField(String targetField) { this.targetField = targetField; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    public String getTransformation() { return transformation; }
    public void setTransformation(String transformation) { this.transformation = transformation; }

    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
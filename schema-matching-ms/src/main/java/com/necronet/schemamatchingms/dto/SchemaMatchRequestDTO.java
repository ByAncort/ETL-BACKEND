package com.necronet.schemamatchingms.dto;

import com.necronet.schemamatchingms.entity.MatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class SchemaMatchRequestDTO {

    private Long integrationId;

    @NotBlank(message = "Source field is required")
    private String sourceField;

    @NotBlank(message = "Target field is required")
    private String targetField;

    @NotNull(message = "Confidence is required")
    private BigDecimal confidence;

    private MatchStatus status;
    private String transformation;
    private Long reviewedBy;

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
}
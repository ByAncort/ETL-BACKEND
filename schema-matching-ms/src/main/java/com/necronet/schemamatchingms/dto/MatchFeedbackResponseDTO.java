package com.necronet.schemamatchingms.dto;

import com.necronet.schemamatchingms.entity.MatchFeedback;
import java.time.LocalDateTime;

public class MatchFeedbackResponseDTO {

    private Long id;
    private Long matchId;
    private Boolean userApproved;
    private String actualTarget;
    private LocalDateTime createdAt;

    public static MatchFeedbackResponseDTO fromEntity(MatchFeedback entity) {
        MatchFeedbackResponseDTO dto = new MatchFeedbackResponseDTO();
        dto.setId(entity.getId());
        dto.setMatchId(entity.getMatchId());
        dto.setUserApproved(entity.getUserApproved());
        dto.setActualTarget(entity.getActualTarget());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public Boolean getUserApproved() { return userApproved; }
    public void setUserApproved(Boolean userApproved) { this.userApproved = userApproved; }

    public String getActualTarget() { return actualTarget; }
    public void setActualTarget(String actualTarget) { this.actualTarget = actualTarget; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
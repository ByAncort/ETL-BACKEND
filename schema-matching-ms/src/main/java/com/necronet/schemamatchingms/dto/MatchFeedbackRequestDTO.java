package com.necronet.schemamatchingms.dto;

import jakarta.validation.constraints.NotNull;

public class MatchFeedbackRequestDTO {

    @NotNull(message = "Match ID is required")
    private Long matchId;

    @NotNull(message = "User approved is required")
    private Boolean userApproved;

    private String actualTarget;

    @NotNull(message = "Reviewed by is required")
    private Long reviewedBy;

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public Boolean getUserApproved() { return userApproved; }
    public void setUserApproved(Boolean userApproved) { this.userApproved = userApproved; }

    public String getActualTarget() { return actualTarget; }
    public void setActualTarget(String actualTarget) { this.actualTarget = actualTarget; }

    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
}
package com.necronet.schemamatchingms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_feedback")
public class MatchFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "user_approved", nullable = false)
    private Boolean userApproved;

    @Column(name = "actual_target", length = 500)
    private String actualTarget;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public MatchFeedback() {}

    public MatchFeedback(Long matchId, Boolean userApproved, String actualTarget) {
        this.matchId = matchId;
        this.userApproved = userApproved;
        this.actualTarget = actualTarget;
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
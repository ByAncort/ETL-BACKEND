package com.necronet.demo.dto;

import com.necronet.demo.entity.WorkspaceProject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProjectResponse {

    private Long id;
    private String projectCode;
    private String projectTitle;
    private String projectCategory;
    private String responsibleManager;
    private String constructionManager;
    private String businessRegion;
    private String projectCountry;
    private String currentStatus;
    private String projectPhase;
    private String summaryDescription;
    private String locationPath;
    private String geoLatitude;
    private String geoLongitude;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalBudget;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse fromEntity(WorkspaceProject entity) {
        ProjectResponse r = new ProjectResponse();
        r.id = entity.getId();
        r.projectCode = entity.getProjectCode();
        r.projectTitle = entity.getProjectTitle();
        r.projectCategory = entity.getProjectCategory();
        r.responsibleManager = entity.getResponsibleManager();
        r.constructionManager = entity.getConstructionManager();
        r.businessRegion = entity.getBusinessRegion();
        r.projectCountry = entity.getProjectCountry();
        r.currentStatus = entity.getCurrentStatus();
        r.projectPhase = entity.getProjectPhase();
        r.summaryDescription = entity.getSummaryDescription();
        r.locationPath = entity.getLocationPath();
        r.geoLatitude = entity.getGeoLatitude();
        r.geoLongitude = entity.getGeoLongitude();
        r.startDate = entity.getStartDate();
        r.endDate = entity.getEndDate();
        r.totalBudget = entity.getTotalBudget();
        r.createdAt = entity.getCreatedAt();
        r.updatedAt = entity.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getProjectCode() { return projectCode; }
    public String getProjectTitle() { return projectTitle; }
    public String getProjectCategory() { return projectCategory; }
    public String getResponsibleManager() { return responsibleManager; }
    public String getConstructionManager() { return constructionManager; }
    public String getBusinessRegion() { return businessRegion; }
    public String getProjectCountry() { return projectCountry; }
    public String getCurrentStatus() { return currentStatus; }
    public String getProjectPhase() { return projectPhase; }
    public String getSummaryDescription() { return summaryDescription; }
    public String getLocationPath() { return locationPath; }
    public String getGeoLatitude() { return geoLatitude; }
    public String getGeoLongitude() { return geoLongitude; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

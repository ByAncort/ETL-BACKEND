package com.necronet.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_projects")
public class WorkspaceProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code", unique = true, length = 50)
    private String projectCode;

    @Column(name = "project_title", length = 200)
    private String projectTitle;

    @Column(name = "project_category", length = 100)
    private String projectCategory;

    @Column(name = "responsible_manager", length = 150)
    private String responsibleManager;

    @Column(name = "construction_manager", length = 150)
    private String constructionManager;

    @Column(name = "business_region", length = 100)
    private String businessRegion;

    @Column(name = "project_country", length = 100)
    private String projectCountry;

    @Column(name = "current_status", length = 50)
    private String currentStatus;

    @Column(name = "project_phase", length = 50)
    private String projectPhase;

    @Column(name = "summary_description", columnDefinition = "TEXT")
    private String summaryDescription;

    @Column(name = "location_path", length = 255)
    private String locationPath;

    @Column(name = "geo_latitude", length = 30)
    private String geoLatitude;

    @Column(name = "geo_longitude", length = 30)
    private String geoLongitude;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_budget", precision = 15, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getProjectCategory() { return projectCategory; }
    public void setProjectCategory(String projectCategory) { this.projectCategory = projectCategory; }

    public String getResponsibleManager() { return responsibleManager; }
    public void setResponsibleManager(String responsibleManager) { this.responsibleManager = responsibleManager; }

    public String getConstructionManager() { return constructionManager; }
    public void setConstructionManager(String constructionManager) { this.constructionManager = constructionManager; }

    public String getBusinessRegion() { return businessRegion; }
    public void setBusinessRegion(String businessRegion) { this.businessRegion = businessRegion; }

    public String getProjectCountry() { return projectCountry; }
    public void setProjectCountry(String projectCountry) { this.projectCountry = projectCountry; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public String getProjectPhase() { return projectPhase; }
    public void setProjectPhase(String projectPhase) { this.projectPhase = projectPhase; }

    public String getSummaryDescription() { return summaryDescription; }
    public void setSummaryDescription(String summaryDescription) { this.summaryDescription = summaryDescription; }

    public String getLocationPath() { return locationPath; }
    public void setLocationPath(String locationPath) { this.locationPath = locationPath; }

    public String getGeoLatitude() { return geoLatitude; }
    public void setGeoLatitude(String geoLatitude) { this.geoLatitude = geoLatitude; }

    public String getGeoLongitude() { return geoLongitude; }
    public void setGeoLongitude(String geoLongitude) { this.geoLongitude = geoLongitude; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

package com.necronet.demo.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProjectCreateRequest {

    @NotBlank(message = "Project code is required")
    private String projectCode;

    @NotBlank(message = "Project title is required")
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
}

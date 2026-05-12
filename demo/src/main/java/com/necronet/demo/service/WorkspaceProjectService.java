package com.necronet.demo.service;

import com.necronet.demo.dto.ProjectCreateRequest;
import com.necronet.demo.dto.ProjectResponse;
import com.necronet.demo.entity.WorkspaceProject;
import com.necronet.demo.repository.WorkspaceProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspaceProjectService {

    private final WorkspaceProjectRepository repository;

    public WorkspaceProjectService(WorkspaceProjectRepository repository) {
        this.repository = repository;
    }

    public ProjectResponse create(ProjectCreateRequest request) {
        WorkspaceProject entity = new WorkspaceProject();
        entity.setProjectCode(request.getProjectCode());
        entity.setProjectTitle(request.getProjectTitle());
        entity.setProjectCategory(request.getProjectCategory());
        entity.setResponsibleManager(request.getResponsibleManager());
        entity.setConstructionManager(request.getConstructionManager());
        entity.setBusinessRegion(request.getBusinessRegion());
        entity.setProjectCountry(request.getProjectCountry());
        entity.setCurrentStatus(request.getCurrentStatus());
        entity.setProjectPhase(request.getProjectPhase());
        entity.setSummaryDescription(request.getSummaryDescription());
        entity.setLocationPath(request.getLocationPath());
        entity.setGeoLatitude(request.getGeoLatitude());
        entity.setGeoLongitude(request.getGeoLongitude());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setTotalBudget(request.getTotalBudget());

        return ProjectResponse.fromEntity(repository.save(entity));
    }

    public List<ProjectResponse> findAll() {
        return repository.findAll().stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    public ProjectResponse findByCode(String code) {
        return repository.findByProjectCode(code)
                .map(ProjectResponse::fromEntity)
                .orElse(null);
    }
}

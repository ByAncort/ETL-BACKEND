package com.necronet.demo.service;

import com.necronet.demo.dto.ProjectCreateRequest;
import com.necronet.demo.dto.ProjectResponse;
import com.necronet.demo.entity.WorkspaceProject;
import com.necronet.demo.repository.WorkspaceProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WorkspaceProjectServiceTest {

    @Mock
    private WorkspaceProjectRepository repository;

    @InjectMocks
    private WorkspaceProjectService service;

    @Captor
    private ArgumentCaptor<WorkspaceProject> entityCaptor;

    @Test
    void create_shouldMapRequestToEntityAndSave() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setProjectCode("PRJ-001");
        request.setProjectTitle("Test Project");
        request.setProjectCategory("Infrastructure");
        request.setResponsibleManager("John Doe");
        request.setConstructionManager("Jane Smith");
        request.setBusinessRegion("North America");
        request.setProjectCountry("USA");
        request.setCurrentStatus("Active");
        request.setProjectPhase("Execution");
        request.setSummaryDescription("A test project");
        request.setLocationPath("New York");
        request.setGeoLatitude("40.7128");
        request.setGeoLongitude("-74.0060");
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 12, 31));
        request.setTotalBudget(new BigDecimal("1000000.00"));

        WorkspaceProject saved = new WorkspaceProject();
        saved.setId(1L);
        saved.setProjectCode("PRJ-001");
        saved.setProjectTitle("Test Project");
        saved.setProjectCategory("Infrastructure");
        saved.setResponsibleManager("John Doe");
        saved.setConstructionManager("Jane Smith");
        saved.setBusinessRegion("North America");
        saved.setProjectCountry("USA");
        saved.setCurrentStatus("Active");
        saved.setProjectPhase("Execution");
        saved.setSummaryDescription("A test project");
        saved.setLocationPath("New York");
        saved.setGeoLatitude("40.7128");
        saved.setGeoLongitude("-74.0060");
        saved.setStartDate(LocalDate.of(2024, 1, 1));
        saved.setEndDate(LocalDate.of(2024, 12, 31));
        saved.setTotalBudget(new BigDecimal("1000000.00"));

        given(repository.save(any(WorkspaceProject.class))).willReturn(saved);

        ProjectResponse result = service.create(request);

        then(repository).should().save(entityCaptor.capture());
        WorkspaceProject captured = entityCaptor.getValue();
        assertThat(captured.getProjectCode()).isEqualTo("PRJ-001");
        assertThat(captured.getProjectTitle()).isEqualTo("Test Project");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProjectCode()).isEqualTo("PRJ-001");
    }

    @Test
    void findAll_shouldReturnAllProjects() {
        WorkspaceProject p1 = new WorkspaceProject();
        p1.setId(1L);
        p1.setProjectCode("PRJ-001");

        WorkspaceProject p2 = new WorkspaceProject();
        p2.setId(2L);
        p2.setProjectCode("PRJ-002");

        given(repository.findAll()).willReturn(List.of(p1, p2));

        List<ProjectResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProjectCode()).isEqualTo("PRJ-001");
        assertThat(result.get(1).getProjectCode()).isEqualTo("PRJ-002");
    }

    @Test
    void findByCode_whenExists_shouldReturnProject() {
        WorkspaceProject entity = new WorkspaceProject();
        entity.setId(1L);
        entity.setProjectCode("PRJ-001");
        entity.setProjectTitle("Test Project");

        given(repository.findByProjectCode("PRJ-001")).willReturn(Optional.of(entity));

        ProjectResponse result = service.findByCode("PRJ-001");

        assertThat(result).isNotNull();
        assertThat(result.getProjectCode()).isEqualTo("PRJ-001");
    }

    @Test
    void findByCode_whenNotExists_shouldReturnNull() {
        given(repository.findByProjectCode("NONEXISTENT")).willReturn(Optional.empty());

        ProjectResponse result = service.findByCode("NONEXISTENT");

        assertThat(result).isNull();
    }
}

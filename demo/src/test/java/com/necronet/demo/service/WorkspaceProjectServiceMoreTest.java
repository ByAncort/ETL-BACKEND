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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WorkspaceProjectServiceMoreTest {

    @Mock
    private WorkspaceProjectRepository repository;

    @InjectMocks
    private WorkspaceProjectService service;

    @Captor
    private ArgumentCaptor<WorkspaceProject> entityCaptor;

    @Test
    void create_shouldSucceed_whenOnlyRequiredFieldsSet() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setProjectCode("PRJ-003");
        request.setProjectTitle("Minimal Project");

        WorkspaceProject saved = new WorkspaceProject();
        saved.setId(3L);
        saved.setProjectCode("PRJ-003");
        saved.setProjectTitle("Minimal Project");

        given(repository.save(any(WorkspaceProject.class))).willReturn(saved);

        ProjectResponse result = service.create(request);

        then(repository).should().save(entityCaptor.capture());
        WorkspaceProject captured = entityCaptor.getValue();
        assertThat(captured.getProjectCode()).isEqualTo("PRJ-003");
        assertThat(captured.getProjectTitle()).isEqualTo("Minimal Project");
        assertThat(captured.getProjectCategory()).isNull();
        assertThat(captured.getResponsibleManager()).isNull();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getProjectCode()).isEqualTo("PRJ-003");
    }

    @Test
    void create_shouldSucceed_whenProjectCodeMissing() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setProjectTitle("No Code Project");

        WorkspaceProject saved = new WorkspaceProject();
        saved.setId(4L);
        saved.setProjectTitle("No Code Project");

        given(repository.save(any(WorkspaceProject.class))).willReturn(saved);

        ProjectResponse result = service.create(request);

        then(repository).should().save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getProjectCode()).isNull();
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(4L);
    }

    @Test
    void create_shouldSucceed_whenAllFieldsNull() {
        ProjectCreateRequest request = new ProjectCreateRequest();

        WorkspaceProject saved = new WorkspaceProject();
        saved.setId(5L);

        given(repository.save(any(WorkspaceProject.class))).willReturn(saved);

        ProjectResponse result = service.create(request);

        then(repository).should().save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getProjectCode()).isNull();
        assertThat(entityCaptor.getValue().getProjectTitle()).isNull();
        assertThat(result).isNotNull();
    }

    @Test
    void findByCode_shouldReturnNull_whenCodeIsNull() {
        given(repository.findByProjectCode(null)).willReturn(Optional.empty());

        ProjectResponse result = service.findByCode(null);

        assertThat(result).isNull();
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoProjects() {
        given(repository.findAll()).willReturn(List.of());

        List<ProjectResponse> result = service.findAll();

        assertThat(result).isEmpty();
    }
}

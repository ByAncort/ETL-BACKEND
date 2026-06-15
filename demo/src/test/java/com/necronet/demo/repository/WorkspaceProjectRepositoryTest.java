package com.necronet.demo.repository;

import com.necronet.demo.entity.WorkspaceProject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WorkspaceProjectRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private WorkspaceProjectRepository repository;

    @Test
    void findByProjectCode_whenExists_shouldReturnEntity() {
        WorkspaceProject project = new WorkspaceProject();
        project.setProjectCode("PRJ-001");
        project.setProjectTitle("Test Project");
        project.setCurrentStatus("Active");

        em.persist(project);
        em.flush();
        em.clear();

        Optional<WorkspaceProject> result = repository.findByProjectCode("PRJ-001");

        assertThat(result).isPresent();
        assertThat(result.get().getProjectCode()).isEqualTo("PRJ-001");
        assertThat(result.get().getProjectTitle()).isEqualTo("Test Project");
    }

    @Test
    void findByProjectCode_whenNotExists_shouldReturnEmpty() {
        em.flush();
        em.clear();

        Optional<WorkspaceProject> result = repository.findByProjectCode("NONEXISTENT");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByProjectCode_whenExists_shouldReturnTrue() {
        WorkspaceProject project = new WorkspaceProject();
        project.setProjectCode("PRJ-002");
        project.setProjectTitle("Another Project");
        project.setCurrentStatus("Active");

        em.persist(project);
        em.flush();
        em.clear();

        boolean exists = repository.existsByProjectCode("PRJ-002");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByProjectCode_whenNotExists_shouldReturnFalse() {
        em.flush();
        em.clear();

        boolean exists = repository.existsByProjectCode("NONEXISTENT");

        assertThat(exists).isFalse();
    }
}

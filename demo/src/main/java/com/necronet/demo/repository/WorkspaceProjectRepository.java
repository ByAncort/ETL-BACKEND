package com.necronet.demo.repository;

import com.necronet.demo.entity.WorkspaceProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkspaceProjectRepository extends JpaRepository<WorkspaceProject, Long> {
    Optional<WorkspaceProject> findByProjectCode(String projectCode);
    boolean existsByProjectCode(String projectCode);
}

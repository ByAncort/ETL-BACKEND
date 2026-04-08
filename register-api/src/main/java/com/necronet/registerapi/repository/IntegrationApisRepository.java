package com.necronet.registerapi.repository;

import com.necronet.registerapi.entity.IntegrationApis;
import com.necronet.registerapi.entity.enums.ExecutionMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationApisRepository extends JpaRepository<IntegrationApis, Long> {
    List<IntegrationApis> findByExecutionMode(ExecutionMode executionMode);
    List<IntegrationApis> findByExecutionModeAndActiveTrue(ExecutionMode executionMode);
    Optional<IntegrationApis> findByName(String name);
    List<IntegrationApis> findByActiveTrue();
}
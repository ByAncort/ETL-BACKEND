package com.necronet.registerapi.repository;

import com.necronet.registerapi.entity.ExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, Long> {
    List<ExecutionHistory> findByIntegrationApiIdOrderByStartTimeDesc(Long integrationApiId);
}
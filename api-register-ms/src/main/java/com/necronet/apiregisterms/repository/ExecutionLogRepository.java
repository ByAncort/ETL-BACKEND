package com.necronet.apiregisterms.repository;

import com.necronet.apiregisterms.entity.ExecutionLog;
import com.necronet.apiregisterms.entity.LogLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, String> {
    List<ExecutionLog> findByExecutionIdOrderByTimestampAsc(String executionId);
    List<ExecutionLog> findByParentIdOrderByTimestampAsc(String parentId);
    List<ExecutionLog> findByLogLevelOrderByTimestampDesc(LogLevel logLevel);
    List<ExecutionLog> findByIntegrationIdOrderByTimestampDesc(String integrationId);
}

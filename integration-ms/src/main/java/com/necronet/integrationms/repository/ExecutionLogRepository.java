package com.necronet.integrationms.repository;

import com.necronet.integrationms.entity.ExecutionLog;
import com.necronet.integrationms.entity.LogLevel;
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

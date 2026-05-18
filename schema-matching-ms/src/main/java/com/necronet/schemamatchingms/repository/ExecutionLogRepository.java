package com.necronet.schemamatchingms.repository;

import com.necronet.schemamatchingms.entity.ExecutionLog;
import com.necronet.schemamatchingms.entity.LogLevel;
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

package com.metacontrol.etlconfig.repository;

import com.metacontrol.etlconfig.entity.ExecutionLog;
import com.metacontrol.etlconfig.entity.LogLevel;
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

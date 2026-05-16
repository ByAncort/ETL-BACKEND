package com.necronet.schemamatchingms.repository;

import com.necronet.schemamatchingms.entity.SchemaMatch;
import com.necronet.schemamatchingms.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SchemaMatchRepository extends JpaRepository<SchemaMatch, Long> {
    List<SchemaMatch> findByIntegrationId(Long integrationId);
    List<SchemaMatch> findByIntegrationIdAndStatus(Long integrationId, MatchStatus status);

    @Query("SELECT CASE WHEN COUNT(sm) > 0 THEN true ELSE false END " +
           "FROM SchemaMatch sm " +
           "WHERE sm.integrationId = :integrationId AND sm.sourceField = :sourceField")
    boolean existsSchemaMatch(Long integrationId, String sourceField);
}
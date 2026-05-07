package com.necronet.schemamatchingms.repository;

import com.necronet.schemamatchingms.entity.SchemaMatch;
import com.necronet.schemamatchingms.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SchemaMatchRepository extends JpaRepository<SchemaMatch, Long> {
    List<SchemaMatch> findByIntegrationId(Long integrationId);
    List<SchemaMatch> findByIntegrationIdAndStatus(Long integrationId, MatchStatus status);
}
package com.necronet.schemamatchingms.repository;

import com.necronet.schemamatchingms.entity.MatchStatus;
import com.necronet.schemamatchingms.entity.SchemaMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SchemaMatchRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SchemaMatchRepository repository;

    private SchemaMatch pendingMatch;
    private SchemaMatch acceptedMatch;
    private SchemaMatch otherIntegrationMatch;

    @BeforeEach
    void setUp() {
        pendingMatch = new SchemaMatch(1L, "source_name", "target_name", new BigDecimal("0.9500"));
        acceptedMatch = new SchemaMatch(1L, "source_email", "target_email", new BigDecimal("0.8500"));
        acceptedMatch.setStatus(MatchStatus.ACCEPTED);
        otherIntegrationMatch = new SchemaMatch(2L, "source_addr", "target_addr", new BigDecimal("0.7000"));
    }

    @Test
    void save_shouldPersistAndAssignId() {
        SchemaMatch saved = repository.save(pendingMatch);
        entityManager.flush();
        entityManager.clear();

        Optional<SchemaMatch> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSourceField()).isEqualTo("source_name");
        assertThat(found.get().getTargetField()).isEqualTo("target_name");
        assertThat(found.get().getIntegrationId()).isEqualTo(1L);
        assertThat(found.get().getConfidence()).isEqualByComparingTo(new BigDecimal("0.9500"));
        assertThat(found.get().getStatus()).isEqualTo(MatchStatus.PENDING);
    }

    @Test
    void findByIntegrationId_shouldReturnOnlyMatchingIntegration() {
        entityManager.persist(pendingMatch);
        entityManager.persist(acceptedMatch);
        entityManager.persist(otherIntegrationMatch);
        entityManager.flush();
        entityManager.clear();

        List<SchemaMatch> results = repository.findByIntegrationId(1L);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(SchemaMatch::getSourceField)
                .containsExactlyInAnyOrder("source_name", "source_email");
    }

    @Test
    void findByIntegrationId_shouldReturnEmptyWhenNoneMatch() {
        entityManager.persist(pendingMatch);
        entityManager.flush();
        entityManager.clear();

        List<SchemaMatch> results = repository.findByIntegrationId(99L);

        assertThat(results).isEmpty();
    }

    @Test
    void findByIntegrationIdAndStatus_shouldReturnFilteredMatches() {
        entityManager.persist(pendingMatch);
        entityManager.persist(acceptedMatch);
        entityManager.persist(otherIntegrationMatch);
        entityManager.flush();
        entityManager.clear();

        List<SchemaMatch> pending = repository.findByIntegrationIdAndStatus(1L, MatchStatus.PENDING);
        List<SchemaMatch> accepted = repository.findByIntegrationIdAndStatus(1L, MatchStatus.ACCEPTED);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getSourceField()).isEqualTo("source_name");

        assertThat(accepted).hasSize(1);
        assertThat(accepted.get(0).getSourceField()).isEqualTo("source_email");
    }

    @Test
    void findByIntegrationIdAndStatus_shouldReturnEmptyWhenNoMatch() {
        entityManager.persist(pendingMatch);
        entityManager.flush();
        entityManager.clear();

        List<SchemaMatch> rejected = repository.findByIntegrationIdAndStatus(1L, MatchStatus.REJECTED);

        assertThat(rejected).isEmpty();
    }

    @Test
    void existsSchemaMatch_shouldReturnTrueWhenExists() {
        entityManager.persist(pendingMatch);
        entityManager.flush();
        entityManager.clear();

        boolean exists = repository.existsSchemaMatch(1L, "source_name");

        assertThat(exists).isTrue();
    }

    @Test
    void existsSchemaMatch_shouldReturnFalseWhenNotExists() {
        entityManager.persist(pendingMatch);
        entityManager.flush();
        entityManager.clear();

        boolean exists = repository.existsSchemaMatch(1L, "nonexistent");

        assertThat(exists).isFalse();
    }
}

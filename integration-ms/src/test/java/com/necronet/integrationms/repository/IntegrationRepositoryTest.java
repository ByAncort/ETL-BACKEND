package com.necronet.integrationms.repository;

import com.necronet.integrationms.entity.Integration;
import com.necronet.integrationms.entity.IntegrationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class IntegrationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Test
    void save_shouldPersistIntegration() {
        Integration integration = new Integration();
        integration.setApiA("api-a");
        integration.setApiB("api-b");
        integration.setDescription("test save");
        integration.setStatus(IntegrationStatus.ACTIVE);

        entityManager.persistAndFlush(integration);
        entityManager.clear();

        Integration found = integrationRepository.findById(integration.getId()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getApiA()).isEqualTo("api-a");
        assertThat(found.getApiB()).isEqualTo("api-b");
        assertThat(found.getDescription()).isEqualTo("test save");
        assertThat(found.getStatus()).isEqualTo(IntegrationStatus.ACTIVE);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByStatus_shouldReturnMatchingIntegrations() {
        Integration active1 = new Integration();
        active1.setApiA("api-1");
        active1.setApiB("api-2");
        active1.setStatus(IntegrationStatus.ACTIVE);

        Integration active2 = new Integration();
        active2.setApiA("api-3");
        active2.setApiB("api-4");
        active2.setStatus(IntegrationStatus.ACTIVE);

        Integration deleted = new Integration();
        deleted.setApiA("api-5");
        deleted.setApiB("api-6");
        deleted.setStatus(IntegrationStatus.DELETED);

        entityManager.persistAndFlush(active1);
        entityManager.persistAndFlush(active2);
        entityManager.persistAndFlush(deleted);
        entityManager.clear();

        List<Integration> activeIntegrations = integrationRepository.findByStatus(IntegrationStatus.ACTIVE);

        assertThat(activeIntegrations).hasSize(2);
        assertThat(activeIntegrations).extracting(Integration::getApiA)
                .containsExactlyInAnyOrder("api-1", "api-3");
    }

    @Test
    void findByStatusNot_shouldExcludeGivenStatus() {
        Integration active = new Integration();
        active.setApiA("api-a");
        active.setApiB("api-b");
        active.setStatus(IntegrationStatus.ACTIVE);

        Integration deleted1 = new Integration();
        deleted1.setApiA("api-c");
        deleted1.setApiB("api-d");
        deleted1.setStatus(IntegrationStatus.DELETED);

        Integration deleted2 = new Integration();
        deleted2.setApiA("api-e");
        deleted2.setApiB("api-f");
        deleted2.setStatus(IntegrationStatus.DELETED);

        entityManager.persistAndFlush(active);
        entityManager.persistAndFlush(deleted1);
        entityManager.persistAndFlush(deleted2);
        entityManager.clear();

        List<Integration> nonDeleted = integrationRepository.findByStatusNot(IntegrationStatus.DELETED);

        assertThat(nonDeleted).hasSize(1);
        assertThat(nonDeleted.get(0).getApiA()).isEqualTo("api-a");
        assertThat(nonDeleted.get(0).getStatus()).isEqualTo(IntegrationStatus.ACTIVE);
    }
}

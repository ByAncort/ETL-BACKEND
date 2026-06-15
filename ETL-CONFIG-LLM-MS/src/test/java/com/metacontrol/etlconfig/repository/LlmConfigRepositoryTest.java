package com.metacontrol.etlconfig.repository;

import com.metacontrol.etlconfig.entity.LlmConfig;
import com.metacontrol.etlconfig.entity.LlmStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LlmConfigRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LlmConfigRepository repository;

    private LlmConfig createConfig(String name, Boolean isDefault) {
        LlmConfig config = new LlmConfig();
        config.setName(name);
        config.setProvider("openai");
        config.setApiKey("sk-xxx");
        config.setBaseUrl("https://api.openai.com");
        config.setModelName("gpt-4");
        config.setIsDefault(isDefault);
        config.setStatus(LlmStatus.active);
        return config;
    }

    @Test
    void saveShouldPersistConfig() {
        LlmConfig config = createConfig("my-config", false);
        LlmConfig saved = entityManager.persistFlushFind(config);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("my-config");
        assertThat(saved.getStatus()).isEqualTo(LlmStatus.active);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByIsDefaultTrueShouldReturnDefault() {
        LlmConfig config1 = createConfig("cfg-1", false);
        LlmConfig config2 = createConfig("cfg-2", true);
        entityManager.persistAndFlush(config1);
        entityManager.persistAndFlush(config2);
        entityManager.clear();

        Optional<LlmConfig> found = repository.findByIsDefaultTrue();

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("cfg-2");
        assertThat(found.get().getIsDefault()).isTrue();
    }

    @Test
    void findByIsDefaultTrueShouldReturnEmptyWhenNoneDefault() {
        LlmConfig config = createConfig("cfg-1", false);
        entityManager.persistAndFlush(config);
        entityManager.clear();

        Optional<LlmConfig> found = repository.findByIsDefaultTrue();

        assertThat(found).isEmpty();
    }

    @Test
    void existsByNameShouldReturnTrueWhenExists() {
        entityManager.persistAndFlush(createConfig("my-config", false));
        entityManager.clear();

        boolean exists = repository.existsByName("my-config");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByNameShouldReturnFalseWhenNotExists() {
        boolean exists = repository.existsByName("non-existent");

        assertThat(exists).isFalse();
    }
}

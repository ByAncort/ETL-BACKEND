package com.metacontrol.etlconfig.service;

import com.metacontrol.etlconfig.dto.LlmConfigRequest;
import com.metacontrol.etlconfig.dto.LlmConfigResponse;
import com.metacontrol.etlconfig.entity.LlmConfig;
import com.metacontrol.etlconfig.entity.LlmStatus;
import com.metacontrol.etlconfig.repository.LlmConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmConfigServiceTest {

    @Mock
    private LlmConfigRepository repository;

    @InjectMocks
    private LlmConfigService service;

    private LlmConfig createConfig(Long id, String name, Boolean isDefault) {
        LlmConfig config = new LlmConfig();
        config.setId(id);
        config.setName(name);
        config.setProvider("openai");
        config.setApiKey("sk-xxx");
        config.setBaseUrl("https://api.openai.com");
        config.setModelName("gpt-4");
        config.setIsDefault(isDefault);
        config.setStatus(LlmStatus.active);
        return config;
    }

    private LlmConfigRequest createRequest(String name, Boolean isDefault) {
        LlmConfigRequest request = new LlmConfigRequest();
        request.setName(name);
        request.setProvider("openai");
        request.setApiKey("sk-xxx");
        request.setBaseUrl("https://api.openai.com");
        request.setModelName("gpt-4");
        request.setIsDefault(isDefault);
        return request;
    }

    @Test
    void createShouldSaveAndReturnResponse() {
        LlmConfigRequest request = createRequest("my-config", false);
        LlmConfig saved = createConfig(1L, "my-config", false);

        given(repository.existsByName("my-config")).willReturn(false);
        given(repository.save(any(LlmConfig.class))).willReturn(saved);

        LlmConfigResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("my-config");
        assertThat(response.getProvider()).isEqualTo("openai");
        verify(repository).save(any(LlmConfig.class));
    }

    @Test
    void createShouldThrowWhenNameExists() {
        LlmConfigRequest request = createRequest("my-config", false);

        given(repository.existsByName("my-config")).willReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
    }

    @Test
    void createShouldClearExistingDefaultWhenNewIsDefault() {
        LlmConfigRequest request = createRequest("new-default", true);
        LlmConfig currentDefault = createConfig(1L, "old-default", true);
        LlmConfig saved = createConfig(2L, "new-default", true);

        given(repository.existsByName("new-default")).willReturn(false);
        given(repository.findByIsDefaultTrue()).willReturn(Optional.of(currentDefault));
        given(repository.save(any(LlmConfig.class))).willReturn(saved);

        LlmConfigResponse response = service.create(request);

        assertThat(response.getIsDefault()).isTrue();
        verify(repository).save(currentDefault);
        assertThat(currentDefault.getIsDefault()).isFalse();
    }

    @Test
    void getByIdShouldReturnResponse() {
        LlmConfig config = createConfig(1L, "my-config", false);
        given(repository.findById(1L)).willReturn(Optional.of(config));

        LlmConfigResponse response = service.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("my-config");
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getAllShouldReturnAllConfigs() {
        LlmConfig c1 = createConfig(1L, "cfg-1", false);
        LlmConfig c2 = createConfig(2L, "cfg-2", true);
        given(repository.findAll()).willReturn(List.of(c1, c2));

        List<LlmConfigResponse> responses = service.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(LlmConfigResponse::getName)
                .containsExactly("cfg-1", "cfg-2");
    }

    @Test
    void getDefaultShouldReturnDefault() {
        LlmConfig config = createConfig(1L, "default-cfg", true);
        given(repository.findByIsDefaultTrue()).willReturn(Optional.of(config));

        LlmConfigResponse response = service.getDefault();

        assertThat(response.getIsDefault()).isTrue();
        assertThat(response.getName()).isEqualTo("default-cfg");
    }

    @Test
    void getDefaultShouldThrowWhenNoneFound() {
        given(repository.findByIsDefaultTrue()).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDefault())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No default");
    }

    @Test
    void updateShouldModifyExistingConfig() {
        LlmConfig existing = createConfig(1L, "old-name", false);
        LlmConfigRequest request = createRequest("new-name", true);

        given(repository.findById(1L)).willReturn(Optional.of(existing));
        given(repository.existsByName("new-name")).willReturn(false);
        given(repository.findByIsDefaultTrue()).willReturn(Optional.empty());
        given(repository.save(any(LlmConfig.class))).willAnswer(i -> i.getArgument(0));

        LlmConfigResponse response = service.update(1L, request);

        assertThat(response.getName()).isEqualTo("new-name");
        assertThat(response.getIsDefault()).isTrue();
    }

    @Test
    void updateShouldThrowWhenNameConflict() {
        LlmConfig existing = createConfig(1L, "old-name", false);
        LlmConfigRequest request = createRequest("taken-name", false);

        given(repository.findById(1L)).willReturn(Optional.of(existing));
        given(repository.existsByName("taken-name")).willReturn(true);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        LlmConfigRequest request = createRequest("any", false);
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteShouldRemoveExisting() {
        given(repository.existsById(1L)).willReturn(true);
        willDoNothing().given(repository).deleteById(1L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteShouldThrowWhenNotFound() {
        given(repository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void setDefaultShouldClearOldAndSetNew() {
        LlmConfig oldDefault = createConfig(1L, "old-default", true);
        LlmConfig newDefault = createConfig(2L, "new-default", false);

        given(repository.findById(2L)).willReturn(Optional.of(newDefault));
        given(repository.findByIsDefaultTrue()).willReturn(Optional.of(oldDefault));
        given(repository.save(any(LlmConfig.class))).willAnswer(i -> i.getArgument(0));

        LlmConfigResponse response = service.setDefault(2L);

        assertThat(response.getIsDefault()).isTrue();
        assertThat(oldDefault.getIsDefault()).isFalse();
        verify(repository, times(2)).save(any(LlmConfig.class));
    }

    @Test
    void setDefaultShouldThrowWhenNotFound() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setDefault(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void clearExistingDefaultShouldDoNothingWhenNoDefault() {
        LlmConfigRequest request = createRequest("cfg", false);
        LlmConfig saved = createConfig(1L, "cfg", false);

        given(repository.existsByName("cfg")).willReturn(false);
        given(repository.save(any(LlmConfig.class))).willReturn(saved);

        LlmConfigResponse response = service.create(request);

        assertThat(response.getName()).isEqualTo("cfg");
        verify(repository, never()).findByIsDefaultTrue();
    }
}

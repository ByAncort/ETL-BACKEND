package com.metacontrol.etlconfig.service;

import com.metacontrol.etlconfig.dto.LlmConfigRequest;
import com.metacontrol.etlconfig.dto.LlmConfigResponse;
import com.metacontrol.etlconfig.entity.LlmConfig;
import com.metacontrol.etlconfig.entity.LlmStatus;
import com.metacontrol.etlconfig.repository.LlmConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmConfigService {

    private final LlmConfigRepository repository;

    @Transactional
    public LlmConfigResponse create(LlmConfigRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new RuntimeException("LLM config with name '" + request.getName() + "' already exists");
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearExistingDefault();
        }

        LlmConfig config = new LlmConfig();
        config.setName(request.getName());
        config.setProvider(request.getProvider());
        config.setApiKey(request.getApiKey());
        config.setBaseUrl(request.getBaseUrl());
        config.setModelName(request.getModelName());
        config.setIsDefault(request.getIsDefault());

        LlmConfig saved = repository.save(config);
        log.info("LLM config created: {} ({})", saved.getName(), saved.getProvider());
        return mapToResponse(saved);
    }

    public LlmConfigResponse getById(Long id) {
        LlmConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("LLM config not found with id: " + id));
        return mapToResponse(config);
    }

    public List<LlmConfigResponse> getAll() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LlmConfigResponse getDefault() {
        LlmConfig config = repository.findByIsDefaultTrue()
                .orElseThrow(() -> new RuntimeException("No default LLM config found"));
        return mapToResponse(config);
    }

    @Transactional
    public LlmConfigResponse update(Long id, LlmConfigRequest request) {
        LlmConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("LLM config not found with id: " + id));

        if (!config.getName().equals(request.getName())
                && repository.existsByName(request.getName())) {
            throw new RuntimeException("LLM config with name '" + request.getName() + "' already exists");
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(config.getIsDefault())) {
            clearExistingDefault();
        }

        config.setName(request.getName());
        config.setProvider(request.getProvider());
        config.setApiKey(request.getApiKey());
        config.setBaseUrl(request.getBaseUrl());
        config.setModelName(request.getModelName());
        config.setIsDefault(request.getIsDefault());

        LlmConfig updated = repository.save(config);
        log.info("LLM config updated: {} ({})", updated.getName(), updated.getProvider());
        return mapToResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("LLM config not found with id: " + id);
        }
        repository.deleteById(id);
        log.info("LLM config deleted with id: {}", id);
    }

    @Transactional
    public LlmConfigResponse setDefault(Long id) {
        LlmConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("LLM config not found with id: " + id));

        clearExistingDefault();
        config.setIsDefault(true);
        LlmConfig updated = repository.save(config);
        log.info("LLM config set as default: {} ({})", updated.getName(), updated.getProvider());
        return mapToResponse(updated);
    }

    private void clearExistingDefault() {
        repository.findByIsDefaultTrue().ifPresent(currentDefault -> {
            currentDefault.setIsDefault(false);
            repository.save(currentDefault);
        });
    }

    private LlmConfigResponse mapToResponse(LlmConfig config) {
        LlmConfigResponse response = new LlmConfigResponse();
        response.setId(config.getId());
        response.setName(config.getName());
        response.setProvider(config.getProvider());
        response.setApiKey(config.getApiKey());
        response.setBaseUrl(config.getBaseUrl());
        response.setModelName(config.getModelName());
        response.setIsDefault(config.getIsDefault());
        response.setStatus(config.getStatus());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());
        return response;
    }
}

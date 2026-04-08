package com.necronet.registerapi.service;

import com.necronet.registerapi.dto.AuthConfigDTO;
import com.necronet.registerapi.dto.EndpointConfigDTO;
import com.necronet.registerapi.dto.IntegrationApiDTO;
import com.necronet.registerapi.entity.*;
import com.necronet.registerapi.entity.EndpointConfig.HttpMethod;
import com.necronet.registerapi.entity.enums.ExecutionMode;
import com.necronet.registerapi.entity.enums.ScheduleFrequency;
import com.necronet.registerapi.exception.ResourceNotFoundException;
import com.necronet.registerapi.repository.AuthConfigRepository;
import com.necronet.registerapi.repository.EndpointConfigRepository;
import com.necronet.registerapi.repository.ExecutionHistoryRepository;
import com.necronet.registerapi.repository.IntegrationApisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationApiService {

    private final IntegrationApisRepository integrationApisRepository;
    private final EndpointConfigRepository endpointConfigRepository;
    private final AuthConfigRepository authConfigRepository;
    private final ExecutionHistoryRepository executionHistoryRepository;

    @Transactional
    public IntegrationApis createIntegrationApi(IntegrationApiDTO dto) {
        log.info("Creando nueva API de integración: {}", dto.getName());

        validateIntegrationApi(dto);

        IntegrationApis integrationApi = new IntegrationApis();
        mapDtoToEntity(dto, integrationApi);

        // Configurar endpoints
        if (dto.getInputEndpoint() != null) {
            integrationApi.setInputEndpoint(createEndpointConfig(dto.getInputEndpoint()));
        }

        if (dto.getOutputEndpoint() != null) {
            integrationApi.setOutputEndpoint(createEndpointConfig(dto.getOutputEndpoint()));
        }

        // Calcular próxima ejecución si es programada
        if (integrationApi.getExecutionMode() == ExecutionMode.SCHEDULED) {
            calculateNextExecutionTime(integrationApi);
        }

        IntegrationApis saved = integrationApisRepository.save(integrationApi);
        log.info("API de integración creada exitosamente con ID: {}", saved.getId());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<IntegrationApis> getAllIntegrationApis() {
        return integrationApisRepository.findAll();
    }

    @Transactional(readOnly = true)
    public IntegrationApis getIntegrationApiById(Long id) {
        return integrationApisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API de integración no encontrada con ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<IntegrationApis> getIntegrationApisByExecutionMode(ExecutionMode mode) {
        return integrationApisRepository.findByExecutionMode(mode);
    }

    @Transactional(readOnly = true)
    public List<IntegrationApis> getActiveScheduledApis() {
        return integrationApisRepository.findByExecutionModeAndActiveTrue(ExecutionMode.SCHEDULED);
    }

    @Transactional
    public IntegrationApis updateIntegrationApi(Long id, IntegrationApiDTO dto) {
        log.info("Actualizando API de integración con ID: {}", id);

        IntegrationApis existing = getIntegrationApiById(id);

        // Actualizar campos básicos
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setExecutionMode(dto.getExecutionMode());
        existing.setScheduleFrequency(dto.getScheduleFrequency());
        existing.setCronExpression(dto.getCronExpression());
        existing.setActive(dto.getActive());

        // Actualizar endpoints
        if (dto.getInputEndpoint() != null) {
            if (existing.getInputEndpoint() != null) {
                updateEndpointConfig(existing.getInputEndpoint(), dto.getInputEndpoint());
            } else {
                existing.setInputEndpoint(createEndpointConfig(dto.getInputEndpoint()));
            }
        }

        if (dto.getOutputEndpoint() != null) {
            if (existing.getOutputEndpoint() != null) {
                updateEndpointConfig(existing.getOutputEndpoint(), dto.getOutputEndpoint());
            } else {
                existing.setOutputEndpoint(createEndpointConfig(dto.getOutputEndpoint()));
            }
        }

        // Recalcular próxima ejecución si es programada
        if (existing.getExecutionMode() == ExecutionMode.SCHEDULED) {
            calculateNextExecutionTime(existing);
        }

        IntegrationApis updated = integrationApisRepository.save(existing);
        log.info("API de integración actualizada exitosamente con ID: {}", id);

        return updated;
    }

    @Transactional
    public void deleteIntegrationApi(Long id) {
        log.info("Eliminando API de integración con ID: {}", id);

        if (!integrationApisRepository.existsById(id)) {
            throw new ResourceNotFoundException("API de integración no encontrada con ID: " + id);
        }

        integrationApisRepository.deleteById(id);
        log.info("API de integración eliminada exitosamente con ID: {}", id);
    }

    @Transactional
    public void toggleIntegrationApiStatus(Long id, Boolean active) {
        IntegrationApis api = getIntegrationApiById(id);
        api.setActive(active);

        if (active && api.getExecutionMode() == ExecutionMode.SCHEDULED) {
            calculateNextExecutionTime(api);
        }

        integrationApisRepository.save(api);
        log.info("Estado de API de integración {} cambiado a: {}", id, active);
    }

    @Transactional(readOnly = true)
    public List<ExecutionHistory> getExecutionHistory(Long integrationApiId) {
        return executionHistoryRepository.findByIntegrationApiIdOrderByStartTimeDesc(integrationApiId);
    }

    private EndpointConfig createEndpointConfig(EndpointConfigDTO dto) {
        EndpointConfig config = new EndpointConfig();
        updateEndpointConfig(config, dto);
        return config;
    }

    private void updateEndpointConfig(EndpointConfig config, EndpointConfigDTO dto) {
        config.setUrl(dto.getUrl());
        config.setMethod(HttpMethod.valueOf(dto.getMethod()));
        config.setAuthType(dto.getAuthType());
        config.setTimeout(dto.getTimeout() != null ? dto.getTimeout() : 30000);
        config.setRetryCount(dto.getRetryCount() != null ? dto.getRetryCount() : 3);
        config.setTypeExample(dto.getTypeExample());
        config.setExample(dto.getExample());

        if (dto.getCustomHeaders() != null) {
            config.setCustomHeaders(dto.getCustomHeaders());
        }

        // Configurar autenticación
        if (dto.getAuthType() != null && dto.getAuthConfig() != null) {
            if (config.getAuthConfig() == null) {
                config.setAuthConfig(new AuthConfig());
            }
            updateAuthConfig(config.getAuthConfig(), dto.getAuthConfig());
        }
    }

    private void updateAuthConfig(AuthConfig authConfig, AuthConfigDTO dto) {
        authConfig.setUsername(dto.getUsername());
        authConfig.setPassword(dto.getPassword());
        authConfig.setToken(dto.getToken());
        authConfig.setApiKeyName(dto.getApiKeyName());
        authConfig.setApiKeyValue(dto.getApiKeyValue());
        authConfig.setClientId(dto.getClientId());
        authConfig.setClientSecret(dto.getClientSecret());
        authConfig.setTokenUrl(dto.getTokenUrl());
        authConfig.setScope(dto.getScope());
        authConfig.setValidationEndpoint(dto.getValidationEndpoint());
        authConfig.setAdditionalConfig(dto.getAdditionalConfig());
    }

    private void mapDtoToEntity(IntegrationApiDTO dto, IntegrationApis entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setExecutionMode(dto.getExecutionMode());
        entity.setScheduleFrequency(dto.getScheduleFrequency());
        entity.setCronExpression(dto.getCronExpression());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
    }

    private void validateIntegrationApi(IntegrationApiDTO dto) {
        if (dto.getExecutionMode() == ExecutionMode.SCHEDULED) {
            if (dto.getScheduleFrequency() == null) {
                throw new IllegalArgumentException("La frecuencia de programación es requerida para ejecución programada");
            }

            if (dto.getScheduleFrequency() == ScheduleFrequency.CUSTOM &&
                    (dto.getCronExpression() == null || dto.getCronExpression().isEmpty())) {
                throw new IllegalArgumentException("La expresión CRON es requerida para frecuencia personalizada");
            }
        }
    }

    private void calculateNextExecutionTime(IntegrationApis api) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextExecution = null;

        if (api.getScheduleFrequency() == ScheduleFrequency.CUSTOM) {
            try {
                CronExpression cronExpression = CronExpression.parse(api.getCronExpression());
                nextExecution = cronExpression.next(now);
            } catch (Exception e) {
                log.error("Error al parsear expresión CRON: {}", api.getCronExpression(), e);
                throw new IllegalArgumentException("Expresión CRON inválida: " + api.getCronExpression());
            }
        } else {
            nextExecution = calculateNextExecutionByFrequency(api.getScheduleFrequency(), now);
        }

        api.setNextExecutionTime(nextExecution);
    }

    private LocalDateTime calculateNextExecutionByFrequency(ScheduleFrequency frequency, LocalDateTime from) {
        switch (frequency) {
            case MINUTELY:
                return from.plusMinutes(1);
            case HOURLY:
                return from.plusHours(1);
            case DAILY:
                return from.plusDays(1);
            case WEEKLY:
                return from.plusWeeks(1);
            case MONTHLY:
                return from.plusMonths(1);
            default:
                return null;
        }
    }
}
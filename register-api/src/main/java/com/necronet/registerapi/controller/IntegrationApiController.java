package com.necronet.registerapi.controller;

import com.necronet.registerapi.dto.IntegrationApiDTO;
import com.necronet.registerapi.entity.ExecutionHistory;
import com.necronet.registerapi.entity.IntegrationApis;
import com.necronet.registerapi.entity.enums.ExecutionMode;
import com.necronet.registerapi.service.IntegrationApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integration-apis")
@RequiredArgsConstructor
@Tag(name = "Integration APIs", description = "API para gestión de integraciones")
@CrossOrigin(origins = "*")
public class IntegrationApiController {

    private final IntegrationApiService integrationApiService;

    @PostMapping
    @Operation(summary = "Crear una nueva API de integración")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "API de integración creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<IntegrationApis> createIntegrationApi(
            @Valid @RequestBody IntegrationApiDTO integrationApiDTO) {
        IntegrationApis created = integrationApiService.createIntegrationApi(integrationApiDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Obtener todas las APIs de integración")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de APIs obtenida exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<IntegrationApis>> getAllIntegrationApis() {
        List<IntegrationApis> apis = integrationApiService.getAllIntegrationApis();
        return ResponseEntity.ok(apis);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una API de integración por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "API encontrada exitosamente"),
            @ApiResponse(responseCode = "404", description = "API no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<IntegrationApis> getIntegrationApiById(
            @Parameter(description = "ID de la API de integración")
            @PathVariable Long id) {
        IntegrationApis api = integrationApiService.getIntegrationApiById(id);
        return ResponseEntity.ok(api);
    }

    @GetMapping("/filter")
    @Operation(summary = "Filtrar APIs de integración por modo de ejecución")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "APIs filtradas exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<IntegrationApis>> getIntegrationApisByMode(
            @Parameter(description = "Modo de ejecución (ORCHESTRATED o SCHEDULED)")
            @RequestParam ExecutionMode mode) {
        List<IntegrationApis> apis = integrationApiService.getIntegrationApisByExecutionMode(mode);
        return ResponseEntity.ok(apis);
    }

    @GetMapping("/active/scheduled")
    @Operation(summary = "Obtener todas las APIs programadas activas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "APIs programadas activas obtenidas exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<IntegrationApis>> getActiveScheduledApis() {
        List<IntegrationApis> apis = integrationApiService.getActiveScheduledApis();
        return ResponseEntity.ok(apis);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una API de integración")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "API actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "API no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<IntegrationApis> updateIntegrationApi(
            @Parameter(description = "ID de la API de integración")
            @PathVariable Long id,
            @Valid @RequestBody IntegrationApiDTO integrationApiDTO) {
        IntegrationApis updated = integrationApiService.updateIntegrationApi(id, integrationApiDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una API de integración")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "API eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "API no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deleteIntegrationApi(
            @Parameter(description = "ID de la API de integración")
            @PathVariable Long id) {
        integrationApiService.deleteIntegrationApi(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activar/Desactivar una API de integración")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "API no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Map<String, Object>> toggleApiStatus(
            @Parameter(description = "ID de la API de integración")
            @PathVariable Long id,
            @Parameter(description = "Estado activo/inactivo")
            @RequestParam Boolean active) {

        integrationApiService.toggleIntegrationApiStatus(id, active);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("active", active);
        response.put("message", active ? "API activada exitosamente" : "API desactivada exitosamente");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/execution-history")
    @Operation(summary = "Obtener historial de ejecuciones de una API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "API no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<ExecutionHistory>> getExecutionHistory(
            @Parameter(description = "ID de la API de integración")
            @PathVariable Long id) {
        List<ExecutionHistory> history = integrationApiService.getExecutionHistory(id);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Ejecutar manualmente una API de integración")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejecución iniciada exitosamente"),
            @ApiResponse(responseCode = "404", description = "API no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Map<String, Object>> executeManually(
            @Parameter(description = "ID de la API de integración")
            @PathVariable Long id) {

        // Verificar que existe
        IntegrationApis api = integrationApiService.getIntegrationApiById(id);

        // Aquí implementarías la lógica de ejecución manual
        // Por ahora solo retornamos un mensaje

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("status", "EXECUTION_STARTED");
        response.put("message", "Ejecución manual iniciada para API: " + api.getName());
        response.put("timestamp", java.time.LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}
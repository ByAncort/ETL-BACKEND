package com.necronet.apiregisterms.controller;

import com.necronet.apiregisterms.dto.ApiRegisterRequest;
import com.necronet.apiregisterms.dto.ApiResponse;
import com.necronet.apiregisterms.dto.ApiUpdateRequest;
import com.necronet.apiregisterms.entity.Apis;
import com.necronet.apiregisterms.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class ApiController {

    private final ApiService apiService;

    @PostMapping
    public ResponseEntity<ApiResponse> registerApi(@RequestBody ApiRegisterRequest request) {
        Apis savedApi = apiService.registerApi(request);
        return ResponseEntity.ok(apiService.toResponse(savedApi));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getApi(@PathVariable Long id) {
        Apis api = apiService.executeRequest(id);
        return api != null 
                ? ResponseEntity.ok(apiService.toResponse(api))
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/auth-api")
    public ResponseEntity<ApiResponse> getAuthApi(@PathVariable Long id) {
        ApiResponse authApiResponse = apiService.getAuthApiResponse(id);
        return authApiResponse != null 
                ? ResponseEntity.ok(authApiResponse)
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateApi(@PathVariable Long id, @RequestBody ApiUpdateRequest request) {
        Apis updatedApi = apiService.updateApi(id, request);
        return updatedApi != null 
                ? ResponseEntity.ok(apiService.toResponse(updatedApi))
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/auth-api")
    public ResponseEntity<ApiResponse> updateAuthApi(@PathVariable Long id, @RequestBody ApiUpdateRequest request) {
        ApiResponse authApiResponse = apiService.updateAuthApi(id, request);
        return authApiResponse != null 
                ? ResponseEntity.ok(authApiResponse)
                : ResponseEntity.notFound().build();
    }
}
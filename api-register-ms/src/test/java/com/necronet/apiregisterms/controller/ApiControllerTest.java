package com.necronet.apiregisterms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.necronet.apiregisterms.dto.ApiRegisterRequest;
import com.necronet.apiregisterms.dto.ApiResponse;
import com.necronet.apiregisterms.dto.ApiUpdateRequest;
import com.necronet.apiregisterms.dto.TestRequest;
import com.necronet.apiregisterms.dto.TestResponse;
import com.necronet.apiregisterms.entity.Apis;
import com.necronet.apiregisterms.entity.AuthType;
import com.necronet.apiregisterms.service.ApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiService apiService;

    private ObjectMapper objectMapper;

    private ApiResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = ApiResponse.builder()
                .id(1L)
                .method("POST")
                .url("http://test.com/api")
                .description("test api")
                .pathParams("/{id}")
                .queryParams("?page=1")
                .body("{\"key\":\"value\"}")
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .authType(AuthType.BEARER)
                .authHeader("Authorization")
                .authValue("test-token")
                .authApiId(2L)
                .authApiUrl("http://auth.com/token")
                .build();
    }

    @Test
    void registerApi_shouldReturn201() throws Exception {
        ApiRegisterRequest request = ApiRegisterRequest.builder()
                .method("POST")
                .url("http://test.com/api")
                .description("test api")
                .build();

        given(apiService.registerApi(any(ApiRegisterRequest.class))).willReturn(new Apis());
        given(apiService.toResponse(any(Apis.class))).willReturn(sampleResponse);

        mockMvc.perform(post("/api-registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.url").value("http://test.com/api"));
    }

    @Test
    void getApi_shouldReturnApiWhenFound() throws Exception {
        given(apiService.executeRequest(1L)).willReturn(new Apis());
        given(apiService.toResponse(any(Apis.class))).willReturn(sampleResponse);

        mockMvc.perform(get("/api-registry/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.url").value("http://test.com/api"));
    }

    @Test
    void getApi_shouldReturn404WhenNotFound() throws Exception {
        given(apiService.executeRequest(1L)).willReturn(null);

        mockMvc.perform(get("/api-registry/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getApiList_shouldReturnList() throws Exception {
        ApiResponse second = ApiResponse.builder().id(2L).method("GET").url("http://test.com/other").build();
        given(apiService.getListApis()).willReturn(List.of(new Apis(), new Apis()));
        given(apiService.toResponse(any(Apis.class))).willReturn(sampleResponse, second);

        mockMvc.perform(get("/api-registry/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void testApi_shouldReturn200WhenSuccess() throws Exception {
        TestResponse testResponse = TestResponse.builder()
                .statusCode(200)
                .body("{\"result\":\"ok\"}")
                .responseTimeMs(150L)
                .timestamp(LocalDateTime.now())
                .build();

        given(apiService.testApi(eq(1L), any(TestRequest.class))).willReturn(testResponse);

        mockMvc.perform(post("/api-registry/1/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pathParams\":\"/123\",\"queryParams\":\"?filter=active\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.body").value("{\"result\":\"ok\"}"));
    }

    @Test
    void testApi_shouldReturn404WhenApiNotFound() throws Exception {
        TestResponse testResponse = TestResponse.builder()
                .statusCode(404)
                .error("API not found")
                .timestamp(LocalDateTime.now())
                .build();

        given(apiService.testApi(eq(1L), any(TestRequest.class))).willReturn(testResponse);

        mockMvc.perform(post("/api-registry/1/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pathParams\":\"/123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.error").value("API not found"));
    }

    @Test
    void testApi_shouldAllowNullBody() throws Exception {
        TestResponse testResponse = TestResponse.builder()
                .statusCode(200)
                .body(null)
                .responseTimeMs(50L)
                .timestamp(LocalDateTime.now())
                .build();

        given(apiService.testApi(eq(1L), org.mockito.ArgumentMatchers.isNull())).willReturn(testResponse);

        mockMvc.perform(post("/api-registry/1/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    void updateApi_shouldReturnUpdated() throws Exception {
        ApiUpdateRequest updateRequest = ApiUpdateRequest.builder()
                .method("PUT")
                .url("http://test.com/updated")
                .description("updated")
                .build();

        given(apiService.updateApi(eq(1L), any(ApiUpdateRequest.class))).willReturn(new Apis());
        given(apiService.toResponse(any(Apis.class))).willReturn(sampleResponse);

        mockMvc.perform(put("/api-registry/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.method").value("POST"));
    }

    @Test
    void updateApi_shouldReturn404WhenNotFound() throws Exception {
        given(apiService.updateApi(eq(1L), any(ApiUpdateRequest.class))).willReturn(null);

        mockMvc.perform(put("/api-registry/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApiUpdateRequest.builder().build())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAuthApi_shouldReturnAuthApiWhenFound() throws Exception {
        ApiResponse authResponse = ApiResponse.builder()
                .id(2L)
                .method("POST")
                .url("http://auth.com/token")
                .build();

        given(apiService.getAuthApiResponse(1L)).willReturn(authResponse);

        mockMvc.perform(get("/api-registry/1/auth-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.url").value("http://auth.com/token"));
    }

    @Test
    void getAuthApi_shouldReturn404WhenNoAuthApi() throws Exception {
        given(apiService.getAuthApiResponse(1L)).willReturn(null);

        mockMvc.perform(get("/api-registry/1/auth-api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAuthApi_shouldReturnUpdated() throws Exception {
        ApiUpdateRequest updateRequest = ApiUpdateRequest.builder()
                .method("POST")
                .url("http://auth.com/updated-token")
                .description("updated auth api")
                .build();

        ApiResponse updatedAuth = ApiResponse.builder()
                .method("POST")
                .url("http://auth.com/updated-token")
                .description("updated auth api")
                .build();

        given(apiService.updateAuthApi(eq(1L), any(ApiUpdateRequest.class))).willReturn(updatedAuth);

        mockMvc.perform(put("/api-registry/1/auth-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://auth.com/updated-token"));
    }

    @Test
    void updateAuthApi_shouldReturn404WhenNoAuthApi() throws Exception {
        given(apiService.updateAuthApi(eq(1L), any(ApiUpdateRequest.class))).willReturn(null);

        mockMvc.perform(put("/api-registry/1/auth-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApiUpdateRequest.builder().build())))
                .andExpect(status().isNotFound());
    }
}

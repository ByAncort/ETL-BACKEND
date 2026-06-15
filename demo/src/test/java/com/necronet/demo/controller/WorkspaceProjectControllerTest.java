package com.necronet.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.necronet.demo.dto.ProjectCreateRequest;
import com.necronet.demo.dto.ProjectResponse;
import com.necronet.demo.service.WorkspaceProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspaceProjectController.class)
class WorkspaceProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkspaceProjectService service;

    @Test
    void create_shouldReturnCreated() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setProjectCode("PRJ-001");
        request.setProjectTitle("Test Project");

        ProjectResponse response = mock(ProjectResponse.class);
        given(response.getId()).willReturn(1L);
        given(response.getProjectCode()).willReturn("PRJ-001");
        given(response.getProjectTitle()).willReturn("Test Project");

        given(service.create(any(ProjectCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/workspace-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.projectCode").value("PRJ-001"));
    }

    @Test
    void listAll_shouldReturnList() throws Exception {
        ProjectResponse r1 = mock(ProjectResponse.class);
        given(r1.getProjectCode()).willReturn("PRJ-001");
        given(r1.getProjectTitle()).willReturn("Project Alpha");

        ProjectResponse r2 = mock(ProjectResponse.class);
        given(r2.getProjectCode()).willReturn("PRJ-002");
        given(r2.getProjectTitle()).willReturn("Project Beta");

        given(service.findAll()).willReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/workspace-projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].projectCode").value("PRJ-001"))
                .andExpect(jsonPath("$[1].projectCode").value("PRJ-002"));
    }

    @Test
    void findByCode_whenExists_shouldReturnProject() throws Exception {
        ProjectResponse response = mock(ProjectResponse.class);
        given(response.getProjectCode()).willReturn("PRJ-001");
        given(response.getProjectTitle()).willReturn("Test Project");

        given(service.findByCode("PRJ-001")).willReturn(response);

        mockMvc.perform(get("/api/workspace-projects/PRJ-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCode").value("PRJ-001"));
    }

    @Test
    void findByCode_whenNotExists_shouldReturnNotFound() throws Exception {
        given(service.findByCode("NONEXISTENT")).willReturn(null);

        mockMvc.perform(get("/api/workspace-projects/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }
}

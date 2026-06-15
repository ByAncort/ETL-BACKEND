package com.necronet.userregistryms.controller;

import com.necronet.userregistryms.dto.RoleRequest;
import com.necronet.userregistryms.dto.RoleResponse;
import com.necronet.userregistryms.repository.RoleRepository;
import com.necronet.userregistryms.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoleService roleService;

    @MockBean
    private RoleRepository roleRepository;

    private RoleResponse sampleRoleResponse(Long id) {
        RoleResponse r = new RoleResponse();
        r.setId(id);
        r.setName("ROLE_TEST");
        r.setDescription("Test role");
        r.setLevelRole(5L);
        r.setIsSystem(false);
        return r;
    }

    private RoleRequest validRoleRequest() {
        RoleRequest r = new RoleRequest();
        r.setName("ROLE_TEST");
        r.setDescription("Test role");
        r.setLevelRole(5L);
        r.setIsSystem(false);
        return r;
    }

    @Test
    void createRole_shouldReturn201() throws Exception {
        RoleResponse resp = sampleRoleResponse(1L);
        given(roleService.createRole(any(RoleRequest.class), anyString())).willReturn(resp);

        mockMvc.perform(post("/api/users/roles")
                        .header("X-User-Name", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRoleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ROLE_TEST"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRole_shouldReturn400_whenInvalid() throws Exception {
        RoleRequest badReq = new RoleRequest();

        mockMvc.perform(post("/api/users/roles")
                        .header("X-User-Name", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRoleById_shouldReturn200() throws Exception {
        RoleResponse resp = sampleRoleResponse(1L);
        given(roleService.getRoleById(1L)).willReturn(resp);

        mockMvc.perform(get("/api/users/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ROLE_TEST"));
    }

    @Test
    void getRoleById_shouldReturn404_whenNotFound() throws Exception {
        given(roleService.getRoleById(99L))
                .willThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Role not found with id: 99"));

        mockMvc.perform(get("/api/users/roles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllRoles_shouldReturn200() throws Exception {
        given(roleService.getAllRoles()).willReturn(List.of(sampleRoleResponse(1L), sampleRoleResponse(2L)));

        mockMvc.perform(get("/api/users/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));
    }

    @Test
    void getRolesByUsername_shouldReturn200() throws Exception {
        given(roleService.getRolesByUsername("johndoe")).willReturn(List.of(sampleRoleResponse(1L)));

        mockMvc.perform(get("/api/users/roles/user/johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void updateRole_shouldReturn200() throws Exception {
        RoleResponse resp = sampleRoleResponse(1L);
        given(roleService.updateRole(anyLong(), any(RoleRequest.class), anyString())).willReturn(resp);

        mockMvc.perform(put("/api/users/roles/1")
                        .header("X-User-Name", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRoleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ROLE_TEST"));
    }

    @Test
    void updateRole_shouldReturn400_whenInvalid() throws Exception {
        RoleRequest badReq = new RoleRequest();

        mockMvc.perform(put("/api/users/roles/1")
                        .header("X-User-Name", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteRole_shouldReturn204() throws Exception {
        willDoNothing().given(roleService).deleteRole(1L, "admin");

        mockMvc.perform(delete("/api/users/roles/1")
                        .header("X-User-Name", "admin"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRole_shouldReturn403_whenSystemRole() throws Exception {
        willThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Cannot delete system role"))
                .given(roleService).deleteRole(1L, "admin");

        mockMvc.perform(delete("/api/users/roles/1")
                        .header("X-User-Name", "admin"))
                .andExpect(status().isForbidden());
    }
}

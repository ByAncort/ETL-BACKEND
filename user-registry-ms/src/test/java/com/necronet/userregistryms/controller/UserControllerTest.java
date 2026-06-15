package com.necronet.userregistryms.controller;

import com.necronet.userregistryms.dto.ForgotPasswordRequest;
import com.necronet.userregistryms.dto.ResetPasswordRequest;
import com.necronet.userregistryms.dto.UserRequest;
import com.necronet.userregistryms.dto.UserResponse;
import com.necronet.userregistryms.entity.UserStatus;
import com.necronet.userregistryms.repository.RoleRepository;
import com.necronet.userregistryms.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleRepository roleRepository;

    private UserResponse sampleUserResponse(Long id) {
        UserResponse r = new UserResponse();
        r.setId(id);
        r.setUsername("johndoe");
        r.setEmail("john@example.com");
        r.setFirstName("John");
        r.setLastName("Doe");
        r.setStatus(UserStatus.pending_verification);
        r.setRoles(Set.of("ROLE_GUEST"));
        return r;
    }

    private UserRequest validUserRequest() {
        UserRequest r = new UserRequest();
        r.setUsername("johndoe");
        r.setEmail("john@example.com");
        r.setPassword("secret123");
        r.setFirstName("John");
        r.setLastName("Doe");
        return r;
    }

    private UserRequest invalidUserRequest() {
        UserRequest r = new UserRequest();
        r.setUsername("");
        r.setEmail("not-an-email");
        r.setPassword("");
        r.setFirstName("");
        r.setLastName("");
        return r;
    }

    // --- createUser ---

    @Test
    void createUser_shouldReturn201() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        given(userService.createUser(any(UserRequest.class))).willReturn(resp);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createUser_shouldReturn400_whenInvalid() throws Exception {
        given(userService.createUser(any(UserRequest.class))).willThrow(new RuntimeException("service error"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserRequest())))
                .andExpect(status().isBadRequest());
    }

    // --- getUserById ---

    @Test
    void getUserById_shouldReturn200_whenAdmin() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        given(userService.getUserById(1L)).willReturn(resp);

        mockMvc.perform(get("/api/users/1")
                        .header("X-User-Roles", "ROLE_ADMIN,ROLE_USER")
                        .header("X-User-Name", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void getUserById_shouldReturn200_whenOwner() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        resp.setUsername("owner");
        given(userService.getUserById(1L)).willReturn(resp);

        mockMvc.perform(get("/api/users/1")
                        .header("X-User-Roles", "ROLE_USER")
                        .header("X-User-Name", "owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("owner"));
    }

    @Test
    void getUserById_shouldReturn403_whenNotOwnerNotAdmin() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        resp.setUsername("owner");
        given(userService.getUserById(1L)).willReturn(resp);

        mockMvc.perform(get("/api/users/1")
                        .header("X-User-Roles", "ROLE_USER")
                        .header("X-User-Name", "intruder"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_shouldReturn403_whenNoRolesHeader() throws Exception {
        given(userService.getUserById(1L)).willReturn(sampleUserResponse(1L));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isForbidden());
    }

    // --- getUserByUsername ---

    @Test
    void getUserByUsername_shouldReturn200_whenAdmin() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        given(userService.getUserByUsername("johndoe")).willReturn(resp);

        mockMvc.perform(get("/api/users/username/johndoe")
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .header("X-User-Name", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void getUserByUsername_shouldReturn200_whenOwner() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        resp.setUsername("johndoe");
        given(userService.getUserByUsername("johndoe")).willReturn(resp);

        mockMvc.perform(get("/api/users/username/johndoe")
                        .header("X-User-Roles", "ROLE_USER")
                        .header("X-User-Name", "johndoe"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserByUsername_shouldReturn403_whenNotOwnerNotAdmin() throws Exception {
        given(userService.getUserByUsername("johndoe")).willReturn(sampleUserResponse(1L));

        mockMvc.perform(get("/api/users/username/johndoe")
                        .header("X-User-Roles", "ROLE_USER")
                        .header("X-User-Name", "intruder"))
                .andExpect(status().isForbidden());
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_shouldReturn200_whenAdmin() throws Exception {
        given(userService.getAllUsers()).willReturn(List.of(sampleUserResponse(1L)));

        mockMvc.perform(get("/api/users")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void getAllUsers_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_shouldReturn403_whenNoRoles() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    // --- updateUser ---

    @Test
    void updateUser_shouldReturn200_whenAdmin() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        given(userService.updateUser(anyLong(), any(UserRequest.class))).willReturn(resp);

        mockMvc.perform(put("/api/users/1")
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .header("X-User-Name", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void updateUser_shouldReturn200_whenOwner() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        resp.setUsername("owner");
        given(userService.getUserById(1L)).willReturn(resp);
        given(userService.updateUser(anyLong(), any(UserRequest.class))).willReturn(resp);

        mockMvc.perform(put("/api/users/1")
                        .header("X-User-Roles", "ROLE_USER")
                        .header("X-User-Name", "owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void updateUser_shouldReturn403_whenNotOwnerNotAdmin() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        resp.setUsername("owner");
        given(userService.getUserById(1L)).willReturn(resp);

        mockMvc.perform(put("/api/users/1")
                        .header("X-User-Roles", "ROLE_USER")
                        .header("X-User-Name", "intruder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_shouldReturn400_whenInvalid() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        given(userService.getUserById(1L)).willReturn(resp);
        given(userService.updateUser(anyLong(), any(UserRequest.class))).willReturn(resp);

        mockMvc.perform(put("/api/users/1")
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .header("X-User-Name", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserRequest())))
                .andExpect(status().isBadRequest());
    }

    // --- deleteUser ---

    @Test
    void deleteUser_shouldReturn204_whenModerator() throws Exception {
        UserResponse resp = sampleUserResponse(5L);
        resp.setUsername("targetuser");
        given(userService.getUserById(5L)).willReturn(resp);
        willDoNothing().given(userService).deleteUser(5L);

        mockMvc.perform(delete("/api/users/5")
                        .header("X-User-Roles", "ROLE_MODERATOR")
                        .header("X-User-Name", "moderator"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_shouldReturn403_whenNotModerator() throws Exception {
        mockMvc.perform(delete("/api/users/1")
                        .header("X-User-Roles", "ROLE_USER")
                        .header("X-User-Name", "user"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_shouldReturn403_whenSelfDeletion() throws Exception {
        UserResponse resp = sampleUserResponse(1L);
        resp.setUsername("moderator");
        given(userService.getUserById(1L)).willReturn(resp);

        mockMvc.perform(delete("/api/users/1")
                        .header("X-User-Roles", "ROLE_MODERATOR")
                        .header("X-User-Name", "moderator"))
                .andExpect(status().isForbidden());
    }

    // --- verifyEmail ---

    @Test
    void verifyEmail_shouldReturn200() throws Exception {
        willDoNothing().given(userService).verifyEmail(1L);
        willDoNothing().given(userService).activateUser(1L);

        mockMvc.perform(post("/api/users/1/verify-email"))
                .andExpect(status().isOk());
    }

    // --- activateUser ---

    @Test
    void activateUser_shouldReturn200_whenAdmin() throws Exception {
        willDoNothing().given(userService).activateUser(1L);
        willDoNothing().given(userService).verifyEmail(1L);

        mockMvc.perform(post("/api/users/1/activate")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void activateUser_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/users/1/activate")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    // --- deactivateUser ---

    @Test
    void deactivateUser_shouldReturn204_whenAdmin() throws Exception {
        willDoNothing().given(userService).deactivateUser(1L);

        mockMvc.perform(post("/api/users/1/deactivate")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deactivateUser_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/users/1/deactivate")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    // --- forgotPassword ---

    @Test
    void forgotPassword_shouldReturn200() throws Exception {
        var request = new ForgotPasswordRequest();
        request.setEmail("john@example.com");
        willDoNothing().given(userService).forgotPassword("john@example.com");

        mockMvc.perform(post("/api/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_shouldReturn400_whenInvalidEmail() throws Exception {
        var request = new ForgotPasswordRequest();
        request.setEmail("not-an-email");

        mockMvc.perform(post("/api/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- resetPassword ---

    @Test
    void resetPassword_shouldReturn200() throws Exception {
        var request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newSecret123");
        willDoNothing().given(userService).resetPassword("valid-token", "newSecret123");

        mockMvc.perform(post("/api/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_shouldReturn400_whenTokenMissing() throws Exception {
        var request = new ResetPasswordRequest();
        request.setToken("");
        request.setNewPassword("newSecret123");

        mockMvc.perform(post("/api/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_shouldReturn400_whenPasswordTooShort() throws Exception {
        var request = new ResetPasswordRequest();
        request.setToken("some-token");
        request.setNewPassword("ab");

        mockMvc.perform(post("/api/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

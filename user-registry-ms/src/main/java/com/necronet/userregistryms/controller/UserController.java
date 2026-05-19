package com.necronet.userregistryms.controller;

import com.necronet.userregistryms.dto.ForgotPasswordRequest;
import com.necronet.userregistryms.dto.ResetPasswordRequest;
import com.necronet.userregistryms.dto.UserRequest;
import com.necronet.userregistryms.dto.UserResponse;
import com.necronet.userregistryms.service.UserService;
import com.necronet.userregistryms.validation.OnCreate;
import com.necronet.userregistryms.validation.OnUpdate;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private void checkAdminAccess(String roles) {
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Requires ROLE_ADMIN");
        }
    }

    private void checkModeratorAccess(String roles) {
        if (roles == null || !roles.contains("ROLE_MODERATOR")) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Requires ROLE_MODERATOR");
        }
    }

    private void checkOwnerOrAdminAccess(String roles, String requesterUsername, Long targetId) {
        boolean isAdmin = roles != null && roles.contains("ROLE_ADMIN");
        if (isAdmin) return;

        UserResponse targetUser = userService.getUserById(targetId);
        if (targetUser == null || requesterUsername == null || !targetUser.getUsername().equals(requesterUsername)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You don't have permission to access this resource");
        }
    }

    private void checkOwnerOrAdminAccessByUsername(String roles, String requesterUsername, String targetUsername) {
        boolean isAdmin = roles != null && roles.contains("ROLE_ADMIN");
        if (isAdmin) return;

        if (requesterUsername == null || !requesterUsername.equals(targetUsername)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You don't have permission to access this resource");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@Validated({Default.class, OnCreate.class}) @RequestBody UserRequest userRequest) {
        UserResponse response = userService.createUser(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Name", required = false) String requesterUsername) {
        checkOwnerOrAdminAccess(roles, requesterUsername, id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(
            @PathVariable String username,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Name", required = false) String requesterUsername) {
        checkOwnerOrAdminAccessByUsername(roles, requesterUsername, username);
        UserResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        checkAdminAccess(roles);
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Validated({Default.class, OnUpdate.class}) @RequestBody UserRequest userRequest,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Name", required = false) String requesterUsername) {
        checkOwnerOrAdminAccess(roles, requesterUsername, id);
        UserResponse response = userService.updateUser(id, userRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Name", required = false) String requesterUsername) {
        checkModeratorAccess(roles);

        UserResponse targetUser = userService.getUserById(id);
        if (requesterUsername != null && requesterUsername.equals(targetUser.getUsername())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You cannot delete yourself");
        }

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/verify-email")
    public ResponseEntity<Void> verifyEmail(@PathVariable Long id) {
        // This endpoint might be called by external validation link, or by admin
        // For now, let's allow it as it's an email verification
        userService.verifyEmail(id);
        userService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        checkAdminAccess(roles);
        userService.activateUser(id);
        userService.verifyEmail(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        checkAdminAccess(roles);
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}

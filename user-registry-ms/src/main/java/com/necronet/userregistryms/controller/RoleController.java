package com.necronet.userregistryms.controller;

import com.necronet.userregistryms.dto.RoleRequest;
import com.necronet.userregistryms.dto.RoleResponse;
import com.necronet.userregistryms.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody RoleRequest roleRequest,
            @RequestHeader(value = "X-User-Name", required = false) String requesterUsername) {
        RoleResponse response = roleService.createRole(roleRequest, requesterUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) {
        RoleResponse response = roleService.getRoleById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<RoleResponse>> getRolesByUsername(@PathVariable String username) {
        List<RoleResponse> roles = roleService.getRolesByUsername(username);
        return ResponseEntity.ok(roles);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest roleRequest,
            @RequestHeader(value = "X-User-Name", required = false) String requesterUsername) {
        RoleResponse response = roleService.updateRole(id, roleRequest, requesterUsername);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", required = false) String requesterUsername) {
        roleService.deleteRole(id, requesterUsername);
        return ResponseEntity.noContent().build();
    }
}

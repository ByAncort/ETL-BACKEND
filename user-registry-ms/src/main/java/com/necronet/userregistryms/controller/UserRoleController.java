package com.necronet.userregistryms.controller;

import com.necronet.userregistryms.dto.AssignRoleRequest;
import com.necronet.userregistryms.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserRoleController {

    private final UserRoleService userRoleService;

    @PostMapping("/assign")
    public ResponseEntity<Void> assignRoleToUser(@RequestBody AssignRoleRequest request) {
        userRoleService.assignRoleToUser(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeRoleFromUser(@RequestParam Long userId, @RequestParam Long roleId) {
        userRoleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}

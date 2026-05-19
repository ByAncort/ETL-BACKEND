package com.necronet.userregistryms.service;

import com.necronet.userregistryms.dto.RoleRequest;
import com.necronet.userregistryms.dto.RoleResponse;
import com.necronet.userregistryms.entity.Role;
import com.necronet.userregistryms.entity.User;
import com.necronet.userregistryms.entity.UserRole;
import com.necronet.userregistryms.repository.RoleRepository;
import com.necronet.userregistryms.repository.UserRepository;
import com.necronet.userregistryms.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public RoleResponse createRole(RoleRequest roleRequest, String requesterUsername) {
        checkAdminLevel(requesterUsername, null);

        if (roleRepository.existsByName(roleRequest.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already exists: " + roleRequest.getName());
        }

        Role role = new Role();
        role.setName(roleRequest.getName());
        role.setDescription(roleRequest.getDescription());
        role.setLevelRole(roleRequest.getLevelRole());
        role.setSystem(roleRequest.getIsSystem());

        Role savedRole = roleRepository.save(role);
        log.info("Role created: {}", savedRole.getName());
        return mapToResponse(savedRole);
    }

    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + id));
        return mapToResponse(role);
    }

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RoleResponse> getRolesByUsername(String username) {
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username);
        }

        List<UserRole> userRoles = userRoleRepository.findByUsuarioId(user.getId());
        return userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRolId()).orElse(null))
                .filter(role -> role != null)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest roleRequest, String requesterUsername) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + id));

        if (role.getSystem()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify system role");
        }

        checkAdminLevel(requesterUsername, role);

        role.setName(roleRequest.getName());
        role.setDescription(roleRequest.getDescription());
        role.setLevelRole(roleRequest.getLevelRole());
        role.setSystem(roleRequest.getIsSystem());

        Role updatedRole = roleRepository.save(role);
        log.info("Role updated: {}", updatedRole.getName());
        return mapToResponse(updatedRole);
    }

    @Transactional
    public void deleteRole(Long id, String requesterUsername) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + id));

        if (role.getSystem()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete system role");
        }

        checkAdminLevel(requesterUsername, role);

        if (userRoleRepository.existsByRolId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete role assigned to one or more users");
        }

        roleRepository.deleteById(id);
        log.info("Role deleted with id: {}", id);
    }

    private void checkAdminLevel(String requesterUsername, Role targetRole) {
        if (requesterUsername == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication required");
        }

        User requester = userRepository.findUserByUsername(requesterUsername);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user not found");
        }

        List<UserRole> requesterRoles = userRoleRepository.findByUsuarioId(requester.getId());

        long requesterHighestLevel = requesterRoles.stream()
                .mapToLong(ur -> roleRepository.findById(ur.getRolId())
                        .map(Role::getLevelRole)
                        .orElse(Long.MAX_VALUE))
                .min()
                .orElse(Long.MAX_VALUE);

        if (requesterHighestLevel == Long.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have any roles assigned");
        }

        if (targetRole != null) {
            Long targetLevel = targetRole.getLevelRole();
            if (targetLevel != null && requesterHighestLevel > targetLevel) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You don't have permission to modify role: " + targetRole.getName());
            }
        }
    }

    private RoleResponse mapToResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        response.setLevelRole(role.getLevelRole());
        response.setIsSystem(role.getSystem());
        response.setCreatedAt(role.getCreatedAt());
        return response;
    }
}

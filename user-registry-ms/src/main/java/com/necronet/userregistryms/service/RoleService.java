package com.necronet.userregistryms.service;

import com.necronet.userregistryms.dto.RoleRequest;
import com.necronet.userregistryms.dto.RoleResponse;
import com.necronet.userregistryms.entity.Role;
import com.necronet.userregistryms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional
    public RoleResponse createRole(RoleRequest roleRequest) {
        if (roleRepository.existsByName(roleRequest.getName())) {
            throw new RuntimeException("Role already exists: " + roleRequest.getName());
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
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
        return mapToResponse(role);
    }

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest roleRequest) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));

        if (role.getSystem()) {
            throw new RuntimeException("Cannot modify system role");
        }

        role.setName(roleRequest.getName());
        role.setDescription(roleRequest.getDescription());
        role.setLevelRole(roleRequest.getLevelRole());
        role.setSystem(roleRequest.getIsSystem());

        Role updatedRole = roleRepository.save(role);
        return mapToResponse(updatedRole);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));

        if (role.getSystem()) {
            throw new RuntimeException("Cannot delete system role");
        }

        roleRepository.deleteById(id);
        log.info("Role deleted with id: {}", id);
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

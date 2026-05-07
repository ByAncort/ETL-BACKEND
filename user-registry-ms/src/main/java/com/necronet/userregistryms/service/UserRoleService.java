package com.necronet.userregistryms.service;

import com.necronet.userregistryms.dto.AssignRoleRequest;
import com.necronet.userregistryms.entity.UserRole;
import com.necronet.userregistryms.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    @Transactional
    public void assignRoleToUser(AssignRoleRequest request) {
        if (userRoleRepository.existsByUsuarioIdAndRolId(request.getUserId(), request.getRoleId())) {
            throw new RuntimeException("User already has this role");
        }

        UserRole userRole = new UserRole();
        userRole.setUsuarioId(request.getUserId());
        userRole.setRolId(request.getRoleId());
        userRole.setAssignedBy(request.getAssignedBy());

        userRoleRepository.save(userRole);
        log.info("Role {} assigned to user {}", request.getRoleId(), request.getUserId());
    }

    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        if (!userRoleRepository.existsByUsuarioIdAndRolId(userId, roleId)) {
            throw new RuntimeException("User does not have this role");
        }

        userRoleRepository.deleteByUsuarioIdAndRolId(userId, roleId);
        log.info("Role {} removed from user {}", roleId, userId);
    }
}

package com.necronet.userregistryms.service;

import com.necronet.userregistryms.dto.AssignRoleRequest;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public void assignRoleToUser(AssignRoleRequest request, String requesterUsername) {
        User requester = userRepository.findUserByUsername(requesterUsername);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user not found");
        }

        Role targetRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + request.getRoleId()));

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + request.getUserId()));

        if (requester.getId().equals(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot assign roles to yourself");
        }

        checkHierarchy(requester.getId(), targetRole);

        if (userRoleRepository.existsByUsuarioIdAndRolId(request.getUserId(), request.getRoleId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has this role");
        }

        UserRole userRole = new UserRole();
        userRole.setUsuarioId(request.getUserId());
        userRole.setRolId(request.getRoleId());
        userRole.setAssignedBy(requester.getId());

        userRoleRepository.save(userRole);
        log.info("Role {} (level={}) assigned to user {} by {}", targetRole.getName(), targetRole.getLevelRole(), targetUser.getUsername(), requesterUsername);
    }

    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId, String requesterUsername) {
        User requester = userRepository.findUserByUsername(requesterUsername);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user not found");
        }

        Role targetRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + roleId));

        if (requester.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot remove roles from yourself");
        }

        checkHierarchy(requester.getId(), targetRole);

        if (!userRoleRepository.existsByUsuarioIdAndRolId(userId, roleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not have this role");
        }

        userRoleRepository.deleteByUsuarioIdAndRolId(userId, roleId);
        log.info("Role {} removed from user {} by {}", roleId, userId, requesterUsername);
    }

    private void checkHierarchy(Long requesterId, Role targetRole) {
        List<UserRole> requesterRoles = userRoleRepository.findByUsuarioId(requesterId);

        long requesterHighestLevel = requesterRoles.stream()
                .mapToLong(ur -> roleRepository.findById(ur.getRolId())
                        .map(Role::getLevelRole)
                        .orElse(Long.MAX_VALUE))
                .min()
                .orElse(Long.MAX_VALUE);

        Long targetLevel = targetRole.getLevelRole();
        if (targetLevel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target role has no level configured");
        }

        if (requesterHighestLevel > targetLevel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have permission to manage role: " + targetRole.getName());
        }
    }
}

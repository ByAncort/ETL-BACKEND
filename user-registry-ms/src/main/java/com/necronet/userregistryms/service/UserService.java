package com.necronet.userregistryms.service;

import com.necronet.userregistryms.client.IdentityServiceClient;
import com.necronet.userregistryms.dto.UserRequest;
import com.necronet.userregistryms.dto.UserResponse;
import com.necronet.userregistryms.entity.User;
import com.necronet.userregistryms.entity.UserRole;
import com.necronet.userregistryms.entity.UserStatus;
import com.necronet.userregistryms.repository.UserRepository;
import com.necronet.userregistryms.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final IdentityServiceClient identityServiceClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        if (userRequest.getUsername() == null || userRequest.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username cannot be empty or blank");
        }
        if (userRequest.getEmail() == null || userRequest.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email cannot be empty or blank");
        }
        if (userRequest.getPassword() == null || userRequest.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty or blank");
        }

        if (userRepository.existsByUsername(userRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(userRequest.getPassword()));
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setStatus(UserStatus.pending_verification);

        User savedUser = userRepository.save(user);

UserRole userRole = new UserRole();
        userRole.setUsuarioId(savedUser.getId());
        userRole.setRolId(4L);
        userRole.setAssignedBy(1L);
        userRoleRepository.save(userRole);
        log.info("Role ROLE_GUEST assigned to user {}", savedUser.getUsername());

        identityServiceClient.registerUserInIdentityService(
                userRequest.getUsername(),
                userRequest.getEmail(),
                userRequest.getPassword()
        );

        log.info("User created successfully: {}", savedUser.getUsername());
        return mapToResponse(savedUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToResponse(user);
    }

    public UserResponse getUserByUsername(String username) {

        return mapToResponse(userRepository.findUserByUsername(username));
    }


    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest userRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        boolean emailChanged = !user.getEmail().equals(userRequest.getEmail());

        if (emailChanged) {
            if (userRepository.existsByEmail(userRequest.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(userRequest.getEmail());
            user.setStatus(UserStatus.pending_verification);
            user.setEmailVerifiedAt(null);
            try {
                identityServiceClient.updateEmail(user.getUsername(), userRequest.getEmail());
                identityServiceClient.disableUser(user.getUsername());
            } catch (Exception e) {
                log.error("Error updating email/status in identity-service for user: {}", user.getUsername(), e);
            }
        }

        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());

        if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(userRequest.getPassword()));
            try {
                identityServiceClient.updatePassword(user.getUsername(), userRequest.getPassword());
            } catch (Exception e) {
                log.error("Error al actualizar la contraseña en identity-service para el usuario: {}", user.getUsername(), e);
            }
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }


        List<UserRole> userRoles = userRoleRepository.findByUsuarioId(id);
        if (!userRoles.isEmpty()) {
            userRoleRepository.deleteAll(userRoles);
            log.info("Deleted {} roles for user id: {}", userRoles.size(), id);
        }


        userRepository.deleteById(id);
        log.info("User deleted with id: {}", id);
    }
    @Transactional
    public void verifyEmail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setStatus(UserStatus.active);
        userRepository.save(user);
        try {
            identityServiceClient.enableUser(user.getUsername());
        } catch (Exception e) {
            log.error("Error al habilitar login de Usuario: {} error: {}",user.getUsername(),e.getMessage());
        }
        log.info("Email verified and user enabled for: {}", user.getUsername());
    }

    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setStatus(UserStatus.active);
        userRepository.save(user);
        try {
            identityServiceClient.enableUser(user.getUsername());
        } catch (Exception e) {
            log.error("Error al habilitar login de Usuario: {} error: {}", user.getUsername(), e.getMessage());
        }
        log.info("User activated for: {}", user.getUsername());
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setStatus(UserStatus.inactive);
        userRepository.save(user);
        try {
            identityServiceClient.disableUser(user.getUsername());
        } catch (Exception e) {
            log.error("Error al deshabilitar login de Usuario: {} error: {}", user.getUsername(), e.getMessage());
        }
        log.info("User deactivated for: {}", user.getUsername());
    }

    @Transactional
    public void updateLastLogin(Long id, String ipAddress) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setStatus(user.getStatus());
        response.setEmailVerifiedAt(user.getEmailVerifiedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setLastLoginIp(user.getLastLoginIp());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setCreatedBy(user.getCreatedBy());

        List<UserRole> userRoles = userRoleRepository.findByUsuarioId(user.getId());
        Set<String> roles = new HashSet<>();
        userRoles.forEach(ur -> {
            if (ur.getRolId() == 1L) roles.add("ROLE_ADMIN");
            else if (ur.getRolId() == 2L) roles.add("ROLE_USER");
            else if (ur.getRolId() == 3L) roles.add("ROLE_MODERATOR");
            else if (ur.getRolId() == 4L) roles.add("ROLE_GUEST");
        });
        response.setRoles(roles);

        return response;
    }
}

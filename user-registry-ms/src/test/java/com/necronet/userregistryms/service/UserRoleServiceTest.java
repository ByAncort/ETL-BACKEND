package com.necronet.userregistryms.service;

import com.necronet.userregistryms.dto.AssignRoleRequest;
import com.necronet.userregistryms.entity.Role;
import com.necronet.userregistryms.entity.User;
import com.necronet.userregistryms.entity.UserRole;
import com.necronet.userregistryms.repository.RoleRepository;
import com.necronet.userregistryms.repository.UserRepository;
import com.necronet.userregistryms.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserRoleService userRoleService;

    private User sampleUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private Role sampleRole(Long id, String name, Long levelRole) {
        Role r = new Role();
        r.setId(id);
        r.setName(name);
        r.setDescription("desc");
        r.setLevelRole(levelRole);
        r.setSystem(false);
        return r;
    }

    private UserRole userRole(Long userId, Long roleId) {
        UserRole ur = new UserRole();
        ur.setUsuarioId(userId);
        ur.setRolId(roleId);
        return ur;
    }

    private AssignRoleRequest assignReq(Long userId, Long roleId) {
        AssignRoleRequest r = new AssignRoleRequest();
        r.setUserId(userId);
        r.setRoleId(roleId);
        return r;
    }

    // --- assignRoleToUser ---

    @Test
    void assignRoleToUser_shouldSucceed() {
        User requester = sampleUser(1L, "admin");
        User target = sampleUser(2L, "johndoe");
        Role role = sampleRole(3L, "ROLE_CUSTOM", 5L);

        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(3L)).willReturn(Optional.of(role));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 1L)));
        given(roleRepository.findById(1L)).willReturn(Optional.of(sampleRole(1L, "ROLE_ADMIN", 1L)));
        given(userRoleRepository.existsByUsuarioIdAndRolId(2L, 3L)).willReturn(false);

        userRoleService.assignRoleToUser(assignReq(2L, 3L), "admin");

        then(userRoleRepository).should().save(any(UserRole.class));
    }

    @Test
    void assignRoleToUser_shouldThrow_whenRequesterNotFound() {
        given(userRepository.findUserByUsername("unknown")).willReturn(null);

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(assignReq(2L, 3L), "unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void assignRoleToUser_shouldThrow_whenSelfAssignment() {
        User requester = sampleUser(1L, "admin");
        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(anyLong())).willReturn(Optional.of(sampleRole(3L, "ROLE_CUSTOM", 5L)));
        given(userRepository.findById(1L)).willReturn(Optional.of(requester));

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(assignReq(1L, 3L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void assignRoleToUser_shouldThrow_whenRoleNotFound() {
        User requester = sampleUser(1L, "admin");
        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(assignReq(2L, 99L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void assignRoleToUser_shouldThrow_whenTargetUserNotFound() {
        User requester = sampleUser(1L, "admin");
        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(3L)).willReturn(Optional.of(sampleRole(3L, "ROLE_CUSTOM", 5L)));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(assignReq(99L, 3L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void assignRoleToUser_shouldThrow_whenInsufficientHierarchy() {
        User requester = sampleUser(1L, "admin");
        User target = sampleUser(2L, "johndoe");
        Role role = sampleRole(3L, "ROLE_ADMIN", 1L);

        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(3L)).willReturn(Optional.of(role));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 4L)));
        given(roleRepository.findById(4L)).willReturn(Optional.of(sampleRole(4L, "ROLE_GUEST", 4L)));

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(assignReq(2L, 3L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void assignRoleToUser_shouldThrow_whenTargetRoleNoLevel() {
        User requester = sampleUser(1L, "admin");
        User target = sampleUser(2L, "johndoe");
        Role role = sampleRole(3L, "ROLE_CUSTOM", null);

        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(3L)).willReturn(Optional.of(role));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 1L)));
        given(roleRepository.findById(1L)).willReturn(Optional.of(sampleRole(1L, "ROLE_ADMIN", 1L)));

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(assignReq(2L, 3L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void assignRoleToUser_shouldThrow_whenAlreadyAssigned() {
        User requester = sampleUser(1L, "admin");
        User target = sampleUser(2L, "johndoe");
        Role role = sampleRole(3L, "ROLE_CUSTOM", 5L);

        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(3L)).willReturn(Optional.of(role));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 1L)));
        given(roleRepository.findById(1L)).willReturn(Optional.of(sampleRole(1L, "ROLE_ADMIN", 1L)));
        given(userRoleRepository.existsByUsuarioIdAndRolId(2L, 3L)).willReturn(true);

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(assignReq(2L, 3L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    // --- removeRoleFromUser ---

    @Test
    void removeRoleFromUser_shouldSucceed() {
        User requester = sampleUser(1L, "admin");
        Role role = sampleRole(3L, "ROLE_CUSTOM", 5L);

        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(3L)).willReturn(Optional.of(role));
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 1L)));
        given(roleRepository.findById(1L)).willReturn(Optional.of(sampleRole(1L, "ROLE_ADMIN", 1L)));
        given(userRoleRepository.existsByUsuarioIdAndRolId(2L, 3L)).willReturn(true);

        userRoleService.removeRoleFromUser(2L, 3L, "admin");

        then(userRoleRepository).should().deleteByUsuarioIdAndRolId(2L, 3L);
    }

    @Test
    void removeRoleFromUser_shouldThrow_whenRequesterNotFound() {
        given(userRepository.findUserByUsername("unknown")).willReturn(null);

        assertThatThrownBy(() -> userRoleService.removeRoleFromUser(2L, 3L, "unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void removeRoleFromUser_shouldThrow_whenSelfRemoval() {
        User requester = sampleUser(1L, "admin");
        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(3L)).willReturn(Optional.of(sampleRole(3L, "ROLE_CUSTOM", 5L)));

        assertThatThrownBy(() -> userRoleService.removeRoleFromUser(1L, 3L, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void removeRoleFromUser_shouldThrow_whenRoleNotFound() {
        User requester = sampleUser(1L, "admin");
        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.removeRoleFromUser(2L, 99L, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void removeRoleFromUser_shouldThrow_whenUserDoesNotHaveRole() {
        User requester = sampleUser(1L, "admin");
        Role role = sampleRole(3L, "ROLE_CUSTOM", 5L);

        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(3L)).willReturn(Optional.of(role));
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 1L)));
        given(roleRepository.findById(1L)).willReturn(Optional.of(sampleRole(1L, "ROLE_ADMIN", 1L)));
        given(userRoleRepository.existsByUsuarioIdAndRolId(2L, 3L)).willReturn(false);

        assertThatThrownBy(() -> userRoleService.removeRoleFromUser(2L, 3L, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void removeRoleFromUser_shouldThrow_whenInsufficientHierarchy() {
        User requester = sampleUser(1L, "admin");
        Role role = sampleRole(3L, "ROLE_ADMIN", 1L);

        given(userRepository.findUserByUsername("admin")).willReturn(requester);
        given(roleRepository.findById(3L)).willReturn(Optional.of(role));
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 4L)));
        given(roleRepository.findById(4L)).willReturn(Optional.of(sampleRole(4L, "ROLE_GUEST", 4L)));

        assertThatThrownBy(() -> userRoleService.removeRoleFromUser(2L, 3L, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }
}

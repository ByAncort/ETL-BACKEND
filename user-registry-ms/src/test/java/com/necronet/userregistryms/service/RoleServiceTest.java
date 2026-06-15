package com.necronet.userregistryms.service;

import com.necronet.userregistryms.dto.RoleRequest;
import com.necronet.userregistryms.dto.RoleResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private RoleService roleService;

    private Role sampleRole(Long id, String name, Long levelRole, boolean isSystem) {
        Role r = new Role();
        r.setId(id);
        r.setName(name);
        r.setDescription("desc");
        r.setLevelRole(levelRole);
        r.setSystem(isSystem);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    private RoleRequest roleRequest(String name, Long levelRole) {
        RoleRequest r = new RoleRequest();
        r.setName(name);
        r.setDescription("desc");
        r.setLevelRole(levelRole);
        r.setIsSystem(false);
        return r;
    }

    private User sampleUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private UserRole userRole(Long userId, Long roleId) {
        UserRole ur = new UserRole();
        ur.setUsuarioId(userId);
        ur.setRolId(roleId);
        return ur;
    }

    // --- createRole ---

    @Test
    void createRole_shouldSucceed() {
        User admin = sampleUser(1L, "admin");
        given(userRepository.findUserByUsername("admin")).willReturn(admin);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 1L)));
        given(roleRepository.findById(1L)).willReturn(Optional.of(sampleRole(1L, "ROLE_ADMIN", 1L, true)));
        given(roleRepository.existsByName("ROLE_NEW")).willReturn(false);
        given(roleRepository.save(any(Role.class))).willAnswer(inv -> {
            Role saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        RoleResponse resp = roleService.createRole(roleRequest("ROLE_NEW", 5L), "admin");

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getName()).isEqualTo("ROLE_NEW");
    }

    @Test
    void createRole_shouldThrow_whenRequesterHasNoRoles() {
        User admin = sampleUser(1L, "admin");
        given(userRepository.findUserByUsername("admin")).willReturn(admin);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of());

        assertThatThrownBy(() -> roleService.createRole(roleRequest("ROLE_NEW", 5L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void createRole_shouldThrow_whenNameExists() {
        User admin = sampleUser(1L, "admin");
        given(userRepository.findUserByUsername("admin")).willReturn(admin);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 1L)));
        given(roleRepository.findById(1L)).willReturn(Optional.of(sampleRole(1L, "ROLE_ADMIN", 1L, true)));
        given(roleRepository.existsByName("ROLE_DUP")).willReturn(true);

        assertThatThrownBy(() -> roleService.createRole(roleRequest("ROLE_DUP", 5L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    void createRole_shouldThrow_whenRequesterNull() {
        assertThatThrownBy(() -> roleService.createRole(roleRequest("ROLE_NEW", 5L), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    // --- getRoleById ---

    @Test
    void getRoleById_shouldReturnRole() {
        Role role = sampleRole(1L, "ROLE_ADMIN", 1L, true);
        given(roleRepository.findById(1L)).willReturn(Optional.of(role));

        RoleResponse resp = roleService.getRoleById(1L);

        assertThat(resp.getName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void getRoleById_shouldThrow_whenNotFound() {
        given(roleRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    // --- getAllRoles ---

    @Test
    void getAllRoles_shouldReturnList() {
        given(roleRepository.findAll()).willReturn(List.of(
                sampleRole(1L, "ROLE_ADMIN", 1L, true),
                sampleRole(2L, "ROLE_USER", 2L, true)
        ));

        List<RoleResponse> roles = roleService.getAllRoles();

        assertThat(roles).hasSize(2);
    }

    @Test
    void getAllRoles_shouldReturnEmpty_whenNoRoles() {
        given(roleRepository.findAll()).willReturn(List.of());

        assertThat(roleService.getAllRoles()).isEmpty();
    }

    // --- getRolesByUsername ---

    @Test
    void getRolesByUsername_shouldReturnRoles() {
        User user = sampleUser(1L, "johndoe");
        given(userRepository.findUserByUsername("johndoe")).willReturn(user);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 1L)));
        given(roleRepository.findById(1L)).willReturn(Optional.of(sampleRole(1L, "ROLE_ADMIN", 1L, true)));

        List<RoleResponse> roles = roleService.getRolesByUsername("johndoe");

        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void getRolesByUsername_shouldThrow_whenUserNotFound() {
        given(userRepository.findUserByUsername("unknown")).willReturn(null);

        assertThatThrownBy(() -> roleService.getRolesByUsername("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    // --- updateRole ---

    @Test
    void updateRole_shouldSucceed() {
        Role role = sampleRole(1L, "ROLE_CUSTOM", 5L, false);
        User admin = sampleUser(1L, "admin");
        given(roleRepository.findById(1L)).willReturn(Optional.of(role));
        given(userRepository.findUserByUsername("admin")).willReturn(admin);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 2L)));
        given(roleRepository.findById(2L)).willReturn(Optional.of(sampleRole(2L, "ROLE_USER", 2L, true)));
        given(roleRepository.save(any(Role.class))).willAnswer(inv -> inv.getArgument(0));

        RoleResponse resp = roleService.updateRole(1L, roleRequest("ROLE_UPDATED", 3L), "admin");

        assertThat(resp.getName()).isEqualTo("ROLE_UPDATED");
    }

    @Test
    void updateRole_shouldThrow_whenSystemRole() {
        Role role = sampleRole(1L, "ROLE_ADMIN", 1L, true);
        given(roleRepository.findById(1L)).willReturn(Optional.of(role));

        assertThatThrownBy(() -> roleService.updateRole(1L, roleRequest("ROLE_ADMIN", 1L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void updateRole_shouldThrow_whenNotFound() {
        given(roleRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.updateRole(99L, roleRequest("ROLE_X", 5L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void updateRole_shouldThrow_whenInsufficientLevel() {
        Role role = sampleRole(1L, "ROLE_CUSTOM", 1L, false);
        User admin = sampleUser(1L, "admin");
        given(roleRepository.findById(1L)).willReturn(Optional.of(role));
        given(userRepository.findUserByUsername("admin")).willReturn(admin);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 2L)));
        given(roleRepository.findById(2L)).willReturn(Optional.of(sampleRole(2L, "ROLE_USER", 3L, true)));

        assertThatThrownBy(() -> roleService.updateRole(1L, roleRequest("ROLE_X", 1L), "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    // --- deleteRole ---

    @Test
    void deleteRole_shouldSucceed() {
        Role role = sampleRole(1L, "ROLE_CUSTOM", 5L, false);
        User admin = sampleUser(1L, "admin");
        given(roleRepository.findById(1L)).willReturn(Optional.of(role));
        given(userRepository.findUserByUsername("admin")).willReturn(admin);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 2L)));
        given(roleRepository.findById(2L)).willReturn(Optional.of(sampleRole(2L, "ROLE_USER", 2L, true)));
        given(userRoleRepository.existsByRolId(1L)).willReturn(false);

        roleService.deleteRole(1L, "admin");

        then(roleRepository).should().deleteById(1L);
    }

    @Test
    void deleteRole_shouldThrow_whenSystemRole() {
        Role role = sampleRole(1L, "ROLE_ADMIN", 1L, true);
        given(roleRepository.findById(1L)).willReturn(Optional.of(role));

        assertThatThrownBy(() -> roleService.deleteRole(1L, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteRole_shouldThrow_whenAssignedToUsers() {
        Role role = sampleRole(1L, "ROLE_CUSTOM", 5L, false);
        User admin = sampleUser(1L, "admin");
        given(roleRepository.findById(1L)).willReturn(Optional.of(role));
        given(userRepository.findUserByUsername("admin")).willReturn(admin);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(userRole(1L, 2L)));
        given(roleRepository.findById(2L)).willReturn(Optional.of(sampleRole(2L, "ROLE_USER", 2L, true)));
        given(userRoleRepository.existsByRolId(1L)).willReturn(true);

        assertThatThrownBy(() -> roleService.deleteRole(1L, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    void deleteRole_shouldThrow_whenNotFound() {
        given(roleRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.deleteRole(99L, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }
}

package com.necronet.userregistryms.service;

import com.necronet.userregistryms.client.IdentityServiceClient;
import com.necronet.userregistryms.dto.UserRequest;
import com.necronet.userregistryms.dto.UserResponse;
import com.necronet.userregistryms.entity.PasswordResetToken;
import com.necronet.userregistryms.entity.User;
import com.necronet.userregistryms.entity.UserRole;
import com.necronet.userregistryms.entity.UserStatus;
import com.necronet.userregistryms.repository.PasswordResetTokenRepository;
import com.necronet.userregistryms.repository.UserRepository;
import com.necronet.userregistryms.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private IdentityServiceClient identityServiceClient;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<PasswordResetToken> tokenCaptor;

    private UserRequest validRequest() {
        UserRequest r = new UserRequest();
        r.setUsername("johndoe");
        r.setEmail("john@example.com");
        r.setPassword("secret123");
        r.setFirstName("John");
        r.setLastName("Doe");
        return r;
    }

    private User sampleUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("johndoe");
        u.setEmail("john@example.com");
        u.setPasswordHash("$2a$10$encodedhash");
        u.setFirstName("John");
        u.setLastName("Doe");
        u.setStatus(UserStatus.pending_verification);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }

    // --- createUser ---

    @Test
    void createUser_shouldSucceed() {
        UserRequest req = validRequest();
        given(userRepository.existsByUsername("johndoe")).willReturn(false);
        given(userRepository.existsByEmail("john@example.com")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse resp = userService.createUser(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getUsername()).isEqualTo("johndoe");
        assertThat(resp.getStatus()).isEqualTo(UserStatus.pending_verification);

        then(userRepository).should().existsByUsername("johndoe");
        then(userRepository).should().existsByEmail("john@example.com");
        then(userRepository).should().save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isNotNull();
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("secret123");
        then(userRoleRepository).should().save(any(UserRole.class));
        then(identityServiceClient).should().registerUserInIdentityService("johndoe", "john@example.com", "secret123");
    }

    @Test
    void createUser_shouldThrow_whenDuplicateUsername() {
        UserRequest req = validRequest();
        given(userRepository.existsByUsername("johndoe")).willReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void createUser_shouldThrow_whenDuplicateEmail() {
        UserRequest req = validRequest();
        given(userRepository.existsByUsername("johndoe")).willReturn(false);
        given(userRepository.existsByEmail("john@example.com")).willReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void createUser_shouldThrow_whenNullUsername() {
        UserRequest req = validRequest();
        req.setUsername(null);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username cannot be empty");
    }

    @Test
    void createUser_shouldThrow_whenBlankEmail() {
        UserRequest req = validRequest();
        req.setEmail("   ");

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email cannot be empty");
    }

    @Test
    void createUser_shouldThrow_whenPasswordMissing() {
        UserRequest req = validRequest();
        req.setPassword(null);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password cannot be empty");
    }

    // --- getUserById ---

    @Test
    void getUserById_shouldReturnUser() {
        User user = sampleUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse resp = userService.getUserById(1L);

        assertThat(resp).isNotNull();
        assertThat(resp.getUsername()).isEqualTo("johndoe");
    }

    @Test
    void getUserById_shouldThrow_whenNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    // --- getUserByUsername ---

    @Test
    void getUserByUsername_shouldReturnUser() {
        User user = sampleUser(1L);
        given(userRepository.findUserByUsername("johndoe")).willReturn(user);

        UserResponse resp = userService.getUserByUsername("johndoe");

        assertThat(resp).isNotNull();
        assertThat(resp.getUsername()).isEqualTo("johndoe");
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_shouldReturnList() {
        given(userRepository.findAll()).willReturn(List.of(sampleUser(1L), sampleUser(2L)));

        List<UserResponse> users = userService.getAllUsers();

        assertThat(users).hasSize(2);
    }

    @Test
    void getAllUsers_shouldReturnEmptyList() {
        given(userRepository.findAll()).willReturn(List.of());

        List<UserResponse> users = userService.getAllUsers();

        assertThat(users).isEmpty();
    }

    // --- updateUser ---

    @Test
    void updateUser_shouldSucceed_withoutPasswordChange() {
        User user = sampleUser(1L);
        UserRequest req = validRequest();
        req.setPassword(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserResponse resp = userService.updateUser(1L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getFirstName()).isEqualTo("John");
    }

    @Test
    void updateUser_shouldSucceed_withPasswordChange() {
        User user = sampleUser(1L);
        UserRequest req = validRequest();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserResponse resp = userService.updateUser(1L, req);

        assertThat(resp).isNotNull();
        then(identityServiceClient).should().updatePassword("johndoe", "secret123");
    }

    @Test
    void updateUser_shouldResetEmailVerification_whenEmailChanged() {
        User user = sampleUser(1L);
        user.setEmail("old@example.com");
        user.setStatus(UserStatus.active);
        user.setEmailVerifiedAt(LocalDateTime.now());
        UserRequest req = validRequest();
        req.setEmail("new@example.com");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserResponse resp = userService.updateUser(1L, req);

        assertThat(resp.getEmail()).isEqualTo("new@example.com");
        assertThat(resp.getStatus()).isEqualTo(UserStatus.pending_verification);
        assertThat(resp.getEmailVerifiedAt()).isNull();
        then(identityServiceClient).should().updateEmail("johndoe", "new@example.com");
        then(identityServiceClient).should().disableUser("johndoe");
    }

    @Test
    void updateUser_shouldThrow_whenDuplicateEmailOnChange() {
        User user = sampleUser(1L);
        user.setEmail("old@example.com");
        UserRequest req = validRequest();
        req.setEmail("existing@example.com");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void updateUser_shouldThrow_whenUserNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, validRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    // --- deleteUser ---

    @Test
    void deleteUser_shouldSucceed() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of());

        userService.deleteUser(1L);

        then(userRepository).should().deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrow_whenNotFound() {
        given(userRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    void deleteUser_shouldThrow_whenModerator() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(createUserRole(1L, 3L)));

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete a user with ROLE_MODERATOR");
    }

    @Test
    void deleteUser_shouldDeleteRoles_whenPresent() {
        given(userRepository.existsById(1L)).willReturn(true);
        List<UserRole> roles = List.of(createUserRole(1L, 2L), createUserRole(1L, 4L));
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(roles);

        userService.deleteUser(1L);

        then(userRoleRepository).should().deleteAll(roles);
        then(userRepository).should().deleteById(1L);
    }

    // --- verifyEmail ---

    @Test
    void verifyEmail_shouldSucceed() {
        User user = sampleUser(1L);
        user.setStatus(UserStatus.pending_verification);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        userService.verifyEmail(1L);

        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.active);
        then(identityServiceClient).should().enableUser("johndoe");
    }

    @Test
    void verifyEmail_shouldThrow_whenNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyEmail(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    // --- activateUser ---

    @Test
    void activateUser_shouldSucceed() {
        User user = sampleUser(1L);
        user.setStatus(UserStatus.inactive);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        userService.activateUser(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.active);
        then(identityServiceClient).should().enableUser("johndoe");
    }

    @Test
    void activateUser_shouldThrow_whenNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateUser(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    // --- deactivateUser ---

    @Test
    void deactivateUser_shouldSucceed() {
        User user = sampleUser(1L);
        user.setStatus(UserStatus.active);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        userService.deactivateUser(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.inactive);
        then(identityServiceClient).should().disableUser("johndoe");
    }

    @Test
    void deactivateUser_shouldThrow_whenNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    // --- updateLastLogin ---

    @Test
    void updateLastLogin_shouldUpdate() {
        User user = sampleUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        userService.updateLastLogin(1L, "192.168.1.1");
        then(userRepository).should().save(any(User.class));
    }

    // --- forgotPassword ---

    @Test
    void forgotPassword_shouldCreateTokenAndSendEmail() {
        User user = sampleUser(1L);
        given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(user));
        willDoNothing().given(passwordResetTokenRepository).deleteByUserId(1L);
        given(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(inv -> inv.getArgument(0));

        userService.forgotPassword("john@example.com");

        then(passwordResetTokenRepository).should().deleteByUserId(1L);
        then(passwordResetTokenRepository).should().save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).isNotBlank();
        assertThat(savedToken.getUserId()).isEqualTo(1L);
        assertThat(savedToken.isUsed()).isFalse();
        then(emailService).should().sendPasswordResetEmail(eq("john@example.com"), anyString());
    }

    @Test
    void forgotPassword_shouldThrow_whenEmailNotFound() {
        given(userRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.forgotPassword("unknown@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with email");
    }

    // --- resetPassword ---

    @Test
    void resetPassword_shouldSucceed() {
        User user = sampleUser(1L);
        PasswordResetToken token = createToken("valid-token", 1L, false);
        token.setExpiryDate(LocalDateTime.now().plusHours(1));
        given(passwordResetTokenRepository.findByToken("valid-token")).willReturn(Optional.of(token));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(inv -> inv.getArgument(0));

        userService.resetPassword("valid-token", "newPassword123");

        then(identityServiceClient).should().updatePassword("johndoe", "newPassword123");
        then(userRepository).should().save(any(User.class));
        then(passwordResetTokenRepository).should().save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().isUsed()).isTrue();
    }

    @Test
    void resetPassword_shouldThrow_whenInvalidToken() {
        given(passwordResetTokenRepository.findByToken("bad-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword("bad-token", "newPass123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid or expired token");
    }

    @Test
    void resetPassword_shouldThrow_whenTokenAlreadyUsed() {
        PasswordResetToken token = createToken("used-token", 1L, true);
        given(passwordResetTokenRepository.findByToken("used-token")).willReturn(Optional.of(token));

        assertThatThrownBy(() -> userService.resetPassword("used-token", "newPass123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token has already been used");
    }

    @Test
    void resetPassword_shouldThrow_whenTokenExpired() {
        PasswordResetToken token = createToken("expired-token", 1L, false);
        token.setExpiryDate(LocalDateTime.now().minusHours(2));
        given(passwordResetTokenRepository.findByToken("expired-token")).willReturn(Optional.of(token));

        assertThatThrownBy(() -> userService.resetPassword("expired-token", "newPass123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token has expired");
    }

    // --- helpers ---

    private UserRole createUserRole(Long userId, Long roleId) {
        UserRole ur = new UserRole();
        ur.setUsuarioId(userId);
        ur.setRolId(roleId);
        ur.setAssignedBy(1L);
        return ur;
    }

    private PasswordResetToken createToken(String tokenValue, Long userId, boolean used) {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(1L);
        t.setToken(tokenValue);
        t.setUserId(userId);
        t.setUsed(used);
        t.setExpiryDate(LocalDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(t, "createdAt", LocalDateTime.now());
        return t;
    }
}

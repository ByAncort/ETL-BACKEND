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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceMoreTest {

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

    @Test
    void createUser_shouldThrow_whenNullUsername() {
        UserRequest req = validRequest();
        req.setUsername(null);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username cannot be empty");
    }

    @Test
    void createUser_shouldThrow_whenBlankUsername() {
        UserRequest req = validRequest();
        req.setUsername("   ");

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username cannot be empty");
    }

    @Test
    void createUser_shouldThrow_whenNullEmail() {
        UserRequest req = validRequest();
        req.setEmail(null);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email cannot be empty");
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
    void createUser_shouldThrow_whenNullPassword() {
        UserRequest req = validRequest();
        req.setPassword(null);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password cannot be empty");
    }

    @Test
    void createUser_shouldThrow_whenBlankPassword() {
        UserRequest req = validRequest();
        req.setPassword("   ");

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password cannot be empty");
    }

    @Test
    void deleteUser_shouldThrow_whenModerator() {
        given(userRepository.existsById(1L)).willReturn(true);
        UserRole moderatorRole = new UserRole();
        moderatorRole.setUsuarioId(1L);
        moderatorRole.setRolId(3L);
        moderatorRole.setAssignedBy(1L);
        given(userRoleRepository.findByUsuarioId(1L)).willReturn(List.of(moderatorRole));

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete a user with ROLE_MODERATOR");
    }

    @Test
    void deleteUser_shouldThrow_whenNotFound() {
        given(userRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    void forgotPassword_shouldThrow_whenEmailNotFound() {
        given(userRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.forgotPassword("unknown@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with email");
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
        PasswordResetToken token = new PasswordResetToken();
        token.setId(1L);
        token.setToken("used-token");
        token.setUserId(1L);
        token.setUsed(true);
        token.setExpiryDate(LocalDateTime.now().plusHours(1));
        given(passwordResetTokenRepository.findByToken("used-token")).willReturn(Optional.of(token));

        assertThatThrownBy(() -> userService.resetPassword("used-token", "newPass123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token has already been used");
    }

    @Test
    void resetPassword_shouldThrow_whenTokenExpired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(1L);
        token.setToken("expired-token");
        token.setUserId(1L);
        token.setUsed(false);
        token.setExpiryDate(LocalDateTime.now().minusHours(2));
        given(passwordResetTokenRepository.findByToken("expired-token")).willReturn(Optional.of(token));

        assertThatThrownBy(() -> userService.resetPassword("expired-token", "newPass123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token has expired");
    }

    @Test
    void updateUser_shouldCallIdentityServiceClient_whenEmailChanged() {
        User user = sampleUser(1L);
        user.setEmail("old@example.com");
        user.setStatus(UserStatus.active);
        UserRequest req = validRequest();
        req.setEmail("new@example.com");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserResponse resp = userService.updateUser(1L, req);

        assertThat(resp.getEmail()).isEqualTo("new@example.com");
        then(identityServiceClient).should().updateEmail("johndoe", "new@example.com");
        then(identityServiceClient).should().disableUser("johndoe");
    }

    @Test
    void verifyEmail_shouldSucceed_whenIdentityClientFails() {
        User user = sampleUser(1L);
        user.setStatus(UserStatus.pending_verification);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        willThrow(new RuntimeException("Identity service down")).given(identityServiceClient).enableUser("johndoe");

        userService.verifyEmail(1L);

        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.active);
        then(identityServiceClient).should().enableUser("johndoe");
    }

    @Test
    void activateUser_shouldSucceed_whenIdentityClientFails() {
        User user = sampleUser(1L);
        user.setStatus(UserStatus.inactive);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        willThrow(new RuntimeException("Identity service down")).given(identityServiceClient).enableUser("johndoe");

        userService.activateUser(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.active);
        then(identityServiceClient).should().enableUser("johndoe");
    }
}

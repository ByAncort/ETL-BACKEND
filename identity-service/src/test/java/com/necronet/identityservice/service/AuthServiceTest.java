package com.necronet.identityservice.service;

import com.necronet.identityservice.client.UserRegistryClient;
import com.necronet.identityservice.entity.UserCredential;
import com.necronet.identityservice.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserCredentialRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRegistryClient userRegistryClient;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<UserCredential> credentialCaptor;

    @Test
    void saveUser_shouldEncodePasswordAndSave() {
        UserCredential credential = new UserCredential();
        credential.setUsername("newuser");
        credential.setEmail("new@example.com");
        credential.setPassword("rawPassword");
        credential.setEnabled(true);

        given(repository.findByUsername("newuser")).willReturn(Optional.empty());
        given(repository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("rawPassword")).willReturn("encodedPassword");
        given(repository.save(any())).willReturn(credential);

        String result = authService.saveUser(credential);

        assertThat(result).isEqualTo("User registered successfully");
        then(repository).should().save(credentialCaptor.capture());
        UserCredential saved = credentialCaptor.getValue();
        assertThat(saved.getPassword()).isEqualTo("encodedPassword");
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void saveUser_whenNotEnabled_shouldSaveDisabled() {
        UserCredential credential = new UserCredential();
        credential.setUsername("newuser");
        credential.setEmail("new@example.com");
        credential.setPassword("rawPassword");
        credential.setEnabled(false);

        given(repository.findByUsername("newuser")).willReturn(Optional.empty());
        given(repository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("rawPassword")).willReturn("encodedPassword");
        given(repository.save(any())).willReturn(credential);

        authService.saveUser(credential);

        then(repository).should().save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().isEnabled()).isFalse();
    }

    @Test
    void saveUser_usernameAlreadyExists_shouldThrow() {
        UserCredential credential = new UserCredential();
        credential.setUsername("existing");
        credential.setEmail("existing@example.com");
        credential.setPassword("password");

        given(repository.findByUsername("existing")).willReturn(Optional.of(new UserCredential()));

        assertThatThrownBy(() -> authService.saveUser(credential))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");
    }

    @Test
    void saveUser_emailAlreadyExists_shouldThrow() {
        UserCredential credential = new UserCredential();
        credential.setUsername("newuser");
        credential.setEmail("existing@example.com");
        credential.setPassword("password");

        given(repository.findByUsername("newuser")).willReturn(Optional.empty());
        given(repository.findByEmail("existing@example.com")).willReturn(Optional.of(new UserCredential()));

        assertThatThrownBy(() -> authService.saveUser(credential))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists");
    }

    @Test
    void generateToken_shouldDelegateToJwtService() {
        given(userRegistryClient.getUserRoles("testuser")).willReturn(Set.of("ROLE_USER"));
        given(jwtService.generateToken("testuser", Set.of("ROLE_USER"))).willReturn("access-token");

        String token = authService.generateToken("testuser");

        assertThat(token).isEqualTo("access-token");
        then(jwtService).should().generateToken("testuser", Set.of("ROLE_USER"));
    }

    @Test
    void generateRefreshToken_shouldDelegateToJwtService() {
        given(userRegistryClient.getUserRoles("testuser")).willReturn(Set.of("ROLE_USER"));
        given(jwtService.generateRefreshToken("testuser")).willReturn("refresh-token");

        String token = authService.generateRefreshToken("testuser");

        assertThat(token).isEqualTo("refresh-token");
    }

    @Test
    void validateToken_shouldDelegateToJwtService() {
        given(jwtService.validateToken("some-token")).willReturn(true);

        Boolean result = authService.validateToken("some-token");

        assertThat(result).isTrue();
        then(jwtService).should().validateToken("some-token");
    }

    @Test
    void validateRefreshToken_shouldDelegateToJwtService() {
        given(jwtService.validateRefreshToken("refresh-token", "testuser")).willReturn(true);

        Boolean result = authService.validateRefreshToken("refresh-token", "testuser");

        assertThat(result).isTrue();
        then(jwtService).should().validateRefreshToken("refresh-token", "testuser");
    }

    @Test
    void extractUsernameFromToken_shouldDelegateToJwtService() {
        given(jwtService.extractUsername("some-token")).willReturn("testuser");

        String username = authService.extractUsernameFromToken("some-token");

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void invalidateToken_shouldDelegateToJwtService() {
        willDoNothing().given(jwtService).addToBlacklist("some-token");

        authService.invalidateToken("some-token");

        then(jwtService).should().addToBlacklist("some-token");
    }

    @Test
    void enableUser_shouldSetEnabledTrue() {
        UserCredential credential = new UserCredential();
        credential.setUsername("testuser");
        credential.setEnabled(false);

        given(repository.findByUsername("testuser")).willReturn(Optional.of(credential));

        authService.enableUser("testuser");

        then(repository).should().save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().isEnabled()).isTrue();
    }

    @Test
    void enableUser_userNotFound_shouldThrow() {
        given(repository.findByUsername("nonexistent")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.enableUser("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void disableUser_shouldSetEnabledFalse() {
        UserCredential credential = new UserCredential();
        credential.setUsername("testuser");
        credential.setEnabled(true);

        given(repository.findByUsername("testuser")).willReturn(Optional.of(credential));

        authService.disableUser("testuser");

        then(repository).should().save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().isEnabled()).isFalse();
    }

    @Test
    void disableUser_userNotFound_shouldThrow() {
        given(repository.findByUsername("nonexistent")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.disableUser("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updatePassword_shouldEncodeAndSave() {
        UserCredential credential = new UserCredential();
        credential.setUsername("testuser");
        credential.setPassword("oldPassword");

        given(repository.findByUsername("testuser")).willReturn(Optional.of(credential));
        given(passwordEncoder.encode("newPassword123")).willReturn("encodedNewPassword");

        authService.updatePassword("testuser", "newPassword123");

        then(repository).should().save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getPassword()).isEqualTo("encodedNewPassword");
    }

    @Test
    void updatePassword_userNotFound_shouldThrow() {
        given(repository.findByUsername("nonexistent")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updatePassword("nonexistent", "newPass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateEmail_shouldUpdateAndSave() {
        UserCredential credential = new UserCredential();
        credential.setUsername("testuser");
        credential.setEmail("old@example.com");

        given(repository.findByUsername("testuser")).willReturn(Optional.of(credential));

        authService.updateEmail("testuser", "new@example.com");

        then(repository).should().save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updateEmail_userNotFound_shouldThrow() {
        given(repository.findByUsername("nonexistent")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateEmail("nonexistent", "new@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}

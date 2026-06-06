package com.necronet.identityservice.service;

import com.necronet.identityservice.client.UserRegistryClient;
import com.necronet.identityservice.entity.UserCredential;
import com.necronet.identityservice.repository.UserCredentialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tema 1 — Seguridad y Autenticacion.
 * Pruebas unitarias de AuthService.validateToken.
 * Verifica la mejora: al desactivar un usuario, su token vigente deja de
 * ser valido (la sesion se cierra), no solo se bloquea el login.
 */
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

    private UserCredential userWithEnabled(boolean enabled) {
        UserCredential c = new UserCredential();
        c.setUsername("diego");
        c.setEnabled(enabled);
        return c;
    }

    @Test
    @DisplayName("Token valido + usuario activo -> valido")
    void validateToken_validAndEnabled_returnsTrue() {
        when(jwtService.validateToken("tok")).thenReturn(true);
        when(jwtService.extractUsername("tok")).thenReturn("diego");
        when(repository.findByUsername("diego")).thenReturn(Optional.of(userWithEnabled(true)));

        assertThat(authService.validateToken("tok")).isTrue();
    }

    @Test
    @DisplayName("Token valido pero usuario desactivado -> invalido (sesion cerrada)")
    void validateToken_validButDisabled_returnsFalse() {
        when(jwtService.validateToken("tok")).thenReturn(true);
        when(jwtService.extractUsername("tok")).thenReturn("diego");
        when(repository.findByUsername("diego")).thenReturn(Optional.of(userWithEnabled(false)));

        assertThat(authService.validateToken("tok")).isFalse();
    }

    @Test
    @DisplayName("Token valido pero usuario inexistente -> invalido")
    void validateToken_userNotFound_returnsFalse() {
        when(jwtService.validateToken("tok")).thenReturn(true);
        when(jwtService.extractUsername("tok")).thenReturn("fantasma");
        when(repository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThat(authService.validateToken("tok")).isFalse();
    }

    @Test
    @DisplayName("Token con firma/expiracion invalida -> invalido sin consultar BD")
    void validateToken_invalidJwt_returnsFalseWithoutDbLookup() {
        when(jwtService.validateToken("bad")).thenReturn(false);

        assertThat(authService.validateToken("bad")).isFalse();
        verify(repository, never()).findByUsername(anyString());
    }
}

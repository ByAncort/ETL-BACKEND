package com.necronet.identityservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tema 1 — Seguridad y Autenticacion.
 * Pruebas unitarias puras de JwtService (sin contexto Spring ni BD).
 * Cubren: generacion/validacion de token, firma, expiracion,
 * blacklist (cierre de sesion en logout) y validacion de refresh token.
 */
class JwtServiceTest {

    // Mismo secreto que application.yaml (Base64, 48 bytes -> valido para HS256).
    private static final String SECRET =
            "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
    private static final long WEEK_MS = 604800000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET", SECRET);
        ReflectionTestUtils.setField(jwtService, "EXPIRATION", WEEK_MS);
        ReflectionTestUtils.setField(jwtService, "REFRESH_EXPIRATION", WEEK_MS);
    }

    @Test
    @DisplayName("Token generado con firma valida se valida como verdadero")
    void generateToken_thenValidate_returnsTrue() {
        String token = jwtService.generateToken("diego", Set.of("ROLE_USER"));

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("diego");
    }

    @Test
    @DisplayName("extractRoles devuelve los roles embebidos en el token")
    void extractRoles_returnsClaimRoles() {
        String token = jwtService.generateToken("diego", Set.of("ROLE_ADMIN", "ROLE_USER"));

        assertThat(jwtService.extractRoles(token))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("Token con firma invalida (manipulado) se rechaza")
    void validateToken_tampered_returnsFalse() {
        String token = jwtService.generateToken("diego");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("String basura no es un token valido")
    void validateToken_garbage_returnsFalse() {
        assertThat(jwtService.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("Logout: token en blacklist deja de ser valido (cierra sesion)")
    void blacklistedToken_returnsFalse() {
        String token = jwtService.generateToken("diego");
        assertThat(jwtService.validateToken(token)).isTrue();

        jwtService.addToBlacklist(token);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Token expirado se rechaza")
    void expiredToken_returnsFalse() {
        JwtService expiredSvc = new JwtService();
        ReflectionTestUtils.setField(expiredSvc, "SECRET", SECRET);
        // Expiracion negativa -> el token nace ya vencido.
        ReflectionTestUtils.setField(expiredSvc, "EXPIRATION", -1000L);
        ReflectionTestUtils.setField(expiredSvc, "REFRESH_EXPIRATION", -1000L);

        String token = expiredSvc.generateToken("diego");

        assertThat(expiredSvc.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Refresh token valido para el mismo usuario se acepta")
    void validateRefreshToken_matchingUser_returnsTrue() {
        String refresh = jwtService.generateRefreshToken("diego");

        assertThat(jwtService.validateRefreshToken(refresh, "diego")).isTrue();
    }

    @Test
    @DisplayName("Refresh token con usuario distinto se rechaza")
    void validateRefreshToken_wrongUser_returnsFalse() {
        String refresh = jwtService.generateRefreshToken("diego");

        assertThat(jwtService.validateRefreshToken(refresh, "felipe")).isFalse();
    }

    @Test
    @DisplayName("Access token no se acepta como refresh token (type != refresh)")
    void validateRefreshToken_accessToken_returnsFalse() {
        String access = jwtService.generateToken("diego");

        assertThat(jwtService.validateRefreshToken(access, "diego")).isFalse();
    }
}

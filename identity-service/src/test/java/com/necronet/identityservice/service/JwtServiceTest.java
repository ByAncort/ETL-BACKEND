package com.necronet.identityservice.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
    private static final long EXPIRATION = 604800000L;
    private static final long REFRESH_EXPIRATION = 604800000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET", SECRET);
        ReflectionTestUtils.setField(jwtService, "EXPIRATION", EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "REFRESH_EXPIRATION", REFRESH_EXPIRATION);
    }

    @Test
    void generateToken_shouldReturnValidJwt() {
        String token = jwtService.generateToken("testuser");
        assertThat(token).isNotNull().isNotBlank();
        Boolean isValid = jwtService.validateToken(token);
        assertThat(isValid).isTrue();
    }

    @Test
    void generateToken_withRoles_shouldIncludeRolesInClaims() {
        Set<String> roles = Set.of("ROLE_ADMIN", "ROLE_USER");
        String token = jwtService.generateToken("admin", roles);
        Set<String> extractedRoles = jwtService.extractRoles(token);
        assertThat(extractedRoles).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void generateToken_withSingleRole_shouldIncludeRoleInClaims() {
        String token = jwtService.generateToken("testuser", "ROLE_MODERATOR");
        Set<String> extractedRoles = jwtService.extractRoles(token);
        assertThat(extractedRoles).containsExactly("ROLE_MODERATOR");
    }

    @Test
    void validateToken_validToken_shouldReturnTrue() {
        String token = jwtService.generateToken("testuser");
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_shouldReturnFalse() {
        String token = createTokenWithExpiration(-1000);
        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_malformedToken_shouldReturnFalse() {
        assertThat(jwtService.validateToken("malformed.jwt.token")).isFalse();
    }

    @Test
    void validateToken_blacklistedToken_shouldReturnFalse() {
        String token = jwtService.generateToken("testuser");
        jwtService.addToBlacklist(token);
        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void extractUsername_shouldReturnCorrectSubject() {
        String token = jwtService.generateToken("testuser");
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void extractRoles_whenTokenHasRoles_shouldReturnThem() {
        Set<String> roles = Set.of("ROLE_USER", "ROLE_MANAGER");
        String token = jwtService.generateToken("testuser", roles);
        Set<String> extracted = jwtService.extractRoles(token);
        assertThat(extracted).containsExactlyInAnyOrder("ROLE_USER", "ROLE_MANAGER");
    }

    @Test
    void extractRoles_whenTokenHasNoRoles_shouldReturnEmpty() {
        Key signKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(signKey)
                .compact();
        Set<String> extracted = jwtService.extractRoles(token);
        assertThat(extracted).isEmpty();
    }

    @Test
    void generateRefreshToken_shouldReturnValidToken() {
        String token = jwtService.generateRefreshToken("testuser");
        assertThat(token).isNotNull().isNotBlank();
        Boolean isValid = jwtService.validateRefreshToken(token, "testuser");
        assertThat(isValid).isTrue();
    }

    @Test
    void generateRefreshToken_withRoles_shouldReturnValidToken() {
        Set<String> roles = Set.of("ROLE_USER");
        String token = jwtService.generateRefreshToken("testuser", roles);
        assertThat(jwtService.validateRefreshToken(token, "testuser")).isTrue();
    }

    @Test
    void validateRefreshToken_validToken_shouldReturnTrue() {
        String token = jwtService.generateRefreshToken("testuser");
        assertThat(jwtService.validateRefreshToken(token, "testuser")).isTrue();
    }

    @Test
    void validateRefreshToken_wrongUsername_shouldReturnFalse() {
        String token = jwtService.generateRefreshToken("testuser");
        assertThat(jwtService.validateRefreshToken(token, "otheruser")).isFalse();
    }

    @Test
    void validateRefreshToken_accessTokenNotRefresh_shouldReturnFalse() {
        String token = jwtService.generateToken("testuser");
        assertThat(jwtService.validateRefreshToken(token, "testuser")).isFalse();
    }

    @Test
    void validateRefreshToken_expiredToken_shouldReturnFalse() {
        String token = createRefreshTokenWithExpiration(-1000);
        assertThat(jwtService.validateRefreshToken(token, "testuser")).isFalse();
    }

    @Test
    void validateRefreshToken_blacklistedToken_shouldReturnFalse() {
        String token = jwtService.generateRefreshToken("testuser");
        jwtService.addToBlacklist(token);
        assertThat(jwtService.validateRefreshToken(token, "testuser")).isFalse();
    }

    @Test
    void addToBlacklist_shouldInvalidateToken() {
        String token = jwtService.generateToken("testuser");
        assertThat(jwtService.validateToken(token)).isTrue();
        jwtService.addToBlacklist(token);
        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void addToBlacklist_shouldInvalidateRefreshToken() {
        String token = jwtService.generateRefreshToken("testuser");
        assertThat(jwtService.validateRefreshToken(token, "testuser")).isTrue();
        jwtService.addToBlacklist(token);
        assertThat(jwtService.validateRefreshToken(token, "testuser")).isFalse();
    }

    @Test
    void extractExpiration_shouldReturnExpirationDate() {
        String token = jwtService.generateToken("testuser");
        Date expiration = jwtService.extractExpiration(token);
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date(System.currentTimeMillis() - 1000));
    }

    @Test
    void cleanupBlacklist_shouldRemoveExpiredTokens() {
        String expiredToken = createTokenWithExpiration(-1000);
        jwtService.addToBlacklist(expiredToken);
        String validToken = jwtService.generateToken("testuser");
        jwtService.addToBlacklist(validToken);

        jwtService.cleanupBlacklist();

        assertThat(jwtService.validateToken(validToken)).isFalse();
    }

    private String createTokenWithExpiration(long expirationOffsetMs) {
        Key signKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .setSubject("testuser")
                .claim("type", "access")
                .claim("roles", Set.of("ROLE_USER"))
                .setIssuedAt(new Date(System.currentTimeMillis() - 100000))
                .setExpiration(new Date(System.currentTimeMillis() + expirationOffsetMs))
                .signWith(signKey)
                .compact();
    }

    private String createRefreshTokenWithExpiration(long expirationOffsetMs) {
        Key signKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .setSubject("testuser")
                .claim("type", "refresh")
                .claim("roles", Set.of("ROLE_USER"))
                .setIssuedAt(new Date(System.currentTimeMillis() - 100000))
                .setExpiration(new Date(System.currentTimeMillis() + expirationOffsetMs))
                .signWith(signKey)
                .compact();
    }
}

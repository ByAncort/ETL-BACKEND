package com.necronet.swiggygateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    private JwtUtil jwtUtil;

    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "EXPIRATION", 3600000L);
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    void validateToken_whenValidToken_shouldReturnTrue() {
        String token = generateToken("testuser", List.of("USER"), 3600000);

        boolean valid = jwtUtil.validateToken(token);

        assertThat(valid).isTrue();
    }

    @Test
    void validateToken_whenExpiredToken_shouldReturnFalse() {
        String token = generateToken("testuser", List.of("USER"), -1000);

        boolean valid = jwtUtil.validateToken(token);

        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_whenMalformedToken_shouldReturnFalse() {
        boolean valid = jwtUtil.validateToken("malformed-token-string");

        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_whenNullToken_shouldReturnFalse() {
        boolean valid = jwtUtil.validateToken(null);

        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_whenEmptyToken_shouldReturnFalse() {
        boolean valid = jwtUtil.validateToken("");

        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_whenBlacklistedToken_shouldReturnFalse() {
        String token = generateToken("testuser", List.of("USER"), 3600000);
        jwtUtil.addToBlacklist(token);

        boolean valid = jwtUtil.validateToken(token);

        assertThat(valid).isFalse();
    }

    @Test
    void extractUsername_shouldReturnSubject() {
        String token = generateToken("johndoe", List.of("USER"), 3600000);

        String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo("johndoe");
    }

    @Test
    void extractUsername_whenInvalidToken_shouldReturnNull() {
        String username = jwtUtil.extractUsername("invalid");

        assertThat(username).isNull();
    }

    @Test
    void extractRoles_shouldReturnJoinedRoles() {
        String token = generateToken("admin", List.of("ADMIN", "USER"), 3600000);

        String roles = jwtUtil.extractRoles(token);

        assertThat(roles).isEqualTo("ADMIN,USER");
    }

    @Test
    void extractRoles_whenNoRolesClaim_shouldReturnDefault() {
        String token = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();

        String roles = jwtUtil.extractRoles(token);

        assertThat(roles).isEqualTo("USER");
    }

    @Test
    void extractRoles_whenInvalidToken_shouldReturnDefault() {
        String roles = jwtUtil.extractRoles("invalid");

        assertThat(roles).isEqualTo("USER");
    }

    @Test
    void extractExpiration_shouldReturnExpirationDate() {
        String token = generateToken("testuser", List.of("USER"), 3600000);

        Date expiration = jwtUtil.extractExpiration(token);

        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void extractExpiration_whenInvalidToken_shouldReturnNull() {
        Date expiration = jwtUtil.extractExpiration("invalid");

        assertThat(expiration).isNull();
    }

    @Test
    void addToBlacklist_shouldBlacklistToken() {
        String token = generateToken("testuser", List.of("USER"), 3600000);

        jwtUtil.addToBlacklist(token);

        assertThat(jwtUtil.isBlacklisted(token)).isTrue();
    }

    @Test
    void validateToken_whenWrongTokenType_shouldReturnFalse() {
        String token = Jwts.builder()
                .subject("testuser")
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();

        boolean valid = jwtUtil.validateToken(token);

        assertThat(valid).isFalse();
    }

    private String generateToken(String subject, java.util.List<String> roles, long expirationMs) {
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }
}

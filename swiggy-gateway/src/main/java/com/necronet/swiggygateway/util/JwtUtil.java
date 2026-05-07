package com.necronet.swiggygateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret:5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437}")
    private String SECRET;

    @Value("${jwt.expiration:1800000}")
    private long EXPIRATION;

    private final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("Token is null or empty");
                return false;
            }

            if (tokenBlacklist.contains(token)) {
                log.warn("Token is blacklisted");
                return false;
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                log.warn("Token has expired");
                return false;
            }

            String tokenType = claims.get("type", String.class);
            if (tokenType != null && !"access".equals(tokenType)) {
                log.warn("Invalid token type: {}", tokenType);
                return false;
            }

            log.debug("Token validated successfully for user: {}", claims.getSubject());
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during token validation: {}", e.getMessage(), e);
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    public String extractRoles(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("roles", String.class);
        } catch (Exception e) {
            log.warn("No roles found in token");
            return "USER";
        }
    }

    public Date extractExpiration(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("Failed to extract expiration from token: {}", e.getMessage());
            return null;
        }
    }

    public void addToBlacklist(String token) {
        tokenBlacklist.add(token);
        log.debug("Token added to blacklist");
    }

    public void removeFromBlacklist(String token) {
        tokenBlacklist.remove(token);
        log.debug("Token removed from blacklist");
    }

    public boolean isBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }

    public void cleanupBlacklist() {
        tokenBlacklist.removeIf(token -> {
            try {
                Date expiration = extractExpiration(token);
                return expiration != null && expiration.before(new Date());
            } catch (Exception e) {
                return true;
            }
        });
        log.debug("Blacklist cleanup completed. Remaining tokens: {}", tokenBlacklist.size());
    }
}
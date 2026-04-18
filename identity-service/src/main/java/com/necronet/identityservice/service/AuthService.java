package com.necronet.identityservice.service;

import com.necronet.identityservice.entity.UserCredential;
import com.necronet.identityservice.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public String saveUser(UserCredential credential) {
        if (repository.findByUsername(credential.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (repository.findByEmail(credential.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        credential.setStatus(com.necronet.identityservice.entity.UserStatusEnum.ACTIVE);
        repository.save(credential);
        return "User registered successfully";
    }

    public String generateToken(String username) {
        return jwtService.generateToken(username);
    }

    public String generateRefreshToken(String username) {
        return jwtService.generateRefreshToken(username);
    }

    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }

    public boolean validateRefreshToken(String refreshToken, String username) {
        return jwtService.validateRefreshToken(refreshToken, username);
    }

    public String extractUsernameFromToken(String token) {
        return jwtService.extractUsername(token);
    }

    public void invalidateToken(String token) {
        jwtService.addToBlacklist(token);
    }
}
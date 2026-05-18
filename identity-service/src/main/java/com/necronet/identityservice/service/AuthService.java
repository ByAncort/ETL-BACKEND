package com.necronet.identityservice.service;

import com.necronet.identityservice.client.UserRegistryClient;
import com.necronet.identityservice.entity.UserCredential;
import com.necronet.identityservice.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRegistryClient userRegistryClient;


    @Transactional
    public String saveUser(UserCredential credential) {
        if (repository.findByUsername(credential.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (repository.findByEmail(credential.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        credential.setRole("USER");
        if (credential.isEnabled()) {
            credential.setEnabled(true);
        } else {
            credential.setEnabled(false);
        }
        repository.save(credential);
        return "User registered successfully";
    }

    public String generateToken(String username) {
        Set<String> roles = userRegistryClient.getUserRoles(username);
        return jwtService.generateToken(username, roles);
    }

    public String generateRefreshToken(String username) {
        Set<String> roles = userRegistryClient.getUserRoles(username);
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

    @Transactional
    public void enableUser(String username) {
        UserCredential credential = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        credential.setEnabled(true);
        repository.save(credential);
    }

    @Transactional
    public void disableUser(String username) {
        UserCredential credential = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        credential.setEnabled(false);
        repository.save(credential);
    }

    @Transactional
    public void updatePassword(String username, String newPassword) {
        UserCredential credential = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        credential.setPassword(passwordEncoder.encode(newPassword));
        repository.save(credential);
    }

    @Transactional
    public void updateEmail(String username, String newEmail) {
        UserCredential credential = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        credential.setEmail(newEmail);
        repository.save(credential);
    }
}
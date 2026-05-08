package com.necronet.identityservice.controller;

import com.necronet.identityservice.dto.AuthRequest;
import com.necronet.identityservice.dto.AuthResponse;
import com.necronet.identityservice.dto.RegisterRequest;
import com.necronet.identityservice.entity.UserCredential;
import com.necronet.identityservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        UserCredential userCredential = new UserCredential();
        userCredential.setUsername(registerRequest.getUsername());
        userCredential.setEmail(registerRequest.getEmail());
        userCredential.setPassword(registerRequest.getPassword());

        String response = authService.saveUser(userCredential);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(response, null));
    }

    @PostMapping("/token")
    public ResponseEntity<AuthResponse> generateToken(@Valid @RequestBody AuthRequest authRequest) {
        try {
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            if (authenticate.isAuthenticated()) {
                String authenticatedUsername = authenticate.getName();
                String accessToken = authService.generateToken(authenticatedUsername);
                String refreshToken = authService.generateRefreshToken(authenticatedUsername);
                return ResponseEntity.ok(new AuthResponse("Authentication successful", accessToken, refreshToken));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("Invalid username/email or password", null, null));
            }
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Invalid username/email or password", null, null));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@RequestParam("token") String token) {
        boolean isValid = authService.validateToken(token);
        if (isValid) {
            return ResponseEntity.ok(new AuthResponse("Token is valid", null));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Token is invalid", null));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        String username = authService.extractUsernameFromToken(refreshToken);
        if (authService.validateRefreshToken(refreshToken, username)) {
            String newAccessToken = authService.generateToken(username);
            return ResponseEntity.ok(new AuthResponse("Token refreshed", newAccessToken, null));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Invalid refresh token", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader("Authorization") String token) {
        authService.invalidateToken(token.replace("Bearer ", ""));
        return ResponseEntity.ok(new AuthResponse("Logged out successfully", null));
    }
}
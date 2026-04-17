package com.necronet.apiregisterms.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "api_id")
    private Apis api;

    @Enumerated(EnumType.STRING)
    private AuthType authType; // BEARER, BASIC, API_KEY, OAUTH2, NONE

    @ManyToOne
    @JoinColumn(name = "header_id")
    private Header header; // Normalmente "Authorization"

    private String credentialValue; // Para Bearer: token, para Basic: base64("user:pass")

    // Campos adicionales para diferentes tipos de auth
    private String username; // Para Basic Auth
    private String password; // Para Basic Auth (encriptado)
    private String tokenEndpoint; // Para OAuth2
    private LocalDateTime tokenExpiry; // Para tokens con expiración

    private LocalDateTime createdAt;
}

package com.necronet.registerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "auth_config")
public class AuthConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Para Basic Auth
    private String username;
    private String password;

    // Para Bearer Token / API Key
    private String token;

    // Para API Key
    private String apiKeyName; // Nombre del header que contiene la API Key
    private String apiKeyValue;

    // Para OAuth2
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String scope;

    // Endpoint de validación
    private String validationEndpoint;

    // Configuración adicional en JSON
    @Column(columnDefinition = "TEXT")
    private String additionalConfig;
}
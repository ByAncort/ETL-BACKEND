package com.metacontrol.etlconfig.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "llm_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "api_key", nullable = false, length = 1000)
    private String apiKey;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "model_name", length = 200)
    private String modelName;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LlmStatus status = LlmStatus.active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

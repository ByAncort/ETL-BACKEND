package com.necronet.registerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationApis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;

    @Column(name = "data_domain", length = 50)
    private String dataDomain;

    @Column(name = "update_frequency", length = 20)
    private String updateFrequency;

    // Relaciones
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "api_input_id", referencedColumnName = "id")
    private ApiInput apiInput;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "api_output_id", referencedColumnName = "id")
    private ApiOutput apiOutput;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_config_id", referencedColumnName = "id")
    private AuthConfig authConfig;

    @OneToMany(mappedBy = "integrationApi", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApiCallHistory> callHistory;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "ml_feature_tags", length = 500)
    private String mlFeatureTags;

    @Column(name = "created_at", updatable = false)
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

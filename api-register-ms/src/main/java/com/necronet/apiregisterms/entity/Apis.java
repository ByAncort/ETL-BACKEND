package com.necronet.apiregisterms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class Apis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "method_id")
    private Method method;

    private String url;
    private String description;
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "api", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AuthConfig authConfig;

    @ManyToOne
    @JoinColumn(name = "auth_api_id")
    private Apis authApi;
}

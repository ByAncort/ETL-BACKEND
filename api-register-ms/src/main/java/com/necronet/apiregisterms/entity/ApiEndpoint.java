package com.necronet.apiregisterms.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ApiEndpoint extends Apis {
    private String pathParams; // Ej: "/users/{id}"
    private String queryParams; // Ej: "?page=1&size=10"
    @Column(columnDefinition = "TEXT")
    private String body;
}

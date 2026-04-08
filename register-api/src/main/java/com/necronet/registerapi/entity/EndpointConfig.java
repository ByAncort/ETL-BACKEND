package com.necronet.registerapi.entity;

import com.necronet.registerapi.entity.enums.AuthType;
import com.necronet.registerapi.entity.enums.TypeExample;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "endpoint_config")
public class EndpointConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    private HttpMethod method = HttpMethod.POST;

    @Enumerated(EnumType.STRING)
    private AuthType authType = AuthType.NONE;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_config_id")
    private AuthConfig authConfig;

    private Integer timeout = 30000; // Timeout en milisegundos

    private Integer retryCount = 3;

    //definene la estructura del enpoint request o response para el procesado en ia
    @Enumerated(EnumType.STRING)
    private TypeExample typeExample;

    @Column(columnDefinition = "json")
    private String example;

    // Headers personalizados
    @ElementCollection
    @CollectionTable(name = "endpoint_headers",
            joinColumns = @JoinColumn(name = "endpoint_config_id"))
    @MapKeyColumn(name = "header_key")
    @Column(name = "header_value")
    private java.util.Map<String, String> customHeaders = new java.util.HashMap<>();

    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH
    }
}
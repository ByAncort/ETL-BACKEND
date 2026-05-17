# ETL Automate - Backend

Ecosistema de microservicios construido con **Spring Boot** para la plataforma **ETL Automate**. Gestiona identidades, conexiones entre APIs, el mapeo semántico de campos con Machine Learning y la ejecución de flujos ETL.

---

## Arquitectura de Microservicios

La plataforma está basada en una arquitectura distribuida:

```mermaid
graph TB
    Cliente["Frontend Web (React)"] -->|"HTTP / REST"| GW

    subgraph Infraestructura
        GW["Spring Cloud Gateway :8080"]
        Eureka["Eureka Service Registry :8761"]
    end

    subgraph Seguridad & Usuarios
        Identity["Identity Service :9898"]
        UserReg["User Registry MS :9090"]
    end

    subgraph Core ETL
        Integration["Integration MS :8082"]
        ApiReg["API Register MS :8083"]
        SaveData["MS Save Data :8001"]
    end

    subgraph Machine Learning
        SchemaMatch["Schema Matching MS :8085"]
        Matcher["Matcher MS :8000"]
        LLMConfig["ETL Config LLM MS :8086"]
    end

    GW -->|"Rutas Abiertas"| Identity
    GW -->|"Rutas Protegidas (JWT)"| Core ETL
    GW -->|"Validación Auth"| Identity

    %% Registro y Descubrimiento
    Identity -.->|"Registra"| Eureka
    UserReg -.->|"Registra"| Eureka
    Core ETL -.->|"Registran"| Eureka
    Machine Learning -.->|"Registran"| Eureka
    GW -.->|"Descubre"| Eureka

    style Infraestructura fill:#4a9eff,color:#fff
    style Seguridad & Usuarios fill:#51cf66,color:#fff
    style Core ETL fill:#ffd43b,color:#333
    style Machine Learning fill:#845ef7,color:#fff
```

## Servicios Principales

1. **Gateway (`swiggy-gateway` - 8080)**: Punto de entrada único. Valida los JWT y enruta el tráfico hacia los microservicios correspondientes.
2. **Eureka (`swiggy-service-registry` - 8761)**: Servidor de descubrimiento. Permite que los servicios se encuentren entre sí.
3. **Identity Service (`identity-service` - 9898)**: Encargado de la autenticación, generación/validación de JWT (HS256) y el inicio de sesión.
4. **User Registry MS (`user-registry-ms` - 9090)**: Gestiona el CRUD de usuarios, roles, y la validación de cuentas por parte del administrador.
5. **API Register MS (`api-register-ms` - 8083)**: Almacena y prueba las configuraciones de los endpoints (ej. NetSuite, Oracle).
6. **Integration MS (`integration-ms` - 8082)**: Define las conexiones entre un API de origen y una de destino.
7. **Schema Matching MS (`schema-matching-ms` - 8085)**: Gestiona el estado y el feedback de las predicciones de equivalencias de campos (schema matching).
8. **Matcher MS (`MATCHER-MS` - 8000)**: Servicio que interactúa con el motor LLM y coordina la generación de sugerencias de mapeo.
9. **ETL Config LLM MS (`ETL-CONFIG-LLM-MS` - 8086)**: Permite configurar y seleccionar el proveedor LLM a utilizar (OpenAI, Anthropic, etc.).
10. **MS Save Data (`MS-SAVE-DATA` - 8001)**: Gestiona el guardado del flujo ETL y sincroniza los datos finales transformados.

## Flujo de Autenticación (JWT)

1. El usuario se registra (`/auth/register`) a través del Gateway -> Identity Service.
2. Un administrador activa al usuario (cambiando su estado en `user-registry-ms`, que sincroniza de forma transparente con `identity-service`).
3. El usuario hace login (`/auth/token`). El Identity Service valida con MySQL y retorna un JWT.
4. Para cualquier ruta protegida (ej. `/api/integrations/**`), el Gateway intercepta la petición, extrae el token y lo valida contra el `identity-service` (endpoint `/validate`) antes de reenviar el tráfico al microservicio destino.

## Tecnologías Utilizadas

- **Core**: Java 21, Spring Boot 3.3.4, Spring Cloud 2023.0.3
- **Infraestructura**: Netflix Eureka, Spring Cloud Gateway
- **Seguridad**: Spring Security, JWT (jjwt), BCrypt
- **Bases de Datos**: MySQL (Múltiples esquemas separados por servicio: `identity_db`, `registry_api`, `schema_matching`, `llm_config_db`) + Spring Data JPA
- **Contenedores**: Docker, Docker Compose

## Ejecución Local con Docker Compose

La forma más sencilla de levantar todo el ecosistema y sus bases de datos asociadas es a través de Docker Compose:

1. Asegúrate de tener **Docker** y **Docker Compose** instalados.
2. Abre una terminal en la raíz de `ETL-BACKEND`.
3. Ejecuta el siguiente comando para construir y levantar todos los contenedores en segundo plano:
   ```bash
   docker-compose up -d --build
   ```
4. Los servicios tomarán unos minutos en compilar e iniciar.
5. Puedes acceder al panel de **Eureka** en [http://localhost:8761](http://localhost:8761) para confirmar qué microservicios se han registrado correctamente.

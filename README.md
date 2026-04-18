# ETL-BACKEND

Backend de microservicios para la plataforma web **ETL-MachingLearning**. Proporciona la infraestructura necesaria para que los usuarios puedan configurar APIs, supervisar el mapeo de campos generado por el modelo de Machine Learning, realizar correcciones manuales y ejecutar integraciones ETL entre sistemas empresariales como **NetSuite** y **Oracle Primavera Unifier**.

---

## Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura de Microservicios](#arquitectura-de-microservicios)
- [Flujo de Autenticación](#flujo-de-autenticación)
- [Detalle de Microservicios](#detalle-de-microservicios)
- [Tecnologías](#tecnologías)
- [Requisitos Previos](#requisitos-previos)
- [Instalación y Ejecución](#instalación-y-ejecución)
- [Endpoints de la API](#endpoints-de-la-api)
- [Relación con ETL-MachingLearning](#relación-con-etl-machinglearning)
- [Contribuir](#contribuir)

---

## Descripción General

Este repositorio contiene el backend de la plataforma ETL, construido con una arquitectura de microservicios usando **Spring Boot** y **Spring Cloud**. Su objetivo es servir como la capa web que permite a los usuarios:

- **Configurar APIs** de origen y destino para procesos ETL.
- **Supervisar el mapeo** de campos generado automáticamente por el módulo de Machine Learning.
- **Corregir manualmente** las equivalencias de campos cuando sea necesario.
- **Ejecutar integraciones** entre los sistemas conectados.

---

## Arquitectura de Microservicios

```mermaid
graph TB
    Cliente["Cliente / Frontend Web"] -->|"Peticiones HTTP"| GW

    subgraph GW["API Gateway"]
        Gateway["Spring Cloud Gateway"]
        AuthFilter["AuthenticationFilter"]
        JwtVal["JwtUtil - Validación JWT"]
        Gateway --> AuthFilter --> JwtVal
    end

    subgraph Eureka["Service Registry :8761"]
        EurekaServer["Netflix Eureka Server"]
    end

    subgraph Identity["Identity Service :9898"]
        AuthCtrl["AuthController"]
        AuthSvc["AuthService"]
        JwtSvc["JwtService HS256"]
        H2["H2 Database"]
        AuthCtrl --> AuthSvc
        AuthSvc --> JwtSvc
        AuthSvc --> H2
    end

    subgraph App["ETL App Service :8081"]
        AppCtrl["SwiggyAppController"]
        AppSvc["SwiggyAppService"]
        AppCtrl --> AppSvc
        AppSvc -.-> RestClient
    end

    subgraph ML["ETL-MachingLearning"]
        MLModel["Modelo ML de Matching"]
        MongoDB[(MongoDB)]
    end

    GW -->|"Rutas abiertas"| Identity
    GW -->|"Rutas protegidas - JWT"| App
    App -.->|"Consulta mapeo y predicciones"| ML

    Identity -.->|"Registra"| Eureka
    App -.->|"Registra"| Eureka
    GW -.->|"Descubre servicios"| Eureka

    style ML fill:#845ef7,color:#fff
    style GW fill:#4a9eff,color:#fff
    style Identity fill:#51cf66,color:#fff
    style App fill:#ffd43b,color:#333
```

---

## Flujo de Autenticación

```mermaid
sequenceDiagram
    actor U as Usuario
    participant FE as Frontend
    participant GW as API Gateway
    participant ID as Identity Service
    participant APP as ETL App Service

    U->>FE: Registrarse / Login
    FE->>GW: POST /auth/register o /auth/token
    GW->>ID: Reenvía (ruta abierta, sin filtro)
    ID->>ID: BCrypt + Genera JWT (HS256)
    ID-->>GW: Token JWT
    GW-->>FE: Token JWT
    FE->>FE: Almacena token

    U->>FE: Acceder a funcionalidad protegida
    FE->>GW: GET /swiggy/* (Bearer JWT)
    GW->>GW: AuthenticationFilter valida JWT
    alt Token válido
        GW->>APP: Reenvía petición
        APP-->>GW: Respuesta
        GW-->>FE: Respuesta
    else Token inválido
        GW-->>FE: 401 Unauthorized
    end
```

---

## Detalle de Microservicios

### 1. Service Registry (Eureka Server)

| Propiedad     | Valor                              |
|---------------|------------------------------------|
| **Puerto**    | 8761                               |
| **Tecnología**| Netflix Eureka Server              |
| **Función**   | Descubrimiento de servicios        |

```mermaid
graph LR
    E["Eureka Server :8761"]
    ID["Identity Service"] -.->|"Registra"| E
    APP["ETL App Service"] -.->|"Registra"| E
    GW["API Gateway"] -.->|"Descubre"| E
```

No se registra a sí mismo (`register-with-eureka: false`, `fetch-registry: false`).

### 2. API Gateway (Spring Cloud Gateway)

| Propiedad     | Valor                                  |
|---------------|----------------------------------------|
| **Tecnología**| Spring Cloud Gateway (WebFlux)         |
| **Función**   | Punto de entrada único, routing y auth |

**Rutas configuradas:**

| Ruta              | Servicio Destino           | Autenticación              |
|-------------------|----------------------------|----------------------------|
| `/auth/**`        | IDENTITY-SERVICE (lb)      | No (ruta abierta)          |
| `/swiggy/**`      | SWIGGY-APP (lb)            | Sí (AuthenticationFilter)  |

**Rutas abiertas (sin autenticación):** `/auth/register`, `/auth/token`, `/eureka`

### 3. Identity Service (Autenticación)

| Propiedad        | Valor                                |
|------------------|--------------------------------------|
| **Puerto**       | 9898                                 |
| **Función**      | Registro, login, generación de JWT   |
| **Base de datos**| PostgreSQL                           |
| **Seguridad**    | Spring Security + BCrypt + JWT HS256 |

```mermaid
erDiagram
    user {
        bigint id PK
        varchar username
        varchar email
        varchar password_hash
        varchar first_name
        varchar last_name
        UserStatusEnum status
    }
    role {
        bigint id PK
        RoleEnum name
        varchar description
    }
    user_role {
        bigint usuario_id FK
        bigint rol_id FK
    }
    user ||--o{ user_role : "has"
    role ||--o{ user_role : "assigned to"
```

### 4. ETL App Service (Lógica de Negocio)

| Propiedad     | Valor                                              |
|---------------|-----------------------------------------------------|
| **Puerto**    | 8081                                                |
| **Función**   | Lógica de negocio, configuración de APIs, mapeo ETL |
| **Comunicación**| RestTemplate con `@LoadBalanced`                  |

Este servicio es el núcleo funcional que se conecta con el módulo de **ETL-MachingLearning** para:
- Obtener predicciones de mapeo del modelo ML.
- Permitir supervisión y corrección de mapeos.
- Ejecutar las integraciones ETL configuradas.

---

## Tecnologías

```mermaid
graph LR
    subgraph Backend
        SB["Spring Boot 3.3.4"]
        SC["Spring Cloud 2023.0.3"]
        SS["Spring Security"]
    end

    subgraph Infraestructura
        EU["Netflix Eureka"]
        SCG["Spring Cloud Gateway"]
    end

    subgraph Seguridad
        JWT["JWT HS256 - jjwt 0.12.6"]
        BC["BCrypt"]
    end

    subgraph Datos
        DB["PostgreSQL Database"]
        JPA["Spring Data JPA"]
    end

    SB --> SC
    SC --> EU
    SC --> SCG
    SB --> SS
    SS --> JWT
    SS --> BC
    SB --> JPA
    JPA --> DB
```

| Componente              | Tecnología                              |
|-------------------------|-----------------------------------------|
| Lenguaje                | Java 21                                 |
| Framework               | Spring Boot 3.3.4                       |
| Cloud                   | Spring Cloud 2023.0.3                   |
| Gateway                 | Spring Cloud Gateway (WebFlux)          |
| Service Discovery       | Netflix Eureka                          |
| Seguridad               | Spring Security + JWT (HS256)           |
| Base de Datos           | PostgreSQL                              |
| ORM                     | Spring Data JPA                         |
| Build                   | Maven                                   |
| Utilidades              | Lombok                                  |

---

## Requisitos Previos

- **Java** 21 o superior
- **Maven** 3.9+ (o usar el wrapper `mvnw` incluido)

---

## Instalación y Ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/ByAncort/ETL-BACKEND.git
cd ETL-BACKEND
```

### 2. Orden de inicio de los servicios

Los servicios deben iniciarse en el siguiente orden:

```mermaid
flowchart LR
    A["1. Service Registry\n:8761"] --> B["2. Identity Service\n:9898"]
    A --> C["3. ETL App Service\n:8081"]
    B --> D["4. API Gateway"]
    C --> D
```

```bash
# 1. Service Registry (Eureka)
cd swiggy-service-registry
./mvnw spring-boot:run

# 2. Identity Service
cd identity-service
./mvnw spring-boot:run

# 3. ETL App Service
cd swiggy-app
./mvnw spring-boot:run

# 4. API Gateway
cd swiggy-gateway
./mvnw spring-boot:run
```

### 3. Verificar que los servicios están registrados

Accede al dashboard de Eureka: [http://localhost:8761](http://localhost:8761)

---

## Endpoints de la API

Todas las peticiones pasan por el **API Gateway**.

### Autenticación (rutas abiertas)

| Método | Ruta               | Body                                                    | Descripción              |
|--------|--------------------|---------------------------------------------------------|--------------------------|
| POST   | `/auth/register`   | `{"name":"...", "email":"...", "password":"..."}`        | Registrar nuevo usuario  |
| POST   | `/auth/token`      | `{"username":"...", "password":"..."}`                   | Obtener token JWT (login)|
| GET    | `/auth/validate`   | `?token=<jwt>`                                           | Validar token JWT        |

### Servicio de Negocio (requiere Bearer JWT)

| Método | Ruta             | Header                          | Descripción                    |
|--------|------------------|---------------------------------|--------------------------------|
| GET    | `/swiggy/home`   | `Authorization: Bearer <token>` | Mensaje de bienvenida          |

### Ejemplo de uso

```bash
# 1. Registrar usuario
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"admin", "email":"admin@etl.com", "password":"secret123"}'

# 2. Obtener token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin", "password":"secret123"}')

# 3. Acceder a ruta protegida
curl http://localhost:8080/swiggy/home \
  -H "Authorization: Bearer $TOKEN"
```

---

## Relación con ETL-MachingLearning

```mermaid
graph TB
    subgraph Backend["ETL-BACKEND (este repo)"]
        GW["API Gateway"]
        AUTH["Identity Service"]
        APP["ETL App Service"]
    end

    subgraph Frontend["Frontend Web"]
        UI["Interfaz de Usuario"]
    end

    subgraph ML["ETL-MachingLearning"]
        SEM["Configuración Semántica"]
        MODEL["Modelo Neuronal"]
        MDB[(MongoDB)]
    end

    UI -->|"HTTP + JWT"| GW
    GW --> AUTH
    GW --> APP

    APP -->|"Consulta predicciones"| MODEL
    APP -->|"Lee/escribe configuración"| SEM
    APP -->|"Datos de entrenamiento"| MDB

    UI -->|"Supervisar mapeo"| APP
    UI -->|"Corregir mapeo"| APP
    UI -->|"Ejecutar integración"| APP

    style Backend fill:#4a9eff,color:#fff
    style ML fill:#845ef7,color:#fff
    style Frontend fill:#51cf66,color:#fff
```

Este backend es la capa de servicio que conecta el **frontend web** con el motor de **Machine Learning**:

- **Configuración de APIs:** Los usuarios configuran las APIs de origen/destino desde la interfaz web.
- **Supervisión de mapeo:** El modelo ML genera predicciones de equivalencia de campos, que los usuarios pueden revisar.
- **Correcciones manuales:** Si el modelo falla en algún mapeo, los usuarios pueden corregirlo directamente.
- **Ejecución de integraciones:** Una vez validado el mapeo, los usuarios pueden ejecutar el proceso ETL completo.

---

## Contribuir

1. Haz un fork del repositorio.
2. Crea una rama para tu feature: `git checkout -b feature/nueva-funcionalidad`
3. Realiza tus cambios y haz commit: `git commit -m "Agregar nueva funcionalidad"`
4. Sube tu rama: `git push origin feature/nueva-funcionalidad`
5. Abre un Pull Request.

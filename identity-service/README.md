# Identity Service (Register API)

Microservicio de autenticacion y registro de usuarios. Genera y valida tokens JWT.

## Tecnologias

- Java 21
- Spring Boot 3.3.4
- Spring Security + JWT (HS256)
- Spring Data JPA + H2
- Eureka Client

## Configuracion

| Propiedad | Valor |
|-----------|-------|
| Puerto | `9898` |
| Eureka | `http://localhost:8761/eureka/` |
| Nombre | `IDENTITY-SERVICE` |

## Endpoints

### Registrar usuario
```
POST /auth/register
Content-Type: application/json

{
  "name": "usuario",
  "email": "usuario@mail.com",
  "password": "123456"
}
```

### Obtener token (login)
```
POST /auth/token
Content-Type: application/json

{
  "username": "usuario",
  "password": "123456"
}
```

### Validar token
```
GET /auth/validate?token=<JWT>
```

## Ejecutar

```bash
./mvnw spring-boot:run
```

> Requiere que el Eureka Server (`swiggy-service-registry`) este corriendo en el puerto 8761.

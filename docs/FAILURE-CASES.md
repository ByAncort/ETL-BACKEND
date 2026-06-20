# Failure Cases
> Catálogo de fallos resueltos en QA — causa raíz, síntoma y resolución

| Propiedad | Valor |
|-----------|-------|
| Proyecto | ETL-BACKEND |
| Rama | qa |
| Última Actualización | 2026-06-19 |

---

## FC-01: Aserción incorrecta en test de rate limit

| Campo | Detalle |
|-------|---------|
| Síntoma | `rateLimit_shouldAddHeaders_onSuccess` falla porque `setStatusCode` nunca se invoca |
| Archivo | `swiggy-gateway/.../filter/AuthenticationFilterMoreTest.java:220` |
| Causa | El test usaba `then(response).should().setStatusCode(any())` en el caso exitoso. El filtro de rate limit NO modifica el status cuando la request es válida — solo pasa al `chain.filter()`. |
| Resolución | Cambiar aserción: `should(never()).setStatusCode(...)` + verificar que `chain.filter(exchange)` sí se ejecuta |
| Commit | `53687da` |

---

## FC-02: `@MockBean` faltante en AuthControllerTest

| Campo | Detalle |
|-------|---------|
| Síntoma | `AuthControllerTest` no carga contexto — `NoSuchBeanDefinitionException` para `SessionLogService` |
| Archivo | `identity-service/.../controller/AuthControllerTest.java` |
| Causa | `AuthController` inyecta `SessionLogService` via `@Autowired`. El test no proveía mock, Spring no encontraba el bean al cargar `@WebMvcTest`. |
| Resolución | Agregar `@MockBean private SessionLogService sessionLogService` |
| Commit | `42415f6` |

---

## FC-03: Mensaje de excepción desincronizado

| Campo | Detalle |
|-------|---------|
| Síntoma | Test `runMatching_shouldThrowException_whenMatcherFails` falla: `hasMessageContaining("Matcher returned error")` no coincide con el mensaje real |
| Archivo | `integration-ms/.../service/IntegrationServiceMoreTest.java:234` |
| Causa | El mensaje real lanzado por el servicio era `"Failed to run matcher"`. El test se escribió con un mensaje distinto al real. |
| Resolución | Cambiar `.hasMessageContaining("Matcher returned error")` → `.hasMessageContaining("Failed to run matcher")` |
| Commit | `33b7751` |

---

## FC-04: `columnDefinition` MySQL incompatible con H2 en tests

| Campo | Detalle |
|-------|---------|
| Síntoma | Tests de `api-register-ms` fallan con `JdbcSQLSyntaxErrorException` — H2 no reconoce `TEXT` ni `LONGTEXT` |
| Archivos | `ApiEndpoint.java`, `AuthCredential.java`, `ExecutionLog.java` |
| Causa | Usaban `@Column(columnDefinition = "TEXT")` y `@Column(columnDefinition = "LONGTEXT")`, sintaxis específica de MySQL. Al correr tests con H2 (modo MySQL simulado), H2 no interpreta esos tipos. |
| Resolución | Reemplazar por `@Lob` — anotación JPA portable entre H2 y PostgreSQL |
| Commit | `a7d6b8d` |

---

## FC-05: Cambio de contraseña no sincronizado entre microservicios

| Campo | Detalle |
|-------|---------|
| Síntoma | Usuario cambia contraseña en `user-registry-ms` pero no puede iniciar sesión — `identity-service` mantiene el hash anterior |
| Archivos | `identity-service/.../controller/AuthController.java`, `AuthService.java`, `dto/UpdatePasswordRequest.java` + `user-registry-ms/.../client/IdentityServiceClient.java`, `service/UserService.java` |
| Causa | `identity-service` no tenía endpoint para actualizar contraseña. `user-registry-ms` hasheaba y guardaba localmente pero nunca notificaba a `identity-service`. |
| Resolución | 1. Nuevo endpoint `PUT /update-password` en `AuthController`<br>2. DTO `UpdatePasswordRequest`<br>3. `AuthService.updatePassword()` hashea y persiste<br>4. `IdentityServiceClient.updatePassword()` llama al endpoint via HTTP<br>5. `UserService.updateUser()` invoca al client después de hashear |
| Commit | `e31ad27` |

---

## FC-06: Query `findByUsername` inconsistente

| Campo | Detalle |
|-------|---------|
| Síntoma | Error de tipo: `findByUsername` retorna `Optional<User>` pero el código esperaba `User` directo; no hay búsqueda case-insensitive |
| Archivo | `user-registry-ms/.../repository/UserRepository.java` |
| Causa | Spring Data JPA genera `findByUsername` como `Optional<User>`. Código cliente esperaba `User` directo. Además login fallaba si el username tenía distinto casing. |
| Resolución | Cambiar a `findUserByUsername` (retorna `User` directo) + agregar `findByUsernameIgnoreCase` para login case-insensitive |
| Commit | `f70098d` |

---

## FC-07: Null safety + rutas abiertas faltantes en RouteValidator

| Campo | Detalle |
|-------|---------|
| Síntoma | `NullPointerException` al acceder a `request.getURI().getPath()` si la URI es null. `/api/users/register` y `/api/users/login` devuelven 401. |
| Archivos | `swiggy-gateway/.../filter/RouteValidator.java`, `exception/GlobalErrorWebExceptionHandler.java` |
| Causa | Gateway no validaba null en URI/path. Las rutas del nuevo `user-registry-ms` no estaban en `OPEN_API_ENDPOINTS`. |
| Resolución | 1. Null checks en `isSecured` y `isPublicEndpoint`<br>2. Agregar rutas faltantes a `OPEN_API_ENDPOINTS`<br>3. Logger en `GlobalErrorWebExceptionHandler` para tracking |
| Commit | `aae0341` |

---

## FC-08: Puertos incorrectos en Dockerfiles

| Campo | Detalle |
|-------|---------|
| Síntoma | Todos los Dockerfiles exponen `EXPOSE 8080`. Contenedores no rutean correctamente. `swiggy-service-registry` referencia `temurin:25-jre` (inexistente). |
| Archivos | `api-register-ms/Dockerfile`, `identity-service/Dockerfile`, `swiggy-service-registry/Dockerfile` |
| Causa | Se copió el mismo Dockerfile sin ajustar puertos al real de cada servicio. |
| Resolución | Corregir `EXPOSE`: `8083` (api-register), `9898` (identity), `8761` (registry). Cambiar `temurin:25-jre` → `temurin:21-jre`. |
| Commit | `6ccd4f4` |

---

## FC-09: jjwt 0.13.0 incompatible con API legacy

| Campo | Detalle |
|-------|---------|
| Síntoma | Error de compilación: `setSigningKey()`, `parseClaimsJws()`, `getBody()` no existen en jjwt 0.13.0 |
| Archivos | `swiggy-gateway/.../util/JwtUtil.java`, `identity-service/.../service/JwtService.java` + `pom.xml` de ambos |
| Causa | jjwt 0.13.0 removió la API legacy. El código usaba `verifyWith()`, `parseSignedClaims()`, `getPayload()` (API nueva) pero `SecretKey` no coincidía. |
| Resolución | Unificar a jjwt `0.12.6` en ambos `pom.xml`. En `JwtUtil` usar `setSigningKey(Key)`, `parseClaimsJws()`, `getBody()`. En `JwtService` safe call con `"refresh".equals(...)` primero. |
| Commits | `ed7e87f`, `78bd40b` |

---

## Resumen por severidad

| FC | Severidad | Tipo | Servicio |
|----|-----------|------|----------|
| FC-01 | Baja | Test assertion | swiggy-gateway |
| FC-02 | Alta | Contexto Spring | identity-service |
| FC-03 | Baja | Test assertion | integration-ms |
| FC-04 | Alta | DDL incompatibilidad | api-register-ms |
| FC-05 | Crítica | Sincronización datos | identity + user-registry |
| FC-06 | Media | Query inconsistency | user-registry-ms |
| FC-07 | Alta | Null safety + routing | swiggy-gateway |
| FC-08 | Media | Configuración Docker | Todos |
| FC-09 | Alta | Dependencias | swiggy-gateway + identity |

# Register API

Microservicio de gestion de integraciones API para la plataforma ETL. Permite registrar, configurar y administrar las APIs de origen y destino utilizadas en los procesos de integracion ETL.

## Tecnologias

| Componente | Tecnologia |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Cloud | Spring Cloud 2023.0.3 |
| Base de Datos | MySQL |
| ORM | Spring Data JPA |
| Documentacion | SpringDoc OpenAPI (Swagger UI) |
| Service Discovery | Netflix Eureka Client |
| Build | Maven |

## Requisitos

- Java 21
- Maven 3.9+ (o usar el wrapper `mvnw` incluido)
- MySQL corriendo en `localhost:3306`
- Base de datos `registry_api` creada
- Eureka Server corriendo en `localhost:8761`

## Configuracion

El servicio corre en el puerto **8083** y se registra automaticamente en Eureka.

Archivo de configuracion: `src/main/resources/application.yml`

## Ejecucion

```bash
cd register-api
./mvnw spring-boot:run
```

## Endpoints

Base path: `/api/v1/integration-apis`

| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/` | Crear nueva API de integracion |
| GET | `/` | Listar todas las APIs |
| GET | `/{id}` | Obtener API por ID |
| GET | `/filter?mode=` | Filtrar por modo de ejecucion (ORCHESTRATED / SCHEDULED) |
| GET | `/active/scheduled` | Obtener APIs programadas activas |
| PUT | `/{id}` | Actualizar una API |
| DELETE | `/{id}` | Eliminar una API |
| PATCH | `/{id}/status?active=` | Activar o desactivar una API |
| GET | `/{id}/execution-history` | Historial de ejecuciones |
| POST | `/{id}/execute` | Ejecutar manualmente una API |

## Documentacion Swagger

Con el servicio corriendo, acceder a:

```
http://localhost:8083/swagger-ui.html
```

## Modelo de Datos

```
IntegrationApis (1) <-> (1) EndpointConfig (input)
IntegrationApis (1) <-> (1) EndpointConfig (output)
EndpointConfig  (1) <-> (1) AuthConfig
IntegrationApis (1) ->  (N) ExecutionHistory
```

## Ejemplo de uso

### Crear API de integracion

```bash
curl -X POST http://localhost:8083/api/v1/integration-apis \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mi Integracion",
    "description": "Integracion de ejemplo",
    "executionMode": "ORCHESTRATED",
    "active": true,
    "inputEndpoint": {
      "url": "http://localhost:8081/api/source",
      "method": "POST",
      "authType": "BEARER_TOKEN",
      "timeout": 30000,
      "retryCount": 3,
      "authConfig": {
        "token": "mi-token"
      }
    },
    "outputEndpoint": {
      "url": "http://localhost:8082/api/destination",
      "method": "POST",
      "authType": "NONE",
      "timeout": 60000,
      "retryCount": 2
    }
  }'
```

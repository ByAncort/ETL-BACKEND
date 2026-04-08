
# Relaciones de entidades
```
IntegrationApis (1) ←→ (1) ApiInput
IntegrationApis (1) ←→ (1) ApiOutput  
IntegrationApis (1) ←→ (1) AuthConfig
IntegrationApis (1) → (N) ApiCallHistory
IntegrationApis (1) → (N) MLLearningData
```


### Crear API de Integración - Orquestada (Simple)
``` json
POST http://localhost:8083/api/v1/integration-apis
Content-Type: application/json

{
"name": "API con Ejemplos para Procesamiento IA",
"description": "API que incluye ejemplos de request/response para procesamiento inteligente",
"executionMode": "ORCHESTRATED",
"active": true,
"inputEndpoint": {
"url": "http://localhost:8081/api/customers/search",
"method": "POST",
"authType": "BEARER_TOKEN",
"timeout": 30000,
"retryCount": 3,
"typeExample": "REQUEST",
"example": "{\n  \"query\": {\n    \"name\": \"John\",\n    \"age_range\": {\n      \"min\": 25,\n      \"max\": 45\n    },\n    \"city\": \"New York\",\n    \"limit\": 100\n  },\n  \"fields\": [\"id\", \"name\", \"email\", \"phone\"],\n  \"sort_by\": \"created_at\",\n  \"sort_order\": \"desc\"\n}",
"authConfig": {
"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
"validationEndpoint": "http://localhost:8081/api/auth/validate"
},
"customHeaders": {
"Content-Type": "application/json",
"Accept": "application/json"
}
},
"outputEndpoint": {
"url": "http://localhost:8082/api/customers/bulk-import",
"method": "POST",
"authType": "OAUTH2",
"timeout": 60000,
"retryCount": 2,
"typeExample": "RESPONSE",
"example": "{\n  \"status\": \"success\",\n  \"data\": {\n    \"imported\": 45,\n    \"failed\": 2,\n    \"total\": 47,\n    \"failures\": [\n      {\n        \"index\": 12,\n        \"reason\": \"Invalid email format\",\n        \"data\": {\n          \"name\": \"Jane Doe\",\n          \"email\": \"invalid-email\"\n        }\n      }\n    ],\n    \"import_id\": \"imp_20240115_143022\"\n  },\n  \"metadata\": {\n    \"processing_time_ms\": 1245,\n    \"timestamp\": \"2024-01-15T14:30:22Z\"\n  }\n}",
"authConfig": {
"clientId": "ia-processor",
"clientSecret": "ia-secret-789",
"tokenUrl": "http://localhost:8082/oauth/token",
"scope": "write:customers",
"validationEndpoint": "http://localhost:8082/oauth/validate"
},
"customHeaders": {
"Content-Type": "application/json"
}
}
}
```
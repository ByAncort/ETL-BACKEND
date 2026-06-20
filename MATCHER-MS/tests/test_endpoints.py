"""Endpoints HTTP de MATCHER-MS: router FastAPI con TestClient."""
from unittest.mock import AsyncMock
from datetime import datetime

from fastapi.testclient import TestClient

from app.main import app
from app.routers import endpoints
from app.services import dataService

client = TestClient(app)

NOW = datetime(2026, 6, 5, 12, 0, 0)


class TestConnectionEndpoint:
    def test_get_connection_ok(self, monkeypatch):
        from app.models import ConnectionResponse
        monkeypatch.setattr(
            endpoints.integration_client, "get_connection",
            AsyncMock(return_value=ConnectionResponse(
                id=1, apiA="10", apiB="20", description="test",
                status="ACTIVE", createdAt=NOW, updatedAt=NOW,
            )),
        )

        response = client.get("/connection/1")

        assert response.status_code == 200
        assert response.json()["apiA"] == "10"

    def test_get_connection_error_devuelve_502(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.integration_client, "get_connection",
            AsyncMock(side_effect=RuntimeError("connection refused")),
        )

        response = client.get("/connection/1")

        assert response.status_code == 502
        assert "connection refused" in response.json()["detail"]


class TestApiDefinitionEndpoint:
    def test_get_api_definition_ok(self, monkeypatch):
        from app.models import ApiRegistryResponse
        monkeypatch.setattr(
            endpoints.api_registry_client, "get_api_definition",
            AsyncMock(return_value=ApiRegistryResponse(
                id=10, method="GET", url="http://api.test", description="test",
                createdAt=NOW,
            )),
        )

        response = client.get("/api-definition/10")

        assert response.status_code == 200
        assert response.json()["id"] == 10

    def test_get_api_definition_502(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.api_registry_client, "get_api_definition",
            AsyncMock(side_effect=Exception("not found")),
        )

        response = client.get("/api-definition/999")

        assert response.status_code == 502


class TestDefaultModelEndpoint:
    def test_get_default_model_ok(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.data_service, "get_default_model",
            AsyncMock(return_value={"provider": "openai", "modelName": "gpt-4"}),
        )

        response = client.get("/default-model")

        assert response.status_code == 200
        assert response.json()["provider"] == "openai"


class TestSchemaMatchEndpoints:
    def test_create_schema_match_ok(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.data_service, "register_schema_match",
            AsyncMock(return_value={"sourceField": "name", "targetField": "nombre"}),
        )

        response = client.post("/schema-matches", json={
            "sourceField": "name", "targetField": "nombre",
            "confidence": 0.9, "integrationId": 1,
        })

        assert response.status_code == 200
        assert response.json()["sourceField"] == "name"

    def test_create_schema_match_422(self):
        response = client.post("/schema-matches", json={})

        assert response.status_code == 422

    def test_create_schema_matches_batch_ok(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.data_service, "register_schema_matches",
            AsyncMock(return_value=[{"sourceField": "name", "targetField": "nombre"}]),
        )

        response = client.post("/schema-matches/batch", json=[{
            "sourceField": "name", "targetField": "nombre",
            "confidence": 0.9, "integrationId": 1,
        }])

        assert response.status_code == 200
        assert len(response.json()) == 1


class TestProcessIntegrationEndpoint:
    def test_process_integration_ok(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.data_service, "process_integration",
            AsyncMock(return_value={"connection": {"id": 1}, "apiA": {}, "apiB": {}}),
        )

        response = client.get("/process-integration/1")

        assert response.status_code == 200
        assert response.json()["connection"]["id"] == 1

    def test_process_integration_502(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.data_service, "process_integration",
            AsyncMock(side_effect=RuntimeError("fail")),
        )

        response = client.get("/process-integration/1")

        assert response.status_code == 502


class TestRunMatchingEndpoint:
    def test_run_matching_ok(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.data_service, "run_matching",
            AsyncMock(return_value={"sourceFields": ["name"], "matches": [], "etl": {}}),
        )

        response = client.post("/run-matching/1")

        assert response.status_code == 200
        assert response.json()["sourceFields"] == ["name"]

    def test_run_matching_502(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.data_service, "run_matching",
            AsyncMock(side_effect=RuntimeError("matching error")),
        )

        response = client.post("/run-matching/1")

        assert response.status_code == 502
        assert "matching error" in response.json()["detail"]

"""Tema 4 — ETL async: pruebas de la capa HTTP (router FastAPI) con TestClient."""
from unittest.mock import AsyncMock

from fastapi.testclient import TestClient

from app.main import app
from app.models.schemas import EtlResponse
from app.routers import endpoints

client = TestClient(app)


def _ok_summary(integration_id: int = 1) -> EtlResponse:
    return EtlResponse(
        integrationId=integration_id,
        sourceApiId=10,
        targetApiId=20,
        totalRecords=2,
        transformedRecords=2,
        loadedRecords=2,
        errors=[],
    )


class TestHealth:
    def test_health_responde_ok(self):
        response = client.get("/api/health")

        assert response.status_code == 200
        assert response.json() == {"status": "ok", "service": "MS-SAVE-DATA"}


class TestRunEtlEndpoint:
    def test_run_etl_delega_en_el_orquestador(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.etl_orchestrator, "run_etl", AsyncMock(return_value=_ok_summary(1))
        )

        response = client.post("/api/etl/run", json={"integrationId": 1})

        assert response.status_code == 200
        body = response.json()
        assert body["integrationId"] == 1
        assert body["loadedRecords"] == 2

    def test_run_etl_by_id_delega_en_el_orquestador(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.etl_orchestrator, "run_etl", AsyncMock(return_value=_ok_summary(5))
        )

        response = client.post("/api/etl/run/5")

        assert response.status_code == 200
        assert response.json()["integrationId"] == 5

    def test_error_inesperado_devuelve_502(self, monkeypatch):
        monkeypatch.setattr(
            endpoints.etl_orchestrator, "run_etl", AsyncMock(side_effect=RuntimeError("boom"))
        )

        response = client.post("/api/etl/run", json={"integrationId": 1})

        assert response.status_code == 502
        assert "boom" in response.json()["detail"]

    def test_request_invalido_devuelve_422(self):
        response = client.post("/api/etl/run", json={})

        assert response.status_code == 422

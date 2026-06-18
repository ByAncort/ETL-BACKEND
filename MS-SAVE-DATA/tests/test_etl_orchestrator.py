"""Tema 4 — ETL async: pruebas del proceso ETL orquestado (MS-SAVE-DATA).

El proceso async (run_etl) debe: ejecutar las fases EXTRACT→TRANSFORM→LOAD,
registrar/notificar al log service central, y devolver un resumen coherente
incluso cuando una fase falla.
"""
from unittest.mock import AsyncMock, MagicMock

import pytest

from app.services.etlOrchestrator import EtlOrchestrator
from tests.conftest import make_connection, make_match


@pytest.fixture
def orchestrator():
    """Orquestador con todos sus colaboradores mockeados (sin red)."""
    orch = EtlOrchestrator()
    orch.integration_client = MagicMock()
    orch.integration_client.get_connection = AsyncMock()
    orch.schema_match_client = MagicMock()
    orch.schema_match_client.get_matches_by_integration_and_status = AsyncMock()
    orch.log_client = MagicMock()
    orch.log_client.send_log = AsyncMock()
    orch.extract_service = MagicMock()
    orch.extract_service.extract_data = AsyncMock()
    orch.transform_service = MagicMock()
    orch.load_service = MagicMock()
    orch.load_service.load_data = AsyncMock()
    return orch


def _wire_happy_path(orch):
    """Configura los mocks para un ETL exitoso de 2 registros."""
    orch.integration_client.get_connection.return_value = make_connection(api_a="10", api_b="20")
    orch.schema_match_client.get_matches_by_integration_and_status.return_value = [
        make_match(source_field="name", target_field="nombre"),
    ]
    orch.extract_service.extract_data.return_value = [{"name": "Ada"}, {"name": "Linus"}]
    orch.transform_service.transform_data.return_value = [{"nombre": "Ada"}, {"nombre": "Linus"}]
    orch.load_service.load_data.return_value = {"loaded": 2, "errors": []}


class TestRunEtlHappyPath:
    async def test_devuelve_resumen_con_conteos_correctos(self, orchestrator):
        _wire_happy_path(orchestrator)

        result = await orchestrator.run_etl(integration_id=1)

        assert result.integrationId == 1
        assert result.sourceApiId == 10
        assert result.targetApiId == 20
        assert result.totalRecords == 2
        assert result.transformedRecords == 2
        assert result.loadedRecords == 2
        assert result.errors == []

    async def test_ejecuta_las_fases_en_orden(self, orchestrator):
        _wire_happy_path(orchestrator)

        await orchestrator.run_etl(integration_id=1)

        orchestrator.integration_client.get_connection.assert_awaited_once_with(1)
        orchestrator.extract_service.extract_data.assert_awaited_once_with(10)
        orchestrator.transform_service.transform_data.assert_called_once()
        orchestrator.load_service.load_data.assert_awaited_once()

    async def test_no_envia_log_de_error_en_exito(self, orchestrator):
        _wire_happy_path(orchestrator)

        await orchestrator.run_etl(integration_id=1)

        # No debe registrarse ningún log de nivel ERROR cuando todo sale bien.
        error_logs = [
            c for c in orchestrator.log_client.send_log.await_args_list
            if c.args and c.args[0].get("logLevel") == "ERROR"
        ]
        assert error_logs == []


class TestRunEtlNotificaFin:
    """Mejora derivada: el proceso async debe notificar su fin al log service."""

    async def test_notifica_fin_exitoso_al_log_service(self, orchestrator):
        _wire_happy_path(orchestrator)

        await orchestrator.run_etl(integration_id=1)

        completion_logs = [
            c for c in orchestrator.log_client.send_log.await_args_list
            if c.args and c.args[0].get("logLevel") == "INFO"
        ]
        assert len(completion_logs) == 1, "se esperaba exactamente una notificación de fin"
        payload = completion_logs[0].args[0]
        assert payload["integrationId"] == "1"
        assert payload["serviceName"] == "ms-save-data"
        assert "loadedRecords" in payload.get("detail", "") or payload.get("durationMs") is not None


class TestRunEtlErroresPorFase:
    async def test_error_de_conexion_devuelve_response_de_error(self, orchestrator):
        orchestrator.integration_client.get_connection.side_effect = RuntimeError("502 down")

        result = await orchestrator.run_etl(integration_id=7)

        assert result.integrationId == 7
        assert result.sourceApiId == 0
        assert result.targetApiId == 0
        assert result.loadedRecords == 0
        assert any("conexión" in e.lower() for e in result.errors)
        # Se notifica el error a la fase CONNECTION.
        orchestrator.log_client.send_log.assert_awaited()
        sent = orchestrator.log_client.send_log.await_args.args[0]
        assert sent["logLevel"] == "ERROR"
        assert "CONNECTION" in sent["message"]

    async def test_sin_matches_aceptados_corta_temprano(self, orchestrator):
        orchestrator.integration_client.get_connection.return_value = make_connection(api_a="10", api_b="20")
        orchestrator.schema_match_client.get_matches_by_integration_and_status.return_value = []

        result = await orchestrator.run_etl(integration_id=1)

        assert result.totalRecords == 0
        assert result.loadedRecords == 0
        assert any("schema matches" in e.lower() for e in result.errors)
        orchestrator.extract_service.extract_data.assert_not_awaited()

    async def test_error_en_extract_se_notifica(self, orchestrator):
        orchestrator.integration_client.get_connection.return_value = make_connection(api_a="10", api_b="20")
        orchestrator.schema_match_client.get_matches_by_integration_and_status.return_value = [make_match()]
        orchestrator.extract_service.extract_data.side_effect = RuntimeError("API 10 returned 500")

        result = await orchestrator.run_etl(integration_id=1)

        assert result.sourceApiId == 10
        assert result.transformedRecords == 0
        assert any("extrayendo" in e.lower() for e in result.errors)
        sent = orchestrator.log_client.send_log.await_args.args[0]
        assert sent["logLevel"] == "ERROR"
        assert "EXTRACT" in sent["message"]
        orchestrator.transform_service.transform_data.assert_not_called()

    async def test_error_en_load_devuelve_conteos_parciales(self, orchestrator):
        orchestrator.integration_client.get_connection.return_value = make_connection(api_a="10", api_b="20")
        orchestrator.schema_match_client.get_matches_by_integration_and_status.return_value = [make_match()]
        orchestrator.extract_service.extract_data.return_value = [{"name": "Ada"}]
        orchestrator.transform_service.transform_data.return_value = [{"nombre": "Ada"}]
        orchestrator.load_service.load_data.side_effect = RuntimeError("destino caído")

        result = await orchestrator.run_etl(integration_id=1)

        assert result.totalRecords == 1
        assert result.transformedRecords == 1
        assert result.loadedRecords == 0
        assert any("cargando" in e.lower() for e in result.errors)
        sent = orchestrator.log_client.send_log.await_args.args[0]
        assert "LOAD" in sent["message"]

    async def test_errores_parciales_de_load_se_propagan(self, orchestrator):
        orchestrator.integration_client.get_connection.return_value = make_connection(api_a="10", api_b="20")
        orchestrator.schema_match_client.get_matches_by_integration_and_status.return_value = [make_match()]
        orchestrator.extract_service.extract_data.return_value = [{"name": "Ada"}, {"name": "Bob"}]
        orchestrator.transform_service.transform_data.return_value = [{"nombre": "Ada"}, {"nombre": "Bob"}]
        orchestrator.load_service.load_data.return_value = {"loaded": 1, "errors": ["HTTP 400 para registro: Bob"]}

        result = await orchestrator.run_etl(integration_id=1)

        assert result.loadedRecords == 1
        assert result.transformedRecords == 2
        assert result.errors == ["HTTP 400 para registro: Bob"]

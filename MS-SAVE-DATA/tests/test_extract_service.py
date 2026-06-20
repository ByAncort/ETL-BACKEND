"""ExtractService: fase EXTRACT del ETL — obtiene definición API, test endpoint, parsea body."""
from unittest.mock import AsyncMock, MagicMock
from datetime import datetime

import pytest

from app.models.schemas import ApiRegistryResponse
from app.services.extractService import ExtractService


def _api_def(*, api_id: int = 10, method: str = "GET", body: str | None = None,
             path_params: str | None = None, query_params: str | None = None) -> ApiRegistryResponse:
    now = datetime(2026, 6, 5, 12, 0, 0)
    return ApiRegistryResponse(
        id=api_id, method=method, url="http://api.test/data",
        description="test", pathParams=path_params, queryParams=query_params,
        body=body, createdAt=now,
    )


@pytest.fixture
def service():
    svc = ExtractService()
    svc.api_registry_client = MagicMock()
    svc.api_registry_client.get_api_definition = AsyncMock()
    svc.api_registry_client.test_api = AsyncMock()
    return svc


class TestExtractData:
    async def test_obtiene_api_def_y_testea_endpoint(self, service):
        service.api_registry_client.get_api_definition.return_value = _api_def()
        service.api_registry_client.test_api.return_value = {
            "statusCode": 200, "body": '[{"id": 1}]', "responseTimeMs": 150,
        }

        records = await service.extract_data(api_id=10)

        assert records == [{"id": 1}]
        service.api_registry_client.get_api_definition.assert_awaited_once_with(10)
        service.api_registry_client.test_api.assert_awaited_once_with(
            api_id=10, path_params="", query_params="", body="",
        )

    async def test_pasa_path_params_y_query_params(self, service):
        service.api_registry_client.get_api_definition.return_value = _api_def(
            path_params="page/1", query_params="limit=10",
        )
        service.api_registry_client.test_api.return_value = {
            "statusCode": 200, "body": "[]", "responseTimeMs": 50,
        }

        await service.extract_data(api_id=10)

        service.api_registry_client.test_api.assert_awaited_once_with(
            api_id=10, path_params="page/1", query_params="limit=10", body="",
        )

    async def test_envia_body_cuando_existe(self, service):
        service.api_registry_client.get_api_definition.return_value = _api_def(
            method="POST", body='{"key": "val"}',
        )
        service.api_registry_client.test_api.return_value = {
            "statusCode": 200, "body": '[{"result": "ok"}]', "responseTimeMs": 50,
        }

        await service.extract_data(api_id=10)

        service.api_registry_client.test_api.assert_awaited_once_with(
            api_id=10, path_params="", query_params="", body='{"key": "val"}',
        )

    async def test_status_no_200_lanza_runtime_error(self, service):
        service.api_registry_client.get_api_definition.return_value = _api_def()
        service.api_registry_client.test_api.return_value = {
            "statusCode": 500, "error": "Internal Server Error",
        }

        with pytest.raises(RuntimeError, match="API 10 returned status 500"):
            await service.extract_data(api_id=10)

    async def test_body_vacio_lanza_runtime_error(self, service):
        service.api_registry_client.get_api_definition.return_value = _api_def()
        service.api_registry_client.test_api.return_value = {
            "statusCode": 200, "body": "", "responseTimeMs": 50,
        }

        with pytest.raises(RuntimeError, match="API 10 returned empty body"):
            await service.extract_data(api_id=10)

    async def test_respuesta_es_lista_directa(self, service):
        service.api_registry_client.get_api_definition.return_value = _api_def()
        service.api_registry_client.test_api.return_value = {
            "statusCode": 200, "body": '[{"a": 1}, {"a": 2}]', "responseTimeMs": 50,
        }

        records = await service.extract_data(api_id=10)

        assert records == [{"a": 1}, {"a": 2}]

    async def test_respuesta_con_wrapper_data(self, service):
        service.api_registry_client.get_api_definition.return_value = _api_def()
        service.api_registry_client.test_api.return_value = {
            "statusCode": 200, "body": '{"data": [{"x": 1}, {"x": 2}]}', "responseTimeMs": 50,
        }

        records = await service.extract_data(api_id=10)

        assert records == [{"x": 1}, {"x": 2}]

    async def test_respuesta_objeto_plano_se_envuelve_en_lista(self, service):
        service.api_registry_client.get_api_definition.return_value = _api_def()
        service.api_registry_client.test_api.return_value = {
            "statusCode": 200, "body": '{"single": "record"}', "responseTimeMs": 50,
        }

        records = await service.extract_data(api_id=10)

        assert records == [{"single": "record"}]

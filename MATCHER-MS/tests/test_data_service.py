"""DataService: orquestación de matching, obtención de datos, refresh token, pandas."""
from unittest.mock import AsyncMock, MagicMock, patch
from datetime import datetime

import httpx
import pytest
from fastapi import HTTPException

from app.models import ConnectionResponse, ApiRegistryResponse, LlmConfigResponse, SchemaMatchResponse, EtlResponse
from app.services.dataService import DataService


NOW = datetime(2026, 6, 5, 12, 0, 0)


def _connection(*, api_a: str = "10", api_b: str = "20") -> ConnectionResponse:
    return ConnectionResponse(id=1, apiA=api_a, apiB=api_b, description="test",
                              status="ACTIVE", createdAt=NOW, updatedAt=NOW)


def _api_def(*, api_id: int = 10, method: str = "GET", url: str = "http://api.test",
             body: str | None = None, path_params: str | None = None,
             query_params: str | None = None, auth_api_id: int | None = None) -> ApiRegistryResponse:
    return ApiRegistryResponse(id=api_id, method=method, url=url, description="test",
                               body=body, pathParams=path_params, queryParams=query_params,
                               authApiId=auth_api_id, createdAt=NOW)


def _llm_config() -> LlmConfigResponse:
    return LlmConfigResponse(id=1, name="default", provider="openai", apiKey="sk-test",
                              baseUrl="http://llm:8000", modelName="gpt-4", isDefault=True,
                              status="ACTIVE", createdAt=NOW, updatedAt=NOW)


def _match() -> SchemaMatchResponse:
    return SchemaMatchResponse(id=1, integrationId=1, sourceField="name", targetField="nombre",
                                confidence=0.9, status="ACCEPTED", createdAt=NOW)


@pytest.fixture
def mock_clients(monkeypatch):
    """Reemplaza los clientes globales del módulo dataService con mocks."""
    from app.services import dataService as ds_module

    int_client = MagicMock()
    int_client.get_connection = AsyncMock()
    monkeypatch.setattr(ds_module, "integration_client", int_client)

    api_client = MagicMock()
    api_client.get_api_definition = AsyncMock()
    monkeypatch.setattr(ds_module, "api_registry_client", api_client)

    llm_client = MagicMock()
    llm_client.get_default_model = AsyncMock()
    monkeypatch.setattr(ds_module, "llm_config_client", llm_client)

    sm_client = MagicMock()
    sm_client.create_match = AsyncMock(return_value=_match())
    sm_client.create_matches_batch = AsyncMock(return_value=[_match()])
    monkeypatch.setattr(ds_module, "schema_match_client", sm_client)

    sd_client = MagicMock()
    sd_client.run_etl = AsyncMock(return_value=EtlResponse(
        integrationId=1, sourceApiId=10, targetApiId=20,
        totalRecords=2, transformedRecords=2, loadedRecords=2, errors=[],
    ))
    monkeypatch.setattr(ds_module, "save_data_client", sd_client)

    return MagicMock(int=int_client, api=api_client, llm=llm_client,
                     sm=sm_client, sd=sd_client)


class TestGetConnection:
    async def test_devuelve_conexion_cuando_existe(self, mock_clients):
        mock_clients.int.get_connection.return_value = _connection()
        svc = DataService()

        result = await svc.get_conection(1)

        assert result.id == 1
        assert result.apiA == "10"

    async def test_lanza_404_si_no_existe(self, mock_clients):
        mock_clients.int.get_connection.return_value = None
        svc = DataService()

        with pytest.raises(HTTPException) as exc:
            await svc.get_conection(999)
        assert exc.value.status_code == 404


class TestGetDefaultModel:
    async def test_devuelve_modelo_default(self, mock_clients):
        mock_clients.llm.get_default_model.return_value = _llm_config()
        svc = DataService()

        result = await svc.get_default_model()

        assert result["provider"] == "openai"
        assert result["modelName"] == "gpt-4"


class TestFetchApiData:
    async def test_consulta_api_y_devuelve_data(self, monkeypatch):
        from app.services import dataService as ds_module
        svc = DataService()
        monkeypatch.setattr(ds_module, "api_registry_client", MagicMock())
        ds_module.api_registry_client.get_api_definition = AsyncMock(return_value=_api_def())

        calls = []

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False
            async def post(self, url, json=None, **kw):
                calls.append(url)
                resp = MagicMock()
                resp.status_code = 200
                resp.json.return_value = {"statusCode": 200, "body": '[{"id": 1}]'}
                resp.raise_for_status = lambda: None
                return resp

        monkeypatch.setattr(ds_module.httpx, "AsyncClient", FakeClient)

        result = await svc.fetch_api_data(api_id=10)

        assert result["statusCode"] == 200

    async def test_401_dispara_refresh_token(self, monkeypatch):
        from app.services import dataService as ds_module
        svc = DataService()
        monkeypatch.setattr(ds_module, "api_registry_client", MagicMock())
        ds_module.api_registry_client.get_api_definition = AsyncMock(
            return_value=_api_def(auth_api_id=99),
        )

        call_count = [0]

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False

            async def request(self, method, url, **kw):
                call_count[0] += 1
                resp = MagicMock()
                resp.status_code = 200
                resp.json.return_value = {"access_token": "new"}
                resp.raise_for_status = lambda: None
                return resp

            async def post(self, url, json=None, **kw):
                call_count[0] += 1
                resp = MagicMock()
                if call_count[0] == 1:
                    resp.status_code = 401
                    resp.json.return_value = {"statusCode": 401, "body": ""}
                else:
                    resp.status_code = 200
                    resp.json.return_value = {"statusCode": 200, "body": '[{"ok": true}]'}
                resp.raise_for_status = lambda: None
                return resp

        monkeypatch.setattr(ds_module.httpx, "AsyncClient", FakeClient)

        result = await svc.fetch_api_data(api_id=10)

        assert result["statusCode"] == 200


class TestProcessDataWithPandas:
    async def test_devuelve_dataframe_valido(self, monkeypatch):
        from app.services import dataService as ds_module
        svc = DataService()
        monkeypatch.setattr(ds_module, "api_registry_client", MagicMock())
        ds_module.api_registry_client.get_api_definition = AsyncMock(return_value=_api_def())

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False
            async def post(self, url, json=None, **kw):
                resp = MagicMock()
                resp.status_code = 200
                resp.json.return_value = {"statusCode": 200, "body": '{"data": [{"name": "Ada", "age": 30}]}'}
                resp.raise_for_status = lambda: None
                return resp

        monkeypatch.setattr(ds_module.httpx, "AsyncClient", FakeClient)

        df = await svc.process_data_with_pandas(api_id=10)

        assert len(df) == 1
        assert list(df.columns) == ["name", "age"]

    async def test_status_no_200_lanza_exception(self, monkeypatch):
        from app.services import dataService as ds_module
        svc = DataService()
        monkeypatch.setattr(ds_module, "api_registry_client", MagicMock())
        ds_module.api_registry_client.get_api_definition = AsyncMock(return_value=_api_def())

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False
            async def post(self, url, json=None, **kw):
                resp = MagicMock()
                resp.status_code = 200
                resp.json.return_value = {"statusCode": 500, "body": ""}
                resp.raise_for_status = lambda: None
                return resp

        monkeypatch.setattr(ds_module.httpx, "AsyncClient", FakeClient)

        with pytest.raises(HTTPException) as exc:
            await svc.process_data_with_pandas(10)
        assert exc.value.status_code == 500

    async def test_body_vacio_lanza_exception(self, monkeypatch):
        from app.services import dataService as ds_module
        svc = DataService()
        monkeypatch.setattr(ds_module, "api_registry_client", MagicMock())
        ds_module.api_registry_client.get_api_definition = AsyncMock(return_value=_api_def())

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False
            async def post(self, url, json=None, **kw):
                resp = MagicMock()
                resp.status_code = 200
                resp.json.return_value = {"statusCode": 200, "body": ""}
                resp.raise_for_status = lambda: None
                return resp

        monkeypatch.setattr(ds_module.httpx, "AsyncClient", FakeClient)

        with pytest.raises(HTTPException) as exc:
            await svc.process_data_with_pandas(10)
        assert exc.value.status_code == 500


class TestRegisterSchemaMatch:
    async def test_register_single_match(self, mock_clients):
        svc = DataService()
        from app.models import SchemaMatchRequest
        req = SchemaMatchRequest(sourceField="name", targetField="nombre", confidence=0.9,
                                  integrationId=1, status="ACCEPTED")

        result = await svc.register_schema_match(req)

        assert result["sourceField"] == "name"
        mock_clients.sm.create_match.assert_awaited_once()

    async def test_register_batch_matches(self, mock_clients):
        svc = DataService()
        from app.models import SchemaMatchRequest
        reqs = [
            SchemaMatchRequest(sourceField="name", targetField="nombre", confidence=0.9,
                                integrationId=1, status="ACCEPTED"),
        ]

        results = await svc.register_schema_matches(reqs)

        assert len(results) == 1
        mock_clients.sm.create_matches_batch.assert_awaited_once()


class TestProcessIntegration:
    async def test_devuelve_todos_los_datos(self, mock_clients):
        mock_clients.int.get_connection.return_value = _connection()
        mock_clients.api.get_api_definition.side_effect = [
            _api_def(api_id=10),
            _api_def(api_id=20),
        ]
        mock_clients.llm.get_default_model.return_value = _llm_config()
        svc = DataService()

        with patch.object(svc, "fetch_api_data", AsyncMock(return_value={"body": "[]"})):
            result = await svc.process_integration(1)

        assert result["connection"]["apiA"] == "10"
        assert result["apiA"]["id"] == 10
        assert result["apiB"]["id"] == 20
        assert result["defaultModel"]["provider"] == "openai"
        assert result["dataA"] == {"body": "[]"}


class TestRunMatching:
    async def test_ejecuta_matching_y_etl(self, mock_clients):
        mock_clients.int.get_connection.return_value = _connection()
        mock_clients.api.get_api_definition.side_effect = [
            _api_def(api_id=10),
            _api_def(api_id=20),
        ]
        svc = DataService()

        with patch.object(svc, "fetch_api_data", AsyncMock(return_value={"body": "[]"})):
            with patch("app.services.dataService.MatchingService") as MockMatching:
                instance = MockMatching.return_value
                instance.match_and_register = AsyncMock(return_value={
                    "sourceFields": ["name"], "targetFields": ["nombre"],
                    "modelUsed": {"provider": "openai"}, "matches": [],
                })
                result = await svc.run_matching(1)

        assert result["sourceFields"] == ["name"]
        assert result["modelUsed"]["provider"] == "openai"
        assert "etl" in result
        mock_clients.sd.run_etl.assert_awaited_once_with(1)

    async def test_etl_falla_no_rompe_respuesta(self, mock_clients):
        mock_clients.int.get_connection.return_value = _connection()
        mock_clients.api.get_api_definition.side_effect = [
            _api_def(api_id=10),
            _api_def(api_id=20),
        ]
        mock_clients.sd.run_etl.side_effect = RuntimeError("ETL caído")
        svc = DataService()

        with patch.object(svc, "fetch_api_data", AsyncMock(return_value={"body": "[]"})):
            with patch("app.services.dataService.MatchingService") as MockMatching:
                instance = MockMatching.return_value
                instance.match_and_register = AsyncMock(return_value={
                    "sourceFields": [], "targetFields": [],
                    "modelUsed": {}, "matches": [],
                })
                result = await svc.run_matching(1)

        assert "error" in result["etl"]
        assert "RuntimeError" in result["etl"]["error"]

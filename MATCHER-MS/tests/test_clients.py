"""Clientes HTTP de MATCHER-MS: IntegrationClient, ApiRegistryClient, LlmConfigClient,
SchemaMatchClient, SemanticModelClient, SaveDataClient."""
from types import SimpleNamespace
from datetime import datetime

import httpx
import pytest

from app import models
from app.models import SchemaMatchRequest
from app.services import clients
from app.services.clients import (
    IntegrationClient, ApiRegistryClient, LlmConfigClient,
    SchemaMatchClient, SemanticModelClient, SaveDataClient,
)


NOW = datetime(2026, 6, 5, 12, 0, 0)


def _make_resp():
    status = _FakeAsyncClient.response_status
    data = _FakeAsyncClient.response_data or {}

    def _raise_for_status():
        if status >= 400:
            raise httpx.HTTPStatusError(f"HTTP {status}", request=None, response=resp)

    resp = SimpleNamespace(
        status_code=status,
        json=lambda: data,
        text="error" if status >= 400 else "",
    )
    resp.raise_for_status = _raise_for_status
    return resp


class _FakeAsyncClient:
    last_url = None
    last_json = None
    response_status = 200
    response_data = None

    def __init__(self, *a, **kw):
        pass

    async def __aenter__(self):
        return self

    async def __aexit__(self, *e):
        return False

    async def get(self, url, **kw):
        _FakeAsyncClient.last_url = url
        return _make_resp()

    async def post(self, url, json=None, **kw):
        _FakeAsyncClient.last_url = url
        _FakeAsyncClient.last_json = json
        return _make_resp()


@pytest.fixture(autouse=True)
def fake_httpx(monkeypatch):
    _FakeAsyncClient.last_url = None
    _FakeAsyncClient.last_json = None
    _FakeAsyncClient.response_status = 200
    _FakeAsyncClient.response_data = None
    monkeypatch.setattr(clients.httpx, "AsyncClient", _FakeAsyncClient)
    return _FakeAsyncClient


def _conn_data():
    return {"id": 1, "apiA": "10", "apiB": "20", "description": "test",
            "status": "ACTIVE", "createdAt": NOW.isoformat(), "updatedAt": NOW.isoformat()}


def _api_data():
    return {"id": 10, "method": "GET", "url": "http://api.test", "description": "test",
            "createdAt": NOW.isoformat()}


def _llm_data():
    return {"id": 1, "name": "default", "provider": "openai", "apiKey": "sk-test",
            "baseUrl": "http://llm:8000", "modelName": "gpt-4", "isDefault": True,
            "status": "ACTIVE", "createdAt": NOW.isoformat(), "updatedAt": NOW.isoformat()}


def _match_data():
    return {"id": 1, "integrationId": 1, "sourceField": "name", "targetField": "nombre",
            "confidence": 0.9, "status": "ACCEPTED", "createdAt": NOW.isoformat()}


def _etl_data():
    return {"integrationId": 1, "sourceApiId": 10, "targetApiId": 20,
            "totalRecords": 2, "transformedRecords": 2, "loadedRecords": 2, "errors": []}


class TestIntegrationClient:
    async def test_get_connection(self):
        _FakeAsyncClient.response_data = _conn_data()
        client = IntegrationClient(base_url="http://int:8082")

        conn = await client.get_connection(1)

        assert conn.id == 1
        assert conn.apiA == "10"
        assert _FakeAsyncClient.last_url == "http://int:8082/api/integrations/connections/1"


class TestApiRegistryClient:
    async def test_get_api_definition(self):
        _FakeAsyncClient.response_data = _api_data()
        client = ApiRegistryClient(base_url="http://reg:8083")

        api = await client.get_api_definition(10)

        assert api.id == 10
        assert _FakeAsyncClient.last_url == "http://reg:8083/api-registry/10"


class TestLlmConfigClient:
    async def test_get_default_model(self):
        _FakeAsyncClient.response_data = _llm_data()
        client = LlmConfigClient(base_url="http://llmcfg:8086")

        cfg = await client.get_default_model()

        assert cfg.provider == "openai"
        assert cfg.modelName == "gpt-4"
        assert _FakeAsyncClient.last_url == "http://llmcfg:8086/api/llm-configs/default"


class TestSchemaMatchClient:
    async def test_create_match(self):
        _FakeAsyncClient.response_data = _match_data()
        client = SchemaMatchClient(base_url="http://schema:8085")
        req = SchemaMatchRequest(sourceField="name", targetField="nombre", confidence=0.9,
                                  integrationId=1, status="ACCEPTED")

        resp = await client.create_match(req)

        assert resp.sourceField == "name"
        assert _FakeAsyncClient.last_url == "http://schema:8085/api/schema-matches"
        assert _FakeAsyncClient.last_json["sourceField"] == "name"

    async def test_create_matches_batch(self):
        _FakeAsyncClient.response_data = [_match_data()]
        client = SchemaMatchClient(base_url="http://schema:8085")
        reqs = [SchemaMatchRequest(sourceField="name", targetField="nombre", confidence=0.9,
                                    integrationId=1, status="ACCEPTED")]

        resps = await client.create_matches_batch(reqs)

        assert len(resps) == 1
        assert resps[0].sourceField == "name"
        assert _FakeAsyncClient.last_url == "http://schema:8085/api/schema-matches/batch"


class TestSemanticModelClient:
    async def test_predict_batch(self):
        _FakeAsyncClient.response_data = {"resultados": []}
        client = SemanticModelClient(base_url="http://model:8002")

        result = await client.predict_batch([["a", "b"]])

        assert _FakeAsyncClient.last_url == "http://model:8002/predict-batch"
        assert _FakeAsyncClient.last_json == {"pairs": [["a", "b"]]}

    async def test_predict_batch_raise_for_status(self):
        _FakeAsyncClient.response_status = 500
        _FakeAsyncClient.response_data = {"error": "fail"}
        client = SemanticModelClient(base_url="http://model:8002")

        with pytest.raises(Exception):
            await client.predict_batch([["a", "b"]])


class TestSaveDataClient:
    async def test_run_etl(self):
        _FakeAsyncClient.response_data = _etl_data()
        client = SaveDataClient(base_url="http://save:8001")

        result = await client.run_etl(1)

        assert result.integrationId == 1
        assert result.loadedRecords == 2
        assert _FakeAsyncClient.last_url == "http://save:8001/api/etl/run/1"

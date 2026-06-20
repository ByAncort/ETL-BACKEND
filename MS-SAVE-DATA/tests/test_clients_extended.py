"""Clientes HTTP de MS-SAVE-DATA: IntegrationClient, ApiRegistryClient, SchemaMatchClient."""
from types import SimpleNamespace
from datetime import datetime

import httpx
import pytest

from app.services import clients
from app.services.clients import IntegrationClient, ApiRegistryClient, SchemaMatchClient


class _FakeAsyncClient:
    last_url = None
    last_json = None
    last_params = None
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
        _FakeAsyncClient.last_params = kw
        return SimpleNamespace(
            status_code=_FakeAsyncClient.response_status,
            json=lambda: _FakeAsyncClient.response_data or {},
            raise_for_status=lambda: None,
        )

    async def post(self, url, json=None, **kw):
        _FakeAsyncClient.last_url = url
        _FakeAsyncClient.last_json = json
        return SimpleNamespace(
            status_code=_FakeAsyncClient.response_status,
            json=lambda: _FakeAsyncClient.response_data or {},
            raise_for_status=lambda: None,
        )


@pytest.fixture(autouse=True)
def fake_httpx(monkeypatch):
    _FakeAsyncClient.last_url = None
    _FakeAsyncClient.last_json = None
    _FakeAsyncClient.last_params = None
    _FakeAsyncClient.response_status = 200
    _FakeAsyncClient.response_data = None
    monkeypatch.setattr(clients.httpx, "AsyncClient", _FakeAsyncClient)
    return _FakeAsyncClient


NOW = datetime(2026, 6, 5, 12, 0, 0)


class TestIntegrationClient:
    async def test_get_connection(self):
        _FakeAsyncClient.response_data = {
            "id": 1, "apiA": "10", "apiB": "20", "description": "test",
            "status": "ACTIVE", "createdAt": NOW.isoformat(), "updatedAt": NOW.isoformat(),
        }
        client = IntegrationClient(base_url="http://int:8082")

        conn = await client.get_connection(1)

        assert conn.id == 1
        assert conn.apiA == "10"
        assert _FakeAsyncClient.last_url == "http://int:8082/api/integrations/connections/1"

    async def test_get_connection_raise_for_status(self):
        _FakeAsyncClient.response_status = 404
        client = IntegrationClient(base_url="http://int:8082")

        with pytest.raises(Exception):
            await client.get_connection(999)


class TestApiRegistryClient:
    async def test_get_api_definition(self):
        _FakeAsyncClient.response_data = {
            "id": 10, "method": "GET", "url": "http://api.test", "description": "test",
            "createdAt": NOW.isoformat(),
        }
        client = ApiRegistryClient(base_url="http://reg:8083")

        api = await client.get_api_definition(10)

        assert api.id == 10
        assert api.method == "GET"
        assert _FakeAsyncClient.last_url == "http://reg:8083/api-registry/10"

    async def test_test_api(self):
        _FakeAsyncClient.response_data = {"statusCode": 200, "body": "[]"}
        client = ApiRegistryClient(base_url="http://reg:8083")

        result = await client.test_api(api_id=10, path_params="p", query_params="q", body="{}")

        assert result["statusCode"] == 200
        assert _FakeAsyncClient.last_url == "http://reg:8083/api-registry/10/test"
        assert _FakeAsyncClient.last_json == {"pathParams": "p", "queryParams": "q", "body": "{}"}

    async def test_send_data(self):
        _FakeAsyncClient.response_data = {"statusCode": 200}
        client = ApiRegistryClient(base_url="http://reg:8083")

        result = await client.send_data(api_id=10, method="POST", body="{}")

        assert result["statusCode"] == 200


class TestSchemaMatchClient:
    async def test_get_matches_by_integration(self):
        _FakeAsyncClient.response_data = [{
            "id": 1, "integrationId": 5, "sourceField": "name", "targetField": "nombre",
            "confidence": 0.95, "status": "ACCEPTED", "createdAt": NOW.isoformat(),
        }]
        client = SchemaMatchClient(base_url="http://schema:8085")

        matches = await client.get_matches_by_integration(5)

        assert len(matches) == 1
        assert matches[0].sourceField == "name"
        assert _FakeAsyncClient.last_url == "http://schema:8085/api/schema-matches/integration/5"

    async def test_get_matches_by_integration_and_status(self):
        _FakeAsyncClient.response_data = []
        client = SchemaMatchClient(base_url="http://schema:8085")

        matches = await client.get_matches_by_integration_and_status(5, "ACCEPTED")

        assert matches == []
        assert _FakeAsyncClient.last_url == "http://schema:8085/api/schema-matches/integration/5/status/ACCEPTED"

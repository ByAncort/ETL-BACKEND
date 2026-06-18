"""Tema 4 — ETL async: pruebas del LogClient (notificar fin al integration-ms).

El proceso ETL "notifica fin" posteando al endpoint de logs de integration-ms.
Si la URL no coincide con el controller real (/api/integrations/logs), el log se
pierde silenciosamente porque send_log traga todas las excepciones.
"""
from types import SimpleNamespace

import httpx
import pytest

from app.services import clients
from app.services.clients import LogClient


class _FakeAsyncClient:
    """Sustituto de httpx.AsyncClient que captura la última petición POST."""

    last_url = None
    last_json = None
    raise_on_post = False

    def __init__(self, *args, **kwargs):
        pass

    async def __aenter__(self):
        return self

    async def __aexit__(self, *exc):
        return False

    async def post(self, url, json=None, **kwargs):
        _FakeAsyncClient.last_url = url
        _FakeAsyncClient.last_json = json
        if _FakeAsyncClient.raise_on_post:
            raise httpx.ConnectError("destino caído")
        return SimpleNamespace(status_code=200)


@pytest.fixture(autouse=True)
def fake_httpx(monkeypatch):
    _FakeAsyncClient.last_url = None
    _FakeAsyncClient.last_json = None
    _FakeAsyncClient.raise_on_post = False
    monkeypatch.setattr(clients.httpx, "AsyncClient", _FakeAsyncClient)
    return _FakeAsyncClient


class TestLogClient:
    async def test_postea_al_endpoint_real_de_integration_ms(self):
        client = LogClient(base_url="http://integration-ms:8082")

        await client.send_log({"message": "fin etl", "logLevel": "INFO"})

        # Debe apuntar al controller real: /api/integrations/logs (no /api/logs).
        assert _FakeAsyncClient.last_url == "http://integration-ms:8082/api/integrations/logs"
        assert _FakeAsyncClient.last_json == {"message": "fin etl", "logLevel": "INFO"}

    async def test_no_propaga_excepciones_de_red(self):
        # El proceso ETL no debe romperse si el log service no responde.
        _FakeAsyncClient.raise_on_post = True
        client = LogClient(base_url="http://integration-ms:8082")

        # No debe lanzar.
        await client.send_log({"message": "x", "logLevel": "ERROR"})

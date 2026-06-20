"""LoadService: fase LOAD del ETL — merge con template, envío individual/batch."""
from unittest.mock import AsyncMock, MagicMock, ANY
from datetime import datetime

import pytest

from app.models.schemas import ApiRegistryResponse
from app.services.loadService import LoadService


def _api_def(*, method: str = "POST", url: str = "http://target.test/api/data",
             body: str | None = '{"name": "", "age": 0}',
             auth_type: str | None = None, auth_value: str | None = None,
             auth_header: str | None = None, path_params: str | None = None) -> ApiRegistryResponse:
    now = datetime(2026, 6, 5, 12, 0, 0)
    return ApiRegistryResponse(
        id=20, method=method, url=url, description="target",
        body=body, pathParams=path_params, authType=auth_type,
        authValue=auth_value, authHeader=auth_header,
        createdAt=now,
    )


@pytest.fixture
def service():
    svc = LoadService()
    svc.api_registry_client = MagicMock()
    svc.api_registry_client.get_api_definition = AsyncMock()
    return svc


class TestBuildUrl:
    def test_sin_path_params(self):
        svc = LoadService()
        api = _api_def(path_params=None)

        url = svc._build_url(api)

        assert url == "http://target.test/api/data"

    def test_con_path_params(self):
        svc = LoadService()
        api = _api_def(path_params="items/42")

        url = svc._build_url(api)

        assert url == "http://target.test/api/data/items/42"


class TestBuildHeaders:
    def test_default_content_type(self):
        svc = LoadService()
        api = _api_def()

        headers = svc._build_headers(api)

        assert headers == {"Content-Type": "application/json"}

    def test_bearer_token(self):
        svc = LoadService()
        api = _api_def(auth_type="BEARER", auth_value="tok123")

        headers = svc._build_headers(api)

        assert headers["Authorization"] == "Bearer tok123"

    def test_api_key_con_header_personalizado(self):
        svc = LoadService()
        api = _api_def(auth_type="API_KEY", auth_value="key456", auth_header="X-Custom-Key")

        headers = svc._build_headers(api)

        assert headers["X-Custom-Key"] == "key456"

    def test_api_key_sin_header_usar_default(self):
        svc = LoadService()
        api = _api_def(auth_type="API_KEY", auth_value="key789", auth_header=None)

        headers = svc._build_headers(api)

        assert headers["X-API-Key"] == "key789"

    def test_auth_value_con_auth_header_explicito(self):
        svc = LoadService()
        api = _api_def(auth_value="token", auth_header="Authorization")

        headers = svc._build_headers(api)

        assert headers["Authorization"] == "token"


class TestMergeRecordWithTemplate:
    def test_merge_sustituye_campos_del_template(self):
        svc = LoadService()
        template = {"name": "", "age": 0}
        record = {"name": "Ada", "age": 30}

        merged = svc._merge_record_with_template(template, record)

        assert merged == {"name": "Ada", "age": 30}

    def test_merge_agrega_campos_nuevos_del_record(self):
        svc = LoadService()
        template = {"name": ""}
        record = {"name": "Ada", "extra": "val"}

        merged = svc._merge_record_with_template(template, record)

        assert merged == {"name": "Ada", "extra": "val"}

    def test_merge_ignora_none_en_record(self):
        svc = LoadService()
        template = {"name": "", "age": 0}
        record = {"name": "Ada", "age": None}

        merged = svc._merge_record_with_template(template, record)

        assert merged["name"] == "Ada"
        assert merged["age"] == 0

    def test_merge_elimina_strings_vacios_del_template_no_sobreescritos(self):
        svc = LoadService()
        template = {"keep": "filled", "remove": ""}
        record = {"keep": "filled"}

        merged = svc._merge_record_with_template(template, record)

        assert "remove" not in merged
        assert merged["keep"] == "filled"


class TestLoadData:
    async def test_lista_vacia_devuelve_sin_llamar_api(self, service):
        result = await service.load_data(target_api_id=20, records=[])

        assert result == {"loaded": 0, "errors": []}
        service.api_registry_client.get_api_definition.assert_not_awaited()

    async def test_carga_exitosa_todos_los_registros(self, service, monkeypatch):
        service.api_registry_client.get_api_definition.return_value = _api_def()

        sent_requests = []

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False

            async def request(self, method, url, headers, content):
                sent_requests.append({"method": method, "url": url, "body": content})
                resp = MagicMock()
                resp.status_code = 200
                return resp

        monkeypatch.setattr("app.services.loadService.httpx.AsyncClient", FakeClient)

        result = await service.load_data(target_api_id=20, records=[{"name": "Ada"}, {"name": "Bob"}])

        assert result["loaded"] == 2
        assert result["errors"] == []
        assert len(sent_requests) == 2

    async def test_registro_falla_individual_acumula_error(self, service, monkeypatch):
        service.api_registry_client.get_api_definition.return_value = _api_def()

        call_count = [0]

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False

            async def request(self, method, url, headers, content):
                call_count[0] += 1
                resp = MagicMock()
                resp.status_code = 400 if call_count[0] == 2 else 200
                resp.text = "Bad Request"
                return resp

        monkeypatch.setattr("app.services.loadService.httpx.AsyncClient", FakeClient)

        result = await service.load_data(target_api_id=20, records=[{"name": "Ada"}, {"name": "Bob"}])

        assert result["loaded"] == 1
        assert len(result["errors"]) == 1
        assert "HTTP 400" in result["errors"][0]

    async def test_error_de_red_individual_se_captura(self, service, monkeypatch):
        service.api_registry_client.get_api_definition.return_value = _api_def()

        call_count = [0]

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False

            async def request(self, method, url, headers, content):
                call_count[0] += 1
                if call_count[0] == 2:
                    raise ConnectionError("timeout")
                resp = MagicMock()
                resp.status_code = 200
                return resp

        monkeypatch.setattr("app.services.loadService.httpx.AsyncClient", FakeClient)

        result = await service.load_data(target_api_id=20, records=[{"name": "Ada"}, {"name": "Bob"}])

        assert result["loaded"] == 1
        assert len(result["errors"]) == 1
        assert "timeout" in result["errors"][0]


class TestLoadBatch:
    async def test_lista_vacia_devuelve_sin_cargar(self, service):
        service.api_registry_client.get_api_definition.return_value = _api_def()

        result = await service.load_batch(target_api_id=20, records=[])

        assert result == {"loaded": 0, "errors": []}

    async def test_batch_exitoso_devuelve_todos_cargados(self, service, monkeypatch):
        service.api_registry_client.get_api_definition.return_value = _api_def()

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False

            async def request(self, method, url, headers, content):
                resp = MagicMock()
                resp.status_code = 201
                resp.text = "ok"
                resp.elapsed.total_seconds.return_value = 0.5
                return resp

        monkeypatch.setattr("app.services.loadService.httpx.AsyncClient", FakeClient)

        result = await service.load_batch(target_api_id=20, records=[{"name": "Ada"}, {"name": "Bob"}])

        assert result["loaded"] == 2
        assert result["errors"] == []

    async def test_batch_falla_con_http_error(self, service, monkeypatch):
        service.api_registry_client.get_api_definition.return_value = _api_def()

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False

            async def request(self, method, url, headers, content):
                resp = MagicMock()
                resp.status_code = 500
                resp.text = "Server Error"
                resp.elapsed.total_seconds.return_value = 1.0
                return resp

        monkeypatch.setattr("app.services.loadService.httpx.AsyncClient", FakeClient)

        result = await service.load_batch(target_api_id=20, records=[{"name": "Ada"}])

        assert result["loaded"] == 0
        assert len(result["errors"]) == 1
        assert "HTTP 500" in result["errors"][0]

    async def test_timeout_se_captura(self, service, monkeypatch):
        service.api_registry_client.get_api_definition.return_value = _api_def()

        class FakeClient:
            def __init__(self, *a, **kw): pass
            async def __aenter__(self): return self
            async def __aexit__(self, *e): return False

            async def request(self, method, url, headers, content):
                import httpx
                raise httpx.TimeoutException("timeout after 120s")

        monkeypatch.setattr("app.services.loadService.httpx.AsyncClient", FakeClient)

        result = await service.load_batch(target_api_id=20, records=[{"name": "Ada"}])

        assert result["loaded"] == 0
        assert any("Timeout" in e for e in result["errors"])

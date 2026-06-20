"""LlmService: cliente para API compatible con OpenAI /chat/completions."""
from types import SimpleNamespace
import httpx
import pytest
from fastapi import HTTPException

from app.services import llmService
from app.services.llmService import LlmService


class _FakeAsyncClient:
    last_url = None
    last_json = None
    response = None

    def __init__(self, *a, **kw):
        pass

    async def __aenter__(self):
        return self

    async def __aexit__(self, *e):
        return False

    async def post(self, url, headers=None, json=None, **kw):
        _FakeAsyncClient.last_url = url
        _FakeAsyncClient.last_json = json
        return _FakeAsyncClient.response


def _resp(status_code=200, payload=None, text=""):
    return SimpleNamespace(
        status_code=status_code,
        json=lambda: payload or {},
        raise_for_status=lambda: None,
        text=text,
    )


@pytest.fixture(autouse=True)
def fake_httpx(monkeypatch):
    _FakeAsyncClient.response = _resp(payload={
        "choices": [{"message": {"content": '[{"sourceField": "a", "targetField": "b", "confidence": 0.9}]'}}],
    })
    _FakeAsyncClient.last_url = None
    _FakeAsyncClient.last_json = None
    monkeypatch.setattr(llmService.httpx, "AsyncClient", _FakeAsyncClient)
    return _FakeAsyncClient


class TestBuildUrl:
    def test_ya_incluye_chat_completions(self):
        svc = LlmService(base_url="http://llm:8000/v1/chat/completions", api_key="k", model_name="gpt-4")

        url = svc._build_url()

        assert url == "http://llm:8000/v1/chat/completions"

    def test_agrega_chat_completions(self):
        svc = LlmService(base_url="http://llm:8000/v1", api_key="k", model_name="gpt-4")

        url = svc._build_url()

        assert url == "http://llm:8000/v1/chat/completions"


class TestMatchFields:
    async def test_devuelve_matches_cuando_llm_responde_ok(self):
        svc = LlmService(base_url="http://llm:8000", api_key="sk-test", model_name="gpt-4")

        matches = await svc.match_fields(["a"], ["b"])

        assert len(matches) == 1
        assert matches[0]["sourceField"] == "a"
        assert matches[0]["targetField"] == "b"
        assert matches[0]["confidence"] == 0.9

    async def test_envia_payload_correcto(self):
        svc = LlmService(base_url="http://llm:8000", api_key="sk-test", model_name="gpt-4")

        await svc.match_fields(["email"], ["correo"])

        assert _FakeAsyncClient.last_json["model"] == "gpt-4"
        assert len(_FakeAsyncClient.last_json["messages"]) == 2
        assert _FakeAsyncClient.last_json["messages"][0]["role"] == "system"
        assert _FakeAsyncClient.last_json["messages"][1]["role"] == "user"

    async def test_incluye_source_y_target_fields_en_user_prompt(self):
        svc = LlmService(base_url="http://llm:8000", api_key="sk-test", model_name="gpt-4")

        await svc.match_fields(["name", "email"], ["nombre", "correo"])

        content = _FakeAsyncClient.last_json["messages"][1]["content"]
        assert '"name"' in content
        assert '"email"' in content
        assert '"nombre"' in content
        assert '"correo"' in content

    async def test_status_400_lanza_502(self):
        _FakeAsyncClient.response = _resp(status_code=400, text="bad request")
        svc = LlmService(base_url="http://llm:8000", api_key="sk-test", model_name="gpt-4")

        with pytest.raises(HTTPException) as exc:
            await svc.match_fields(["a"], ["b"])
        assert exc.value.status_code == 502
        assert "400" in exc.value.detail

    async def test_respuesta_sin_choices_lanza_502(self):
        _FakeAsyncClient.response = _resp(payload={})
        svc = LlmService(base_url="http://llm:8000", api_key="sk-test", model_name="gpt-4")

        with pytest.raises(HTTPException) as exc:
            await svc.match_fields(["a"], ["b"])
        assert exc.value.status_code == 502
        assert "sin choices" in exc.value.detail

    async def test_content_vacio_lanza_502(self):
        _FakeAsyncClient.response = _resp(payload={"choices": [{"message": {"content": ""}}]})
        svc = LlmService(base_url="http://llm:8000", api_key="sk-test", model_name="gpt-4")

        with pytest.raises(HTTPException) as exc:
            await svc.match_fields(["a"], ["b"])
        assert exc.value.status_code == 502
        assert "vacío" in exc.value.detail

    async def test_respuesta_con_code_block_json(self):
        _FakeAsyncClient.response = _resp(payload={"choices": [{"message": {
            "content": "```json\n[{\"sourceField\": \"a\", \"targetField\": \"b\", \"confidence\": 0.9}]\n```",
        }}]})
        svc = LlmService(base_url="http://llm:8000", api_key="sk-test", model_name="gpt-4")

        matches = await svc.match_fields(["a"], ["b"])

        assert len(matches) == 1
        assert matches[0]["sourceField"] == "a"

    async def test_respuesta_invalida_no_json_lanza_502(self):
        _FakeAsyncClient.response = _resp(payload={"choices": [{"message": {"content": "no es json"}}]})
        svc = LlmService(base_url="http://llm:8000", api_key="sk-test", model_name="gpt-4")

        with pytest.raises(HTTPException) as exc:
            await svc.match_fields(["a"], ["b"])
        assert exc.value.status_code == 502
        assert "JSON" in exc.value.detail

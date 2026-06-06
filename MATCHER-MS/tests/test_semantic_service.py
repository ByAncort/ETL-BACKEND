"""Tema 6 — Mapeo Semántico: SemanticService (cliente del modelo ML ensemble).

Genera el producto cartesiano de campos, consulta al modelo semántico y
conserva solo los pares marcados como similares, mapeando score→confidence.
"""
from types import SimpleNamespace

import httpx
import pytest
from fastapi import HTTPException

from app.services import semanticService
from app.services.semanticService import SemanticService


class _FakeAsyncClient:
    """Sustituto de httpx.AsyncClient que devuelve una respuesta predefinida."""

    response = None
    last_url = None
    last_json = None

    def __init__(self, *args, **kwargs):
        pass

    async def __aenter__(self):
        return self

    async def __aexit__(self, *exc):
        return False

    async def post(self, url, json=None, **kwargs):
        _FakeAsyncClient.last_url = url
        _FakeAsyncClient.last_json = json
        return _FakeAsyncClient.response


def _resp(status_code=200, payload=None, text=""):
    return SimpleNamespace(
        status_code=status_code,
        json=lambda: payload or {},
        text=text,
    )


@pytest.fixture(autouse=True)
def fake_httpx(monkeypatch):
    _FakeAsyncClient.response = _resp()
    _FakeAsyncClient.last_url = None
    _FakeAsyncClient.last_json = None
    monkeypatch.setattr(semanticService.httpx, "AsyncClient", _FakeAsyncClient)
    return _FakeAsyncClient


class TestMatchFields:
    async def test_genera_pares_cartesianos_y_consulta_predict_batch(self):
        _FakeAsyncClient.response = _resp(payload={"resultados": []})
        service = SemanticService(base_url="http://model:8000")

        await service.match_fields(["a", "b"], ["x", "y"])

        assert _FakeAsyncClient.last_url == "http://model:8000/predict-batch"
        assert _FakeAsyncClient.last_json == {"pairs": [["a", "x"], ["a", "y"], ["b", "x"], ["b", "y"]]}

    async def test_conserva_solo_similares_y_mapea_confidence(self):
        _FakeAsyncClient.response = _resp(payload={"resultados": [
            {"campo1": "email", "campo2": "correo", "son_similares": True, "score_final": 0.93},
            {"campo1": "email", "campo2": "edad", "son_similares": False, "score_final": 0.10},
        ]})
        service = SemanticService(base_url="http://model:8000")

        matches = await service.match_fields(["email"], ["correo", "edad"])

        assert matches == [{"sourceField": "email", "targetField": "correo", "confidence": 0.93}]

    async def test_listas_vacias_devuelven_sin_llamar_al_modelo(self):
        service = SemanticService(base_url="http://model:8000")

        assert await service.match_fields([], ["x"]) == []
        assert await service.match_fields(["a"], []) == []
        assert _FakeAsyncClient.last_url is None

    async def test_status_no_200_lanza_502(self):
        _FakeAsyncClient.response = _resp(status_code=500, text="modelo caído")
        service = SemanticService(base_url="http://model:8000")

        with pytest.raises(HTTPException) as exc:
            await service.match_fields(["a"], ["b"])
        assert exc.value.status_code == 502

    async def test_score_ausente_usa_default(self):
        _FakeAsyncClient.response = _resp(payload={"resultados": [
            {"campo1": "a", "campo2": "b", "son_similares": True},
        ]})
        service = SemanticService(base_url="http://model:8000")

        matches = await service.match_fields(["a"], ["b"])

        assert matches == [{"sourceField": "a", "targetField": "b", "confidence": 0.5}]


class TestBaseUrl:
    async def test_normaliza_barra_final(self):
        service = SemanticService(base_url="http://model:8000/")
        _FakeAsyncClient.response = _resp(payload={"resultados": []})

        await service.match_fields(["a"], ["b"])

        assert _FakeAsyncClient.last_url == "http://model:8000/predict-batch"

"""Tema 5 — Integración E2E: pipeline de matching (MatchingService.match_and_register).

Flujo: extraer campos de ambas APIs → obtener matches del LLM (o caer al modelo
semántico si el LLM no está disponible) → registrar cada match en schema-matching-ms.
Todos los colaboradores (clientes + modelos) se mockean.
"""
from datetime import datetime
from unittest.mock import AsyncMock, MagicMock

import pytest

from app.models import LlmConfigResponse, SchemaMatchResponse
from app.services import matchingService
from app.services.matchingService import MatchingService
from tests.conftest import make_api_def


DATA_A = {"body": '[{"name": "x", "email": "y"}]'}
DATA_B = {"body": '[{"nombre": "z", "correo": "w"}]'}
RAW = [{"sourceField": "name", "targetField": "nombre", "confidence": 0.9}]


def _llm_config(api_key="sk-test"):
    return LlmConfigResponse(
        id=1, name="default", provider="openai", apiKey=api_key,
        baseUrl="http://llm:8000", modelName="gpt-4", isDefault=True,
        status="ACTIVE", createdAt=datetime(2026, 6, 5), updatedAt=datetime(2026, 6, 5),
    )


def _match_resp(req):
    return SchemaMatchResponse(
        id=99, integrationId=req.integrationId, sourceField=req.sourceField,
        targetField=req.targetField, confidence=req.confidence,
        status=req.status or "ACCEPTED", createdAt=datetime(2026, 6, 5),
    )


@pytest.fixture
def patched(monkeypatch):
    """Mockea los clientes y los modelos del módulo matchingService."""
    llm_cfg = MagicMock()
    llm_cfg.get_default_model = AsyncMock()
    monkeypatch.setattr(matchingService, "llm_config_client", llm_cfg)

    smc = MagicMock()
    smc.create_match = AsyncMock(side_effect=lambda req: _match_resp(req))
    monkeypatch.setattr(matchingService, "schema_match_client", smc)

    llm_instance = MagicMock()
    llm_instance.match_fields = AsyncMock(return_value=RAW)
    llm_class = MagicMock(return_value=llm_instance)
    monkeypatch.setattr(matchingService, "LlmService", llm_class)

    sem_instance = MagicMock()
    sem_instance.match_fields = AsyncMock(return_value=RAW)
    sem_class = MagicMock(return_value=sem_instance)
    monkeypatch.setattr(matchingService, "SemanticService", sem_class)

    return MagicMock(llm_cfg=llm_cfg, smc=smc, llm=llm_instance, llm_class=llm_class,
                     sem=sem_instance, sem_class=sem_class)


async def _run(patched):
    service = MatchingService()
    return await service.match_and_register(
        integration_id=42,
        data_a=DATA_A, data_b=DATA_B,
        api_a_def=make_api_def(method="GET"),
        api_b_def=make_api_def(method="GET", api_id=2),
    )


class TestLlmPath:
    async def test_usa_llm_cuando_hay_config_valida(self, patched):
        patched.llm_cfg.get_default_model.return_value = _llm_config()

        result = await _run(patched)

        assert result["sourceFields"] == ["name", "email"]
        assert result["targetFields"] == ["nombre", "correo"]
        assert result["modelUsed"]["provider"] == "openai"
        patched.llm.match_fields.assert_awaited_once()
        patched.sem.match_fields.assert_not_awaited()
        assert len(result["matches"]) == 1


class TestSemanticFallback:
    async def test_cae_a_semantico_si_llm_falla(self, patched):
        patched.llm_cfg.get_default_model.side_effect = RuntimeError("LLM caído")

        result = await _run(patched)

        assert result["modelUsed"]["provider"] == "semantic-model"
        patched.sem.match_fields.assert_awaited_once()
        assert len(result["matches"]) == 1

    async def test_cae_a_semantico_si_config_sin_apikey(self, patched):
        patched.llm_cfg.get_default_model.return_value = _llm_config(api_key="")

        result = await _run(patched)

        assert result["modelUsed"]["provider"] == "semantic-model"
        patched.sem.match_fields.assert_awaited_once()


class TestRegistro:
    async def test_registra_cada_match_como_accepted_con_integration_id(self, patched):
        patched.llm_cfg.get_default_model.return_value = _llm_config()

        await _run(patched)

        patched.smc.create_match.assert_awaited_once()
        req = patched.smc.create_match.await_args.args[0]
        assert req.sourceField == "name"
        assert req.targetField == "nombre"
        assert req.status == "ACCEPTED"
        assert req.integrationId == 42
        assert req.confidence == 0.9

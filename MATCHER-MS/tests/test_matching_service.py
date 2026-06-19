"""Tema 6 — Mapeo Semántico: extracción de campos a mapear (MatchingService).

Antes de matchear, el servicio extrae la lista de campos de cada API:
- de APIs de escritura (POST/PUT/PATCH) → del body schema declarado
- de APIs de lectura (GET) → de la respuesta de prueba
"""
import pytest
from fastapi import HTTPException

from app.services.matchingService import MatchingService
from tests.conftest import make_api_def


@pytest.fixture
def service():
    return MatchingService()


class TestExtractFromBodySchema:
    def test_extrae_claves_del_body_schema(self, service):
        api = make_api_def(method="POST", body='{"nombre": "", "correo": "", "edad": 0}')

        fields = service._extract_fields({}, api)

        assert fields == ["nombre", "correo", "edad"]

    def test_sin_body_schema_lanza_502(self, service):
        api = make_api_def(method="POST", body=None)

        with pytest.raises(HTTPException) as exc:
            service._extract_fields({}, api)
        assert exc.value.status_code == 502

    def test_body_schema_invalido_lanza_502(self, service):
        api = make_api_def(method="PUT", body="{no es json}")

        with pytest.raises(HTTPException) as exc:
            service._extract_fields({}, api)
        assert exc.value.status_code == 502


class TestExtractFromResponse:
    def test_respuesta_lista_de_objetos(self, service):
        api = make_api_def(method="GET")
        data = {"body": '[{"id": 1, "name": "Ada"}, {"id": 2, "name": "Bob"}]'}

        fields = service._extract_fields(data, api)

        assert fields == ["id", "name"]

    def test_respuesta_con_envoltura_data(self, service):
        api = make_api_def(method="GET")
        data = {"body": '{"data": [{"sku": "x", "price": 10}]}'}

        fields = service._extract_fields(data, api)

        assert fields == ["sku", "price"]

    def test_respuesta_objeto_plano(self, service):
        api = make_api_def(method="GET")
        data = {"body": '{"campoA": 1, "campoB": 2}'}

        fields = service._extract_fields(data, api)

        assert fields == ["campoA", "campoB"]

    def test_body_vacio_lanza_502(self, service):
        api = make_api_def(method="GET")

        with pytest.raises(HTTPException) as exc:
            service._extract_fields({"body": ""}, api)
        assert exc.value.status_code == 502

    def test_body_no_parseable_lanza_502(self, service):
        api = make_api_def(method="GET")

        with pytest.raises(HTTPException) as exc:
            service._extract_fields({"body": "<<no json>>"}, api)
        assert exc.value.status_code == 502

    def test_lista_data_vacia_lanza_502(self, service):
        api = make_api_def(method="GET")

        with pytest.raises(HTTPException) as exc:
            service._extract_fields({"body": '{"data": []}'}, api)
        assert exc.value.status_code == 502

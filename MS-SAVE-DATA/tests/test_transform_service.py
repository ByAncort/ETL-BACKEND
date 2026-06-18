"""Tema 4 — ETL async: pruebas de la fase TRANSFORM (lógica pura, sin red).

Verifican el mapeo de campos origen→destino y las transformaciones soportadas.
"""
from app.services.transformService import TransformService
from tests.conftest import make_match


class TestTransformRecord:
    def test_mapea_campos_segun_matches(self):
        service = TransformService()
        matches = [
            make_match(source_field="name", target_field="nombre"),
            make_match(source_field="email", target_field="correo"),
        ]
        record = {"name": "Ada", "email": "ada@example.com", "extra": "ignorado"}

        result = service.transform_record(record, matches)

        assert result == {"nombre": "Ada", "correo": "ada@example.com"}

    def test_campo_origen_ausente_queda_none(self):
        service = TransformService()
        matches = [make_match(source_field="phone", target_field="telefono")]

        result = service.transform_record({"name": "Ada"}, matches)

        assert result == {"telefono": None}

    def test_sin_matches_devuelve_dict_vacio(self):
        service = TransformService()

        result = service.transform_record({"name": "Ada"}, [])

        assert result == {}


class TestApplyTransformation:
    def test_int_convierte_string_numerico(self):
        service = TransformService()
        matches = [make_match(source_field="age", target_field="edad", transformation="int()")]

        result = service.transform_record({"age": "42"}, matches)

        assert result == {"edad": 42}

    def test_int_desde_float_trunca(self):
        service = TransformService()
        matches = [make_match(source_field="age", target_field="edad", transformation="int()")]

        result = service.transform_record({"age": "42.9"}, matches)

        assert result == {"edad": 42}

    def test_int_invalido_deja_valor_original(self):
        service = TransformService()
        matches = [make_match(source_field="age", target_field="edad", transformation="int()")]

        result = service.transform_record({"age": "no-numero"}, matches)

        assert result == {"edad": "no-numero"}

    def test_float_convierte(self):
        service = TransformService()
        matches = [make_match(source_field="price", target_field="precio", transformation="float()")]

        result = service.transform_record({"price": "9.99"}, matches)

        assert result == {"precio": 9.99}

    def test_upper_y_lower(self):
        service = TransformService()
        matches = [
            make_match(source_field="a", target_field="mayus", transformation="upper()"),
            make_match(source_field="b", target_field="minus", transformation="lower()"),
        ]

        result = service.transform_record({"a": "hola", "b": "CHAU"}, matches)

        assert result == {"mayus": "HOLA", "minus": "chau"}

    def test_bool_desde_string(self):
        service = TransformService()
        matches = [make_match(source_field="flag", target_field="activo", transformation="bool()")]

        assert service.transform_record({"flag": "true"}, matches) == {"activo": True}
        assert service.transform_record({"flag": "no"}, matches) == {"activo": False}

    def test_transformacion_desconocida_deja_valor_original(self):
        service = TransformService()
        matches = [make_match(source_field="x", target_field="y", transformation="explota()")]

        result = service.transform_record({"x": "valor"}, matches)

        assert result == {"y": "valor"}


class TestTransformData:
    def test_transforma_todos_los_registros(self):
        service = TransformService()
        matches = [make_match(source_field="name", target_field="nombre")]
        records = [{"name": "Ada"}, {"name": "Linus"}, {"name": "Grace"}]

        result = service.transform_data(records, matches)

        assert result == [{"nombre": "Ada"}, {"nombre": "Linus"}, {"nombre": "Grace"}]

    def test_lista_vacia_devuelve_lista_vacia(self):
        service = TransformService()
        matches = [make_match(source_field="name", target_field="nombre")]

        assert service.transform_data([], matches) == []

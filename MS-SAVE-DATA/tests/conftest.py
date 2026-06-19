"""Fixtures y factories compartidas para los tests de MS-SAVE-DATA."""
from datetime import datetime

import pytest

from app.models.schemas import ConnectionResponse, SchemaMatchResponse


def make_connection(
    *,
    connection_id: int = 1,
    api_a: str = "10",
    api_b: str = "20",
    status: str = "ACTIVE",
) -> ConnectionResponse:
    """Crea una ConnectionResponse válida para los tests."""
    now = datetime(2026, 6, 5, 12, 0, 0)
    return ConnectionResponse(
        id=connection_id,
        apiA=api_a,
        apiB=api_b,
        description="conexión de prueba",
        status=status,
        createdAt=now,
        updatedAt=now,
    )


def make_match(
    *,
    match_id: int = 1,
    source_field: str = "name",
    target_field: str = "nombre",
    confidence: float = 0.99,
    status: str = "ACCEPTED",
    transformation: str | None = None,
) -> SchemaMatchResponse:
    """Crea un SchemaMatchResponse válido para los tests."""
    return SchemaMatchResponse(
        id=match_id,
        integrationId=1,
        sourceField=source_field,
        targetField=target_field,
        confidence=confidence,
        status=status,
        transformation=transformation,
        createdAt=datetime(2026, 6, 5, 12, 0, 0),
    )


@pytest.fixture
def connection():
    return make_connection()


@pytest.fixture
def accepted_matches():
    return [
        make_match(match_id=1, source_field="name", target_field="nombre"),
        make_match(match_id=2, source_field="age", target_field="edad", transformation="int()"),
    ]

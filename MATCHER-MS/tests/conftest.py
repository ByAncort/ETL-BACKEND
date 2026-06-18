"""Fixtures y factories compartidas para los tests de MATCHER-MS."""
from datetime import datetime

from app.models import ApiRegistryResponse


def make_api_def(
    *,
    api_id: int = 1,
    method: str = "GET",
    url: str = "http://api.test/recurso",
    body: str | None = None,
) -> ApiRegistryResponse:
    """Crea una ApiRegistryResponse mínima válida para los tests."""
    return ApiRegistryResponse(
        id=api_id,
        method=method,
        url=url,
        description="api de prueba",
        body=body,
        createdAt=datetime(2026, 6, 5, 12, 0, 0),
    )

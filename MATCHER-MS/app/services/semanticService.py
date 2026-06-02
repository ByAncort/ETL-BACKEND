import os
import httpx
from fastapi import HTTPException

SEMANTIC_MODEL_URL_ENV = "SEMANTIC_MODEL_URL"
DEFAULT_SEMANTIC_MODEL_URL = "http://localhost:8000"


class SemanticService:

    def __init__(self, base_url: str = None):
        self.base_url = (base_url or os.getenv(SEMANTIC_MODEL_URL_ENV, DEFAULT_SEMANTIC_MODEL_URL)).rstrip("/")

    async def match_fields(self, source_fields: list[str], target_fields: list[str]) -> list[dict]:
        if not source_fields or not target_fields:
            return []

        pairs = [[s, t] for s in source_fields for t in target_fields]
        url = f"{self.base_url}/predict-batch"

        async with httpx.AsyncClient(timeout=120.0) as client:
            response = await client.post(url, json={"pairs": pairs})
            if response.status_code != 200:
                raise HTTPException(
                    status_code=502,
                    detail=f"Semantic model rechazó solicitud ({response.status_code}): {response.text[:500]}",
                )
            data = response.json()

        resultados = data.get("resultados", [])
        matches = []
        for r in resultados:
            if r.get("son_similares", False):
                matches.append({
                    "sourceField": r["campo1"],
                    "targetField": r["campo2"],
                    "confidence": float(r.get("score_final", 0.5)),
                })

        return matches

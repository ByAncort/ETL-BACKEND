import json
import httpx
from fastapi import HTTPException
from starlette import status

SYSTEM_PROMPT = """
Eres un experto en mapeo de esquemas entre APIs.
Dado un listado de campos fuente y un listado de campos objetivo,
encuentra las correspondencias más probables entre ellos.
Responde ÚNICAMENTE con un JSON array de objetos con la siguiente estructura:
[
  {
    "sourceField": "nombre_campo_fuente",
    "targetField": "nombre_campo_objetivo",
    "confidence": 0.95,
    "transformation": "expresión opcional si requiere transformación"
  }
]
confidence debe ser un valor entre 0 y 1.
transformation es opcional, solo si el campo necesita conversión (ej. "int(value)", "str(value)").
"""

USER_PROMPT_TEMPLATE = """
Campos fuente (API A):
{source_fields}

Campos objetivo (API B):
{target_fields}
"""


class LlmService:
    def __init__(self, base_url: str, api_key: str, model_name: str):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.model_name = model_name

    def _build_url(self) -> str:
        base = self.base_url.rstrip("/")
        if "/chat/completions" in base:
            return base
        return f"{base}/chat/completions"

    async def match_fields(self, source_fields: list[str], target_fields: list[str]) -> list[dict]:
        url = self._build_url()
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": self.model_name,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": USER_PROMPT_TEMPLATE.format(
                    source_fields=json.dumps(source_fields, indent=2),
                    target_fields=json.dumps(target_fields, indent=2),
                )},
            ],
            "temperature": 0.1,
        }

        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(url, headers=headers, json=payload)
            if response.status_code == 400:
                detail = response.text[:500]
                raise HTTPException(
                    status_code=502,
                    detail=f"LLM rechazó la solicitud (400). URL: {url} | modelo: {self.model_name} | respuesta: {detail}",
                )
            response.raise_for_status()
            data = response.json()

        choices = data.get("choices")
        if not choices:
            raise HTTPException(status_code=502, detail=f"Respuesta del LLM sin choices: {data}")

        content = choices[0].get("message", {}).get("content", "")
        if not content:
            raise HTTPException(status_code=502, detail="El LLM devolvió content vacío")

        content = content.strip()
        if content.startswith("```"):
            content = content.split("\n", 1)[1]
            content = content.rsplit("```", 1)[0]

        try:
            matches = json.loads(content)
        except json.JSONDecodeError as e:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail=f"El LLM devolvió una respuesta que no es JSON válido: {e}. Respuesta: {content[:500]}",
            )

        return matches

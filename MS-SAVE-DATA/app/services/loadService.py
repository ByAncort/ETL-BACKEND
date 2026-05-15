import json
import httpx
from app.services.clients import ApiRegistryClient


class LoadService:
    def __init__(self):
        self.api_registry_client = ApiRegistryClient()

    async def load_data(self, target_api_id: int, records: list[dict]) -> dict:
        if not records:
            return {"loaded": 0, "errors": []}

        target_def = await self.api_registry_client.get_api_definition(target_api_id)

        headers = self._build_headers(target_def)
        url = target_def.url

        errors = []
        loaded_count = 0

        async with httpx.AsyncClient(timeout=60.0) as client:
            for record in records:
                try:
                    body = json.dumps(record)
                    response = await client.request(
                        method=target_def.method,
                        url=url,
                        headers=headers,
                        content=body,
                    )

                    if response.status_code < 400:
                        loaded_count += 1
                    else:
                        errors.append(
                            f"HTTP {response.status_code} para registro: {response.text[:200]}"
                        )

                except Exception as e:
                    errors.append(f"Error cargando registro: {str(e)}")

        return {
            "loaded": loaded_count,
            "errors": errors,
        }

    async def load_batch(self, target_api_id: int, records: list[dict]) -> dict:
        if not records:
            return {"loaded": 0, "errors": []}

        target_def = await self.api_registry_client.get_api_definition(target_api_id)
        headers = self._build_headers(target_def)
        url = target_def.url

        body = json.dumps(records)

        async with httpx.AsyncClient(timeout=120.0) as client:
            try:
                response = await client.request(
                    method=target_def.method,
                    url=url,
                    headers=headers,
                    content=body,
                )

                if response.status_code < 400:
                    return {"loaded": len(records), "errors": []}
                else:
                    return {
                        "loaded": 0,
                        "errors": [
                            f"HTTP {response.status_code}: {response.text[:500]}"
                        ],
                    }

            except Exception as e:
                return {"loaded": 0, "errors": [f"Error: {str(e)}"]}

    def _build_headers(self, api_def) -> dict:
        headers = {"Content-Type": "application/json"}

        if api_def.authValue and api_def.authHeader:
            headers[api_def.authHeader] = api_def.authValue
        elif api_def.authValue and api_def.authType == "BEARER":
            headers["Authorization"] = f"Bearer {api_def.authValue}"
        elif api_def.authValue and api_def.authType == "API_KEY":
            header_name = api_def.authHeader or "X-API-Key"
            headers[header_name] = api_def.authValue

        return headers

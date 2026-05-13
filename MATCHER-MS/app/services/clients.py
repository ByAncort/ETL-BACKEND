import httpx
from app.models import ConnectionResponse, ApiRegistryResponse, \
    LlmConfigResponse, SchemaMatchRequest, SchemaMatchResponse


class IntegrationClient:
    def __init__(self, base_url: str = "http://localhost:8082"):
        self.base_url = base_url

    async def get_connection(self, connection_id: int) -> ConnectionResponse:
        url = f"{self.base_url}/api/integrations/connections/{connection_id}"
        async with httpx.AsyncClient() as client:
            response = await client.get(url)
            response.raise_for_status()
            return ConnectionResponse(**response.json())


class ApiRegistryClient:
    def __init__(self, base_url: str = "http://localhost:8083"):
        self.base_url = base_url

    async def get_api_definition(self, api_id: int) -> ApiRegistryResponse:
        url = f"{self.base_url}/api-registry/{api_id}"
        async with httpx.AsyncClient() as client:
            response = await client.get(url)
            response.raise_for_status()
            return ApiRegistryResponse(**response.json())


class LlmConfigClient:
    def __init__(self, base_url: str = "http://localhost:8086"):
        self.base_url = base_url

    async def get_default_model(self) -> LlmConfigResponse:
        url = f"{self.base_url}/api/llm-configs/default"
        async with httpx.AsyncClient() as client:
            response = await client.get(url)
            response.raise_for_status()
            return LlmConfigResponse(**response.json())


class SchemaMatchClient:
    def __init__(self, base_url: str = "http://localhost:8085"):
        self.base_url = base_url

    async def create_match(self, request: SchemaMatchRequest) -> SchemaMatchResponse:
        url = f"{self.base_url}/api/schema-matches"
        async with httpx.AsyncClient() as client:
            response = await client.post(url, json=request.model_dump(mode="json", exclude_none=True))
            response.raise_for_status()
            return SchemaMatchResponse(**response.json())

    async def create_matches_batch(self, matches: list[SchemaMatchRequest]) -> list[SchemaMatchResponse]:
        url = f"{self.base_url}/api/schema-matches/batch"
        payload = {"matches": [m.model_dump(mode="json", exclude_none=True) for m in matches]}
        async with httpx.AsyncClient() as client:
            response = await client.post(url, json=payload)
            response.raise_for_status()
            return [SchemaMatchResponse(**item) for item in response.json()]
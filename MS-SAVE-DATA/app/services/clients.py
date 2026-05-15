import os
import httpx
from app.models.schemas import ConnectionResponse, ApiRegistryResponse, SchemaMatchResponse


class IntegrationClient:
    def __init__(self, base_url: str = None):
        self.base_url = base_url or os.getenv("INTEGRATION_MS_URL", "http://localhost:8082")

    async def get_connection(self, connection_id: int) -> ConnectionResponse:
        url = f"{self.base_url}/api/integrations/connections/{connection_id}"
        async with httpx.AsyncClient() as client:
            response = await client.get(url)
            response.raise_for_status()
            return ConnectionResponse(**response.json())


class ApiRegistryClient:
    def __init__(self, base_url: str = None):
        self.base_url = base_url or os.getenv("API_REGISTRY_MS_URL", "http://localhost:8083")

    async def get_api_definition(self, api_id: int) -> ApiRegistryResponse:
        url = f"{self.base_url}/api-registry/{api_id}"
        async with httpx.AsyncClient() as client:
            response = await client.get(url)
            response.raise_for_status()
            return ApiRegistryResponse(**response.json())

    async def test_api(self, api_id: int, path_params: str = "", query_params: str = "", body: str = "") -> dict:
        url = f"{self.base_url}/api-registry/{api_id}/test"
        payload = {
            "pathParams": path_params,
            "queryParams": query_params,
            "body": body,
        }
        async with httpx.AsyncClient() as client:
            response = await client.post(url, json=payload)
            response.raise_for_status()
            return response.json()

    async def send_data(self, api_id: int, method: str, path_params: str = "", query_params: str = "", body: str = "") -> dict:
        url = f"{self.base_url}/api-registry/{api_id}/test"
        payload = {
            "pathParams": path_params,
            "queryParams": query_params,
            "body": body,
        }
        async with httpx.AsyncClient() as client:
            response = await client.post(url, json=payload)
            response.raise_for_status()
            return response.json()


class SchemaMatchClient:
    def __init__(self, base_url: str = None):
        self.base_url = base_url or os.getenv("SCHEMA_MATCHING_MS_URL", "http://localhost:8085")

    async def get_matches_by_integration(self, integration_id: int) -> list[SchemaMatchResponse]:
        url = f"{self.base_url}/api/schema-matches/integration/{integration_id}"
        async with httpx.AsyncClient() as client:
            response = await client.get(url)
            response.raise_for_status()
            return [SchemaMatchResponse(**item) for item in response.json()]

    async def get_matches_by_integration_and_status(self, integration_id: int, status: str) -> list[SchemaMatchResponse]:
        url = f"{self.base_url}/api/schema-matches/integration/{integration_id}/status/{status}"
        async with httpx.AsyncClient() as client:
            response = await client.get(url)
            response.raise_for_status()
            return [SchemaMatchResponse(**item) for item in response.json()]

import httpx
from app.models import ConnectionResponse, ApiRegistryResponse


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

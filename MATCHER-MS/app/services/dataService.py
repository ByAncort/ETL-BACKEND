import os
import json
import pandas as pd
import httpx
from fastapi import HTTPException
from starlette import status

from app.models import ConnectionResponse, SchemaMatchRequest
from app.services import IntegrationClient, ApiRegistryClient, LlmConfigClient, SchemaMatchClient
from app.services.matchingService import MatchingService

integration_client = IntegrationClient()
api_registry_client = ApiRegistryClient()
llm_config_client = LlmConfigClient()
schema_match_client = SchemaMatchClient()


class DataService:

    async def get_conection(self, connection_id: int) -> ConnectionResponse:
        integration = await integration_client.get_connection(connection_id=connection_id)

        if not integration:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND)

        return integration

    async def get_default_model(self) -> dict:
        config = await llm_config_client.get_default_model()
        return {
            "id": config.id,
            "name": config.name,
            "provider": config.provider,
            "modelName": config.modelName,
            "baseUrl": config.baseUrl,
        }

    async def run_matching(self, integration_id: int) -> dict:
        connection = await self.get_conection(integration_id)
        api_a_id = int(connection.apiA)
        api_b_id = int(connection.apiB)

        api_a_def = await api_registry_client.get_api_definition(api_a_id)
        api_b_def = await api_registry_client.get_api_definition(api_b_id)

        data_a = await self.fetch_api_data(api_a_id)
        data_b = await self.fetch_api_data(api_b_id)

        matching = MatchingService()
        return await matching.match_and_register(
            integration_id=integration_id,
            data_a=data_a,
            data_b=data_b,
            api_a_def=api_a_def,
            api_b_def=api_b_def,
        )

    async def register_schema_match(self, request: SchemaMatchRequest) -> dict:
        response = await schema_match_client.create_match(request)
        return response.model_dump(mode="json")

    async def register_schema_matches(self, matches: list[SchemaMatchRequest]) -> list[dict]:
        responses = await schema_match_client.create_matches_batch(matches)
        return [r.model_dump(mode="json") for r in responses]

    async def process_integration(self, integration_id: int) -> dict:
        connection = await self.get_conection(integration_id)
        api_a_id = int(connection.apiA)
        api_b_id = int(connection.apiB)

        api_a_def = await api_registry_client.get_api_definition(api_a_id)
        api_b_def = await api_registry_client.get_api_definition(api_b_id)

        data_a = await self.fetch_api_data(api_a_id)
        data_b = await self.fetch_api_data(api_b_id)

        default_model = await self.get_default_model()

        return {
            "connection": connection.model_dump(mode="json"),
            "apiA": api_a_def.model_dump(mode="json"),
            "apiB": api_b_def.model_dump(mode="json"),
            "dataA": data_a,
            "dataB": data_b,
            "defaultModel": default_model,
        }

    async def fetch_api_data(self, api_id: int) -> dict:
        registry_base = os.getenv("API_REGISTRY_MS_URL", "http://localhost:8083")
        test_url = f"{registry_base}/api-registry/{api_id}/test"
        api_def = await api_registry_client.get_api_definition(api_id)
        payload = {
            "pathParams": api_def.pathParams or "",
            "queryParams": api_def.queryParams or "",
            "body": api_def.body or "",
        }

        async with httpx.AsyncClient() as client:
            response = await client.post(test_url, json=payload)
            response.raise_for_status()
            data = response.json()

            if data.get("statusCode") == 401:
                await self._refresh_token(api_id)

                retry_response = await client.post(test_url, json=payload)
                retry_response.raise_for_status()
                data = retry_response.json()

            return data

    async def _refresh_token(self, api_id: int):
        api_detail = await api_registry_client.get_api_definition(api_id)

        if not api_detail.authApiId:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="La API devolvió 401 pero no tiene una API de autenticación (authApiId) configurada."
            )

        auth_api_detail = await api_registry_client.get_api_definition(api_detail.authApiId)

        async with httpx.AsyncClient() as client:
            auth_response = await client.request(
                method=auth_api_detail.method,
                url=auth_api_detail.url,
                json=json.loads(auth_api_detail.body) if auth_api_detail.body else None
            )
            auth_response.raise_for_status()
            new_token_data = auth_response.json()

            print("Token renovado exitosamente:", new_token_data)

    async def process_data_with_pandas(self, api_id: int) -> pd.DataFrame:
        raw_response = await self.fetch_api_data(api_id)

        if raw_response.get("statusCode") != 200:
            raise HTTPException(
                status_code=raw_response.get("statusCode", 500),
                detail="Error al consumir la API remota"
            )

        body_str = raw_response.get("body")
        if not body_str:
            raise HTTPException(status_code=500, detail="El body de la respuesta está vacío.")

        try:
            body_json = json.loads(body_str)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail="No se pudo parsear el body como JSON.")

        data_list = body_json.get("data", [])

        df = pd.DataFrame(data_list)

        if 'date' in df.columns:
            df['date'] = pd.to_datetime(df['date'])

        return df
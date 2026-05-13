from fastapi import APIRouter, HTTPException
from app.services import IntegrationClient, ApiRegistryClient

router = APIRouter()
integration_client = IntegrationClient()
api_registry_client = ApiRegistryClient()


@router.get("/connection/{connection_id}")
async def get_connection(connection_id: int):
    try:
        return await integration_client.get_connection(connection_id)
    except Exception as e:
        raise HTTPException(status_code=502, detail=str(e))


@router.get("/api-definition/{api_id}")
async def get_api_definition(api_id: int):
    try:
        return await api_registry_client.get_api_definition(api_id)
    except Exception as e:
        raise HTTPException(status_code=502, detail=str(e))

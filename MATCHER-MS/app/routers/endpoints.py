import traceback
from fastapi import APIRouter, HTTPException
from app.models import SchemaMatchRequest
from app.services import IntegrationClient, ApiRegistryClient, LlmConfigClient
from app.services.dataService import DataService

router = APIRouter()
integration_client = IntegrationClient()
api_registry_client = ApiRegistryClient()
llm_config_client = LlmConfigClient()
data_service = DataService()


def _handle_error(e: Exception):
    tb = traceback.format_exc()
    print(f"ERROR: {type(e).__name__}: {e}")
    print(tb)
    detail = f"{type(e).__name__}: {e}" if str(e) else f"{type(e).__name__}: (sin mensaje)"
    raise HTTPException(status_code=502, detail=detail)


@router.get("/connection/{connection_id}")
async def get_connection(connection_id: int):
    try:
        return await integration_client.get_connection(connection_id)
    except Exception as e:
        _handle_error(e)


@router.get("/api-definition/{api_id}")
async def get_api_definition(api_id: int):
    try:
        return await api_registry_client.get_api_definition(api_id)
    except Exception as e:
        _handle_error(e)


@router.get("/api-datos/integracion/{integration_id}")
async def get_api_datos(integration_id: int):
    try:
        return await data_service.fetch_api_data(integration_id)
    except Exception as e:
        _handle_error(e)


@router.get("/default-model")
async def get_default_model():
    try:
        return await data_service.get_default_model()
    except Exception as e:
        _handle_error(e)


@router.post("/schema-matches")
async def create_schema_match(request: SchemaMatchRequest):
    try:
        return await data_service.register_schema_match(request)
    except Exception as e:
        _handle_error(e)


@router.post("/schema-matches/batch")
async def create_schema_matches(matches: list[SchemaMatchRequest]):
    try:
        return await data_service.register_schema_matches(matches)
    except Exception as e:
        _handle_error(e)


@router.get("/process-integration/{integration_id}")
async def process_integration(integration_id: int):
    try:
        return await data_service.process_integration(integration_id)
    except Exception as e:
        _handle_error(e)


@router.post("/run-matching/{integration_id}")
async def run_matching(integration_id: int):
    try:
        return await data_service.run_matching(integration_id)
    except Exception as e:
        _handle_error(e)
import traceback
from fastapi import APIRouter, HTTPException
from app.models.schemas import EtlRequest, EtlResponse
from app.services.etlOrchestrator import EtlOrchestrator

router = APIRouter(prefix="/api")
etl_orchestrator = EtlOrchestrator()


def _handle_error(e: Exception):
    tb = traceback.format_exc()
    print(f"ERROR: {type(e).__name__}: {e}")
    print(tb)
    detail = f"{type(e).__name__}: {e}" if str(e) else f"{type(e).__name__}: (sin mensaje)"
    raise HTTPException(status_code=502, detail=detail)


@router.get("/health")
async def health():
    return {"status": "ok", "service": "MS-SAVE-DATA"}


@router.post("/etl/run", response_model=EtlResponse)
async def run_etl(request: EtlRequest):
    try:
        return await etl_orchestrator.run_etl(request.integrationId)
    except Exception as e:
        _handle_error(e)


@router.post("/etl/run/{integration_id}", response_model=EtlResponse)
async def run_etl_by_id(integration_id: int):
    try:
        return await etl_orchestrator.run_etl(integration_id)
    except Exception as e:
        _handle_error(e)

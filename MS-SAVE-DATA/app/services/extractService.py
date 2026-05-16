import json
import logging
from app.services.clients import ApiRegistryClient

logger = logging.getLogger(__name__)


class ExtractService:
    def __init__(self):
        self.api_registry_client = ApiRegistryClient()

    async def extract_data(self, api_id: int) -> list[dict]:
        api_def = await self.api_registry_client.get_api_definition(api_id)
        logger.info(
            f"[EXTRACT] API origen: id=%s url=%s method=%s",
            api_id, api_def.url, api_def.method,
        )
        logger.info(
            f"[EXTRACT] Ejecutando test endpoint apiId=%s con pathParams=%s queryParams=%s",
            api_id, api_def.pathParams or "", api_def.queryParams or "",
        )

        test_result = await self.api_registry_client.test_api(
            api_id=api_id,
            path_params=api_def.pathParams or "",
            query_params=api_def.queryParams or "",
            body=api_def.body or "",
        )

        status_code = test_result.get("statusCode")
        if status_code != 200:
            error_msg = test_result.get("error", "unknown error")
            logger.error(
                f"[EXTRACT] API %s retornó status %s: %s",
                api_id, status_code, error_msg,
            )
            raise RuntimeError(
                f"API {api_id} returned status {status_code}: {error_msg}"
            )

        body_str = test_result.get("body")
        if not body_str:
            logger.error(f"[EXTRACT] API %s retornó body vacío", api_id)
            raise RuntimeError(f"API {api_id} returned empty body")

        body_json = json.loads(body_str)

        if isinstance(body_json, list):
            records = body_json
        else:
            data_list = body_json.get("data")
            if data_list is not None and isinstance(data_list, list):
                records = data_list
            else:
                records = [body_json]

        logger.info(
            f"[EXTRACT] Extraídos %d registros de apiId=%s",
            len(records), api_id,
        )

        if records:
            sample_keys = list(records[0].keys())
            logger.info(
                f"[EXTRACT] Campos del primer registro (%d): %s",
                len(sample_keys), sample_keys,
            )
            logger.info(
                f"[EXTRACT] Muestra primer registro: %s",
                json.dumps(records[0], ensure_ascii=False)[:300],
            )
        else:
            logger.warning(f"[EXTRACT] No se extrajeron registros de apiId=%s", api_id)

        logger.info(
            f"[EXTRACT] ResponseTimeMs: %s",
            test_result.get("responseTimeMs", "N/A"),
        )

        return records

import json
import logging
import httpx
from app.services.clients import ApiRegistryClient

logger = logging.getLogger(__name__)


class LoadService:
    def __init__(self):
        self.api_registry_client = ApiRegistryClient()

    def _build_url(self, api_def) -> str:
        url = api_def.url.rstrip("/")
        if api_def.pathParams:
            url += "/" + api_def.pathParams.lstrip("/")
        return url

    def _build_base_body(self, api_def) -> dict:
        if api_def.body:
            try:
                return json.loads(api_def.body)
            except json.JSONDecodeError:
                logger.warning(f"[LOAD] No se pudo parsear body template, se usa dict vacío")
        return {}

    def _merge_record_with_template(self, template: dict, record: dict) -> dict:
        merged = template.copy()
        for key in list(merged.keys()):
            if key in record and record[key] is not None:
                merged[key] = record[key]
            else:
                if isinstance(merged[key], dict):
                    merged[key] = None
                elif isinstance(merged[key], str) and not merged[key].strip():
                    del merged[key]
        for key, val in record.items():
            if key not in merged and val is not None:
                merged[key] = val
        return merged

    async def load_data(self, target_api_id: int, records: list[dict]) -> dict:
        if not records:
            return {"loaded": 0, "errors": []}

        target_def = await self.api_registry_client.get_api_definition(target_api_id)
        base_template = self._build_base_body(target_def)

        logger.info(
            f"[LOAD] Template base tiene %d campos: %s",
            len(base_template), list(base_template.keys()),
        )

        headers = self._build_headers(target_def)
        url = self._build_url(target_def)

        errors = []
        loaded_count = 0

        async with httpx.AsyncClient(timeout=60.0) as client:
            for i, record in enumerate(records):
                try:
                    merged = self._merge_record_with_template(base_template, record)
                    body = json.dumps(merged)
                    if i == 0:
                        logger.info(
                            f"[LOAD] Primer body enviado (%d chars): %s",
                            len(body), body[:500],
                        )
                    response = await client.request(
                        method=target_def.method,
                        url=url,
                        headers=headers,
                        content=body,
                    )

                    if response.status_code < 400:
                        loaded_count += 1
                    else:
                        logger.warning(
                            f"[LOAD] Registro %d falló: HTTP %s - %s",
                            i, response.status_code, response.text[:200],
                        )
                        errors.append(
                            f"HTTP {response.status_code} para registro: {response.text[:200]}"
                        )

                except Exception as e:
                    logger.error(f"[LOAD] Error en registro %d: %s", i, str(e))
                    errors.append(f"Error cargando registro: {str(e)}")

        return {
            "loaded": loaded_count,
            "errors": errors,
        }

    async def load_batch(self, target_api_id: int, records: list[dict]) -> dict:
        if not records:
            logger.warning(f"[LOAD] No hay registros para cargar a apiId=%s", target_api_id)
            return {"loaded": 0, "errors": []}

        target_def = await self.api_registry_client.get_api_definition(target_api_id)
        base_template = self._build_base_body(target_def)

        logger.info(
            f"[LOAD] Template base tiene %d campos: %s",
            len(base_template), list(base_template.keys()),
        )

        headers = self._build_headers(target_def)
        url = self._build_url(target_def)

        logger.info(
            f"[LOAD] Destino: %s %s (pathParams=%s, authType=%s)",
            target_def.method, url, target_def.pathParams or "ninguno", target_def.authType or "NONE",
        )

        safe_headers = {k: v for k, v in headers.items() if k.lower() != "authorization"}
        logger.info(
            f"[LOAD] Headers (sin auth): %s",
            safe_headers,
        )

        merged_records = [self._merge_record_with_template(base_template, r) for r in records]
        body = json.dumps(merged_records)
        payload_bytes = len(body.encode("utf-8"))

        logger.info(
            f"[LOAD] Enviando %d registros (~%.1f KB) a %s %s",
            len(records), payload_bytes / 1024, target_def.method, url,
        )

        async with httpx.AsyncClient(timeout=120.0) as client:
            try:
                response = await client.request(
                    method=target_def.method,
                    url=url,
                    headers=headers,
                    content=body,
                )

                logger.info(
                    f"[LOAD] Respuesta destino: HTTP %s (%.1fs)",
                    response.status_code, response.elapsed.total_seconds(),
                )

                response_body = response.text[:500]
                logger.info(f"[LOAD] Body respuesta: %s", response_body)

                if response.status_code < 400:
                    logger.info(
                        f"[LOAD] Cargados %d/%d registros exitosamente",
                        len(records), len(records),
                    )
                    return {"loaded": len(records), "errors": []}
                else:
                    logger.error(
                        f"[LOAD] Falló carga batch: HTTP %s - %s",
                        response.status_code, response_body,
                    )
                    return {
                        "loaded": 0,
                        "errors": [
                            f"HTTP {response.status_code}: {response_body[:500]}"
                        ],
                    }

            except httpx.TimeoutException:
                logger.error(f"[LOAD] Timeout cargando datos en %s", url)
                return {"loaded": 0, "errors": [f"Timeout: {url} no respondió en 120s"]}
            except Exception as e:
                logger.error(f"[LOAD] Error cargando datos: %s", str(e))
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

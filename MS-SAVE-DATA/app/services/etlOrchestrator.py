import time
import traceback
import logging
from app.services.clients import IntegrationClient, SchemaMatchClient, LogClient
from app.services.extractService import ExtractService
from app.services.transformService import TransformService
from app.services.loadService import LoadService
from app.models.schemas import EtlResponse

logger = logging.getLogger(__name__)


class EtlOrchestrator:
    def __init__(self):
        self.integration_client = IntegrationClient()
        self.schema_match_client = SchemaMatchClient()
        self.log_client = LogClient()
        self.extract_service = ExtractService()
        self.transform_service = TransformService()
        self.load_service = LoadService()

    async def _send_error_log(self, integration_id: int, phase: str, message: str, duration_ms: float = 0):
        log_data = {
            "serviceName": "ms-save-data",
            "className": "EtlOrchestrator",
            "methodName": "run_etl",
            "logLevel": "ERROR",
            "message": f"[ETL] Error en fase {phase}: {message}",
            "detail": traceback.format_exc(),
            "durationMs": int(duration_ms * 1000),
            "integrationId": str(integration_id),
        }
        await self.log_client.send_log(log_data)

    async def run_etl(self, integration_id: int) -> EtlResponse:
        start = time.time()
        errors = []
        logger.info(f"[ETL] ===== INICIANDO ETL integrationId=%d =====", integration_id)

        connection = None
        try:
            connection = await self.integration_client.get_connection(integration_id)
            logger.info(
                f"[ETL] Conexión obtenida: apiA(id=%s) → apiB(id=%s) [status=%s]",
                connection.apiA, connection.apiB, connection.status,
            )
        except Exception as e:
            logger.error(f"[ETL] Error obteniendo conexión: %s", str(e))
            await self._send_error_log(integration_id, "CONNECTION", str(e))
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=0,
                targetApiId=0,
                totalRecords=0,
                transformedRecords=0,
                loadedRecords=0,
                errors=[f"Error obteniendo conexión: {str(e)}"],
            )

        source_api_id = int(connection.apiA)
        target_api_id = int(connection.apiB)

        t = time.time()
        try:
            matches = await self.schema_match_client.get_matches_by_integration_and_status(
                integration_id, "ACCEPTED"
            )
            if not matches:
                logger.warning(
                    f"[ETL] No hay schema matches ACCEPTED para integrationId=%d. "
                    f"Los matches deben ser revisados y aceptados antes de ejecutar el ETL",
                    integration_id,
                )
            logger.info(
                f"[ETL] Obtenidos %d schema matches en %.2fs",
                len(matches), time.time() - t,
            )
        except Exception as e:
            logger.error(f"[ETL] Error obteniendo matches: %s", str(e))
            await self._send_error_log(integration_id, "MATCHES", str(e))
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=source_api_id,
                targetApiId=target_api_id,
                totalRecords=0,
                transformedRecords=0,
                loadedRecords=0,
                errors=[f"Error obteniendo matches: {str(e)}"],
            )

        if not matches:
            logger.warning(f"[ETL] No hay schema matches aprobados para integrationId=%d", integration_id)
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=source_api_id,
                targetApiId=target_api_id,
                totalRecords=0,
                transformedRecords=0,
                loadedRecords=0,
                errors=["No hay schema matches aprobados para esta integración"],
            )

        total_records = 0
        transformed_count = 0
        loaded_count = 0
        source_data = []

        t = time.time()
        try:
            source_data = await self.extract_service.extract_data(source_api_id)
            total_records = len(source_data)
            logger.info(
                f"[ETL] Fase EXTRACT completada en %.2fs: %d registros",
                time.time() - t, total_records,
            )
        except Exception as e:
            logger.error(f"[ETL] Error en EXTRACT: %s", str(e))
            await self._send_error_log(integration_id, "EXTRACT", str(e))
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=source_api_id,
                targetApiId=target_api_id,
                totalRecords=total_records,
                transformedRecords=0,
                loadedRecords=0,
                errors=[f"Error extrayendo datos: {str(e)}"],
            )

        t = time.time()
        try:
            transformed_data = self.transform_service.transform_data(source_data, matches)
            transformed_count = len(transformed_data)
            logger.info(
                f"[ETL] Fase TRANSFORM completada en %.2fs: %d registros",
                time.time() - t, transformed_count,
            )
        except Exception as e:
            logger.error(f"[ETL] Error en TRANSFORM: %s", str(e))
            await self._send_error_log(integration_id, "TRANSFORM", str(e))
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=source_api_id,
                targetApiId=target_api_id,
                totalRecords=total_records,
                transformedRecords=0,
                loadedRecords=0,
                errors=[f"Error transformando datos: {str(e)}"],
            )

        t = time.time()
        try:
            load_result = await self.load_service.load_data(target_api_id, transformed_data)
            loaded_count = load_result.get("loaded", 0)
            errors.extend(load_result.get("errors", []))
            logger.info(
                f"[ETL] Fase LOAD completada en %.2fs: %d/%d cargados",
                time.time() - t, loaded_count, transformed_count,
            )
        except Exception as e:
            logger.error(f"[ETL] Error en LOAD: %s", str(e))
            await self._send_error_log(integration_id, "LOAD", str(e))
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=source_api_id,
                targetApiId=target_api_id,
                totalRecords=total_records,
                transformedRecords=transformed_count,
                loadedRecords=0,
                errors=[f"Error cargando datos: {str(e)}"],
            )

        total_time = time.time() - start
        logger.info(f"[ETL] ===== ETL COMPLETADO en %.2fs =====", total_time)
        logger.info(
            f"[ETL] Resumen: %d extraídos → %d transformados → %d cargados | %d errores",
            total_records, transformed_count, loaded_count, len(errors),
        )

        if errors:
            for i, err in enumerate(errors):
                logger.error(f"[ETL] Error #%d: %s", i + 1, err)

        return EtlResponse(
            integrationId=integration_id,
            sourceApiId=source_api_id,
            targetApiId=target_api_id,
            totalRecords=total_records,
            transformedRecords=transformed_count,
            loadedRecords=loaded_count,
            errors=errors,
        )

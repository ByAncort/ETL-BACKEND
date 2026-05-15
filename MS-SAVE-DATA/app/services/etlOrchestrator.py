from app.services.clients import IntegrationClient, SchemaMatchClient
from app.services.extractService import ExtractService
from app.services.transformService import TransformService
from app.services.loadService import LoadService
from app.models.schemas import EtlResponse


class EtlOrchestrator:
    def __init__(self):
        self.integration_client = IntegrationClient()
        self.schema_match_client = SchemaMatchClient()
        self.extract_service = ExtractService()
        self.transform_service = TransformService()
        self.load_service = LoadService()

    async def run_etl(self, integration_id: int) -> EtlResponse:
        errors = []

        try:
            connection = await self.integration_client.get_connection(integration_id)
        except Exception as e:
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

        try:
            matches = await self.schema_match_client.get_matches_by_integration_and_status(
                integration_id, "ACCEPTED"
            )
            if not matches:
                matches = await self.schema_match_client.get_matches_by_integration_and_status(
                    integration_id, "APPROVED"
                )
        except Exception as e:
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
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=source_api_id,
                targetApiId=target_api_id,
                totalRecords=0,
                transformedRecords=0,
                loadedRecords=0,
                errors=["No hay schema matches aprobados para esta integración"],
            )

        try:
            source_data = await self.extract_service.extract_data(source_api_id)
        except Exception as e:
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=source_api_id,
                targetApiId=target_api_id,
                totalRecords=0,
                transformedRecords=0,
                loadedRecords=0,
                errors=[f"Error extrayendo datos: {str(e)}"],
            )

        total_records = len(source_data)

        try:
            transformed_data = self.transform_service.transform_data(source_data, matches)
        except Exception as e:
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=source_api_id,
                targetApiId=target_api_id,
                totalRecords=total_records,
                transformedRecords=0,
                loadedRecords=0,
                errors=[f"Error transformando datos: {str(e)}"],
            )

        transformed_count = len(transformed_data)

        try:
            load_result = await self.load_service.load_batch(target_api_id, transformed_data)
            loaded_count = load_result.get("loaded", 0)
            errors.extend(load_result.get("errors", []))
        except Exception as e:
            return EtlResponse(
                integrationId=integration_id,
                sourceApiId=source_api_id,
                targetApiId=target_api_id,
                totalRecords=total_records,
                transformedRecords=transformed_count,
                loadedRecords=0,
                errors=[f"Error cargando datos: {str(e)}"],
            )

        return EtlResponse(
            integrationId=integration_id,
            sourceApiId=source_api_id,
            targetApiId=target_api_id,
            totalRecords=total_records,
            transformedRecords=transformed_count,
            loadedRecords=loaded_count,
            errors=errors,
        )

import json
from fastapi import HTTPException
from starlette import status

from app.models import SchemaMatchRequest, ApiRegistryResponse
from app.services import LlmConfigClient, SchemaMatchClient
from app.services.llmService import LlmService

llm_config_client = LlmConfigClient()
schema_match_client = SchemaMatchClient()


class MatchingService:

    def _extract_fields_from_body_schema(self, api_def: ApiRegistryResponse) -> list[str]:
        if not api_def.body:
            raise HTTPException(status_code=502, detail=f"La API {api_def.method} {api_def.url} no tiene body schema definido.")
        try:
            schema = json.loads(api_def.body)
        except json.JSONDecodeError as e:
            raise HTTPException(status_code=502, detail=f"Body schema inválido en API {api_def.id}: {e}")
        if isinstance(schema, dict):
            return list(schema.keys())
        return []

    def _extract_fields_from_response(self, data: dict) -> list[str]:
        body_str = data.get("body")
        if not body_str:
            raise HTTPException(status_code=502, detail=f"El body de respuesta está vacío. Respuesta: {str(data)[:300]}")
        try:
            body_json = json.loads(body_str)
        except json.JSONDecodeError as e:
            raise HTTPException(status_code=502, detail=f"No se pudo parsear el body respuesta como JSON: {e}. Body: {body_str[:300]}")

        if isinstance(body_json, list):
            if body_json and isinstance(body_json[0], dict):
                return list(body_json[0].keys())
            raise HTTPException(status_code=502, detail="El body respuesta es un array pero no contiene objetos.")

        data_list = body_json.get("data")
        if data_list is None:
            return list(body_json.keys())

        if not isinstance(data_list, list):
            raise HTTPException(status_code=502, detail=f"El campo 'data' no es una lista. Tipo: {type(data_list).__name__}")

        if not data_list:
            raise HTTPException(status_code=502, detail="La lista 'data' está vacía.")

        first_item = data_list[0]
        if isinstance(first_item, dict):
            return list(first_item.keys())
        raise HTTPException(status_code=502, detail=f"Los elementos de 'data' no son objetos: {first_item}")

    def _extract_fields(self, test_data: dict, api_def: ApiRegistryResponse) -> list[str]:
        if api_def.method.upper() in ("POST", "PUT", "PATCH"):
            return self._extract_fields_from_body_schema(api_def)
        return self._extract_fields_from_response(test_data)

    async def match_and_register(
        self,
        integration_id: int,
        data_a: dict,
        data_b: dict,
        api_a_def: ApiRegistryResponse,
        api_b_def: ApiRegistryResponse,
    ) -> dict:
        fields_a = self._extract_fields(data_a, api_a_def)
        fields_b = self._extract_fields(data_b, api_b_def)

        llm_config = await llm_config_client.get_default_model()
        llm = LlmService(
            base_url=llm_config.baseUrl,
            api_key=llm_config.apiKey,
            model_name=llm_config.modelName or "gpt-4",
        )

        raw_matches = await llm.match_fields(fields_a, fields_b)

        requests = []
        for m in raw_matches:
            requests.append(SchemaMatchRequest(
                sourceField=m["sourceField"],
                targetField=m["targetField"],
                confidence=float(m.get("confidence", 0.5)),
                integrationId=integration_id,
                transformation=m.get("transformation"),
            ))

        registered = []
        for req in requests:
            resp = await schema_match_client.create_match(req)
            registered.append(resp.model_dump(mode="json"))

        return {
            "sourceFields": fields_a,
            "targetFields": fields_b,
            "modelUsed": {
                "provider": llm_config.provider,
                "modelName": llm_config.modelName,
            },
            "matches": registered,
        }

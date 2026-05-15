import json
from app.services.clients import ApiRegistryClient


class ExtractService:
    def __init__(self):
        self.api_registry_client = ApiRegistryClient()

    async def extract_data(self, api_id: int) -> list[dict]:
        api_def = await self.api_registry_client.get_api_definition(api_id)
        test_result = await self.api_registry_client.test_api(
            api_id=api_id,
            path_params=api_def.pathParams or "",
            query_params=api_def.queryParams or "",
            body=api_def.body or "",
        )

        if test_result.get("statusCode") != 200:
            raise RuntimeError(
                f"API {api_id} returned status {test_result.get('statusCode')}: {test_result.get('error', 'unknown error')}"
            )

        body_str = test_result.get("body")
        if not body_str:
            raise RuntimeError(f"API {api_id} returned empty body")

        body_json = json.loads(body_str)

        if isinstance(body_json, list):
            return body_json

        data_list = body_json.get("data")
        if data_list is not None and isinstance(data_list, list):
            return data_list

        return [body_json]

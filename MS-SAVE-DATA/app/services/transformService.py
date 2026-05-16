import logging
from app.models.schemas import SchemaMatchResponse

logger = logging.getLogger(__name__)


class TransformService:

    def transform_record(self, record: dict, matches: list[SchemaMatchResponse]) -> dict:
        transformed = {}
        for match in matches:
            source_field = match.sourceField
            target_field = match.targetField
            transformation = match.transformation

            if source_field in record:
                value = record[source_field]
                if transformation:
                    value = self._apply_transformation(value, transformation)
                transformed[target_field] = value
            else:
                transformed[target_field] = None

        return transformed

    def transform_data(self, records: list[dict], matches: list[SchemaMatchResponse]) -> list[dict]:
        logger.info(
            f"[TRANSFORM] Aplicando %d schema matches sobre %d registros",
            len(matches), len(records),
        )

        for m in matches:
            logger.info(
                f"[TRANSFORM] Mapeo: '%s' → '%s' (transf=%s, confianza=%.4f)",
                m.sourceField,
                m.targetField,
                m.transformation or "ninguna",
                m.confidence,
            )

        result = [self.transform_record(r, matches) for r in records]

        if result:
            logger.info(
                f"[TRANSFORM] Registro transformado ejemplo: %s",
                {k: str(v)[:50] for k, v in result[0].items()},
            )

        logger.info(
            f"[TRANSFORM] Transformados %d/%d registros exitosamente",
            len(result), len(records),
        )

        return result

    def _apply_transformation(self, value, transformation: str):
        transformation = transformation.strip()

        if transformation.startswith("int(") and transformation.endswith(")"):
            try:
                return int(float(value))
            except (ValueError, TypeError):
                logger.warning(
                    f"[TRANSFORM] Fallo int() para valor '%s', se deja original", value,
                )
                return value

        if transformation.startswith("float(") and transformation.endswith(")"):
            try:
                return float(value)
            except (ValueError, TypeError):
                return value

        if transformation.startswith("str(") and transformation.endswith(")"):
            return str(value)

        if transformation.startswith("bool(") and transformation.endswith(")"):
            if isinstance(value, str):
                return value.lower() in ("true", "1", "yes")
            return bool(value)

        if transformation.startswith("upper(") and transformation.endswith(")"):
            return str(value).upper() if value else value

        if transformation.startswith("lower(") and transformation.endswith(")"):
            return str(value).lower() if value else value

        logger.warning(
            f"[TRANSFORM] Transformación desconocida '%s', se deja valor original",
            transformation,
        )
        return value

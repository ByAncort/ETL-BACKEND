from pydantic import BaseModel
from datetime import datetime
from typing import Optional, Any


class ConnectionResponse(BaseModel):
    id: int
    apiA: str
    apiB: str
    description: str
    status: str
    createdAt: datetime
    updatedAt: datetime


class ApiRegistryResponse(BaseModel):
    id: int
    method: str
    url: str
    description: str
    pathParams: Optional[str] = None
    queryParams: Optional[str] = None
    body: Optional[str] = None
    createdAt: datetime
    authType: Optional[str] = None
    authHeader: Optional[str] = None
    authHeaderValue: Optional[str] = None
    authApiId: Optional[int] = None
    authApiUrl: Optional[str] = None
    authValue: Optional[str] = None


class SchemaMatchResponse(BaseModel):
    id: int
    integrationId: Optional[int] = None
    sourceField: str
    targetField: str
    confidence: float
    status: str
    transformation: Optional[str] = None
    reviewedBy: Optional[int] = None
    reviewedAt: Optional[datetime] = None
    createdAt: datetime


class EtlRequest(BaseModel):
    integrationId: int


class EtlResponse(BaseModel):
    integrationId: int
    sourceApiId: int
    targetApiId: int
    totalRecords: int
    transformedRecords: int
    loadedRecords: int
    errors: list[str]


class TestResponse(BaseModel):
    statusCode: int
    body: Optional[str] = None
    headers: Optional[dict[str, str]] = None
    responseTimeMs: Optional[int] = None
    error: Optional[str] = None

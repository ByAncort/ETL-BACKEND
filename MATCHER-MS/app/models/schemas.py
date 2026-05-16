from pydantic import BaseModel
from datetime import datetime
from typing import Optional


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


class LlmConfigResponse(BaseModel):
    id: int
    name: str
    provider: str
    apiKey: str
    baseUrl: str
    modelName: Optional[str] = None
    isDefault: bool
    status: str
    createdAt: datetime
    updatedAt: datetime


class SchemaMatchRequest(BaseModel):
    sourceField: str
    targetField: str
    confidence: float
    integrationId: Optional[int] = None
    status: Optional[str] = None
    transformation: Optional[str] = None
    reviewedBy: Optional[int] = None


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

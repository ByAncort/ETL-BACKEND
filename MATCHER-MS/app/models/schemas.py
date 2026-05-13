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

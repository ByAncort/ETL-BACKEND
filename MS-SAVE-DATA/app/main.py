from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers.endpoints import router

app = FastAPI(title="MS-SAVE-DATA", description="Microservicio de carga ETL - Extracción, Transformación y Carga")


app.include_router(router)


@app.get("/")
async def root():
    return {"message": "MS-SAVE-DATA - ETL Load Service"}

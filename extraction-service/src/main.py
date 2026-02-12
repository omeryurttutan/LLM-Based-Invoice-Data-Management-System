from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from src.api.health import router as health_router

app = FastAPI(
    title="Fatura OCR Extraction Service",
    description="LLM-based invoice data extraction service",
    version="0.1.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3001"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_router)

@app.get("/")
async def root():
    return {"message": "Fatura OCR Extraction Service"}

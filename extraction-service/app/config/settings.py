from typing import List, Optional
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    APP_NAME: str = "Fatura OCR Extraction Service"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False
    LOG_LEVEL: str = "INFO"
    ALLOWED_ORIGINS: str = "http://localhost:3001,http://localhost:8082"
    
    # LLM API Keys
    GEMINI_API_KEY: Optional[str] = None
    OPENAI_API_KEY: Optional[str] = None
    ANTHROPIC_API_KEY: Optional[str] = None

    # LLM Fallback Chain
    LLM_CHAIN_ORDER: str = "GEMINI,GPT,CLAUDE"
    LLM_FALLBACK_DELAY_SECONDS: int = 2
    LLM_CHAIN_ENABLED: bool = True

    
    # Service URLs
    SPRING_BOOT_URL: str = "http://localhost:8080"
    
    # Processing Settings
    MAX_FILE_SIZE_MB: int = 10
    SUPPORTED_FORMATS: str = "pdf,jpg,jpeg,png"
    REQUEST_TIMEOUT: int = 60

    # RabbitMQ Settings
    RABBITMQ_HOST: str = "rabbitmq"
    RABBITMQ_PORT: int = 5672
    RABBITMQ_USER: str = "fatura_mq"
    RABBITMQ_PASSWORD: str = "mq_secret_2026"
    RABBITMQ_EXTRACTION_QUEUE: str = "invoice.extraction.queue"
    RABBITMQ_RESULT_EXCHANGE: str = "invoice.extraction.result"
    RABBITMQ_RESULT_ROUTING_KEY: str = "extraction.result"
    RABBITMQ_PREFETCH_COUNT: int = 1
    RABBITMQ_VHOST: str = "/"
    RABBITMQ_HEARTBEAT: int = 600
    RABBITMQ_RECONNECT_DELAY: int = 5
    RABBITMQ_RECONNECT_MAX_DELAY: int = 60

    # XML Parser Settings (Phase 18)
    XML_PARSER_MAX_FILE_SIZE_MB: int = 50
    XML_PARSER_DEFAULT_CONFIDENCE: float = 98.0
    XML_PARSER_STRIP_NAMESPACES: bool = False

    INVOICE_FILE_STORAGE_PATH: str = "/data/invoices"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    @property
    def allowed_origins_list(self) -> List[str]:
        return [origin.strip() for origin in self.ALLOWED_ORIGINS.split(",")]

    @property
    def supported_formats_list(self) -> List[str]:
        return [fmt.strip().lower() for fmt in self.SUPPORTED_FORMATS.split(",")]

settings = Settings()

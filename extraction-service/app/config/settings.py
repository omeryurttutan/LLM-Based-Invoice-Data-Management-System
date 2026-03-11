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
    GEMINI_MODEL: str = "gemini-2.5-flash"
    GEMINI_TIMEOUT: int = 30
    GEMINI_MAX_RETRIES: int = 2
    GEMINI_TEMPERATURE: float = 0.1
    GEMINI_MAX_OUTPUT_TOKENS: int = 8192

    # Gemini 2.5 Flash (Fallback 1) — shares GEMINI_API_KEY
    GEMINI_2_5_FLASH_MODEL: str = "gemini-2.5-flash"
    GEMINI_2_5_FLASH_TIMEOUT: int = 30
    GEMINI_2_5_FLASH_MAX_RETRIES: int = 2
    GEMINI_2_5_FLASH_TEMPERATURE: float = 0.1
    GEMINI_2_5_FLASH_MAX_OUTPUT_TOKENS: int = 8192

    # OpenAI GPT-5 nano (Fallback 2)
    OPENAI_API_KEY: Optional[str] = None
    OPENAI_MODEL: str = "gpt-5-nano"
    OPENAI_TIMEOUT: int = 30
    OPENAI_MAX_RETRIES: int = 2
    OPENAI_TEMPERATURE: float = 0.1
    OPENAI_MAX_TOKENS: int = 4096
    OPENAI_MAX_OUTPUT_TOKENS: int = 4096

    # Anthropic (kept for compatibility, not in default chain)
    ANTHROPIC_API_KEY: Optional[str] = None
    ANTHROPIC_MODEL: str = "claude-3-haiku-20240307"
    ANTHROPIC_TIMEOUT: int = 30
    ANTHROPIC_MAX_RETRIES: int = 2
    ANTHROPIC_TEMPERATURE: float = 0.1
    ANTHROPIC_MAX_TOKENS: int = 4096

    # LLM Fallback Chain
    LLM_CHAIN_ORDER: str = "GEMINI_3_FLASH,GEMINI_2_5_FLASH,GPT5_NANO"
    LLM_FALLBACK_DELAY_SECONDS: int = 2
    LLM_CHAIN_ENABLED: bool = True
    LLM_DEFAULT_PROVIDER: str = "GEMINI_3_FLASH"
    LLM_HEALTH_WINDOW_MINUTES: int = 10
    LLM_HEALTH_UNHEALTHY_THRESHOLD: int = 5

    
    # Service URLs
    SPRING_BOOT_URL: str = "http://localhost:8082"
    INTERNAL_API_KEY: str = "fatura-ocr-internal-secret-key-2026"
    
    
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

    # LLM Cost Settings (Per 1K Tokens)
    LLM_COST_GEMINI_INPUT_PER_1K: float = 0.00015
    LLM_COST_GEMINI_OUTPUT_PER_1K: float = 0.0006
    LLM_COST_GPT_INPUT_PER_1K: float = 0.005
    LLM_COST_GPT_OUTPUT_PER_1K: float = 0.015
    LLM_COST_CLAUDE_INPUT_PER_1K: float = 0.0008
    LLM_COST_CLAUDE_OUTPUT_PER_1K: float = 0.004
    
    LLM_USAGE_REPORTING_ENABLED: bool = True

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

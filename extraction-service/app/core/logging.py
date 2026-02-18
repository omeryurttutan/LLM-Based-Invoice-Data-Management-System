import logging
import sys
import structlog
import os
from app.config.settings import settings

def configure_logging():
    """
    Configure structured logging for the application.
    """
    
    # Set default log level
    log_level = getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)
    
    # Configure processors
    shared_processors = [
        structlog.contextvars.merge_contextvars,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
    ]

    # Configure structlog
    if settings.DEBUG:
        # Development mode: pretty printing
        structlog.configure(
            processors=shared_processors + [
                structlog.dev.ConsoleRenderer()
            ],
            context_class=dict,
            logger_factory=structlog.stdlib.LoggerFactory(),
            wrapper_class=structlog.stdlib.BoundLogger,
            cache_logger_on_first_use=True,
        )
    else:
        # Production mode: JSON logging
        structlog.configure(
            processors=shared_processors + [
                structlog.processors.JSONRenderer()
            ],
            context_class=dict,
            logger_factory=structlog.stdlib.LoggerFactory(),
            wrapper_class=structlog.stdlib.BoundLogger,
            cache_logger_on_first_use=True,
        )

    # Configure standard library logging
    handlers = [logging.StreamHandler(sys.stdout)]
    
    # Add File Handler if logs directory exists/is configured
    log_file_path = "/var/log/extraction-service/extraction.log"
    # Ensure directory exists or use a local one for dev
    if not os.path.exists("/var/log/extraction-service"):
        # Fallback for dev/local
        log_file_path = "extraction.log"
        
    file_handler = logging.FileHandler(log_file_path)
    handlers.append(file_handler)

    logging.basicConfig(
        format="%(message)s",
        handlers=handlers,
        level=log_level,
    )
    
    # Quiet down some libraries
    logging.getLogger("uvicorn.access").handlers = []
    # propagate uvicorn errors to root logger
    logging.getLogger("uvicorn.error").handlers = []

logger = structlog.get_logger()

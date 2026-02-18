import json
import time
import threading
import os
from typing import Optional
import pika
from datetime import datetime

from app.config.settings import settings
from app.core.logging import logger
from app.messaging.connection_manager import RabbitMQConnectionManager
from app.messaging.message_models import ExtractionRequest, ExtractionResultMessage
from app.messaging.publisher import ResultPublisher
import structlog
from app.services.extraction.extraction_service import ExtractionService
from app.services.usage_reporter import UsageReporter
from app.core.exceptions import ExtractionServiceException

class ExtractionConsumer(threading.Thread):
    """
    RabbitMQ Consumer that runs in a background thread.
    Consumes invoice extraction requests and publishes results.
    """
    def __init__(self):
        super().__init__()
        self.daemon = True # Daemon thread exits when main program exits
        self.should_stop = False
        self.connection_manager = RabbitMQConnectionManager()
        self.extraction_service = ExtractionService()
        self.usage_reporter = UsageReporter()
        self.is_ready = False # Health check flag

    def run(self):
        """
        Main consumer loop.
        """
        logger.info("consumer_thread_started")
        while not self.should_stop:
            try:
                channel = self.connection_manager.connect()
                self.publisher = ResultPublisher(channel)
                
                # Set up consumer
                channel.basic_consume(
                    queue=settings.RABBITMQ_EXTRACTION_QUEUE,
                    on_message_callback=self.process_message,
                    auto_ack=False
                )
                
                self.is_ready = True
                logger.info("consumer_listening", queue=settings.RABBITMQ_EXTRACTION_QUEUE)
                channel.start_consuming()
            except Exception as e:
                self.is_ready = False
                logger.error("consumer_crashed", error=str(e))
                if not self.should_stop:
                    time.sleep(settings.RABBITMQ_RECONNECT_DELAY)
    
    def stop(self):
        """
        Stops the consumer gracefully.
        """
        logger.info("consumer_stopping")
        self.should_stop = True
        self.is_ready = False
        self.connection_manager.close()

    def process_message(self, ch, method, properties, body):
        """
        Callback for processing a single message.
        """
        start_time = time.time()
        correlation_id = properties.correlation_id
        
        try:
            # 1. Parse Message
            try:
                body_dict = json.loads(body)
                request = ExtractionRequest(**body_dict)
                correlation_id = request.correlation_id # Use from body if valid
            except Exception as e:
                logger.error("message_parsing_failed", error=str(e), body=body[:100])
                # Malformed message -> Reject (Dead Letter)
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
                return

            logger.info("message_received", 
                        message_id=request.message_id, 
                        invoice_id=request.invoice_id, 
                        file_name=request.file_name)

            # 2. Read File
            file_path = request.file_path
            # Ensure path is absolute provided by message or relative to shared volume? 
            # Prompt says: "Path to the file on shared storage (e.g., /data/invoices/uuid-filename.pdf)"
            # So we use it as is.
            
            if not os.path.exists(file_path):
                logger.warning("file_not_found", path=file_path)
                self._send_error_result(
                    request, 
                    "FILE_NOT_FOUND", 
                    f"File not found at {file_path}",
                    start_time
                )
                ch.basic_ack(delivery_tag=method.delivery_tag)
                return

            try:
                with open(file_path, "rb") as f:
                    file_bytes = f.read()
            except Exception as e:
                logger.error("file_read_error", path=file_path, error=str(e))
                self._send_error_result(
                    request, 
                    "FILE_READ_ERROR", 
                    f"Could not read file: {str(e)}", 
                    start_time
                )
                ch.basic_ack(delivery_tag=method.delivery_tag)
                return

            # Bind correlation_id to logger context
            structlog.contextvars.bind_contextvars(correlation_id=correlation_id)

            # 3. Process Extraction (Sync call to async method requires async runner?)
            # ExtractionService methods are async. We are in a sync callback.
            # We need to run async code synchronously here.
            import asyncio
            
            # We can create a new event loop for this thread or use asyncio.run()
            # Since this is a dedicated thread, asyncio.run() is cleaner for each task designated.
            # But ExtractionService initialization might have set up things? No, it's stateless mostly.
            # However, async defs need an event loop.
            try:
                extraction_result = asyncio.run(
                    self.extraction_service.process_file_content(
                        file_bytes, 
                        request.file_name, 
                        request.file_type
                    )
                )
                
                # Report Usage
                if extraction_result.input_tokens is not None and extraction_result.output_tokens is not None:
                    self.usage_reporter.report_usage(
                        provider=extraction_result.provider,
                        model="auto", # Model is dynamic/fallback, usage reporter might need update or just use provider
                        request_type="EXTRACTION",
                        input_tokens=extraction_result.input_tokens,
                        output_tokens=extraction_result.output_tokens,
                        success=True,
                        duration_ms=extraction_result.processing_time_ms,
                        invoice_id=request.invoice_id,
                        correlation_id=correlation_id
                    )

                # 4. Success Result
                result_message = ExtractionResultMessage(
                    correlation_id=request.correlation_id,
                    invoice_id=request.invoice_id,
                    status="COMPLETED",
                    invoice_data=extraction_result.data.model_dump(mode='json'),
                    confidence_score=extraction_result.confidence_score,
                    provider=extraction_result.provider,
                    suggested_status=extraction_result.suggested_status,
                    processing_duration_ms=int((time.time() - start_time) * 1000)
                )
                
                self.publisher.publish_result(result_message)
                ch.basic_ack(delivery_tag=method.delivery_tag)
                
            except Exception as e:
                # Map known exceptions to error codes
                error_code = "INTERNAL_ERROR"
                error_message = str(e)
                
                # Error code mapping based on exception type and message
                error_type = type(e).__name__
                error_str = str(e).lower()
                
                if error_type in ("AllProvidersFailedError",) or "all providers failed" in error_str:
                    error_code = "ALL_PROVIDERS_FAILED"
                elif error_type in ("LLMTimeoutError",) or "timeout" in error_str:
                    error_code = "TIMEOUT"
                elif error_type in ("LLMRateLimitError",) or "rate limit" in error_str:
                    error_code = "RATE_LIMIT"
                elif error_type in ("XMLParserError", "XMLParseError") or ("xml" in error_str and "pars" in error_str):
                    error_code = "XML_PARSE_ERROR"
                elif error_type in ("NotEInvoiceError",) or "not a ubl" in error_str or "not an e-invoice" in error_str:
                    error_code = "NOT_EINVOICE"
                elif isinstance(e, ExtractionServiceException):
                    error_code = getattr(e, 'code', 'INTERNAL_ERROR')
                
                logger.error("extraction_processing_failed", error=str(e), invoice_id=request.invoice_id)
                self._send_error_result(request, error_code, error_message, start_time)
                ch.basic_ack(delivery_tag=method.delivery_tag)

        except Exception as e:
            # Catch-all for consumer logic failure
            logger.error("consumer_critical_error", error=str(e))
            # If we haven't acked yet, we might want to nack or just let connection drop?
            # Safer to nack if we can? Or just exit and let RabbitMQ redeliver?
            # If we crash, RabbitMQ will redeliver.
            try:
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
            except:
                pass

    def _send_error_result(self, request: ExtractionRequest, code: str, message: str, start_time: float):
        result = ExtractionResultMessage(
            correlation_id=request.correlation_id,
            invoice_id=request.invoice_id,
            status="FAILED",
            error_code=code,
            error_message=message,
            processing_duration_ms=int((time.time() - start_time) * 1000)
        )
        self.publisher.publish_result(result)

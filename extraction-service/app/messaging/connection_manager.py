import pika
import time
from typing import Optional
from app.config.settings import settings
from app.core.logging import logger

class RabbitMQConnectionManager:
    """
    Manages RabbitMQ connection and channel.
    Handles reconnection logic with exponential backoff.
    """
    def __init__(self):
        self._connection: Optional[pika.BlockingConnection] = None
        self._channel: Optional[pika.adapters.blocking_connection.BlockingChannel] = None
        self._credentials = pika.PlainCredentials(settings.RABBITMQ_USER, settings.RABBITMQ_PASSWORD)
        self._parameters = pika.ConnectionParameters(
            host=settings.RABBITMQ_HOST,
            port=settings.RABBITMQ_PORT,
            virtual_host=settings.RABBITMQ_VHOST,
            credentials=self._credentials,
            heartbeat=settings.RABBITMQ_HEARTBEAT,
            blocked_connection_timeout=300 
        )
        self._reconnect_delay = settings.RABBITMQ_RECONNECT_DELAY
        self._reconnect_max_delay = settings.RABBITMQ_RECONNECT_MAX_DELAY

    def connect(self) -> pika.adapters.blocking_connection.BlockingChannel:
        """
        Connects to RabbitMQ and returns a channel.
        Retries indefinitely with exponential backoff until successful.
        """
        attempt = 0
        while True:
            try:
                if self._connection and not self._connection.is_closed:
                    if self._channel and not self._channel.is_closed:
                        return self._channel

                logger.info("rabbitmq_connecting", host=settings.RABBITMQ_HOST, attempt=attempt)
                self._connection = pika.BlockingConnection(self._parameters)
                self._channel = self._connection.channel()
                
                # Declare queue (idempotent — Spring Boot also declares it)
                self._channel.queue_declare(
                    queue=settings.RABBITMQ_EXTRACTION_QUEUE, 
                    durable=True,
                    arguments={
                        "x-dead-letter-exchange": "invoice.extraction.dlx",
                        "x-dead-letter-routing-key": "extraction.dead"
                    }
                )
                self._channel.basic_qos(prefetch_count=settings.RABBITMQ_PREFETCH_COUNT)
                
                logger.info("rabbitmq_connected")
                return self._channel

            except pika.exceptions.AMQPConnectionError as e:
                attempt += 1
                delay = self._calculate_backoff(attempt)
                logger.error("rabbitmq_connection_failed", error=str(e), retry_in=delay, attempt=attempt)
                time.sleep(delay)
            except Exception as e:
                attempt += 1
                delay = self._calculate_backoff(attempt)
                logger.error("rabbitmq_unexpected_error", error=str(e), retry_in=delay, attempt=attempt)
                time.sleep(delay)

    def _calculate_backoff(self, attempt: int) -> float:
        """
        Calculate exponential backoff delay.
        delay = min(initial_delay * 2^(attempt-1), max_delay)
        """
        delay = self._reconnect_delay * (2 ** (attempt - 1))
        return min(delay, self._reconnect_max_delay)

    def close(self):
        """Closes the connection safely."""
        try:
            if self._connection and not self._connection.is_closed:
                logger.info("rabbitmq_closing_connection")
                self._connection.close()
        except Exception as e:
            logger.warning("rabbitmq_close_error", error=str(e))

    def get_channel(self):
        """Ensure connection is open and return channel."""
        if not self._connection or self._connection.is_closed or not self._channel or self._channel.is_closed:
            return self.connect()
        return self._channel

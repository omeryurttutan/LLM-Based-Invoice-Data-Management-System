import pika
import json
from app.config.settings import settings
from app.core.logging import logger
from app.messaging.message_models import ExtractionResultMessage

class ResultPublisher:
    """
    Publishes extraction results to RabbitMQ.
    """
    def __init__(self, channel: pika.adapters.blocking_connection.BlockingChannel):
        self._channel = channel

    def publish_result(self, result: ExtractionResultMessage):
        """
        Publishes the extraction result to the result exchange.
        """
        try:
            properties = pika.BasicProperties(
                delivery_mode=2,  # Persistent message
                content_type='application/json',
                correlation_id=result.correlation_id,
                message_id=result.message_id,
                timestamp=int(json.dumps(result.timestamp).strip('"')) if isinstance(result.timestamp, int) else None 
                # Timestamp in properties usually int, but model has str ISO. 
                # Pika timestamp is okay as int? Let's check spec. AMQP timestamp is 64-bit int. 
                # But we put ISO string in body. Pika properties timestamp is usually optional.
                # Let's just set headers or leave timestamp to body.
            )
            
            # Ensure exchange exists (idempotent)
            self._channel.exchange_declare(
                exchange=settings.RABBITMQ_RESULT_EXCHANGE,
                exchange_type='direct',
                durable=True
            )

            self._channel.basic_publish(
                exchange=settings.RABBITMQ_RESULT_EXCHANGE,
                routing_key=settings.RABBITMQ_RESULT_ROUTING_KEY,
                body=result.model_dump_json(),
                properties=properties
            )
            
            logger.info("result_published", 
                        message_id=result.message_id, 
                        correlation_id=result.correlation_id, 
                        status=result.status)
        except Exception as e:
            logger.error("failed_to_publish_result", error=str(e), correlation_id=result.correlation_id)
            raise e

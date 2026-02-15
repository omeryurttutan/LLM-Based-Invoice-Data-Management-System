import pytest
from unittest.mock import MagicMock, patch, AsyncMock
from app.messaging.consumer import RabbitMQConsumer
import json

class TestRabbitMQConsumer:
    
    @pytest.fixture
    def mock_aio_pika(self):
        with patch('app.messaging.consumer.aio_pika') as mock:
            yield mock

    @pytest.mark.asyncio
    async def test_process_message_success(self, mock_aio_pika):
        """Test successful message processing."""
        # Setup mock dependencies
        mock_connection = AsyncMock()
        mock_channel = AsyncMock()
        mock_queue = AsyncMock()
        mock_aio_pika.connect_robust.return_value = mock_connection
        mock_connection.channel.return_value = mock_channel
        mock_channel.declare_queue.return_value = mock_queue
        
        # Create a mock callback
        mock_callback = AsyncMock()
        
        consumer = RabbitMQConsumer(amqp_url="amqp://guest:guest@localhost:5672/", queue_name="test.queue")
        await consumer.connect()
        
        # Simulate receiving a message
        mock_message = AsyncMock()
        mock_message.body = json.dumps({"payload": "data"}).encode()
        
        # Depending on implementation, consumer might have a process_message method 
        # that is called by aio_pika's iterator or callback.
        # Assuming process_message(message, callback) structure for unit testing logic independent of loop
        
        # Testing the internal logic if accessible, or mocking the callback behavior
        # Here we assume there's a handle_message method
        
        await consumer.handle_message(mock_message, mock_callback)
        
        # callback called with payload
        mock_callback.assert_called_once()
        # message acked
        mock_message.ack.assert_called_once()

    @pytest.mark.asyncio
    async def test_process_message_failure(self, mock_aio_pika):
        """Test message processing failure (nack/reject)."""
        mock_callback = AsyncMock()
        mock_callback.side_effect = Exception("Processing Failed")
        
        consumer = RabbitMQConsumer(amqp_url="amqp://")
        
        mock_message = AsyncMock()
        mock_message.body = b"invalid json"
        
        # Process message
        # If implementation handles JSON error gracefully:
        await consumer.handle_message(mock_message, mock_callback)
        
        # If JSON parse fails, callback might not be called or nack is called
        # Check nack logic
        mock_message.nack.assert_called_once() 

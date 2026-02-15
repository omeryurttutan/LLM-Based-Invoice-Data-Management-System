import pytest
from unittest.mock import MagicMock, patch, AsyncMock
from app.messaging.producer import RabbitMQProducer
import json

class TestRabbitMQProducer:
    
    @pytest.fixture
    def mock_aio_pika(self):
        with patch('app.messaging.producer.aio_pika') as mock:
            yield mock

    @pytest.mark.asyncio
    async def test_publish_message(self, mock_aio_pika):
        """Test publishing a message."""
        # Setup mock connection and channel
        mock_connection = AsyncMock()
        mock_channel = AsyncMock()
        mock_aio_pika.connect_robust.return_value = mock_connection
        mock_connection.channel.return_value = mock_channel
        
        producer = RabbitMQProducer(amqp_url="amqp://guest:guest@localhost:5672/")
        await producer.connect()
        
        test_message = {"key": "value"}
        await producer.publish(test_message, routing_key="test.queue")
        
        # Verify publish called
        mock_channel.default_exchange.publish.assert_called_once()
        
        # Verify message body
        call_args = mock_channel.default_exchange.publish.call_args
        message_obj = call_args[0][0] # First arg is Message object
        assert json.loads(message_obj.body) == test_message

    @pytest.mark.asyncio
    async def test_publish_not_connected(self):
        """Test publishing without connection raises error."""
        producer = RabbitMQProducer(amqp_url="amqp://guest:guest@localhost:5672/")
        # Don't call connect()
        
        with pytest.raises(Exception): # Specific exception depends on implementation
            await producer.publish({"key": "value"}, "test.queue")


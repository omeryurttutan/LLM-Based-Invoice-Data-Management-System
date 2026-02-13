import pytest
import json
import asyncio
from unittest.mock import MagicMock, patch, AsyncMock
from app.messaging.consumer import ExtractionConsumer
from app.messaging.message_models import ExtractionResultMessage

def test_consumer_integration_flow():
    # This test mocks RabbitMQ and LLM but uses real Consumer + ExtractionService logic
    
    with patch('app.messaging.consumer.RabbitMQConnectionManager') as MockConnMgr, \
         patch('app.messaging.consumer.ExtractionService') as MockServiceCls:
        
        # Setup Consumer
        consumer = ExtractionConsumer()
        
        # Mock RabbitMQ interactions
        mock_channel = MagicMock()
        consumer.connection_manager.connect.return_value = mock_channel
        
        # Mock ExtractionService instance
        mock_service_instance = MockServiceCls.return_value
        
        # Mock Extraction Result
        mock_extraction_result = MagicMock()
        mock_extraction_result.data.model_dump.return_value = {"invoice_number": "INT-123"}
        mock_extraction_result.confidence_score = 98.0
        mock_extraction_result.provider = "GEMINI"
        mock_extraction_result.suggested_status = "AUTO_VERIFIED"
        
        # Critical: ExtractionService.process_file_content is called via asyncio.run defined in Consumer class
        # mocking asyncio.run in test context is tricky if we are already async. 
        # But consumer logic is sync (run method).
        # We need to simulate the callback execution.
        
        # Since we can't easily run the actual consumer.process_message because it calls asyncio.run
        # and we might be in an async test function (if using pytest-asyncio).
        # Actually, consumer.process_message is synchronous.
        # So we can call it directly.
        
        # Mock process_file_content to return a coroutine
        async def mock_process(*args, **kwargs):
            return mock_extraction_result
            
        mock_service_instance.process_file_content.side_effect = mock_process 
        # But wait, asyncio.run(coro) works.
        
        # Prepare Message
        body = json.dumps({
            "message_id": "msg-int-1",
            "invoice_id": 999,
            "company_id": 1,
            "user_id": 1,
            "file_path": "/tmp/test_invoice.xml", # We will mock file read
            "file_name": "test_invoice.xml",
            "file_type": "application/xml",
            "file_size": 200,
            "timestamp": "2024-01-01T12:00:00Z",
            "correlation_id": "corr-int-1"
        })
        properties = MagicMock()
        properties.correlation_id = "corr-int-1"
        method = MagicMock()
        method.delivery_tag = 100
        
        # Mock Publisher
        consumer.publisher = MagicMock()
        
        # Mock File System
        with patch('os.path.exists', return_value=True), \
             patch('builtins.open', new_callable=MagicMock) as mock_open:
            
            mock_file = MagicMock()
            mock_file.__enter__.return_value.read.return_value = b"<Invoice>...</Invoice>"
            mock_open.return_value = mock_file
            
            # Execut Callback
            consumer.process_message(mock_channel, method, properties, body)
            
        # Assertions
        mock_service_instance.process_file_content.assert_called_once()
        consumer.publisher.publish_result.assert_called_once()
        
        args, _ = consumer.publisher.publish_result.call_args
        result = args[0]
        
        assert isinstance(result, ExtractionResultMessage)
        assert result.invoice_id == 999
        assert result.status == "COMPLETED"
        assert result.invoice_data["invoice_number"] == "INT-123"
        
        mock_channel.basic_ack.assert_called_once_with(delivery_tag=100)

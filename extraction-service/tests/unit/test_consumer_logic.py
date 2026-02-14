import pytest
import json
import logging
from unittest.mock import MagicMock, patch, AsyncMock
from app.messaging.consumer import ExtractionConsumer
from app.messaging.message_models import ExtractionRequest, ExtractionResultMessage

# Mock ExtractionResponse model structure
class MockExtractionResponse:
    def __init__(self, data=None, confidence_score=0.9, provider="MOCK", suggested_status="AUTO_VERIFIED"):
        self.data = MagicMock()
        self.data.model_dump.return_value = data or {"invoice_number": "123"}
        self.confidence_score = confidence_score
        self.provider = provider
        self.suggested_status = suggested_status

@pytest.fixture
def consumer():
    with patch('app.messaging.consumer.RabbitMQConnectionManager'), \
         patch('app.messaging.consumer.ExtractionService'):
        consumer = ExtractionConsumer()
        # Mock internal components
        consumer.connection_manager = MagicMock()
        consumer.extraction_service = MagicMock()
        consumer.publisher = MagicMock()
        return consumer

def test_process_message_success(consumer):
    # Setup
    body = json.dumps({
        "message_id": "msg-123",
        "invoice_id": 1,
        "company_id": 1,
        "user_id": 1,
        "file_path": "/path/to/file.pdf",
        "file_name": "file.pdf",
        "file_type": "application/pdf",
        "file_size": 100,
        "timestamp": "2024-01-01T10:00:00Z",
        "correlation_id": "corr-123"
    })
    properties = MagicMock()
    properties.correlation_id = "corr-123"
    channel = MagicMock()
    method = MagicMock()
    method.delivery_tag = 1

    # Mocks
    consumer.extraction_service.process_file_content = AsyncMock(return_value=MockExtractionResponse())
    
    with patch('os.path.exists', return_value=True), \
         patch('builtins.open', new_callable=MagicMock) as mock_open: # Ensure this mocks open correctly
         # Note: builtins.open is tricky in some envs, using mock_open from unittest.mock is better?
         # But usually simple patch works if we don't iterate lines.
         mock_file = MagicMock()
         mock_file.__enter__.return_value.read.return_value = b"fake-content"
         mock_open.return_value = mock_file
         
         # Execute
         consumer.process_message(channel, method, properties, body)

    # Assert
    consumer.extraction_service.process_file_content.assert_called_once()
    consumer.publisher.publish_result.assert_called_once()
    args, _ = consumer.publisher.publish_result.call_args
    result_msg = args[0]
    
    assert isinstance(result_msg, ExtractionResultMessage)
    assert result_msg.status == "COMPLETED"
    assert result_msg.invoice_id == 1
    assert result_msg.confidence_score == 0.9
    
    channel.basic_ack.assert_called_once_with(delivery_tag=1)

def test_process_message_file_not_found(consumer):
    body = json.dumps({
        "message_id": "msg-123",
        "invoice_id": 1,
        "company_id": 1,
        "user_id": 1,
        "file_path": "/path/to/missing.pdf",
        "file_name": "missing.pdf",
        "file_type": "application/pdf",
        "file_size": 100,
        "timestamp": "2024-01-01T10:00:00Z",
        "correlation_id": "corr-123"
    })
    properties = MagicMock()
    channel = MagicMock()
    method = MagicMock()
    
    with patch('os.path.exists', return_value=False):
         consumer.process_message(channel, method, properties, body)
         
    consumer.publisher.publish_result.assert_called_once()
    args, _ = consumer.publisher.publish_result.call_args
    result_msg = args[0]
    
    assert result_msg.status == "FAILED"
    assert result_msg.error_code == "FILE_NOT_FOUND"
    channel.basic_ack.assert_called_once()

def test_process_message_malformed(consumer):
    body = "not json"
    properties = MagicMock()
    channel = MagicMock()
    method = MagicMock()
    
    consumer.process_message(channel, method, properties, body)
    
    channel.basic_nack.assert_called_once_with(delivery_tag=method.delivery_tag, requeue=False)
    consumer.publisher.publish_result.assert_not_called()

def test_process_message_extraction_failed(consumer):
    # Setup
    body = json.dumps({
        "message_id": "msg-123",
        "invoice_id": 1,
        "company_id": 1,
        "user_id": 1,
        "file_path": "/path/to/file.pdf",
        "file_name": "file.pdf",
        "file_type": "application/pdf",
        "file_size": 100,
        "timestamp": "2024-01-01T10:00:00Z",
        "correlation_id": "corr-123"
    })
    properties = MagicMock()
    channel = MagicMock()
    method = MagicMock()

    # Mocks
    consumer.extraction_service.process_file_content = AsyncMock(side_effect=Exception("All providers failed"))
    
    with patch('os.path.exists', return_value=True), \
         patch('builtins.open', new_callable=MagicMock) as mock_open:
         mock_file = MagicMock()
         mock_file.__enter__.return_value.read.return_value = b"fake-content"
         mock_open.return_value = mock_file
         
         consumer.process_message(channel, method, properties, body)

    # Assert
    consumer.publisher.publish_result.assert_called_once()
    args, _ = consumer.publisher.publish_result.call_args
    result_msg = args[0]
    
    assert result_msg.status == "FAILED"
    assert result_msg.error_code == "ALL_PROVIDERS_FAILED"
    channel.basic_ack.assert_called_once()

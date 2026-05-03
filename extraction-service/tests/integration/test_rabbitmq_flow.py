"""
Tests for RabbitMQ consumer message processing flow.
These tests mock RabbitMQ, the file system, and the LLM to test
the consumer's business logic (message parsing, error handling, result publishing).
"""
import pytest
import json
from unittest.mock import MagicMock, patch
from datetime import datetime

from app.messaging.message_models import ExtractionResultMessage


def _make_body(overrides=None):
    """Create a valid ExtractionRequest message body."""
    base = {
        "message_id": "msg-123",
        "invoice_id": 123,
        "company_id": 1,
        "user_id": 1,
        "file_path": "/data/invoices/test_invoice.jpg",
        "file_name": "test_invoice.jpg",
        "file_type": "image/jpeg",
        "file_size": 1024,
        "priority": "NORMAL",
        "attempt": 1,
        "max_attempts": 3,
        "timestamp": datetime.utcnow().isoformat(),
        "correlation_id": "corr-123",
    }
    if overrides:
        base.update(overrides)
    return json.dumps(base)


def test_consumer_happy_path(
    mock_gemini_api,
    sample_image_bytes: bytes,
):
    """
    Test: Message → Parse → Read File → Extract → Publish Result.
    """
    with patch("app.messaging.consumer.RabbitMQConnectionManager"), \
         patch("app.messaging.consumer.ResultPublisher"):
        from app.messaging.consumer import ExtractionConsumer
        consumer = ExtractionConsumer()

    consumer.publisher = MagicMock()

    with patch("builtins.open", new_callable=MagicMock) as mock_open:
        mock_file = MagicMock()
        mock_file.__enter__.return_value.read.return_value = sample_image_bytes
        mock_open.return_value = mock_file

        with patch("os.path.exists", return_value=True):
            body = _make_body()
            mock_ch = MagicMock()
            mock_method = MagicMock()
            mock_method.delivery_tag = 1
            mock_props = MagicMock()
            mock_props.correlation_id = "corr-123"

            consumer.process_message(mock_ch, mock_method, mock_props, body)

            assert consumer.publisher.publish_result.called
            result = consumer.publisher.publish_result.call_args[0][0]

            assert isinstance(result, ExtractionResultMessage)
            assert result.status == "COMPLETED"
            assert result.invoice_id == 123
            assert result.correlation_id == "corr-123"
            assert result.invoice_data is not None
            assert result.provider == "GEMINI"

            mock_ch.basic_ack.assert_called_with(delivery_tag=1)


def test_consumer_file_not_found():
    """File does not exist → FAILED result with FILE_NOT_FOUND."""
    with patch("app.messaging.consumer.RabbitMQConnectionManager"), \
         patch("app.messaging.consumer.ResultPublisher"):
        from app.messaging.consumer import ExtractionConsumer
        consumer = ExtractionConsumer()

    consumer.publisher = MagicMock()

    with patch("os.path.exists", return_value=False):
        body = _make_body({
            "message_id": "msg-404",
            "invoice_id": 404,
            "file_name": "missing.jpg",
            "file_path": "/missing/path.jpg",
            "correlation_id": "corr-404",
        })
        mock_ch = MagicMock()
        mock_method = MagicMock()
        mock_method.delivery_tag = 2
        mock_props = MagicMock()

        consumer.process_message(mock_ch, mock_method, mock_props, body)

        assert consumer.publisher.publish_result.called
        result = consumer.publisher.publish_result.call_args[0][0]

        assert result.status == "FAILED"
        assert result.error_code == "FILE_NOT_FOUND"

        mock_ch.basic_ack.assert_called_with(delivery_tag=2)


def test_consumer_malformed_message():
    """Malformed JSON → Nack (dead letter)."""
    with patch("app.messaging.consumer.RabbitMQConnectionManager"), \
         patch("app.messaging.consumer.ResultPublisher"):
        from app.messaging.consumer import ExtractionConsumer
        consumer = ExtractionConsumer()

    body = "INVALID JSON"
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 3
    mock_props = MagicMock()

    consumer.process_message(mock_ch, mock_method, mock_props, body)

    mock_ch.basic_nack.assert_called_with(delivery_tag=3, requeue=False)

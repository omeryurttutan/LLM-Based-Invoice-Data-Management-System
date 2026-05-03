"""
Integration test conftest — patches RabbitMQ consumer globally.
"""
import pytest
import unittest.mock
from unittest.mock import MagicMock


@pytest.fixture(autouse=True)
def _patch_rabbitmq():
    """Auto-patch ExtractionConsumer for every integration test."""
    with unittest.mock.patch("app.messaging.consumer.RabbitMQConnectionManager"), \
         unittest.mock.patch("app.messaging.consumer.ResultPublisher"), \
         unittest.mock.patch("app.main.ExtractionConsumer") as MockConsumer:
        mock_instance = MagicMock()
        MockConsumer.return_value = mock_instance
        yield MockConsumer

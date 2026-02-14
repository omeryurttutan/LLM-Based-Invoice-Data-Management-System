import pytest
from unittest.mock import MagicMock, patch, PropertyMock
import pika
from app.messaging.connection_manager import RabbitMQConnectionManager

@patch('app.messaging.connection_manager.pika.BlockingConnection')
def test_connect_success(mock_connection_cls):
    # Setup mock
    mock_connection = MagicMock()
    mock_channel = MagicMock()
    mock_connection.channel.return_value = mock_channel
    mock_connection.is_closed = False
    mock_channel.is_closed = False
    
    mock_connection_cls.return_value = mock_connection
    
    manager = RabbitMQConnectionManager()
    channel = manager.connect()
    
    assert channel == mock_channel
    mock_connection_cls.assert_called_once()
    mock_channel.queue_declare.assert_called_once()

@patch('app.messaging.connection_manager.time.sleep')
@patch('app.messaging.connection_manager.pika.BlockingConnection')
def test_reconnect_exponential_backoff(mock_connection_cls, mock_sleep):
    """Test that connection retries use exponential backoff and eventually succeed."""
    mock_connection = MagicMock()
    mock_channel = MagicMock()
    mock_connection.channel.return_value = mock_channel
    mock_connection.is_closed = False
    mock_channel.is_closed = False
    
    # Fail 3 times, then succeed
    mock_connection_cls.side_effect = [
        pika.exceptions.AMQPConnectionError("Connection refused"),
        pika.exceptions.AMQPConnectionError("Connection refused"),
        pika.exceptions.AMQPConnectionError("Connection refused"),
        mock_connection
    ]
    
    manager = RabbitMQConnectionManager()
    channel = manager.connect()
    
    assert channel == mock_channel
    assert mock_connection_cls.call_count == 4
    # Verify exponential backoff delays: 5, 10, 20
    assert mock_sleep.call_count == 3
    calls = [c[0][0] for c in mock_sleep.call_args_list]
    assert calls[0] == 5   # 5 * 2^0
    assert calls[1] == 10  # 5 * 2^1
    assert calls[2] == 20  # 5 * 2^2

@patch('app.messaging.connection_manager.pika.BlockingConnection')
def test_get_channel_reuses_connection(mock_connection_cls):
    mock_connection = MagicMock()
    mock_channel = MagicMock()
    mock_connection.channel.return_value = mock_channel
    mock_connection.is_closed = False
    mock_channel.is_closed = False
    mock_connection_cls.return_value = mock_connection
    
    manager = RabbitMQConnectionManager()
    manager._connection = mock_connection
    manager._channel = mock_channel
    
    channel = manager.get_channel()
    assert channel == mock_channel
    mock_connection_cls.assert_not_called()

def test_calculate_backoff():
    """Test exponential backoff calculation with max cap."""
    manager = RabbitMQConnectionManager.__new__(RabbitMQConnectionManager)
    manager._reconnect_delay = 5
    manager._reconnect_max_delay = 60
    
    assert manager._calculate_backoff(1) == 5    # 5 * 2^0
    assert manager._calculate_backoff(2) == 10   # 5 * 2^1
    assert manager._calculate_backoff(3) == 20   # 5 * 2^2
    assert manager._calculate_backoff(4) == 40   # 5 * 2^3
    assert manager._calculate_backoff(5) == 60   # capped at 60
    assert manager._calculate_backoff(6) == 60   # capped at 60

@patch('app.messaging.connection_manager.pika.BlockingConnection')
def test_connection_parameters_include_vhost(mock_connection_cls):
    """Verify ConnectionParameters include virtual_host."""
    manager = RabbitMQConnectionManager()
    params = manager._parameters
    assert params.virtual_host == "/"

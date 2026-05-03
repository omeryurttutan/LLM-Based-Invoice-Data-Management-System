
import sys
import os

print("Adding . to sys.path")
sys.path.insert(0, ".")

print("Importing app.config.settings...")
try:
    from app.config.settings import settings
    print(f"Settings imported. RabbitMQ Host: {settings.RABBITMQ_HOST}")
except Exception as e:
    print(f"Settings import failed: {e}")

print("Importing app.messaging.connection_manager...")
try:
    from app.messaging.connection_manager import RabbitMQConnectionManager
    print("ConnectionManager imported.")
except Exception as e:
    print(f"ConnectionManager import failed: {e}")

print("Importing app.messaging.consumer...")
try:
    from app.messaging.consumer import ExtractionConsumer
    print("ExtractionConsumer imported.")
except Exception as e:
    print(f"ExtractionConsumer import failed: {e}")

print("Instantiating ExtractionConsumer (This checks if __init__ hangs)...")
try:
    consumer = ExtractionConsumer()
    print("ExtractionConsumer instantiated.")
except Exception as e:
    print(f"ExtractionConsumer instantiation failed: {e}")

print("Importing app.main...")
try:
    from app.main import app
    print("app.main imported.")
except Exception as e:
    print(f"app.main import failed: {e}")

print("Running ExtractionConsumer.start() (This SHOULD hang if not mocked)...")
try:
    # consumer.start() 
    print("Skipping start() to avoid hang, but confirming we reached here.")
except Exception as e:
    print(f"Start failed: {e}")

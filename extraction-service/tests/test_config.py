import pytest
import os
from unittest.mock import patch
from app.config.settings import Settings

def test_settings_load_defaults():
    # Unset env vars to test defaults
    # Note: os.environ changes might affect other tests if not careful, 
    # but for unit testing settings this is standard.
    # Ideally use pytest's monkeypatch fixture but Settings definition 
    # is often loaded at import time. 
    with patch.dict(os.environ, {}, clear=True):
        settings = Settings(_env_file=None)
        assert settings.APP_NAME == "Fatura OCR Extraction Service"
        assert settings.DEBUG is False
        assert settings.LOG_LEVEL == "INFO"

def test_allowed_origins_list():
    settings = Settings(ALLOWED_ORIGINS="http://a.com, http://b.com")
    assert settings.allowed_origins_list == ["http://a.com", "http://b.com"]

def test_supported_formats_list():
    settings = Settings(SUPPORTED_FORMATS="PDF, JPG")
    assert settings.supported_formats_list == ["pdf", "jpg"]

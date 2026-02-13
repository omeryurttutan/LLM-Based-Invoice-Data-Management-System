import time
from datetime import datetime, timedelta
from typing import Dict
from app.models.provider_status import ProviderHealth, HealthStatus
from app.config.settings import settings

class ProviderHealthManager:
    """
    Tracks health status of LLM providers.
    Uses a time window to track failure counts and determine status.
    """
    
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ProviderHealthManager, cls).__new__(cls)
            cls._instance._minit()
        return cls._instance

    def _minit(self):
        self.providers: Dict[str, ProviderHealth] = {}
        # Simple in-memory tracking. For multi-process/instance, would need redis/db.
        # Given this is a graduation project, in-memory is acceptable.
        
        self.window_minutes = 10
        self.unhealthy_threshold = 5
        self.degraded_threshold = 2
        
    def get_health(self, provider_name: str) -> ProviderHealth:
        if provider_name not in self.providers:
            self.providers[provider_name] = ProviderHealth(name=provider_name)
        return self.providers[provider_name]

    def record_success(self, provider_name: str):
        health = self.get_health(provider_name)
        health.last_success = datetime.now()
        health.total_successes += 1
        # Reset current window failures on success? 
        # Strategy: Strict window reset or just decrement?
        # Let's reset window count on success to be optimistic, 
        # or we keep it and let it expire. 
        # For simplicity: One success is good sign, but doesn't erase history immediately if underlying issue persists.
        # But if we are successful, clearly it's working now.
        health.failure_count_window = 0 
        self._update_status(health)

    def record_failure(self, provider_name: str):
        health = self.get_health(provider_name)
        health.last_failure = datetime.now()
        health.total_failures += 1
        health.failure_count_window += 1
        self._update_status(health)

    def _update_status(self, health: ProviderHealth):
        # Check if window expired (reset count if last failure was long ago)
        # Actually logic needs to be cleaner: failures should expire.
        # Simplified: If last failure was > window ago, reset count.
        if health.last_failure and (datetime.now() - health.last_failure) > timedelta(minutes=self.window_minutes):
             health.failure_count_window = 0

        if health.failure_count_window >= self.unhealthy_threshold:
            health.status = HealthStatus.UNHEALTHY
        elif health.failure_count_window >= self.degraded_threshold:
            health.status = HealthStatus.DEGRADED
        else:
            health.status = HealthStatus.HEALTHY

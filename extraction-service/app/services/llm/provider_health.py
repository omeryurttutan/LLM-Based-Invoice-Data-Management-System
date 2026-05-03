import time
from collections import deque
from datetime import datetime, timedelta
from typing import Dict
from app.models.provider_status import ProviderHealth, HealthStatus
from app.config.settings import settings


class ProviderHealthManager:
    """
    Tracks health status of LLM providers using a sliding window.
    
    Uses a window of the last 10 requests per provider to determine status:
    - HEALTHY: 8+ successes out of 10 (≤ 2 failures)
    - DEGRADED: 4-7 successes out of 10 (3-6 failures)  
    - UNHEALTHY: 0-3 successes out of 10 (7+ failures)
    
    Recovery:
    - UNHEALTHY providers are retried every 5 minutes (recovery probe)
    - Single success on probe → DEGRADED
    - 3 consecutive successes → HEALTHY
    """

    WINDOW_SIZE = 10
    HEALTHY_MIN_SUCCESSES = 8     # 8+ out of 10 → HEALTHY
    DEGRADED_MIN_SUCCESSES = 4    # 4-7 out of 10 → DEGRADED
    # 0-3 out of 10 → UNHEALTHY
    RECOVERY_INTERVAL_MINUTES = 5
    CONSECUTIVE_SUCCESSES_FOR_HEALTHY = 3

    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ProviderHealthManager, cls).__new__(cls)
            cls._instance._minit()
        return cls._instance

    def _minit(self):
        self.providers: Dict[str, ProviderHealth] = {}
        # Sliding window: deque of booleans (True=success, False=failure)
        self._windows: Dict[str, deque] = {}

    def get_health(self, provider_name: str) -> ProviderHealth:
        if provider_name not in self.providers:
            self.providers[provider_name] = ProviderHealth(name=provider_name)
            self._windows[provider_name] = deque(maxlen=self.WINDOW_SIZE)
        return self.providers[provider_name]

    def _get_window(self, provider_name: str) -> deque:
        if provider_name not in self._windows:
            self._windows[provider_name] = deque(maxlen=self.WINDOW_SIZE)
        return self._windows[provider_name]

    def record_success(self, provider_name: str):
        health = self.get_health(provider_name)
        window = self._get_window(provider_name)

        window.append(True)
        health.last_success = datetime.now()
        health.total_successes += 1
        health.consecutive_successes += 1

        self._update_status(health, window)

    def record_failure(self, provider_name: str):
        health = self.get_health(provider_name)
        window = self._get_window(provider_name)

        window.append(False)
        health.last_failure = datetime.now()
        health.total_failures += 1
        health.consecutive_successes = 0  # Reset on failure

        self._update_status(health, window)

    def _update_status(self, health: ProviderHealth, window: deque):
        """Update health status based on sliding window."""
        if len(window) == 0:
            health.status = HealthStatus.HEALTHY
            return

        total = len(window)
        successes = sum(1 for result in window if result)

        # Use proportional thresholds: success_rate determines status
        # HEALTHY: >= 80% success rate (8/10 when full)
        # DEGRADED: >= 40% success rate (4/10 when full)
        # UNHEALTHY: < 40% success rate
        success_rate = successes / total

        if success_rate >= 0.8:
            health.status = HealthStatus.HEALTHY
        elif success_rate >= 0.4:
            health.status = HealthStatus.DEGRADED
        else:
            health.status = HealthStatus.UNHEALTHY

        # Recovery shortcut: 3 consecutive successes → HEALTHY
        if health.consecutive_successes >= self.CONSECUTIVE_SUCCESSES_FOR_HEALTHY:
            health.status = HealthStatus.HEALTHY

    def should_attempt_recovery(self, provider_name: str) -> bool:
        """Check if an UNHEALTHY provider should be probed for recovery."""
        health = self.get_health(provider_name)
        if health.status != HealthStatus.UNHEALTHY:
            return False

        now = datetime.now()
        if health.last_recovery_probe is None:
            health.last_recovery_probe = now
            return True

        elapsed = now - health.last_recovery_probe
        if elapsed >= timedelta(minutes=self.RECOVERY_INTERVAL_MINUTES):
            health.last_recovery_probe = now
            return True

        return False

    def get_window_stats(self, provider_name: str) -> dict:
        """Get sliding window statistics for a provider."""
        window = self._get_window(provider_name)
        health = self.get_health(provider_name)
        total = len(window)
        successes = sum(1 for r in window if r) if total > 0 else 0
        failures = total - successes

        return {
            "window_size": self.WINDOW_SIZE,
            "total_in_window": total,
            "successes": successes,
            "failures": failures,
            "success_rate": round(successes / total * 100, 1) if total > 0 else 100.0,
            "status": health.status.value,
            "consecutive_successes": health.consecutive_successes,
            "total_successes": health.total_successes,
            "total_failures": health.total_failures,
            "last_success": health.last_success.isoformat() if health.last_success else None,
            "last_failure": health.last_failure.isoformat() if health.last_failure else None,
        }

    def reset(self):
        """Reset all health data. Useful for testing."""
        self.providers.clear()
        self._windows.clear()

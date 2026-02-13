import pytest
from datetime import datetime, timedelta
from unittest.mock import patch
from app.services.llm.provider_health import ProviderHealthManager, HealthStatus
from app.models.provider_status import ProviderHealth


class TestProviderHealth:
    """Unit tests for ProviderHealthManager — Phase 16 requirement."""

    @pytest.fixture(autouse=True)
    def health_manager(self):
        # Reset singleton for each test
        ProviderHealthManager._instance = None
        self.manager = ProviderHealthManager()
        return self.manager

    def test_initial_state_is_healthy(self):
        """All providers start as HEALTHY."""
        health = self.manager.get_health("GEMINI")
        assert health.status == HealthStatus.HEALTHY
        assert health.failure_count_window == 0
        assert health.total_failures == 0
        assert health.total_successes == 0

    def test_failure_increments_count(self):
        """Each failure increases failure_count_window and total_failures."""
        self.manager.record_failure("GPT")
        health = self.manager.get_health("GPT")
        assert health.failure_count_window == 1
        assert health.total_failures == 1

    def test_success_resets_window(self):
        """A success resets the failure_count_window to 0."""
        self.manager.record_failure("CLAUDE")
        self.manager.record_failure("CLAUDE")
        self.manager.record_success("CLAUDE")
        health = self.manager.get_health("CLAUDE")
        assert health.failure_count_window == 0
        assert health.total_successes == 1

    def test_degraded_threshold(self):
        """2-4 failures within window → DEGRADED status."""
        self.manager.record_failure("GEMINI")
        self.manager.record_failure("GEMINI")
        health = self.manager.get_health("GEMINI")
        assert health.status == HealthStatus.DEGRADED

    def test_unhealthy_threshold(self):
        """5+ failures within window → UNHEALTHY status."""
        for _ in range(5):
            self.manager.record_failure("GPT")
        health = self.manager.get_health("GPT")
        assert health.status == HealthStatus.UNHEALTHY

    def test_health_window_expiry(self):
        """Failures older than the window are ignored (count resets)."""
        self.manager.record_failure("CLAUDE")
        self.manager.record_failure("CLAUDE")
        health = self.manager.get_health("CLAUDE")
        assert health.status == HealthStatus.DEGRADED

        # Simulate last failure was 11 minutes ago (beyond 10 min window)
        health.last_failure = datetime.now() - timedelta(minutes=11)
        self.manager._update_status(health)
        assert health.status == HealthStatus.HEALTHY
        assert health.failure_count_window == 0

    def test_success_records_timestamp(self):
        """record_success sets last_success timestamp."""
        self.manager.record_success("GEMINI")
        health = self.manager.get_health("GEMINI")
        assert health.last_success is not None
        assert isinstance(health.last_success, datetime)

    def test_failure_records_timestamp(self):
        """record_failure sets last_failure timestamp."""
        self.manager.record_failure("GPT")
        health = self.manager.get_health("GPT")
        assert health.last_failure is not None
        assert isinstance(health.last_failure, datetime)

    def test_multiple_providers_independent(self):
        """Health tracking for each provider is independent."""
        self.manager.record_failure("GEMINI")
        self.manager.record_failure("GEMINI")
        self.manager.record_failure("GEMINI")

        assert self.manager.get_health("GEMINI").status == HealthStatus.DEGRADED
        assert self.manager.get_health("GPT").status == HealthStatus.HEALTHY
        assert self.manager.get_health("CLAUDE").status == HealthStatus.HEALTHY

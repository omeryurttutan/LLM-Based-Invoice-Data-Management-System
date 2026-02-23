"""
Unit tests for ProviderHealthManager — Phase 16.
Tests sliding window health tracking with HEALTHY/DEGRADED/UNHEALTHY states.
"""
import pytest
from datetime import datetime, timedelta
from unittest.mock import patch
from app.services.llm.provider_health import ProviderHealthManager, HealthStatus
from app.models.provider_status import ProviderHealth


class TestProviderHealth:
    """Unit tests for ProviderHealthManager — Phase 16 sliding window."""

    @pytest.fixture(autouse=True)
    def health_manager(self):
        # Reset singleton for each test
        ProviderHealthManager._instance = None
        self.manager = ProviderHealthManager()
        return self.manager

    # ─── Initial State ──────────────────────────────────────────────────────

    def test_initial_state_is_healthy(self):
        """All providers start as HEALTHY."""
        health = self.manager.get_health("GEMINI_3_FLASH")
        assert health.status == HealthStatus.HEALTHY
        assert health.total_failures == 0
        assert health.total_successes == 0
        assert health.consecutive_successes == 0

    # ─── Sliding Window: HEALTHY ────────────────────────────────────────────

    def test_healthy_with_8_successes_out_of_10(self):
        """8+ successes out of 10 → HEALTHY."""
        for _ in range(8):
            self.manager.record_success("GEMINI_3_FLASH")
        for _ in range(2):
            self.manager.record_failure("GEMINI_3_FLASH")

        health = self.manager.get_health("GEMINI_3_FLASH")
        # Last two were failures, so consecutive_successes = 0
        # Window: 8 successes + 2 failures = 8/10 → HEALTHY
        assert health.status == HealthStatus.HEALTHY

    def test_healthy_with_all_successes(self):
        """10/10 successes → HEALTHY."""
        for _ in range(10):
            self.manager.record_success("GEMINI_3_FLASH")

        health = self.manager.get_health("GEMINI_3_FLASH")
        assert health.status == HealthStatus.HEALTHY
        assert health.total_successes == 10

    # ─── Sliding Window: DEGRADED ───────────────────────────────────────────

    def test_degraded_with_5_successes_out_of_10(self):
        """5 successes out of 10 → DEGRADED."""
        for _ in range(5):
            self.manager.record_success("GEMINI_2_5_FLASH")
        for _ in range(5):
            self.manager.record_failure("GEMINI_2_5_FLASH")

        health = self.manager.get_health("GEMINI_2_5_FLASH")
        assert health.status == HealthStatus.DEGRADED

    def test_degraded_with_4_successes_out_of_10(self):
        """4 successes out of 10 → DEGRADED (boundary)."""
        for _ in range(4):
            self.manager.record_success("GPT5_NANO")
        for _ in range(6):
            self.manager.record_failure("GPT5_NANO")

        health = self.manager.get_health("GPT5_NANO")
        assert health.status == HealthStatus.DEGRADED

    # ─── Sliding Window: UNHEALTHY ──────────────────────────────────────────

    def test_unhealthy_with_3_successes_out_of_10(self):
        """3 successes out of 10 → UNHEALTHY."""
        for _ in range(3):
            self.manager.record_success("GEMINI_3_FLASH")
        for _ in range(7):
            self.manager.record_failure("GEMINI_3_FLASH")

        health = self.manager.get_health("GEMINI_3_FLASH")
        assert health.status == HealthStatus.UNHEALTHY

    def test_unhealthy_with_all_failures(self):
        """0/10 successes → UNHEALTHY."""
        for _ in range(10):
            self.manager.record_failure("GEMINI_3_FLASH")

        health = self.manager.get_health("GEMINI_3_FLASH")
        assert health.status == HealthStatus.UNHEALTHY

    # ─── Recovery: Consecutive Successes ────────────────────────────────────

    def test_recovery_3_consecutive_successes(self):
        """3 consecutive successes → HEALTHY regardless of window."""
        # First create an UNHEALTHY state
        for _ in range(10):
            self.manager.record_failure("GPT5_NANO")
        assert self.manager.get_health("GPT5_NANO").status == HealthStatus.UNHEALTHY

        # Now 3 consecutive successes should recover to HEALTHY
        for _ in range(3):
            self.manager.record_success("GPT5_NANO")

        health = self.manager.get_health("GPT5_NANO")
        assert health.status == HealthStatus.HEALTHY
        assert health.consecutive_successes == 3

    def test_consecutive_successes_reset_on_failure(self):
        """Failure resets consecutive success counter."""
        self.manager.record_success("GEMINI_3_FLASH")
        self.manager.record_success("GEMINI_3_FLASH")
        assert self.manager.get_health("GEMINI_3_FLASH").consecutive_successes == 2

        self.manager.record_failure("GEMINI_3_FLASH")
        assert self.manager.get_health("GEMINI_3_FLASH").consecutive_successes == 0

    # ─── Recovery Probe Timing ──────────────────────────────────────────────

    def test_recovery_probe_first_time(self):
        """First recovery probe attempt should always return True."""
        for _ in range(10):
            self.manager.record_failure("GPT5_NANO")
        assert self.manager.should_attempt_recovery("GPT5_NANO") is True

    def test_recovery_probe_too_soon(self):
        """Recovery probe should be rejected if < 5 minutes since last."""
        for _ in range(10):
            self.manager.record_failure("GPT5_NANO")
        # First probe
        self.manager.should_attempt_recovery("GPT5_NANO")
        # Second probe immediately → too soon
        assert self.manager.should_attempt_recovery("GPT5_NANO") is False

    def test_recovery_probe_after_interval(self):
        """Recovery probe should succeed after 5-minute interval."""
        for _ in range(10):
            self.manager.record_failure("GPT5_NANO")
        # First probe
        self.manager.should_attempt_recovery("GPT5_NANO")

        # Simulate time passage
        health = self.manager.get_health("GPT5_NANO")
        health.last_recovery_probe = datetime.now() - timedelta(minutes=6)

        assert self.manager.should_attempt_recovery("GPT5_NANO") is True

    def test_recovery_probe_not_for_healthy(self):
        """Healthy providers don't need recovery probing."""
        self.manager.record_success("GEMINI_3_FLASH")
        assert self.manager.should_attempt_recovery("GEMINI_3_FLASH") is False

    # ─── Sliding Window Overflow ────────────────────────────────────────────

    def test_window_slides_beyond_10(self):
        """Window only keeps last 10 results as old ones drop off."""
        # Record 10 failures
        for _ in range(10):
            self.manager.record_failure("GEMINI_3_FLASH")
        assert self.manager.get_health("GEMINI_3_FLASH").status == HealthStatus.UNHEALTHY

        # Record 8 successes (pushing out 8 old failures)
        for _ in range(8):
            self.manager.record_success("GEMINI_3_FLASH")

        # Window now: [F, F, S, S, S, S, S, S, S, S] = 8 successes
        health = self.manager.get_health("GEMINI_3_FLASH")
        assert health.status == HealthStatus.HEALTHY

    # ─── Multiple Providers Independent ─────────────────────────────────────

    def test_multiple_providers_independent(self):
        """Health tracking for each provider is independent."""
        self.manager.record_failure("GEMINI_3_FLASH")
        self.manager.record_failure("GEMINI_3_FLASH")
        self.manager.record_failure("GEMINI_3_FLASH")
        self.manager.record_failure("GEMINI_3_FLASH")

        # 0 successes / 4 total = 0% → UNHEALTHY
        assert self.manager.get_health("GEMINI_3_FLASH").status == HealthStatus.UNHEALTHY
        assert self.manager.get_health("GEMINI_2_5_FLASH").status == HealthStatus.HEALTHY
        assert self.manager.get_health("GPT5_NANO").status == HealthStatus.HEALTHY

    # ─── Window Stats ───────────────────────────────────────────────────────

    def test_get_window_stats(self):
        """Window stats include all required fields."""
        for _ in range(7):
            self.manager.record_success("GEMINI_3_FLASH")
        for _ in range(3):
            self.manager.record_failure("GEMINI_3_FLASH")

        stats = self.manager.get_window_stats("GEMINI_3_FLASH")
        assert stats["window_size"] == 10
        assert stats["total_in_window"] == 10
        assert stats["successes"] == 7
        assert stats["failures"] == 3
        assert stats["success_rate"] == 70.0
        assert stats["status"] == "DEGRADED"

    # ─── Timestamps ─────────────────────────────────────────────────────────

    def test_success_records_timestamp(self):
        """record_success sets last_success timestamp."""
        self.manager.record_success("GEMINI_3_FLASH")
        health = self.manager.get_health("GEMINI_3_FLASH")
        assert health.last_success is not None
        assert isinstance(health.last_success, datetime)

    def test_failure_records_timestamp(self):
        """record_failure sets last_failure timestamp."""
        self.manager.record_failure("GPT5_NANO")
        health = self.manager.get_health("GPT5_NANO")
        assert health.last_failure is not None
        assert isinstance(health.last_failure, datetime)

    # ─── Reset ──────────────────────────────────────────────────────────────

    def test_reset_clears_all(self):
        """Reset should clear all provider health data."""
        self.manager.record_success("GEMINI_3_FLASH")
        self.manager.record_failure("GPT5_NANO")
        self.manager.reset()

        # After reset, providers should start fresh
        assert len(self.manager.providers) == 0

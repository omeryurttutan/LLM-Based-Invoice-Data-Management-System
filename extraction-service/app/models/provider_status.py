from datetime import datetime
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field


class HealthStatus(str, Enum):
    HEALTHY = "HEALTHY"
    DEGRADED = "DEGRADED"
    UNHEALTHY = "UNHEALTHY"


class ProviderHealth(BaseModel):
    name: str
    status: HealthStatus = HealthStatus.HEALTHY
    last_success: Optional[datetime] = None
    last_failure: Optional[datetime] = None
    failure_count_window: int = 0  # Kept for backward compatibility
    total_failures: int = 0
    total_successes: int = 0
    consecutive_successes: int = 0
    last_recovery_probe: Optional[datetime] = None

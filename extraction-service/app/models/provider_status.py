from datetime import datetime
from enum import Enum
from pydantic import BaseModel, Field

class HealthStatus(str, Enum):
    HEALTHY = "HEALTHY"
    DEGRADED = "DEGRADED"
    UNHEALTHY = "UNHEALTHY"

class ProviderHealth(BaseModel):
    name: str
    status: HealthStatus = HealthStatus.HEALTHY
    last_success: datetime | None = None
    last_failure: datetime | None = None
    failure_count_window: int = 0  # Failures in current window
    total_failures: int = 0
    total_successes: int = 0

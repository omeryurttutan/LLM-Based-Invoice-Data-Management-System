# Log Aggregation Options

This document outlines the recommended strategies for aggregating and analyzing logs from the Fatura OCR system, which now produces structured JSON logs.

## Current Setup

- **Format:** JSON (structlog for Python, LogstashEncoder for Java, custom format for Nginx).
- **Storage:**
  - Docker `json-file` driver (stdout/stderr).
  - Local disk volumes (`./logs/*` mapped to specific containers).
- **Rotation:** Managed by Docker (10MB max, 3 files).

## Recommended Aggregation Stacks

### 1. ELK Stack (Elasticsearch, Logstash, Kibana) - Recommended for Scale

- **Pros:** Industry standard, powerful analysis, rich visualization.
- **Cons:** High resource usage (Java-based), complex setup.
- **Integration:** Use Filebeat to ship logs from the mounted `./logs` directory or Docker containers to Logstash/Elasticsearch.

### 2. PLG Stack (Promtail, Loki, Grafana) - Recommended for Efficiency

- **Pros:** Lightweight, integrates well with Grafana (which we might use for metrics), cost-effective.
- **Cons:** Less indexing capability than Elasticsearch (indexes labels, not full text), but sufficient for logs.
- **Integration:** Run Promtail as a sidecar to read shared log volumes or Docker logs and push to Loki.

### 3. Cloud Native (AWS CloudWatch / Google Cloud Logging)

- **Pros:** Managed service, no maintenance, infinite retention.
- **Cons:** Cost can scale with volume.
- **Integration:** Configure Docker `awslogs` or `gcplogs` driver instead of `json-file`.

## Action Plan

1. **Immediate:** Use `grep` / `jq` on local log files for debugging.
2. **Short Term:** Deploy a lightweight Promtail + Loki + Grafana stack if on-premise.
3. **Long Term:** Evaluate SaaS solutions (Datadog, New Relic) if budget permits.

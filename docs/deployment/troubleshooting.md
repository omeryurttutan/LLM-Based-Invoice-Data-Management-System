# Troubleshooting Guide

This guide helps you diagnose and resolve common issues with the Fatura OCR System.

## 1. Service Startup Issues

| Symptom                      | Possible Cause            | Solution                                                                                                 |
| ---------------------------- | ------------------------- | -------------------------------------------------------------------------------------------------------- |
| **Backend fails to start**   | Database not ready        | Check `docker-compose logs fatura-db`. Ensure PostgreSQL is accepting connections.                       |
| **Backend fails to start**   | Migration failed          | Check logs for Flyway errors. Fix the offending SQL script in `backend/src/main/resources/db/migration`. |
| **Backend fails to start**   | Port 8080 collision       | Run `lsof -i :8080` to find the process using the port and kill it.                                      |
| **Frontend fails to start**  | Node dependencies missing | Run `npm install` inside the frontend directory if running locally. Rebuild Docker image.                |
| **Extraction Service fails** | Missing API Keys          | Ensure `.env` contains `GEMINI_API_KEY`. Check logs for `KeyError`.                                      |
| **Nginx fails to start**     | Invalid config            | Run `docker exec fatura-nginx nginx -t` to validate configuration.                                       |
| **Nginx fails to start**     | SSL cert missing          | Ensure SSL certificates exist in `certbot/conf/`. Run `generate-ssl.sh`.                                 |

## 2. Extraction & LLM Issues

| Symptom                         | Possible Cause  | Solution                                                                  |
| ------------------------------- | --------------- | ------------------------------------------------------------------------- |
| **Extraction stuck in PENDING** | RabbitMQ down   | Check RabbitMQ status. Restart: `docker compose restart fatura-rabbitmq`. |
| **Extraction stuck in PENDING** | Worker crashed  | Check `extraction-service` logs. Restart container.                       |
| **Low confidence scores**       | Blurry image    | Ask user to upload a higher resolution image.                             |
| **Low confidence scores**       | Unknown layout  | Manually verify. System will learn from corrections (Supplier Templates). |
| **LLM Provider Error**          | Quota exceeded  | Check your billing status for Gemini/OpenAI/Anthropic.                    |
| **"Model not found" error**     | API deprecation | Update the model name in `extraction-service/src/llm/providers.py`.       |
| **XML e-Invoice error**         | Invalid XML     | Validate the XML file against UBL-TR schematron.                          |

## 3. Authentication & Access

| Symptom                      | Possible Cause    | Solution                                                            |
| ---------------------------- | ----------------- | ------------------------------------------------------------------- |
| **Can't log in**             | User locked       | Wait 30 mins or unlock via Admin Panel (`is_account_non_locked`).   |
| **Can't log in**             | Redis down        | Auth tokens are stored in Redis. Check Redis health.                |
| **403 Forbidden**            | Insufficient Role | Ask Admin to upgrade your role (e.g., to MANAGER).                  |
| **Session expires too soon** | Clock skew        | Ensure server time is synchronized (`sudo timedatectl set-ntp on`). |
| **Token invalid error**      | Secret rotation   | If `JWT_SECRET` was changed in `.env`, all users must re-login.     |

## 4. Performance Issues

| Symptom            | Possible Cause     | Solution                                                      |
| ------------------ | ------------------ | ------------------------------------------------------------- |
| **Slow Dashboard** | Heavy aggregation  | Run `VACUUM ANALYZE` on PostgreSQL. Check indexes.            |
| **Slow Dashboard** | Redis miss         | Check Redis memory usage. Eviction might be happening.        |
| **High CPU usage** | Python worker loop | Check extraction logs for infinite loops or heavy processing. |
| **Memory leak**    | Java Heap Space    | Increase JVM memory in `Dockerfile.backend.prod` (`-Xmx`).    |

## 5. Notification Issues

| Symptom                   | Possible Cause     | Solution                                                          |
| ------------------------- | ------------------ | ----------------------------------------------------------------- |
| **No Emails**             | SMTP Auth failed   | Check `MAIL_USERNAME` and `MAIL_PASSWORD` in `.env`.              |
| **Emails to Spam**        | SPF/DKIM missing   | Configure DNS records for your domain.                            |
| **No Push Notifications** | Invalid VAPID keys | Regenerate VAPID keys and update `.env`.                          |
| **WebSocket disconnect**  | Nginx timeout      | Increase `proxy_read_timeout` in `nginx.conf` for `/ws` location. |

## 6. Export Issues

| Symptom                 | Possible Cause    | Solution                                                             |
| ----------------------- | ----------------- | -------------------------------------------------------------------- |
| **Export Timeout**      | Dataset too large | Filter by date range (e.g., 1 month) before exporting.               |
| **Encoding Characters** | Wrong charset     | Mikro requires Windows-1254. Ensure you selected the correct format. |
| **Empty Export**        | Status filter     | Accounting exports only include VERIFIED invoices. Check status.     |

---

## Need More Help?

1. **Check Logs:** `docker compose logs -f [service_name]`
2. **Search Issues:** Check the GitHub Issues tracker.
3. **Contact Support:** Email `admin@faturaocr.com`.

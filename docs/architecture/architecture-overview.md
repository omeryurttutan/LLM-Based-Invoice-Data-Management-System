# Architecture Overview

This document describes the high-level design and data flow of the Fatura OCR System.

## 1. System Architecture

The Fatura OCR system uses a microservices-based architecture to ensure scalability, maintainability, and separation of concerns.

### Core Services

1.  **Backend API (Spring Boot):** The central nervous system. Handles business logic, authentication, database interactions, and orchestrates the flow of data.
2.  **Frontend (Next.js):** A modern, responsive web application for users to interact with the system.
3.  **Extraction Service (Python FastAPI):** A specialized service for AI/LLM operations, handling image processing and data extraction.

### Infrastructure Components

- **PostgreSQL:** Primary relational database for storing users, invoices, and structured data.
- **Redis:** In-memory data store used for caching, session management, and rate limiting.
- **RabbitMQ:** Message broker for asynchronous communication between the Backend and Extraction Service.
- **Nginx:** Reverse proxy handling SSL termination and routing.

## 2. Data Flow

### Invoice Processing Pipeline

1.  **Upload:** User uploads a file (JPEG/PNG/PDF/XML) via the Frontend.
2.  **Storage:** Backend saves the file metadata and content.
3.  **Queueing:** Backend publishes a `FileUploadedEvent` to RabbitMQ.
4.  **Consumption:** Extraction Service consumes the event.
    - _If XML:_ Parses UBL-TR format directly.
    - _If Image/PDF:_ Preprocesses the image (grayscale, resize).
5.  **LLM Extraction:** Extraction Service calls Gemini Flash (primary).
    - _On Failure:_ Falls back to GPT-4o-mini.
    - _On Failure:_ Falls back to Claude 3.5 Haiku.
6.  **Normalization:** JSON output from LLM is validated and standardized.
7.  **Callback:** Extraction Service sends results back to Backend via RabbitMQ (`ExtractionCompletedEvent`).
8.  **Persistence:** Backend saves the extracted data and confidence scores.
9.  **Notification:** Backend notifies the user via WebSocket (Frontend updates in real-time).

## 3. Technology Stack

### Backend

- **Language:** Java 17
- **Framework:** Spring Boot 3.2
- **Data:** Spring Data JPA, Hibernate, Flyway
- **Security:** Spring Security, JWT

### Frontend

- **Framework:** Next.js 14 (App Router)
- **Library:** React 19
- **Styling:** Tailwind CSS, Shadcn/ui
- **State:** Zustand, TanStack Query

### Extraction Service

- **Language:** Python 3.11
- **Framework:** FastAPI
- **AI SDKs:** Google Generative AI, OpenAI, Anthropic
- **Libraries:** Pillow, Pydantic, lxml

## 4. Security Measures

- **Authentication:** JWT (JSON Web Tokens) with rotation.
- **Encryption:** Sensitive data (KVKK/GDPR) is encrypted at rest using AES-256.
- **Communication:** All internal and external traffic is encrypted via TLS/SSL.
- **Access Control:** Role-Based Access Control (RBAC) enforces permissions (ADMIN, MANAGER, ACCOUNTANT, INTERN).

## 5. Monitoring

The system is monitored using Spring Boot Actuator and custom metrics.

- **Health Checks:** `/actuator/health` for service status.
- **Metrics:** Prometheus-compatible endpoints for resource usage.
- **Logs:** Centralized logging with correlation IDs for tracing requests across services.

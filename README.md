# Fatura OCR ve Veri Yönetim Sistemi (Invoice OCR and Data Management System)

![Build Status](https://github.com/MFurkanAkdag/Fatura-OCR/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

**Fatura OCR ve Veri Yönetim Sistemi** is an enterprise-grade invoice automation platform that leverages Large Language Models (LLMs) to extract structured data from invoice images and PDFs. It eliminates manual data entry, validates extracted information with high precision, and integrates seamlessly with major Turkish accounting software.

## 🏗️ Architecture

The system follows a hybrid microservices architecture:

- **Backend:** Java 17, Spring Boot 3.2 (Business Logic, Auth, Data)
- **Frontend:** Next.js 14, React 19, TypeScript (Responsive PWA)
- **Extraction Service:** Python 3.11, FastAPI (AI/LLM Processing)
- **Infrastructure:** PostgreSQL, Redis, RabbitMQ, Docker, Nginx

## ✨ Key Features

- **AI-Powered Extraction:** Uses Gemini 1.5 Flash, GPT-4o, and Claude 3.5 Haiku to extract data with >95% accuracy.
- **Multi-Format Support:** Processes JPEG, PNG, PDF, and UBL-TR e-Invoice XML files.
- **Smart Validation:** Confidence scoring system highlights low-certainty fields for manual review.
- **Supplier Learning:** Automatically learns and applies category rules based on supplier history.
- **Accounting Integration:** Exports verified data to Logo, Mikro, Netsis, and Luca formats.
- **Enterprise Security:** Role-Based Access Control (RBAC), KVKK (GDPR) compliance, and full audit trails.
- **Real-time Notifications:** WebSocket-based in-app alerts, emails, and web push notifications.
- **Interactive Dashboard:** Visual analytics for spending trends, top suppliers, and category distribution.

## 🚀 Quick Start (Local Development)

### Prerequisites

- **Java 17 & Maven** (for Backend)
- **Node.js 20+ & npm** (for Frontend)
- **Python 3.11+** (for Extraction Service)
- **PostgreSQL 15+, Redis 7+, RabbitMQ 3.x** (Running locally on custom ports)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/MFurkanAkdag/Fatura-OCR.git
   cd Fatura-OCR
   ```

2. **Configure Environment**
   - Backend: `backend/src/main/resources/application.yml` is already configured for local ports.
   - Extraction Service: `extraction-service/.env` is already configured.

3. **Run Services**
   - **Backend**: `cd backend && mvn spring-boot:run`
   - **Frontend**: `cd frontend && npm install && npm run dev`
   - **Extraction**: `cd extraction-service && pip install -r requirements.txt && python app/main.py`

### Access the Application
- **Frontend:** [http://localhost:3001](http://localhost:3001)
- **Backend API (Swagger):** [http://localhost:8082/api/docs](http://localhost:8082/api/docs)
- **RabbitMQ Management:** [http://localhost:15673](http://localhost:15673) (if configured locally)

## 📂 Project Structure

```
fatura-ocr-system/
├── backend/                # Spring Boot application
├── frontend/               # Next.js web application
├── extraction-service/     # Python FastAPI AI service
├── infrastructure/         # Docker compose and config
├── docs/                   # Documentation
│   ├── user-guide/         # User & Admin guides (Turkish)
│   ├── deployment/         # Runbook & Troubleshooting (English)
│   └── architecture/       # System design docs (English)
└── scripts/                # Utility scripts
```

## 📚 Documentation

- **[End-User Guide (Turkish)](docs/user-guide/kullanici-kilavuzu.md):** For accountants and managers.
- **[Admin Guide (Turkish)](docs/user-guide/admin-kilavuzu.md):** For system administrators.
- **[Deployment Runbook](docs/deployment/deployment-runbook.md):** For DevOps and operations.
- **[Troubleshooting Guide](docs/deployment/troubleshooting.md):** Solutions for common issues.
- **[Architecture Overview](docs/architecture/architecture-overview.md):** High-level system design.

## 🛠️ Development

### Running Tests

- **Backend:** `cd backend && ./mvnw test`
- **Frontend:** `cd frontend && npm test`
- **Extraction:** `cd extraction-service && pytest`

### Branch Strategy

- `main`: Production-ready code.
- `develop`: Integration branch.
- `feature/*`: New features.

## 👥 Team

- **Muhammed Furkan Akdağ** - AI/LLM & Frontend
- **Ömer Talha Yurttutan** - Backend & Infrastructure

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Special thanks to our advisors and the open-source community for the tools that made this project possible.

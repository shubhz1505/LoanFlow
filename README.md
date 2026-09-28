# 🏦 LoanFlow — Enterprise Loan Management Platform

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green?style=for-the-badge&logo=springboot)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-black?style=for-the-badge&logo=apachekafka)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Minikube-326CE5?style=for-the-badge&logo=kubernetes)
![Python](https://img.shields.io/badge/Python-3.13-yellow?style=for-the-badge&logo=python)
![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688?style=for-the-badge&logo=fastapi)

**A production-grade microservices platform for end-to-end loan lifecycle management**

*ML-powered credit scoring · Maker-Checker approval workflow · Real-time Kafka event streaming · OCR document verification · Full DevOps automation*

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Services](#-services)
- [Tech Stack](#-tech-stack)
- [Key Features](#-key-features)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Kafka Event Design](#-kafka-event-design)
- [API Reference](#-api-reference)
- [ML Credit Engine](#-ml-credit-engine)
- [Database Design](#-database-design)
- [Security](#-security)
- [Observability](#-observability)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Kubernetes Deployment](#-kubernetes-deployment)
- [Author](#-author)

---

## 🌟 Overview

LoanFlow is a **production-grade fintech microservices platform** that simulates a real-world loan management system used by banks and NBFCs. It covers the complete loan lifecycle — from user registration and KYC verification, through ML-powered credit scoring, maker-checker officer approval, EMI schedule generation, payment tracking with double-entry accounting, and automated notifications.

### What Makes This Different

| Feature | Description |
|---|---|
| **ML Credit Scoring** | Random Forest model trained on 50,000 synthetic applicants with SHAP explainability |
| **Maker-Checker Workflow** | Two-officer approval system — prevents unilateral loan decisions |
| **Kafka Event Streaming** | 10 Kafka topics — fully event-driven, zero synchronous coupling between services |
| **Double-Entry Ledger** | Every payment creates debit + credit entries — real banking accounting |
| **OCR Document Verification** | Apache Tika extracts income from salary slips and cross-validates with declarations |
| **Resilience4j Circuit Breakers** | All inter-service calls protected — automatic fallback to manual review |
| **Dead Letter Queue** | Failed Kafka messages captured, logged, and replayable |
| **Flyway Migrations** | Schema-per-service, version-controlled DB changes |
| **Spring AOP Audit** | Every business action auto-logged via aspect-oriented programming |
| **JWT + Refresh Token Rotation** | Reuse detection — stolen tokens invalidate the entire session |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                            │
│              Web App · Mobile App · Admin Portal                │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                       API GATEWAY  :8080                        │
│         JWT Validation · Rate Limiting · Request Routing        │
│              Stamps X-User-Id, X-User-Role headers              │
└──────┬──────────┬──────────┬──────────┬──────────┬─────────────┘
       │          │          │          │          │
       ▼          ▼          ▼          ▼          ▼
  [User Svc]  [Loan Svc] [Credit Svc] [Doc Svc] [EMI Svc]
   :8081       :8082       :8083       :8084      :8085
                                │
                                ▼
                        [ML Engine :9000]
                        FastAPI + scikit-learn

       │          │          │          │          │
       └──────────┴──────────┴──────────┴──────────┘
                           │
                    ┌──────▼──────┐
                    │ KAFKA BUS   │
                    │  9 Topics   │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   [Payment Svc]  [Notification Svc]  [Audit Svc]
      :8086            :8087             :8088

                    ┌─────────────┐
                    │  EUREKA     │
                    │   :8761     │
                    │  Registry   │
                    └─────────────┘
```

---

## 🧩 Services

| Service | Port | Description |
|---|---|---|
| **Eureka Server** | 8761 | Service registry — all microservices register here |
| **API Gateway** | 8080 | Single entry point — JWT auth, rate limiting, routing |
| **User Service** | 8081 | Registration, login, JWT issuance, KYC state machine |
| **Loan Service** | 8082 | Loan lifecycle state machine, maker-checker workflow |
| **Credit Service** | 8083 | Orchestrates ML scoring, publishes credit results |
| **Document Service** | 8084 | MinIO file storage, Apache Tika OCR, income extraction |
| **EMI Service** | 8085 | Amortization schedule, overdue detection, NPA flagging |
| **Payment Service** | 8086 | Payment processing, double-entry ledger, reconciliation |
| **Notification Service** | 8087 | Email notifications triggered by Kafka events |
| **Audit Service** | 8088 | Immutable audit trail via Spring AOP + Kafka |
| **ML Credit Engine** | 9000 | FastAPI + scikit-learn Random Forest credit scorer |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Core Backend** | Java 21, Spring Boot 3.3.4 |
| **API Gateway** | Spring Cloud Gateway |
| **Service Discovery** | Netflix Eureka |
| **Inter-service Sync** | OpenFeign + Resilience4j (circuit breaker, retry, timeout) |
| **Async Messaging** | Apache Kafka 3.7 |
| **ML Engine** | FastAPI + scikit-learn + joblib |
| **Databases** | MySQL 8.0 (schema per service) |
| **Caching** | Redis |
| **File Storage** | MinIO (S3-compatible) |
| **Schema Migrations** | Flyway |
| **Security** | Spring Security + JWT (JJWT 0.12.3) + BCrypt |
| **Observability** | ELK Stack + Prometheus + Grafana |
| **Containerization** | Docker + Docker Compose |
| **Orchestration** | Kubernetes + HPA |
| **CI/CD** | Jenkins + GitHub Actions |
| **Build Tool** | Maven 3.9 (multi-module) |

---

## ✨ Key Features

### 🔐 Security
- JWT access tokens (15 min) + opaque refresh tokens (7 days)
- Refresh token rotation with **reuse detection** — stolen token revokes entire session
- Account lockout after 5 failed login attempts (30-min lock)
- BCrypt password hashing with cost factor 12
- Role-based access: `APPLICANT`, `LOAN_OFFICER`, `SENIOR_OFFICER`, `ADMIN`
- KYC verification state machine with max 3 attempts

### 🏦 Loan Lifecycle State Machine
```
DRAFT → SUBMITTED → DOCUMENT_PENDING → CREDIT_CHECK_IN_PROGRESS
      → UNDER_REVIEW → CONDITIONALLY_APPROVED → APPROVED
      → DISBURSEMENT_PENDING → DISBURSED → ACTIVE → CLOSED
                                                   ↘ NPA → WRITTEN_OFF
                            (REJECTED at any stage)
```

### 🤖 ML Credit Scoring
- Random Forest trained on **50,000 synthetic loan applicants**
- AUC-ROC: **0.79**
- Features: income, DTI ratio, credit utilization, employment type, existing loan burden
- Score range: 300–900 (higher = better)
- Risk tiers: LOW / MEDIUM / HIGH / VERY_HIGH / MANUAL_REVIEW
- Circuit breaker: if ML engine is down → loan auto-routes to manual review

### 📋 Maker-Checker Approval
- **Maker** (Loan Officer): reviews credit assessment, recommends approve/reject with suggested terms
- **Checker** (Senior Officer): gives final approval — must be a **different** officer than maker
- Prevents single-point fraud in loan decisions
- Full audit trail of every decision with timestamps

### 📨 Event-Driven Architecture
- 10 Kafka topics with partitioning by loan ID (ordering guarantee per loan)
- Idempotent producers (exactly-once delivery)
- Dead Letter Queue (DLT) for failed messages with full exception context
- 3 consumer partitions per topic matching 3 concurrent consumer threads

### 📊 Double-Entry Ledger
- Every EMI payment creates: DEBIT (borrower account) + CREDIT (principal + interest + penalty)
- Running balance tracked per loan
- Nightly reconciliation job
- Idempotency via unique transaction reference check

---

## 📂 Project Structure

```
LoanFlow/
├── pom.xml                          ← Root parent POM (version management)
│
├── commons/                         ← Shared library (zero dependencies on services)
│   └── src/main/java/com/loanflow/commons/
│       ├── dto/ApiResponse.java
│       ├── enums/                   ← LoanStatus, KycStatus, UserRole, RiskTier...
│       ├── events/                  ← All Kafka event schemas
│       ├── exceptions/              ← LoanFlowException hierarchy
│       ├── kafka/                   ← KafkaConfig, EventPublisher, DlqMonitor
│       └── utils/                   ← EmiCalculator, CorrelationIdUtils
│
├── eureka-server/                   ← Netflix Eureka service registry
├── api-gateway/                     ← Spring Cloud Gateway + JWT filter
│
├── user-service/                    ← Auth, KYC, JWT management
│   └── src/main/resources/db/migration/
│       ├── V1__create_users_table.sql
│       ├── V2__create_refresh_tokens_table.sql
│       └── V3__create_kyc_audit_log_table.sql
│
├── loan-service/                    ← Core loan lifecycle + maker-checker
│   └── src/main/resources/db/migration/
│       └── V1__create_loan_schema.sql
│
├── credit-service/                  ← ML orchestration + credit assessment storage
│   └── src/main/resources/db/migration/
│       └── V1__create_credit_schema.sql
│
├── document-service/                ← MinIO + OCR + document verification
│   └── src/main/resources/db/migration/
│       └── V1__create_document_schema.sql
│
├── emi-service/                     ← Amortization + scheduler + NPA detection
├── payment-service/                 ← Ledger + payment processing
├── notification-service/            ← Email notifications (Kafka consumers)
├── audit-service/                   ← Immutable audit trail + Spring AOP
│
├── ml-credit-engine/                ← FastAPI Python ML service
│   ├── main.py                      ← FastAPI app + scoring endpoints
│   ├── train.py                     ← Model training + synthetic data generation
│   ├── requirements.txt
│   ├── Dockerfile
│   └── models/                      ← Trained model artifacts (gitignored)
│
├── k8s/                             ← Kubernetes manifests
│   ├── configmaps/
│   ├── secrets/
│   ├── deployments/
│   ├── hpa/                         ← Horizontal Pod Autoscalers
│   └── ingress/
│
├── monitoring/
│   └── prometheus.yml               ← Prometheus scrape config
│
├── docker-compose.yml               ← Full local stack (15 containers)
├── Jenkinsfile                      ← Jenkins CI/CD pipeline
└── .github/workflows/ci.yml        ← GitHub Actions pipeline
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Java | 21 (Temurin) | Spring Boot services |
| Maven | 3.9+ | Multi-module build |
| Python | 3.10+ | ML credit engine |
| Docker Desktop | Latest | Container runtime |
| Docker Compose | Latest | Local orchestration |
| Minikube | Latest | Local Kubernetes |
| MySQL Workbench | Any | DB inspection |

### Step 1 — Clone the Repository

```bash
git clone https://github.com/shubhz1505/LoanFlow.git
cd LoanFlow
```

### Step 2 — Build Commons Module First

```bash
cd commons
mvn clean install -DskipTests
cd ..
```

### Step 3 — Train the ML Model

```bash
cd ml-credit-engine
pip install -r requirements.txt
python train.py
```

Expected output:
```
Generated 50000 samples — 86.7% good borrowers
AUC-ROC: 0.7945
Model saved to models/
```

### Step 4 — Start ML Engine

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 9000 --reload
```

Verify: http://localhost:9000/health

### Step 5 — Build All Spring Boot Services

```bash
cd ..
mvn clean install -DskipTests
```

### Step 6 — Start Full Stack with Docker Compose

```bash
docker-compose up --build
```

### Step 7 — Verify Everything is Running

| Service | URL | Credentials |
|---|---|---|
| API Gateway | http://localhost:8080 | — |
| Eureka Dashboard | http://localhost:8761 | eureka-admin / eureka-secret-2024 |
| Kafka UI | http://localhost:8089 | — |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |
| Grafana | http://localhost:3000 | admin / loanflow@grafana |
| Kibana | http://localhost:5601 | — |
| Jenkins | http://localhost:8090 | — |
| ML Engine Docs | http://localhost:9000/docs | — |

---

## 📡 Kafka Event Design

| Topic | Publisher | Consumers | Trigger |
|---|---|---|---|
| `user.kyc.verified` | User Service | Loan, Notification, Audit | KYC approved by officer |
| `loan.application.submitted` | Loan Service | Credit, Document, Audit, Notification | User submits loan |
| `loan.credit.scored` | Credit Service | Loan, Audit | ML engine returns score |
| `loan.approved` | Loan Service | EMI, Notification, Audit | Checker approves |
| `loan.rejected` | Loan Service | Notification, Audit | Maker or Checker rejects |
| `document.verified` | Document Service | Loan, Audit | Officer verifies document |
| `payment.received` | Payment Service | EMI, Audit, Notification | Borrower makes payment |
| `payment.overdue` | EMI Service | Notification, Audit, Loan | Daily scheduler detects overdue |
| `emi.due.reminder` | EMI Service | Notification | 3 days before EMI due |

### Dead Letter Queue Strategy

```
Failed message → Retry 1 (1s) → Retry 2 (1s) → Retry 3 (1s)
                                                      ↓
                                          loan.application.submitted.DLT
                                                      ↓
                                              DlqMonitor logs ERROR
                                                      ↓
                                          Kibana alert → Ops team
```

---

## 📬 API Reference

### Authentication

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

### KYC

```http
POST /api/v1/kyc/submit              # Applicant submits KYC docs
POST /api/v1/kyc/review              # Officer approves/rejects KYC
GET  /api/v1/kyc/history/{userId}    # Full KYC audit trail
```

### Loans

```http
POST /api/v1/loans                          # Submit loan application
GET  /api/v1/loans/my                       # My applications (paginated)
GET  /api/v1/loans/{loanId}                 # Application detail + state history
GET  /api/v1/loans/review/queue             # Officer review queue
POST /api/v1/loans/review/maker-decision    # First officer recommendation
POST /api/v1/loans/review/checker-decision  # Senior officer final decision
```

### Documents

```http
POST /api/v1/documents/loan/{loanId}/upload   # Upload document (multipart)
GET  /api/v1/documents/loan/{loanId}          # List all documents
POST /api/v1/documents/{docId}/verify         # Officer verifies
POST /api/v1/documents/{docId}/reject         # Officer rejects
GET  /api/v1/documents/{docId}/url            # Pre-signed download URL (1hr)
GET  /api/v1/documents/loan/{loanId}/complete # Are all required docs verified?
```

### Payments

```http
POST /api/v1/payments                    # Process EMI payment
GET  /api/v1/payments/loan/{loanId}      # Payment history
GET  /api/v1/payments/ledger/{loanId}    # Full double-entry ledger
GET  /api/v1/payments/ledger/{loanId}/reconcile  # Debit/credit balance check
```

### Credit

```http
GET /api/v1/credit/assessment/{loanId}   # Credit score + SHAP explanation
```

### ML Engine

```http
GET  /health                             # Health check
POST /score                              # Score a loan application
```

### Request Headers (from Gateway)

```
Authorization: Bearer <accessToken>    # Required for protected routes
X-User-Id: <userId>                    # Stamped by gateway after JWT validation
X-User-Role: APPLICANT|LOAN_OFFICER|SENIOR_OFFICER|ADMIN
X-User-Email: <email>
X-Correlation-ID: <uuid>              # Distributed tracing ID
```

---

## 🤖 ML Credit Engine

### Model Details

| Property | Value |
|---|---|
| Algorithm | Random Forest Classifier |
| Training samples | 50,000 synthetic applicants |
| AUC-ROC | 0.79 |
| Features | 10 engineered features |
| Score range | 300 – 900 |
| Inference time | ~50ms |

### Input Features

| Feature | Description |
|---|---|
| `monthly_income` | Monthly take-home income (₹) |
| `requested_amount` | Loan amount requested (₹) |
| `tenure_months` | Loan duration in months |
| `existing_emi_amount` | Current monthly EMI obligations (₹) |
| `existing_loan_count` | Number of active loans |
| `debt_to_income_ratio` | Existing EMI / income × 100 |
| `credit_utilization` | (Existing + projected EMI) / income × 100 |
| `employment_type_encoded` | SALARIED=0, SELF_EMPLOYED=1, BUSINESS=2... |
| `loan_to_income_ratio` | Requested amount / monthly income |
| `affordability_ratio` | (Amount/tenure) / income |

### Score → Risk Tier Mapping

| Score | Risk Tier | Interest Rate | Action |
|---|---|---|---|
| 800+ | LOW | 9.5% | Auto-approve eligible |
| 750–799 | LOW | 10.5% | Officer review |
| 700–749 | MEDIUM | 11.5% | Officer review |
| 650–699 | MEDIUM | 13.0% | Officer review |
| 600–649 | HIGH | 15.0% | Senior officer review |
| 550–599 | HIGH | 18.0% | Strict review |
| < 550 | VERY_HIGH | 24.0% | Auto-reject |
| N/A | MANUAL_REVIEW | — | ML engine unavailable |

---

## 🗄️ Database Design

Each service owns its own MySQL schema. **No cross-service DB joins.**

| Service | Schema | Key Tables |
|---|---|---|
| User Service | `loanflow_users` | users, refresh_tokens, kyc_audit_log |
| Loan Service | `loanflow_loans` | loan_applications, loan_state_history |
| Credit Service | `loanflow_credit` | credit_assessments |
| Document Service | `loanflow_documents` | loan_documents |
| EMI Service | `loanflow_emi` | loan_accounts, emi_schedule |
| Payment Service | `loanflow_payments` | payments, ledger_entries |
| Notification Service | `loanflow_notifications` | notifications |
| Audit Service | `loanflow_audit` | audit_logs |

### EMI Amortization (Reducing Balance)

```
EMI = P × r × (1+r)^n / ((1+r)^n - 1)

Where:
  P = Principal (approved loan amount)
  r = Monthly interest rate (annual rate / 1200)
  n = Tenure in months
```

---

## 🔐 Security

### JWT Token Design

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "APPLICANT",
  "kycStatus": "VERIFIED",
  "fullName": "Shubham Adhav",
  "iat": 1234567890,
  "exp": 1234568790
}
```

- Access token: **15 minutes** (short-lived, stateless)
- Refresh token: **7 days** (opaque UUID, stored in DB, revocable)
- Algorithm: **HMAC-SHA256**
- Refresh token rotation with reuse detection

### API Gateway Security Flow

```
Client Request → API Gateway
                    ↓
              Validate JWT signature
              Check expiry
              Extract claims
                    ↓
              Stamp headers:
              X-User-Id, X-User-Role, X-User-Email
                    ↓
              Route to microservice
              (downstream services trust headers)
```

---

## 📊 Observability

### Prometheus Metrics

Every Spring Boot service exposes `/actuator/prometheus`. Metrics include:
- HTTP request rate, latency, error rate
- JVM memory, GC, thread counts
- Kafka producer/consumer lag
- Resilience4j circuit breaker state
- HikariCP connection pool utilization

### Grafana Dashboards

- **Service Overview**: request rate, p95 latency, error rate per service
- **Loan Pipeline**: applications by status, approval rate, avg processing time
- **Kafka**: consumer lag, message throughput, DLQ message count
- **JVM**: heap usage, GC pauses, thread count

### Distributed Tracing

Every request gets a `X-Correlation-ID` header at the gateway. This ID travels through:
- All HTTP headers to downstream services
- All Kafka event payloads
- All log lines via MDC (Mapped Diagnostic Context)
- All audit log entries

Search Kibana with one query:
```
correlationId: "LOAN-A3F9B2C1"
```
→ See the complete lifecycle of a loan across all 10 services.

---

## ⚙️ CI/CD Pipeline

### Jenkins Pipeline Stages

```
1. Checkout Code
2. Build Commons Module
3. Build & Test All Services (parallel)
4. Code Quality — SonarQube analysis
5. Build Docker Images (parallel — 11 images)
6. Push to Docker Hub
7. Update K8s manifests with new image tags
8. Deploy to Kubernetes
9. Wait for rollout completion
10. Health check all pods
11. Notify (Slack / Email)
```

### GitHub Actions

Runs on every push to `master`:
- Matrix build — all services built in parallel
- ML engine trained and model validated
- Docker images built and pushed
- Kubernetes deployment triggered

### Required Secrets

```
DOCKER_USERNAME      Docker Hub username
DOCKER_PASSWORD      Docker Hub password
KUBECONFIG           Kubernetes cluster config
MAIL_USERNAME        SMTP email for notifications
MAIL_PASSWORD        SMTP app password
```

---

## ☸️ Kubernetes Deployment

```bash
# Start Minikube
minikube start --memory=8192 --cpus=4

# Apply all manifests
kubectl apply -f k8s/configmaps/  -n loanflow
kubectl apply -f k8s/secrets/     -n loanflow
kubectl apply -f k8s/deployments/ -n loanflow
kubectl apply -f k8s/hpa/         -n loanflow
kubectl apply -f k8s/ingress/     -n loanflow

# Check status
kubectl get pods -n loanflow
kubectl get hpa  -n loanflow
```

### Horizontal Pod Autoscalers

| Service | Min Replicas | Max Replicas | CPU Threshold |
|---|---|---|---|
| API Gateway | 2 | 6 | 70% |
| Loan Service | 2 | 8 | 70% |
| User Service | 2 | 6 | 70% |
| Credit Service | 2 | 6 | 65% |
| ML Engine | 2 | 8 | 60% |
| Payment Service | 2 | 6 | 70% |

---

## 🗓️ Build Roadmap

- [x] Commons module — shared events, DTOs, exceptions
- [x] Kafka infrastructure — producer, consumer, DLQ
- [x] Eureka Server — secured service registry
- [x] API Gateway — JWT auth, rate limiting, routing
- [x] User Service — registration, login, JWT, KYC state machine
- [x] Loan Service — state machine, maker-checker workflow
- [x] Credit Service — ML orchestration, Resilience4j
- [x] ML Credit Engine — FastAPI + Random Forest + SHAP
- [x] Document Service — MinIO + Apache Tika OCR
- [ ] EMI Service — amortization + NPA detection
- [ ] Payment Service — double-entry ledger
- [ ] Notification Service — event-driven emails
- [ ] Audit Service — Spring AOP interceptor
- [ ] Docker Compose — full 15-container stack
- [ ] Kubernetes — deployments, HPA, ingress
- [ ] Jenkins + GitHub Actions — full CI/CD

---

## 👨‍💻 Author

**Shubham Adhav**

- 🎓 B.Tech — MIT ADT University, Pune (2022–2026)
- 💼 Pursuing Backend Developer roles
- 🛠️ Core Stack: Java · Spring Boot · Microservices · Kafka · Docker · Kubernetes

---


---

<div align="center">

**Built with ❤️ to demonstrate production-grade backend engineering**

*If this project helped you, please ⭐ star the repository*

</div>

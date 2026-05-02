<div align="center">

<h1>🌿 Serenity</h1>

<p><strong>A microservice-oriented mental healthcare system</strong></p>

<p>
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/Angular-DD0031?style=for-the-badge&logo=angular&logoColor=white" />
  <img src="https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white" />
  <img src="https://img.shields.io/badge/Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white" />
</p>
<p>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-336791?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" />
  <img src="https://img.shields.io/badge/SonarQube-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white" />
  <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=JSON%20web%20tokens&logoColor=white" />
  <img src="https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socketdotio&logoColor=white" />
</p>

<p>
  <img src="https://img.shields.io/github/license/haythem-abdellaoui/Serenity?style=flat-square" />
  <img src="https://img.shields.io/github/last-commit/haythem-abdellaoui/Serenity?style=flat-square" />
  <img src="https://img.shields.io/github/issues/haythem-abdellaoui/Serenity?style=flat-square" />
</p>

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [What's in this repo](#-whats-in-this-repo)
  - [Apps](#apps)
  - [Core Backend Services](#core-backend-services-gateway-routed)
  - [AI / ML Services](#ai--ml-services)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Running Locally](#running-locally)
  - [Running with Docker](#running-with-docker)
  - [Running with Kubernetes](#running-with-kubernetes)
- [Configuration](#-configuration)
- [Testing & Quality](#-testing--quality)
- [Useful Docs](#-useful-docs)
- [Contributing](#-contributing)

---

## 🌟 Overview

**Serenity** is a microservice-oriented mental healthcare platform. It covers patient management, appointments, teleconsultations, prescriptions, insurance claims, marketplace, and AI-assisted clinical features — all behind a unified API gateway, with real-time WebSocket support and a modular, independently deployable service design.

- **Frontend apps** live under `apps/`
- **Backend services** live under `services/` (one folder = one deployable process)
- The **API gateway** (`services/API_Gatewya`) is the single HTTP entrypoint used by the web UI

Architecture overview: see `docs/ARCHITECTURE.md`.

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────┐
│                   Angular Web App (:4200)                 │
│              Insurance Portal (Node/Express)              │
│               Pharmacy Mobile (Flutter)                   │
└──────────────────────┬───────────────────────────────────┘
                       │ HTTP / WebSocket
              ┌────────▼────────┐
              │   API Gateway   │  Spring Cloud Gateway (:8082)
              │  /api/** routes │  WebSocket proxy
              └────────┬────────┘
        ┌──────────────┼──────────────┬──────────────┬──────────────┐
        │              │              │              │              │
 ┌──────▼──────┐ ┌─────▼──────┐ ┌────▼──────┐ ┌────▼──────┐ ┌────▼──────┐
 │user-service │ │appointment │ │ insurance │ │ pharmacy  │ │marketplace│
 │   (:8081)   │ │  (:8091)   │ │  (:8090)  │ │  (:8093)  │ │  (:8088)  │
 │MySQL+Redis  │ │   MySQL    │ │   MySQL   │ │   MySQL   │ │   MySQL   │
 │  +WebSocket │ └────────────┘ └───────────┘ └───────────┘ └───────────┘
 └─────────────┘
 ┌─────────────┐
 │Doctors_svc  │
 │             │
 │PostgreSQL   │
 │Redis+WSocket│
 └─────────────┘

 ┌─────────────────────────────────────────────────────────────────────┐
 │                        AI / ML Services                             │
 │  pharmacy-ml (FastAPI :8096) · monitoring-ai (FastAPI :5150)        │
 │  ai-severity-derbel (Flask :5001) · whisper-ai · claim-risk-model   │
 └─────────────────────────────────────────────────────────────────────┘
```

---

## 📁 What's in this repo

### Apps

| App | Stack | Port | Description |
|---|---|---|---|
| `apps/web-app` | Angular 17, SCSS, RxJS | **4200** | Main SPA for patients & admins |
| `apps/insurance-portal` | Node.js / Express | **3000** | External portal API for insurance flows |
| `apps/pharmacy-mobile` | Flutter | — | Mobile uploader for pharmacists |

### Core Backend Services (gateway-routed)

Routed by the gateway based on path prefix — see `services/API_Gatewya/src/main/resources/application.yml`.

| Service | Port | Database | Description |
|---|---|---|---|
| `services/API_Gatewya` | **8082** | — | Spring Cloud Gateway — routes `/api/**` & WebSocket paths |
| `services/user-service` | **8081** | MySQL + Redis | Auth, users, profiles, file uploads — WebSocket support |
| `services/appointment-service` | **8091** | MySQL | Appointments, teleconsultations, notifications |
| `services/insurance-service` | **8090** | MySQL | Claims, reimbursements, OCR pipeline hooks |
| `services/pharmacy-service` | **8093** | MySQL | Prescriptions & pharmacy workflows |
| `services/marketplace-service` | **8088** | MySQL | Marketplace, catalog, cart |
| `services/monitoring-service` | **8085** | — | Monitoring endpoints + weekly doctor digest |
| `services/derbelmicroservice` | — | MySQL | Medical records / dashboard domain |
| `services/Doctors_service` | — | PostgreSQL + Redis | Doctor-domain API — WebSocket support |

### AI / ML Services

| Service | Stack | Port | Description |
|---|---|---|---|
| `services/pharmacy-ml` | FastAPI | **8096** | CNOPT prescription verification |
| `services/monitoring-ai` | FastAPI | **5150** | Crisis risk prediction |
| `services/ai-severity-derbel-service` | Flask | **5001** | Diagnosis severity prediction |
| `services/whisper-ai-service` | Python | — | Speech-to-text support + dataset workspace |
| `services/claim-risk-model` | Jupyter / Python | — | Claim risk model notebooks & trained assets |

---

## 🛠️ Tech Stack

### Frontend
| Technology | Purpose |
|---|---|
| **Angular 17** | Main SPA (SCSS, RxJS) |
| **Flutter** | Pharmacy mobile app |
| **Node.js / Express** | Insurance portal API |

### Backend
| Technology | Purpose |
|---|---|
| **Java 17 + Spring Boot** | Core microservices |
| **Spring Cloud Gateway** | API gateway & routing |
| **Spring Security + JWT** | Stateless authentication |
| **OAuth2** | Third-party / social login |
| **WebSocket** | Real-time bidirectional communication (teleconsultations, live notifications) |
| **FastAPI** | High-performance AI/ML service APIs |
| **Flask** | Severity prediction service |

### Databases & Caching
| Technology | Purpose |
|---|---|
| **MySQL** | Primary relational store for most services |
| **PostgreSQL** | Appointment & medical records services |
| **Redis** | Caching & session management (`user-service`) |

### AI / Machine Learning
| Technology | Purpose |
|---|---|
| **Jupyter Notebooks** | Model training & experimentation (claim risk, severity) |
| **scikit-learn / PyTorch** | ML model development |
| **OpenAI Whisper** | Speech-to-text transcription |
| **FastAPI / Flask** | Model serving & inference APIs |

### DevOps & Quality
| Technology | Purpose |
|---|---|
| **Docker** | Containerization of all services |
| **Kubernetes** | Container orchestration & scaling |
| **SonarQube** | Static analysis & code quality gate |
| **JUnit** | Unit & integration testing for Spring Boot services |
| **Maven** | Java build tooling |

---

## 🚀 Getting Started

### Prerequisites

- Java **17+**
- Maven **3.8+**
- Node.js **18+** & npm
- Angular CLI: `npm i -g @angular/cli`
- Python **3.9+** & pip *(for AI/ML services)*
- MySQL **8.x**
- PostgreSQL **14+**
- Redis *(optional, used by user-service)*
- Docker & Docker Compose
- kubectl *(for Kubernetes deployment)*

---

### Running Locally

This repo is intentionally multi-service — only run the services needed for the features you're testing.

**1. Clone the repository**
```bash
git clone https://github.com/haythem-abdellaoui/Serenity.git
cd Serenity
```

**2. Start MySQL & PostgreSQL**

Create databases as needed (most services auto-create the schema on first run):

```
healthcare_db   → user / insurance services
pharmacy_db     → pharmacy-service
marketplace_db  → marketplace-service
appointment_db  → appointment-service (PostgreSQL)
medical_db      → derbelmicroservice (PostgreSQL)
```

**3. Run the minimal backend**
```bash
# Terminal 1
cd services/user-service && mvn spring-boot:run

# Terminal 2
cd services/appointment-service && mvn spring-boot:run

# Terminal 3 — gateway last
cd services/API_Gatewya && mvn spring-boot:run
```

Add other services only as needed (insurance, pharmacy, marketplace, etc.).

**4. Run the Angular web app**
```bash
cd apps/web-app
npm install
ng serve
```

- Web UI: `http://localhost:4200`
- Gateway (API base): `http://localhost:8082`

**5. Run AI/ML services** *(optional)*
```bash
# FastAPI — pharmacy ML
cd services/pharmacy-ml
pip install -r requirements.txt
uvicorn main:app --port 8096

# FastAPI — monitoring AI
cd "services/monitoring -ai"
pip install -r requirements.txt
uvicorn main:app --port 5150

# Flask — severity prediction
cd services/ai-severity-derbel-service
pip install -r requirements.txt
python app.py
```

---

## 🔧 Configuration

Most services read from **environment variables** with local defaults. **Keep secrets out of Git!**


## 🧪 Testing & Quality

**Run all JUnit tests**
```bash
mvn test
```

**Run tests with coverage report**
```bash
mvn verify
```

**Analyze with SonarQube**

Start SonarQube locally (default: `http://localhost:9000`), then:

```bash
mvn sonar:sonar \
  -Dsonar.projectKey=Serenity \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your_sonar_token
```

**Explore ML notebooks**
```bash
cd services/claim-risk-model
jupyter notebook
```

---

## 📚 Useful Docs

| Document | Description |
|---|---|
| `docs/ARCHITECTURE.md` | Repo structure and core service roles |
| `docs/ADDING_A_SERVICE.md` | How to add a new backend service |
| `docs/APPOINTMENTS_EXPLAINED_LIKE_IM_5.md` | Simple walk-through of the appointments feature |
| `services/README.md` | Concise service list and recommended run order |
| `services/monitoring-service/README_WEEKLY_DIGEST.md` | Weekly doctor digest feature |
| `services/pharmacy-ml/README.md` | CNOPT verification ML service |
| `services/monitoring-ai/README.md` | Crisis risk prediction AI service |
| `services/whisper-ai-service/dataset/README.md` | Speech-to-text dataset info |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m 'feat: add your feature'`
4. Push to the branch: `git push origin feature/your-feature-name`
5. Open a Pull Request

**Please ensure:**
- All JUnit tests pass: `mvn test`
- SonarQube reports no new critical issues
- One folder = one deployable (`apps/` for frontends, `services/` for backends)
- The SPA calls backends through the gateway (`:8082`) unless explicitly bypassing for debugging
- Secrets are never committed to Git

---

<div align="center">
  <p>Made with ❤️ by <a href="https://github.com/haythem-abdellaoui/Serenity">Haythem Abdellaoui , Talel Ben Aziza , Talel Boukhris , Rayen Boussaidi , Rayen Derbel , Ahmed Hamda , Med Raef Hosni</a></p>
</div>

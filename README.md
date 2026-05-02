# Serenity

Serenity is a **microservice-oriented mental healthcare system**.

- **Frontend apps** live under `apps/`
- **Backend services** live under `services/` (one folder = one deployable process)
- The **API gateway** (`services/API_Gatewya`) is the single HTTP entrypoint used by the web UI

Architecture overview: see `docs/ARCHITECTURE.md`.

## What’s in this repo

### Apps

- **`apps/web-app`** (Angular, port **4200**): main SPA for patients/admins
- **`apps/insurance-portal`** (Node/Express, port **3000** by convention): external portal API used by insurance flows (see `apps/insurance-portal/server.js`)
- **`apps/pharmacy-mobile`** (Flutter): minimal mobile uploader for pharmacists (see `apps/pharmacy-mobile/README.md`)

### Core backend services (gateway-routed)

These services are routed by the gateway based on path prefix (see `services/API_Gatewya/src/main/resources/application.yml`).

- **`services/user-service`** (Spring Boot, port **8081**): auth, users, profiles, uploads
- **`services/appointment-service`** (Spring Boot, port **8091**): appointments, teleconsultations, notifications
- **`services/insurance-service`** (Spring Boot, port **8090**): claims, reimbursements, OCR pipeline hooks
- **`services/pharmacy-service`** (Spring Boot, port **8093**): prescriptions & pharmacy workflows
- **`services/marketplace-service`** (Spring Boot, port **8088**): marketplace/catalog/cart
- **`services/API_Gatewya`** (Spring Cloud Gateway, port **8082**): routes `/api/**` and websocket paths to the right backend

### Additional services / experiments

- **`services/monitoring-service`** (Spring Boot, port **8085** in its config): monitoring endpoints + weekly doctor digest (see `services/monitoring-service/README_WEEKLY_DIGEST.md`)
- **`services/derbelmicroservice`** (Spring Boot): medical records / dashboard domain (see `services/derbelmicroservice/HELP.md`)
- **`services/Doctors_service`** (Spring Boot): additional doctor-domain API (see its `application.properties`)
- **AI/ML services**
  - **`services/pharmacy-ml`** (FastAPI, port **8096**): CNOPT verification (see `services/pharmacy-ml/README.md`)
  - **`services/monitoring -ai`** (FastAPI, port **5150**): crisis risk prediction (see `services/monitoring -ai/README.md`)
  - **`services/ai-severity-derbel-service`** (Flask, port **5001**): diagnosis severity prediction (see `services/ai-severity-derbel-service/README.md`)
  - **`services/whisper-ai-service`**: speech-to-text support + dataset workspace (see `services/whisper-ai-service/dataset/README.md`)
  - **`services/claim-risk-model`**: claim risk model notebooks & assets

For a concise service list and run order, see `services/README.md`.

## Tech stack

- **Frontend**: Angular 17 (SCSS, RxJS)
- **Backend**: Java 17, Spring Boot (multi-service), Spring Cloud Gateway
- **Databases**: MySQL (per-service DB or shared depending on config)
- **Optional infra**: Redis (used by `user-service` in dev config)
- **Build tooling**: Maven, Node.js/npm

## Quickstart (local dev)

This repo is intentionally “multi-service”: you only need to run the services used by the screens/features you’re testing.

### Prerequisites

- Java **17+**
- Maven **3.8+**
- Node.js **18+**
- MySQL **8.x**
- Angular CLI (for `apps/web-app`): `npm i -g @angular/cli`

### 1) Start MySQL

Create databases as needed (many services default to creating the DB if missing). Common local defaults include:

- `healthcare_db` (user/appointment/insurance depending on config)
- `pharmacy_db`
- `marketplace_db`

### 2) Run the minimal backend set

Start these first:

```bash
cd services/user-service
mvn spring-boot:run
```

```bash
cd services/appointment-service
mvn spring-boot:run
```

Then start the gateway:

```bash
cd services/API_Gatewya
mvn spring-boot:run
```

Add other services only if you need their routes (insurance, pharmacy, marketplace, etc.).

### 3) Run the web app

```bash
cd apps/web-app
npm install
ng serve
```

- Web UI: `http://localhost:4200`
- Gateway (API base): `http://localhost:8082`

## Configuration (important)

Most services read configuration from **environment variables** with local defaults. Keep secrets out of Git!!!!

- **Database**: prefer `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (or the service-specific equivalents)
- **JWT**: `JWT_SECRET` must match across services that issue/validate tokens (gateway, user-service, insurance-service, etc.)
- **Internal service calls**: services use an `INTERNAL_API_KEY` / `X-Internal-Key` convention for internal endpoints

Appointment-service is strict about required env vars; see:
`services/appointment-service/src/main/resources/application.properties`.

## Useful docs

- `docs/ARCHITECTURE.md` — repo structure and core service roles
- `docs/ADDING_A_SERVICE.md` — how to add a new backend service
- `docs/APPOINTMENTS_EXPLAINED_LIKE_IM_5.md` — simple walk-through of the appointments feature

## Notes for contributors

- **One folder = one deployable**: apps under `apps/`, services under `services/`
- **Gateway-first routing**: the SPA should call backends via the gateway (`:8082`) unless you’re intentionally bypassing it for debugging
- **Aggregator Maven POM**: the root `pom.xml` is an IntelliJ-friendly aggregator so multiple services show up together

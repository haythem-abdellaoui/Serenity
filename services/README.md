# Services

Backend services. **One folder = one deployable service** (own process, port, config).

## Current services

| Service | Path | Port | Description |
|---------|------|------|-------------|
| **API_Gatewya** | `services/API_Gatewya/` | 8082 | Spring Cloud Gateway; routes `/api/**` to user-service, appointment-service, insurance-service, and other backends (see `application.yml`). |
| **user-service** | `services/user-service/` | 8081 | Auth, user CRUD, profiles. |
| **appointment-service** | `services/appointment-service/` | 8091 | Appointments, calendar, notifications. |
| **insurance-service** | `services/insurance-service/` | 8090 (default in gateway) | Insurance claims, reimbursements. |
| **pharmacy-service** | `services/pharmacy-service/` | 8093 (per gateway) | Pharmacy products, prescriptions. |
| **marketplace-service** | `services/marketplace-service/` | 8088 | Mental health products, checkout. |
| **pharmacy-ml** | `services/pharmacy-ml/` | 8096 | Python FastAPI service for CNOPT document verification. |

Other folders under `services/` (monitoring, doctors, microservices, etc.) are additional deployables—see each module’s `pom.xml` and `application.yml`.

## Run order (typical)

1. **user-service** — `cd services/user-service && mvn spring-boot:run`.
2. **appointment-service** — `cd services/appointment-service && mvn spring-boot:run`.
3. **insurance-service** / **pharmacy-service** / **marketplace-service** — as needed for the features you are testing.
4. **API_Gatewya** — `cd services/API_Gatewya && mvn spring-boot:run`.

The Angular app points at the gateway (e.g. `http://localhost:8082/api`). Services can share the same MySQL database (`healthcare_db`) where configured.

## Adding another service

Create a new folder under `services/` (e.g. `notifications-service`), same layout as an existing service. See [docs/ADDING_A_SERVICE.md](../docs/ADDING_A_SERVICE.md).

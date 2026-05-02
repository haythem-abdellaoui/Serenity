# Serenity — Microservice-oriented architecture

The repo is organized so each backend domain is a **separate service** under `services/`. One folder = one deployable API.

## Repository layout

```
healthcare-system/
├── apps/
│   └── web-app/                 # Angular SPA (calls API gateway)
│
├── services/
│   ├── user-service/            # Auth, user CRUD, profiles (port 8081)
│   ├── API_Gatewya/             # Spring Cloud Gateway (port 8082)
│   ├── appointment-service/     # Appointments (port 8091)
│   ├── insurance-service/       # Claims (port per service config)
│   └── …                        # Other domain services (pharmacy, marketplace, etc.)
│
├── docs/
│   ├── ARCHITECTURE.md
│   └── ADDING_A_SERVICE.md
└── README.md
```

## Core services (gateway-routed)

| Service | Port | Role |
|---------|------|------|
| **user-service** | 8081 | Login, register, user management, profiles. JWT auth. |
| **API_Gatewya** | 8082 | Routes `/api/auth/**`, `/api/users/**`, `/api/appointments/**`, `/api/insurance/**`, and other configured paths to backends. |
| **appointment-service** | 8091 | Appointments, calendar, notifications. |

The web-app uses the gateway as its API base URL. Insurance and other domains are implemented as separate services behind the same gateway where configured.

## Run order (minimal)

1. MySQL up, database created.
2. `cd services/user-service && mvn spring-boot:run`
3. `cd services/appointment-service && mvn spring-boot:run`
4. `cd services/API_Gatewya && mvn spring-boot:run`
5. `cd apps/web-app && npm install && ng serve`

Add **insurance-service** and any other services your features require before testing those flows.

See [README.md](../README.md) and [services/README.md](../services/README.md) for details.

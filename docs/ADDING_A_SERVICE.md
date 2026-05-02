# Adding a new microservice

This repo is structured so new backend services are **siblings** of `user-service`. Use this when you add another domain service (for example `notifications-service`).

## 1. Create the service folder

```text
services/
  user-service/      # existing
  your-service/      # new — one folder per service
```

## 2. Bootstrap the new service

**Option A – Copy from user-service (Spring Boot)**

- Copy `services/user-service` to `services/<your-service-name>`.
- In the new folder:
  - Change `artifactId` and `name` in `pom.xml`.
  - Change `server.port` in `application.yml`.
  - Change main class package/name if you want.
  - Remove everything that does not belong to this domain; keep only controllers, entities, repos, and services for the new domain.
  - Adjust `application.yml` (DB, etc.) for this service (own DB or shared, depending on your strategy).

**Option B – New stack (e.g. Node, Go)**

- Create `services/<your-service-name>/` with its own build file and structure.
- Use a dedicated port and document it in `services/README.md`.
- Expose a clear API (REST or other) so other services or the web-app can call it.

## 3. Document the service

- Add a row for the new service in `services/README.md` (table: name, path, port, short description).
- If other services or the web-app call it, document the base URL and main endpoints (in README or `docs/`).
- Register a route in `services/API_Gatewya` if the SPA should call it through the gateway.

## 4. Call it from the web-app or other services

- In the Angular app, add (or reuse) an env/config for the new service base URL and call it via `HttpClient` (often via the gateway).
- From another service, use `RestTemplate`, `WebClient`, or an HTTP client to call the new service’s API. No shared in-process code between services.

## Rules of thumb

- **One folder under `services/` = one runnable process.** No “mini-services” as subfolders inside `user-service`.
- **Own port, own config.** Each service has its own `application.yml` (or equivalent) and port.
- **HTTP (or messages) between services.** No direct DB access from one service into another’s DB when you go full microservices.
- **Domain-owned data.** Keep each domain’s tables and logic inside its service; other services and apps call it via its API.

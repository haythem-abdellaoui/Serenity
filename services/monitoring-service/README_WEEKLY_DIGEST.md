# Weekly Doctor Digest Scheduler

This feature adds an automatic weekly portfolio recap for doctors.

## What It Does

- Runs every Monday at 08:00 (configurable)
- Computes, per doctor:
  - `crisisCount` (mood score <= 3 during the target week)
  - `worseningPatients` (average mood dropped vs previous week)
  - `noCheckinPatients` (assigned patients with no mood entry in target week)
- Stores one digest per doctor per week in `weekly_doctor_digests`
- Pushes SSE event `doctor-weekly-digest` to connected doctors

## Configuration

`src/main/resources/application.yml`

The service now loads local secrets from `.env` in this folder via:

- `spring.config.import=optional:file:.env[.properties]`

Create local env file from template:

```powershell
cd C:\Users\Rayen\Desktop\pi\Serenity\services\monitoring-service
Copy-Item .env.example .env
```

Important keys in `.env`:

- `SERVER_PORT`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`
- `MONITORING_AI_URL`, `MONITORING_AI_ENABLED`
- `MONITORING_WEEKLY_DIGEST_CRON`, `MONITORING_WEEKLY_DIGEST_TIMEZONE`
- `MONITORING_CLEANUP_CRON`, `MONITORING_CLEANUP_TIMEZONE`, `MONITORING_CLEANUP_RETENTION_DAYS`

- `app.digest.cron` (default: `0 0 8 ? * MON`)
- `app.digest.timezone` (default: `Africa/Tunis`)

Environment override examples:

- `MONITORING_WEEKLY_DIGEST_CRON`
- `MONITORING_WEEKLY_DIGEST_TIMEZONE`

## API Endpoints

Base path: `/api/monitoring/digests`

- `GET /doctor/{doctorId}/latest` (DOCTOR, own id only)
- `GET /doctor/{doctorId}` (DOCTOR, own id only)
- `POST /run-weekly` (ADMIN or DOCTOR, manual trigger)

## Try It Locally

```powershell
cd C:\Users\Rayen\Desktop\pi\Serenity\services\monitoring-service
mvn -DskipTests compile
```

Manual generation:

```powershell
curl -X POST "http://localhost:8085/api/monitoring/digests/run-weekly" -H "Authorization: Bearer <JWT>"
```

Latest digest for doctor 6:

```powershell
curl "http://localhost:8085/api/monitoring/digests/doctor/6/latest" -H "Authorization: Bearer <JWT>"
```


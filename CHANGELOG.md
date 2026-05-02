# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html) when versions are tagged.

## [Unreleased]

### Added
- Root `README.md` expanded to reflect the real repo contents (apps + services), include a practical local quickstart, and link to existing docs in `docs/`.
- `services/insurance-service/.env.example` for local secret management.

### Changed
- `services/insurance-service/src/main/resources/application.yml` now reads configuration from a local `.env` file (gitignored) via `spring.config.import`, and no longer contains embedded default values for runtime configuration.

### Security
- Reduced risk of accidentally committing secrets by moving insurance-service configuration to environment variables loaded from a local `.env` (ignored by git).

## [2026-04-25]

### Changed
- Appointments: whisper/AI-related configuration and fixes (`application.properties` + appointment integration tweaks).

## [2026-04-23]

### Added
- Appointment AI integration and incremental fixes for whisper-based features.

## [2026-04-22]

### Added
- Pharmacy CNOPT verification: dataset + verifier integration work, plus training notebook.

## [2026-04-18]

### Added
- `apps/pharmacy-mobile`: Flutter mobile uploader app for pharmacist photo upload.

## [2026-04-17]

### Added
- Monitoring AI integration and scheduler-related improvements.

## [2026-04-15]

### Added
- Insurance: AI claim risk model integrated into the admin interface (plus model notebook/artifacts in-repo).

## [2026-04-10]

### Added
- Insurance OCR: mismatch detection / audit support for claim documents.

## [2026-04-09]

### Added
- Insurance claims: more complete lifecycle/workflow.

### Security
- Removed tracked secrets from the repository history going forward (untracked secrets file + cleanup).

## [2026-04-01]

### Added
- Appointments: CRUD + calendar/time handling + meet/teleconsultation work (merged from feature branches).

## [2026-03-31]

### Changed
- Marketplace: UX/content updates merged back into main.

### Security
- Addressed exposed credential(s) flagged by static analysis (Sonar) in configuration.

## [2026-03-27]

### Added
- API Gateway (`services/API_Gatewya`) added to route the SPA through a single entrypoint and support service routing/websockets.

## [2026-03-26]

### Added
- Marketplace module + microservice integration.
- Doctor verification: instant notifications using Redis + WebSocket.

## [2026-03-24]

### Added
- Insurance claims: notification system to follow claim progression without manually checking reimbursements.

## [2026-03-22]

### Added
- Pharmacy service module bootstrapped and initial stock/prescription workflows started.

## [2026-03-19]

### Added
- Initial auth/role selection flow groundwork.

---

<!-- (duplicate commit-by-commit section removed) -->
- **Commit 2**
  - Added: frontend/src
  - Changed: backend/src, frontend/src
- **Commit 3**
  - Changed: backend/src
- **Commit 4**
  - Changed: frontend/src
- **Commit 5**
  - Changed: frontend/src
- **Commit 6**
  - Changed: frontend/src

### 2026-03-14
- **Commit 7**
  - Added: backend/src, frontend/src, insurance-portal/package.json, insurance-portal/package-lock.json, insurance-portal/public, insurance-portal/server.js
  - Changed: .gitignore, backend/src, frontend/src
- **Commit 8**
  - Changed: .gitignore, backend/src, frontend/src
- **Commit 9**
  - Added: apps/insurance-portal, apps/web-app, docs/ARCHITECTURE.md, services/platform-api
  - Changed: README.md
- **Commit 10**
  - Added: apps/web-app
  - Changed: README.md
- **Commit 11**
  - Added: apps/README.md, docs/ADDING_A_SERVICE.md, docs/ARCHITECTURE.md, services/README.md, services/user-service
  - Changed: README.md
- **Commit 12**
  - Added: services/insurance-service, services/README.md, services/user-service
  - Changed: apps/web-app, docs/ARCHITECTURE.md, README.md
- **Commit 13**
  - Added: pom.xml
  - Changed: .gitignore, services/user-service
- **Commit 14**
  - Added: pom.xml, services/user-service
- **Commit 15**
  - Added: services/API_Gatewya
  - Changed: apps/web-app

### 2026-03-17
- **Commit 16**
  - Added: services/API_Gatewya
  - Changed: services/API_Gatewya, services/user-service
- **Commit 17**
  - Changed: services/API_Gatewya
- **Commit 18**
  - Added: services/API_Gatewya
- **Commit 19**
  - Changed: services/user-service
- **Commit 20**
  - Added: services/API_Gatewya
  - Changed: services/API_Gatewya, services/user-service

### 2026-03-18
- **Commit 21**
  - Added: services/Doctors_service

### 2026-03-19
- **Commit 22**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 23**
  - Changed: apps/web-app, services/user-service
- **Commit 24**
  - Changed: apps/web-app
- **Commit 25**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 26**
  - Added: services/user-service
  - Changed: services/user-service
- **Commit 27**
  - Added: apps/web-app
  - Changed: apps/web-app

### 2026-03-20
- **Commit 28**
  - Changed: apps/web-app
- **Commit 29**
  - Added: services/insurance-service
  - Changed: apps/insurance-portal, services/API_Gatewya, services/insurance-service

### 2026-03-22
- **Commit 30**
  - Added: apps/web-app, services/pharmacy-service
  - Changed: apps/web-app, services/API_Gatewya
- **Commit 31**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 32**
  - Changed: apps/web-app, services/API_Gatewya, services/user-service
- **Commit 33**
  - Added: services/user-service
  - Changed: apps/web-app, services/user-service
- **Commit 34**
  - Changed: services/API_Gatewya
- **Commit 35**
  - Added: services/derbelmicroservice
  - Changed: apps/web-app
- **Commit 36**
  - Added: services/derbelmicroservice
  - Changed: services/derbelmicroservice
- **Commit 37**
  - Added: apps/web-app, services/derbelmicroservice
  - Changed: apps/web-app, services/derbelmicroservice

### 2026-03-23
- **Commit 38**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 39**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 40**
  - Changed: apps/web-app
- **Commit 41**
  - Added: apps/web-app, services/API_Gatewya, services/Doctors_service
  - Changed: apps/web-app, services/API_Gatewya, services/Doctors_service, services/user-service
- **Commit 42**
  - Changed: apps/web-app, services/API_Gatewya, services/Doctors_service
- **Commit 43**
  - Added: apps/web-app, services/pharmacy-service
  - Changed: apps/web-app, services/API_Gatewya, services/pharmacy-service

### 2026-03-24
- **Commit 44**
  - Added: services/API_Gatewya
  - Changed: apps/web-app, services/API_Gatewya, services/Doctors_service
- **Commit 45**
  - Added: services/pharmacy-service
  - Changed: apps/web-app, services/pharmacy-service
- **Commit 46**
  - Added: services/insurance-service
  - Changed: apps/insurance-portal, apps/web-app, services/API_Gatewya, services/insurance-service
- **Commit 47**
  - Changed: apps/web-app
- **Commit 48**
  - Added: services/insurance-service
  - Changed: apps/web-app, services/insurance-service
- **Commit 49**
  - Added: apps/insurance-portal
  - Changed: apps/insurance-portal, apps/web-app, services/insurance-service
- **Commit 50**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 51**
  - Added: apps/web-app
  - Changed: apps/web-app, services/user-service
- **Commit 52**
  - Added: apps/web-app
  - Changed: apps/web-app

### 2026-03-25
- **Commit 53**
  - Added: apps/web-app, services/pharmacy-service
  - Changed: .gitignore, apps/web-app, services/pharmacy-service
- **Commit 54**
  - No file changes (merge/metadata)
- **Commit 55**
  - Changed: apps/web-app
- **Commit 56**
  - Changed: apps/web-app, services/user-service
- **Commit 57**
  - Added: apps/web-app
  - Changed: apps/web-app, services/Doctors_service, services/user-service
- **Commit 58**
  - Changed: apps/web-app
- **Commit 59**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 60**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 61**
  - Added: services/insurance-service
  - Changed: apps/insurance-portal, apps/web-app, services/API_Gatewya, services/insurance-service, services/user-service
- **Commit 62**
  - Added: apps/web-app
  - Changed: apps/web-app, services/pharmacy-service
- **Commit 63**
  - No file changes (merge/metadata)
- **Commit 64**
  - Changed: apps/web-app, services/user-service
- **Commit 65**
  - Changed: apps/insurance-portal, apps/web-app
- **Commit 66**
  - Added: apps/web-app, services/user-service
  - Changed: apps/web-app, services/API_Gatewya, services/user-service

### 2026-03-26
- **Commit 67**
  - Added: services/Doctors_service
  - Changed: apps/web-app, services/API_Gatewya, services/Doctors_service
- **Commit 68**
  - Changed: apps/web-app, services/API_Gatewya, services/Doctors_service
- **Commit 69**
  - Added: services/user-service
  - Changed: apps/insurance-portal, apps/web-app, services/API_Gatewya, services/insurance-service, services/user-service
- **Commit 70**
  - Changed: apps/insurance-portal, services/API_Gatewya
- **Commit 71**
  - Changed: apps/web-app
- **Commit 72**
  - No file changes (merge/metadata)
- **Commit 73**
  - Changed: apps/web-app, services/API_Gatewya
- **Commit 74**
  - No file changes (merge/metadata)
- **Commit 75**
  - Added: services/derbelmicroservice
  - Changed: apps/web-app, services/derbelmicroservice
- **Commit 76**
  - Added: apps/web-app, services/marketplace-service
  - Changed: apps/web-app, services/API_Gatewya, services/README.md, services/user-service
- **Commit 77**
  - No file changes (merge/metadata)
- **Commit 78**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 79**
  - Added: services/Doctors_service
  - Changed: apps/web-app, services/Doctors_service
- **Commit 80**
  - Added: apps/web-app, services/marketplace-service
  - Changed: apps/web-app, services/API_Gatewya, services/marketplace-service, services/README.md
- **Commit 81**
  - Added: services/Doctors_service
  - Changed: services/Doctors_service
- **Commit 82**
  - Changed: apps/web-app
- **Commit 83**
  - Added: apps/web-app, services/user-service
  - Changed: apps/web-app, services/user-service

### 2026-03-27
- **Commit 84**
  - Changed: apps/web-app
- **Commit 85**
  - Changed: apps/web-app
- **Commit 86**
  - Changed: apps/web-app
- **Commit 87**
  - Added: apps/web-app, services/marketplace-service
  - Changed: apps/web-app, services/marketplace-service
- **Commit 88**
  - Added: services/marketplace-service
  - Changed: apps/web-app, services/marketplace-service
- **Commit 89**
  - Changed: apps/web-app, services/API_Gatewya
- **Commit 90**
  - Changed: services/Doctors_service
- **Commit 91**
  - Changed: apps/web-app, services/API_Gatewya
- **Commit 92**
  - Changed: apps/web-app
- **Commit 93**
  - Changed: apps/web-app
- **Commit 94**
  - Added: apps/web-app, services/derbelmicroservice
  - Changed: apps/web-app, services/API_Gatewya, services/derbelmicroservice
- **Commit 95**
  - Added: apps/web-app
  - Changed: apps/web-app, services/user-service
- **Commit 96**
  - Changed: apps/web-app
- **Commit 97**
  - Changed: apps/web-app
- **Commit 98**
  - Added: apps/web-app
  - Changed: services/API_Gatewya, services/Doctors_service
- **Commit 99**
  - Changed: services/Doctors_service
- **Commit 100**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 101**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 102**
  - Changed: apps/web-app
- **Commit 103**
  - Changed: apps/web-app
- **Commit 104**
  - Changed: apps/web-app
- **Commit 105**
  - Changed: apps/web-app, services/user-service
- **Commit 106**
  - Changed: apps/web-app

### 2026-03-28
- **Commit 107**
  - Changed: apps/web-app
- **Commit 108**
  - Added: services/pharmacy-service
  - Changed: .gitignore, apps/web-app, services/pharmacy-service
- **Commit 109**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 110**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 111**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 112**
  - Added: apps/web-app, services/derbelmicroservice
  - Changed: apps/web-app, services/derbelmicroservice
- **Commit 113**
  - Added: apps/web-app, services/monitoring-service
  - Changed: apps/web-app, services/user-service
- **Commit 114**
  - Changed: apps/web-app
- **Commit 115**
  - Changed: apps/web-app, services/API_Gatewya, services/pharmacy-service
- **Commit 116**
  - Changed: apps/web-app
- **Commit 117**
  - Changed: apps/web-app, services/user-service

### 2026-03-29
- **Commit 118**
  - Changed: apps/web-app
- **Commit 119**
  - No file changes (merge/metadata)
- **Commit 120**
  - No file changes (merge/metadata)
- **Commit 121**
  - Changed: apps/insurance-portal, apps/web-app
- **Commit 122**
  - Changed: apps/web-app
- **Commit 123**
  - Changed: apps/web-app
- **Commit 124**
  - Added: services/user-service
  - Changed: apps/web-app, services/user-service
- **Commit 125**
  - No file changes (merge/metadata)

### 2026-03-30
- **Commit 126**
  - Changed: apps/insurance-portal, apps/web-app, services/API_Gatewya, services/pharmacy-service, services/user-service
- **Commit 127**
  - Added: services/Doctors_service
  - Changed: apps/web-app, services/Doctors_service
- **Commit 128**
  - Changed: apps/web-app, services/user-service
- **Commit 129**
  - Changed: apps/web-app
- **Commit 130**
  - Changed: apps/web-app
- **Commit 131**
  - No file changes (merge/metadata)
- **Commit 132**
  - Added: services/user-service
  - Changed: apps/insurance-portal, apps/web-app, services/derbelmicroservice, services/user-service
- **Commit 133**
  - No file changes (merge/metadata)

### 2026-03-31
- **Commit 134**
  - Added: services/marketplace-service
  - Changed: apps/web-app, services/Doctors_service, services/marketplace-service
- **Commit 135**
  - No file changes (merge/metadata)
- **Commit 136**
  - Changed: apps/insurance-portal, apps/web-app
- **Commit 137**
  - Added: apps/web-app
  - Changed: apps/insurance-portal, apps/web-app
- **Commit 138**
  - Changed: services/Doctors_service
- **Commit 139**
  - Changed: apps/web-app
- **Commit 140**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 141**
  - Changed: apps/web-app
- **Commit 142**
  - Added: services/pharmacy-service
  - Changed: services/pharmacy-service
- **Commit 143**
  - Changed: apps/web-app
- **Commit 144**
  - Added: services/pharmacy-service
  - Changed: services/pharmacy-service

### 2026-04-01
- **Commit 145**
  - Added: apps/web-app, services/user-service
  - Changed: apps/web-app, services/user-service
- **Commit 146**
  - Added: apps/insurance-portal, apps/web-app, services/API_Gatewya, services/appointment-service, services/insurance-service, services/pharmacy-service, services/user-service
  - Changed: .gitignore, apps/web-app, docs/ARCHITECTURE.md, pom.xml, README.md, services/API_Gatewya, services/README.md, services/user-service
- **Commit 147**
  - No file changes (merge/metadata)
- **Commit 148**
  - Changed: apps/insurance-portal, apps/web-app
- **Commit 149**
  - Changed: services/monitoring-service, services/user-service
- **Commit 150**
  - Added: Serenity
- **Commit 151**
  - Changed: .gitignore, apps/README.md, apps/web-app, docs/ADDING_A_SERVICE.md, docs/ARCHITECTURE.md, pom.xml, README.md, services/API_Gatewya, services/README.md, services/user-service
- **Commit 152**
  - No file changes (merge/metadata)

### 2026-04-04
- **Commit 153**
  - Added: services/insurance-service
  - Changed: apps/web-app, services/Doctors_service
- **Commit 154**
  - Changed: apps/web-app
- **Commit 155**
  - Added: services/derbelmicroservice
  - Changed: apps/web-app, services/derbelmicroservice

### 2026-04-05
- **Commit 156**
  - Added: services/insurance-service
  - Changed: apps/insurance-portal, apps/web-app, services/insurance-service
- **Commit 157**
  - Added: services/user-service
  - Changed: apps/insurance-portal, apps/web-app, services/insurance-service, services/user-service
- **Commit 158**
  - Added: services/insurance-service
  - Changed: apps/insurance-portal, apps/web-app, services/insurance-service

### 2026-04-08
- **Commit 159**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 160**
  - Changed: apps/web-app
- **Commit 161**
  - Added: services/user-service
  - Changed: apps/web-app, services/API_Gatewya, services/appointment-service, services/derbelmicroservice, services/Doctors_service, services/pharmacy-service, services/user-service
- **Commit 162**
  - Changed: services/Doctors_service, services/user-service
- **Commit 163**
  - No file changes (merge/metadata)
- **Commit 164**
  - Added: apps/web-app
  - Changed: apps/web-app, services/API_Gatewya

### 2026-04-09
- **Commit 165**
  - No file changes (merge/metadata)
- **Commit 166**
  - Changed: apps/web-app
- **Commit 167**
  - Added: services/insurance-service
  - Changed: apps/insurance-portal, apps/web-app, services/insurance-service
- **Commit 168**
  - Changed: apps/insurance-portal, apps/web-app
- **Commit 169**
  - Added: services/Doctors_service
  - Changed: .gitignore, services/Doctors_service, services/user-service
- **Commit 170**
  - No file changes (merge/metadata)
- **Commit 171**
  - Changed: .gitignore
- **Commit 172**
  - Changed: services/Doctors_service
- **Commit 173**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 174**
  - Added: apps/web-app, services/pharmacy-service, services/user-service
  - Changed: apps/web-app, services/pharmacy-service, services/user-service
- **Commit 175**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 176**
  - Changed: apps/web-app, services/API_Gatewya, services/Doctors_service
- **Commit 177**
  - No file changes (merge/metadata)
- **Commit 178**
  - Changed: apps/web-app, services/pharmacy-service, services/user-service

### 2026-04-10
- **Commit 179**
  - Added: apps/web-app, services/insurance-service
  - Changed: apps/insurance-portal, apps/web-app, services/insurance-service
- **Commit 180**
  - No file changes (merge/metadata)
- **Commit 181**
  - Changed: apps/web-app

### 2026-04-11
- **Commit 182**
  - Added: services/user-service
  - Changed: apps/insurance-portal, apps/web-app, services/user-service
- **Commit 183**
  - Changed: apps/web-app
- **Commit 184**
  - Changed: apps/web-app
- **Commit 185**
  - No file changes (merge/metadata)

### 2026-04-12
- **Commit 186**
  - Added: AI/Datasets, AI/gru_api, AI/mental_health.csv, AI/mental-health-text-classification-analysis.ipynb
- **Commit 187**
  - Added: services/derbelmicroservice
  - Changed: apps/web-app, services/derbelmicroservice
- **Commit 188**
  - Added: services/ai-severity-derbel-service, services/derbelmicroservice
  - Changed: apps/web-app
- **Commit 189**
  - Added: services/ai-severity-derbel-service, services/derbelmicroservice
  - Changed: apps/web-app, services/ai-severity-derbel-service

### 2026-04-13
- **Commit 190**
  - Added: services/derbelmicroservice
  - Changed: apps/web-app, services/derbelmicroservice

### 2026-04-14
- **Commit 191**
  - Added: apps/pharmacy-mobile, services/pharmacy-service
  - Changed: apps/web-app, services/pharmacy-service
- **Commit 192**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 193**
  - Added: .venv/Lib, .venv/Scripts, services/claim-risk-model
  - Changed: apps/web-app
- **Commit 194**
  - Changed: .gitignore, services/claim-risk-model
- **Commit 195**
  - No file changes (merge/metadata)
- **Commit 196**
  - No file changes (merge/metadata)
- **Commit 197**
  - Changed: apps/insurance-portal
- **Commit 198**
  - Added: services/pharmacy-service
  - Changed: apps/web-app, services/pharmacy-service
- **Commit 199**
  - Added: services/Doctors_service
  - Changed: AI/gru_api, apps/web-app, services/Doctors_service

### 2026-04-15
- **Commit 200**
  - Changed: apps/insurance-portal
- **Commit 201**
  - Added: services/insurance-service
  - Changed: apps/web-app, services/claim-risk-model, services/insurance-service
- **Commit 202**
  - Changed: apps/web-app, services/ai-severity-derbel-service, services/claim-risk-model
- **Commit 203**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 204**
  - Added: services/Doctors_service
  - Changed: apps/web-app, services/Doctors_service
- **Commit 205**
  - Added: services/Doctors_service
  - Changed: apps/web-app, services/Doctors_service
- **Commit 206**
  - Added: services/insurance-service
  - Changed: apps/web-app, services/insurance-service

### 2026-04-16
- **Commit 207**
  - Changed: apps/web-app, services/derbelmicroservice

### 2026-04-17
- **Commit 208**
  - Added: services/monitoring, services/monitoring-service
  - Changed: apps/web-app, services/monitoring-service

### 2026-04-18
- **Commit 209**
  - Added: apps/pharmacy-mobile, services/pharmacy-ml, services/pharmacy-service
  - Changed: apps/web-app, services/pharmacy-service, services/README.md

### 2026-04-19
- **Commit 210**
  - Changed: AI/mental-health-text-classification-analysis.ipynb, apps/web-app, services/Doctors_service
- **Commit 211**
  - No file changes (merge/metadata)

### 2026-04-20
- **Commit 212**
  - Added: services/ai-severity-derbel-service
  - Changed: apps/web-app, services/ai-severity-derbel-service, services/derbelmicroservice, services/user-service
- **Commit 213**
  - No file changes (merge/metadata)
- **Commit 214**
  - No file changes (merge/metadata)

### 2026-04-21
- **Commit 215**
  - Changed: apps/insurance-portal, apps/web-app, services/claim-risk-model, services/insurance-service, services/user-service

### 2026-04-22
- **Commit 216**
  - Added: data/cnopt_dataset, services/pharmacy-ml, services/pharmacy-service
  - Changed: apps/web-app, services/pharmacy-ml, services/pharmacy-service
- **Commit 217**
  - Added: services/pharmacy-ml
- **Commit 218**
  - Changed: .gitignore
- **Commit 219**
  - Added: apps/web-app
  - Changed: apps/web-app, services/pharmacy-ml, services/pharmacy-service, services/README.md
- **Commit 220**
  - No file changes (merge/metadata)

### 2026-04-23
- **Commit 221**
  - Added: apps/web-app, services/appointment-service, services/user-service, services/whisper-ai-service
  - Changed: .gitignore, apps/web-app, services/API_Gatewya, services/appointment-service
- **Commit 222**
  - Added: services/Doctors_service
  - Changed: apps/web-app, services/whisper-ai-service

### 2026-04-25
- **Commit 223**
  - Added: docs/APPOINTMENTS_EXPLAINED_LIKE_IM_5.md, services/appointment-service, services/monitoring
  - Changed: apps/pharmacy-mobile, apps/web-app, services/appointment-service, services/whisper-ai-service
<!-- COMMIT_DIFF_END -->
 

### Changed
- Appointments: whisper/AI-related configuration and fixes (`application.properties` + appointment integration tweaks).

## [2026-04-23]

### Added
- Appointment AI integration and incremental fixes for whisper-based features.

## [2026-04-22]

### Added
- Pharmacy CNOPT verification: dataset + verifier integration work, plus training notebook.

## [2026-04-18]

### Added
- `apps/pharmacy-mobile`: Flutter mobile uploader app for pharmacist photo upload.

## [2026-04-17]

### Added
- Monitoring AI integration and scheduler-related improvements.

## [2026-04-15]

### Added
- Insurance: AI claim risk model integrated into the admin interface (plus model notebook/artifacts in-repo).

## [2026-04-10]

### Added
- Insurance OCR: mismatch detection / audit support for claim documents.

## [2026-04-09]

### Added
- Insurance claims: more complete lifecycle/workflow.

### Security
- Removed tracked secrets from the repository history going forward (untracked secrets file + cleanup).

## [2026-04-01]

### Added
- Appointments: CRUD + calendar/time handling + meet/teleconsultation work (merged from feature branches).

## [2026-03-31]

### Changed
- Marketplace: UX/content updates merged back into main.

### Security
- Addressed exposed credential(s) flagged by static analysis (Sonar) in configuration.

## [2026-03-27]

### Added
- API Gateway (`services/API_Gatewya`) added to route the SPA through a single entrypoint and support service routing/websockets.

## [2026-03-26]

### Added
- Marketplace module + microservice integration.
- Doctor verification: instant notifications using Redis + WebSocket.

## [2026-03-24]

### Added
- Insurance claims: notification system to follow claim progression without manually checking reimbursements.

## [2026-03-22]

### Added
- Pharmacy service module bootstrapped and initial stock/prescription workflows started.

## [2026-03-19]

### Added
- Initial auth/role selection flow groundwork.

---

## Commit-by-commit history (diff-based, no hashes)

This is an autogenerated, chronological, **diff-based** view from `main`.

- No commit hashes
- No commit messages
- Shows only **Added** and **Changed** (deletions are intentionally omitted)

<!-- COMMIT_DIFF_START -->
### 2026-03-06
- **Commit 1**
  - No file changes (merge/metadata)
- **Commit 2**
  - Added: frontend/src
  - Changed: backend/src, frontend/src
- **Commit 3**
  - Changed: backend/src
- **Commit 4**
  - Changed: frontend/src
- **Commit 5**
  - Changed: frontend/src
- **Commit 6**
  - Changed: frontend/src

### 2026-03-14
- **Commit 7**
  - Added: backend/src, frontend/src, insurance-portal/package.json, insurance-portal/package-lock.json, insurance-portal/public, insurance-portal/server.js
  - Changed: .gitignore, backend/src, frontend/src
- **Commit 8**
  - Changed: .gitignore, backend/src, frontend/src
- **Commit 9**
  - Added: apps/insurance-portal, apps/web-app, docs/ARCHITECTURE.md, services/platform-api
  - Changed: README.md
- **Commit 10**
  - Added: apps/web-app
  - Changed: README.md
- **Commit 11**
  - Added: apps/README.md, docs/ADDING_A_SERVICE.md, docs/ARCHITECTURE.md, services/README.md, services/user-service
  - Changed: README.md
- **Commit 12**
  - Added: services/insurance-service, services/README.md, services/user-service
  - Changed: apps/web-app, docs/ARCHITECTURE.md, README.md
- **Commit 13**
  - Added: pom.xml
  - Changed: .gitignore, services/user-service
- **Commit 14**
  - Added: pom.xml, services/user-service
- **Commit 15**
  - Added: services/API_Gatewya
  - Changed: apps/web-app

### 2026-03-17
- **Commit 16**
  - Added: services/API_Gatewya
  - Changed: services/API_Gatewya, services/user-service
- **Commit 17**
  - Changed: services/API_Gatewya
- **Commit 18**
  - Added: services/API_Gatewya
- **Commit 19**
  - Changed: services/user-service
- **Commit 20**
  - Added: services/API_Gatewya
  - Changed: services/API_Gatewya, services/user-service

### 2026-03-18
- **Commit 21**
  - Added: services/Doctors_service

### 2026-03-19
- **Commit 22**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 23**
  - Changed: apps/web-app, services/user-service
- **Commit 24**
  - Changed: apps/web-app
- **Commit 25**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 26**
  - Added: services/user-service
  - Changed: services/user-service
- **Commit 27**
  - Added: apps/web-app
  - Changed: apps/web-app

### 2026-03-20
- **Commit 28**
  - Changed: apps/web-app
- **Commit 29**
  - Added: services/insurance-service
  - Changed: apps/insurance-portal, services/API_Gatewya, services/insurance-service

### 2026-03-22
- **Commit 30**
  - Added: apps/web-app, services/pharmacy-service
  - Changed: apps/web-app, services/API_Gatewya
- **Commit 31**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 32**
  - Changed: apps/web-app, services/API_Gatewya, services/user-service
- **Commit 33**
  - Added: services/user-service
  - Changed: apps/web-app, services/user-service
- **Commit 34**
  - Changed: services/API_Gatewya
- **Commit 35**
  - Added: services/derbelmicroservice
  - Changed: apps/web-app
- **Commit 36**
  - Added: services/derbelmicroservice
  - Changed: services/derbelmicroservice
- **Commit 37**
  - Added: apps/web-app, services/derbelmicroservice
  - Changed: apps/web-app, services/derbelmicroservice

### 2026-03-23
- **Commit 38**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 39**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 40**
  - Changed: apps/web-app
- **Commit 41**
  - Added: apps/web-app, services/API_Gatewya, services/Doctors_service
  - Changed: apps/web-app, services/API_Gatewya, services/Doctors_service, services/user-service
- **Commit 42**
  - Changed: apps/web-app, services/API_Gatewya, services/Doctors_service
- **Commit 43**
  - Added: apps/web-app, services/pharmacy-service
  - Changed: apps/web-app, services/API_Gatewya, services/pharmacy-service

### 2026-03-24
- **Commit 44**
  - Added: services/API_Gatewya
  - Changed: apps/web-app, services/API_Gatewya, services/Doctors_service
- **Commit 45**
  - Added: services/pharmacy-service
  - Changed: apps/web-app, services/pharmacy-service
- **Commit 46**
  - Added: services/insurance-service
  - Changed: apps/insurance-portal, apps/web-app, services/API_Gatewya, services/insurance-service
- **Commit 47**
  - Changed: apps/web-app
- **Commit 48**
  - Added: services/insurance-service
  - Changed: apps/web-app, services/insurance-service
- **Commit 49**
  - Added: apps/insurance-portal
  - Changed: apps/insurance-portal, apps/web-app, services/insurance-service
- **Commit 50**
  - Changed: apps/web-app, services/Doctors_service
- **Commit 51**
  - Added: apps/web-app
  - Changed: apps/web-app, services/user-service
- **Commit 52**
  - Added: apps/web-app
  - Changed: apps/web-app

### 2026-03-25
- **Commit 53**
  - Added: apps/web-app, services/pharmacy-service
  - Changed: .gitignore, apps/web-app, services/pharmacy-service
- **Commit 54**
  - No file changes (merge/metadata)
- **Commit 55**
  - Changed: apps/web-app
- **Commit 56**
  - Changed: apps/web-app, services/user-service
- **Commit 57**
  - Added: apps/web-app
  - Changed: apps/web-app, services/Doctors_service, services/user-service
- **Commit 58**
  - Changed: apps/web-app
- **Commit 59**
  - Added: apps/web-app
  - Changed: apps/web-app
- **Commit 60**
  - Changed: apps/web-app, services/Doctors_service
<!-- COMMIT_DIFF_END -->

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html) when versions are tagged.

## [Unreleased]

### Added
- Root `README.md` expanded to reflect the real repo contents (apps + services), include a practical local quickstart, and link to existing docs in `docs/`.
- `services/insurance-service/.env.example` for local secret management.

### Changed
- `services/insurance-service/src/main/resources/application.yml` now reads configuration from a local `.env` file (gitignored) via `spring.config.import`, and no longer contains embedded default values for runtime configuration.

### Security
- Reduced risk of accidentally committing secrets by moving insurance-service configuration to environment variables loaded from a local `.env` (ignored by git).

## [2026-04-25]

### Changed
- Appointments: whisper/AI-related configuration and fixes (`application.properties` + appointment integration tweaks).

## [2026-04-23]

### Added
- Appointment AI integration and incremental fixes for whisper-based features.

## [2026-04-22]

### Added
- Pharmacy CNOPT verification: dataset + verifier integration work, plus training notebook.

## [2026-04-18]

### Added
- `apps/pharmacy-mobile`: Flutter mobile uploader app for pharmacist photo upload.

## [2026-04-17]

### Added
- Monitoring AI integration and scheduler-related improvements.

## [2026-04-15]

### Added
- Insurance: AI claim risk model integrated into the admin interface (plus model notebook/artifacts in-repo).

## [2026-04-10]

### Added
- Insurance OCR: mismatch detection / audit support for claim documents.

## [2026-04-09]

### Added
- Insurance claims: more complete lifecycle/workflow.

### Security
- Removed tracked secrets from the repository history going forward (untracked secrets file + cleanup).

## [2026-04-01]

### Added
- Appointments: CRUD + calendar/time handling + meet/teleconsultation work (merged from feature branches).

## [2026-03-31]

### Changed
- Marketplace: UX/content updates merged back into main.

### Security
- Addressed exposed credential(s) flagged by static analysis (Sonar) in configuration.

## [2026-03-27]

### Added
- API Gateway (`services/API_Gatewya`) added to route the SPA through a single entrypoint and support service routing/websockets.

## [2026-03-26]

### Added
- Marketplace module + microservice integration.
- Doctor verification: instant notifications using Redis + WebSocket.

## [2026-03-24]

### Added
- Insurance claims: notification system to follow claim progression without manually checking reimbursements.

## [2026-03-22]

### Added
- Pharmacy service module bootstrapped and initial stock/prescription workflows started.

## [2026-03-19]

### Added
- Initial auth/role selection flow groundwork.

---

*** End of File
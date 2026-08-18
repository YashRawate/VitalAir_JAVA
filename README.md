# Vital Air — Java / Spring Boot Edition

Hyper-local air quality intelligence: predicts AQI in blind spots between official
monitoring stations, forecasts short-term trends, and routes people around
polluted areas. This is a from-scratch **Java 21 / Spring Boot 3** reimplementation
of a hackathon-winning Python/FastAPI prototype — see [Acknowledgement](#acknowledgement).

## What changed vs. the original prototype

This was a deliberate re-architecture, not a line-by-line port. Specifically:

| Area | Original (Python) | This version (Java) |
|---|---|---|
| API layer | FastAPI, single `main.py` (1700+ lines) | Spring MVC, layered `controller/service/repository` |
| Interpolation | Inline IDW only; RBF and kriging existed in `idw.py`/`grid_generator.py` but were **never actually called** | All three (IDW, RBF, simplified kriging) implemented as a `Strategy` pattern and wired into the live heatmap path |
| Background jobs | Two independent AWS Lambdas (`data_collector.py`, `ml_processor.py`), decoupled from the live API | `@Scheduled` jobs in the same Spring Boot process (see [Scoping decisions](#scoping-decisions)) |
| Persistence | DynamoDB + S3, ad hoc | Normalized relational schema via Spring Data JPA (Postgres in prod, H2 in-memory for local dev) |
| Auth | None | Spring Security + JWT, role-based (`USER`/`ADMIN`) |
| API keys | Hardcoded literal fallback values in source | Environment variables only, via `@ConfigurationProperties` — see [Security note](#security-note-api-keys) |
| Heatmap generation | Live fan-out across ~10 cities per request | Interpolated from persisted sensor history (populated by the scheduled collector), with a live-fetch fallback if the database is empty |

## Scoping decisions

The original prototype's README and slides described an AI chatbot and a trained
ML model. Neither exists in the actual source — the "ML" is physics/heuristics-based
spatial interpolation (IDW/RBF/kriging) plus a pattern-based short-term forecast
(hour-of-day multipliers), not a trained predictive model, and there's no chatbot
code anywhere. This migration keeps that honest:

- **No chatbot** was added. Adding a fake one to "preserve" a feature that was
  never built would be worse than not having it.
- **No trained ML model** was added either. The interpolation and forecast logic
  is faithfully ported and, in the interpolation case, genuinely improved (three
  real strategies instead of one dead-code one wired into two).
- **Background collection** runs as in-process `@Scheduled` jobs rather than
  separate Lambdas, trading some deployment flexibility for a single, simpler
  deployable — the explicit trade-off made for this migration.

## Architecture

```
vital-air-backend/
├── src/main/java/com/vitalair/
│   ├── controller/     REST endpoints (thin - delegate to services)
│   ├── service/         Business logic, incl. service/interpolation/ (Strategy pattern)
│   ├── repository/      Spring Data JPA repositories
│   ├── entity/           JPA entities (see schema below)
│   ├── dto/               Request/response DTOs - entities never leave the service layer
│   ├── config/           @ConfigurationProperties, CORS, caching, security, RestClient
│   ├── security/         JWT filter, token provider, UserDetailsService
│   ├── exception/       Custom exceptions + @RestControllerAdvice
│   └── util/               AqiCalculator (EPA breakpoint math), HaversineUtil
├── src/main/resources/
│   ├── application.yml          Base config + region/city reference data
│   ├── application-prod.yml    Postgres overrides
│   └── static/index.html         Bundled frontend (see Deployment)
├── Dockerfile
├── docker-compose.yml    Postgres + backend, for local dev
├── render.yaml                  Render blueprint
├── railway.json                Railway config
└── .env.example

frontend/
├── index.html               Standalone copy of the frontend, for split deployments
├── config.example.js       Backend URL override for split deployments
├── vercel.json
└── netlify.toml
```

### Database schema

`User` · `SensorReading` · `Forecast` (+ `ForecastPoint`) · `AqiZone` · `RouteQuery` ·
`PredictionHistory` · `City`. Region/city reference data (bounding boxes, base AQI
values) live in `application.yml` under `vitalair.regions.*` rather than as a
hardcoded dict, so new regions can be added without a rebuild.

### API surface

| Endpoint | Notes |
|---|---|
| `GET /api/predict/{lat}/{lon}` | Current AQI at a point |
| `GET /api/forecast?lat=&lon=&hours=` | Short-term forecast |
| `GET /api/heatmap?region=` | Interpolated grid |
| `GET /api/zones?region=` | Concentric severity rings |
| `GET /api/route/safe?start_lat=&start_lon=&end_lat=&end_lon=` | Direct vs. AQI-optimized route |
| `GET /api/sensors?region=` | Latest reading per location |
| `GET /api/hotspots?region=` | High-AQI locations |
| `GET /api/pollution-sources?region=` | Seasonal source breakdown |
| `GET /api/locations/search?query=` | City/location autocomplete |
| `POST /api/auth/register`, `POST /api/auth/login` | JWT issuance |
| `POST /api/admin/collect-now` | Manually trigger the scheduled collector (`ADMIN` only) |
| `GET /actuator/health` | Health check |

All read endpoints above are public, matching the original prototype's open API.

## Running locally

**Fastest path (Docker Compose, includes Postgres):**

```bash
cd vital-air-backend
cp .env.example .env   # fill in VITALAIR_JWT_SECRET at minimum; API keys optional
docker compose up --build
```

Then open `http://localhost:8080` — the bundled frontend is served at `/`, calling
the API at same-origin `/api/*`.

**Without Docker** (uses the in-memory H2 database by default, zero setup):

```bash
cd vital-air-backend
export VITALAIR_JWT_SECRET="a-random-string-at-least-32-characters-long"
mvn spring-boot:run
```

Third-party API keys (`OPENWEATHER_API_KEY`, `OPENAQ_API_KEY`, etc.) are optional —
the multi-API fallback chain degrades to Open-Meteo (no key required) or, failing
that, each city's static base AQI, same as the original.

## Deployment

**Single deployable (recommended to start):** the Spring Boot app serves the
bundled frontend itself from `src/main/resources/static/index.html`. Deploy the
backend to Render (`render.yaml` included) or Railway (`railway.json` included);
nothing else needed.

**Split deployment** (frontend on Vercel/Netlify, backend on Render/Railway):
deploy `frontend/` as a static site, and set the backend's URL via
`frontend/config.example.js` → rename to `config.js`, fill in the URL, and add
`<script src="config.js"></script>` before the main script block in `index.html`.
You'll also need to update `CorsConfig` to restrict `allowedOriginPatterns` to your
actual frontend origin instead of `*` before going to production this way.

## Security note: API keys

The original `main.py` had real-looking third-party API keys hardcoded as literal
fallback defaults in source. This version reads every credential exclusively from
environment variables (`ApiKeysProperties`, `JwtProperties`) — nothing is ever
hardcoded. If the original keys were ever live, rotate them; treat anything that
was in a public/shared zip as compromised.

## Known simplifications

- No Flyway/Liquibase migrations — Hibernate manages the schema (`ddl-auto:
  update`) in both profiles. Fine for a portfolio/learning deployment; a real
  production rollout should add versioned migrations.
- The heatmap endpoint interpolates from persisted sensor history rather than
  live-fetching ~400 grid points per request (what the original did) — much
  cheaper, but means a fresh deployment's first `/api/heatmap` call before the
  first scheduled collection run falls back to a live per-city fetch instead of
  a fine-grained grid.
- `RouteQuery`/`PredictionHistory` are new audit tables the original didn't have;
  nothing currently reads them back out via an API (a natural next step, e.g. a
  "my past routes" endpoint once accounts are wired into the frontend).

## Acknowledgement

This project was originally developed as a team during the TECHNEX'26 Eco-Hackathon:
K Praveen Kumar (Cloud/AWS), Yash Kumar Rawate (software development support),
Abhijeet Kumar (Frontend/UI), and G. Sandhya Reddy (ECE/map integration). The
original idea, concept, and hackathon solution belong to the entire team.

This repository is an independent reimplementation using Java and Spring Boot,
built for learning, portfolio development, and backend engineering practice. It
is not the original hackathon submission.

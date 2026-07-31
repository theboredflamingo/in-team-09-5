# ReconX — Enterprise Trade Reconciliation Platform (Student Starter)

> Deutsche Bank — TDI 2026 Graduate Technical Training Programme  
> **Advanced Track (Intermediate-Hybrid)** | 10-Day Case Study | Version 1.0

This repository is the **starter scaffold** for the ReconX case study. Each day of the programme adds another layer to the system. By Day 10 you and your team will have built, dockerised, tested, and monitored a near-production-grade trade reconciliation platform with Kafka event streaming, JWT-backed RBAC, a React 19 dashboard, and a CI/CD pipeline that ships Docker images to GHCR.

---

## What you will build

A near-production-grade trade reconciliation platform used (in concept) by an Ops team to detect and resolve mismatches between internal trade records and external counterparty/custodian feeds — built across 10 days, 165 tickets.

```
       ┌──────────┐        ┌──────────────────────────┐        ┌────────────┐
       │  React   │  HTTPS │  Spring Boot REST API    │  JDBC  │ PostgreSQL │
       │ Frontend │ ─────▶ │  recon-service (Java 21) │ ─────▶ │  (Liqui-   │
       │  + Vite  │        │  + Spring Security/JWT   │        │   base     │
       └────┬─────┘        │  + Actuator/Micrometer   │        │   migs)    │
            │              └────────┬─────────────────┘        └─────┬──────┘
            │ SSE                   │  KafkaTemplate / @KafkaListener│
            │                       ▼                                ▼
            │              ┌──────────────────┐               ┌────────────┐
            └──────────────│  Apache Kafka    │               │ recon_*    │
                           │  trade-events    │               │ audit_log  │
                           │  recon-results   │               │ mat. views │
                           │  system-alerts   │               └────────────┘
                           │  + DLQ topics    │
                           └────────┬─────────┘
                                    ▼
                           ┌─────────────────────────┐
                           │ ReconConsumer (auto-rec)│
                           │ AuditConsumer (history) │
                           │ AlertConsumer  (notify) │
                           └─────────────────────────┘

  /actuator/prometheus ─▶ Prometheus (scrape) ─▶ Grafana dashboards + alerts
```

---

## Repository layout

```
├── db/                            ← Day 1: standalone SQL assets
│   ├── queries.sql                ← Analytical queries (window fns, CTEs, JSONB)
│   ├── partitioning.sql           ← Monthly trade partitions
│   └── erd.md                     ← Mermaid ER diagram
│
│   NOTE: Liquibase changelogs live on the JVM classpath at
│         backend/src/main/resources/db/changelog/ — not here.
│
├── backend/                       ← Days 2-6, 9: Java 21 + Spring Boot 3 + Kafka
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/dbtraining/reconx/
│       ├── ReconxApplication.java
│       ├── model/                 ← Day 2-3: sealed TradeType hierarchy, value objects
│       ├── repository/            ← Day 4-5: Spring Data JPA + Specifications
│       ├── service/               ← Day 3-6: reconciliation engine, analytics
│       ├── controller/            ← Day 5: REST API endpoints
│       ├── dto/                   ← Request/response DTOs, TradeEvent, MapStruct mappers
│       ├── exception/             ← Custom hierarchy + @RestControllerAdvice
│       ├── config/                ← Swagger, JPA, Liquibase, Cache, Kafka config
│       ├── security/              ← Day 5: JWT filter, RBAC
│       ├── kafka/                 ← Day 9: producers, consumers, DLQ
│       └── observability/         ← Day 6: custom Micrometer metrics
│
├── static-dashboard/              ← Day 7: vanilla HTML/CSS/JS (pre-React exercise)
│   ├── dashboard.html
│   ├── trades.html
│   ├── recon.html
│   ├── css/style.css
│   └── js/*.js
│
├── frontend/                      ← Day 8-9: React 19 + Vite recon-ui
│   ├── package.json
│   ├── vite.config.js
│   ├── Dockerfile
│   └── src/
│       ├── App.jsx
│       ├── components/            ← DataTable (compound), TradeRow, StatCard, …
│       ├── hooks/                 ← useTradeStream, useDebouncedSearch
│       ├── context/               ← ThemeProvider, AuthProvider
│       ├── services/              ← apiService.js
│       └── pages/                 ← Dashboard, Trades, Login, AddTrade
│
├── monitoring/                    ← Day 6 + 10: Prometheus / Grafana
│   ├── prometheus/prometheus.yml
│   └── grafana/provisioning/
│
├── .github/workflows/ci.yml       ← Day 10: GitHub Actions pipeline
└── docker-compose.yml             ← Day 10: 7-service stack
```

The full per-day walkthrough lives in [`./student-guides/`](./student-guides/README.md) when present. **Read [`student-guides/day0/README.md`](./student-guides/day0/README.md) before you start.**

---

## Prerequisites

- **Java 21+** (Temurin recommended)
- **Maven 3.9+** (or use `./mvnw` in `backend/`)
- **Node.js 20+** and npm
- **[Docker Desktop](https://www.docker.com/products/docker-desktop/)** — allocate ≥ 6 GB RAM (Kafka + Postgres + Prometheus + Grafana is heavier than Intermediate)
- **Git**
- IDE: IntelliJ IDEA (backend) + VS Code / Cursor (frontend) recommended

Verify Docker is running:

```bash
docker --version
docker compose version
```

---

## Quick start — full stack (Docker, recommended)

From the project root:

```bash
docker compose up -d --build
```

The first run builds the backend and frontend images and may take a few minutes.

### Check that services are up

```bash
docker compose ps
```

All core services should show **healthy** status:

| Service    | Container           | URL                              |
|------------|---------------------|----------------------------------|
| Frontend   | `reconx-frontend`   | http://localhost:5173            |
| Backend    | `reconx-backend`    | http://localhost:8080            |
| Grafana    | `reconx-grafana`    | http://localhost:3000            |
| Prometheus | `reconx-prometheus` | http://localhost:9090            |
| PostgreSQL | `reconx-postgres`   | `localhost:5432`                 |
| Kafka      | `reconx-kafka`      | `localhost:9092`                 |

### Verify the backend

```bash
curl http://localhost:8080/api/actuator/health
```

Expected response: `{"status":"UP"}`

### Useful URLs (Docker)

| What            | URL                                              |
|-----------------|--------------------------------------------------|
| Swagger UI      | http://localhost:8080/api/swagger-ui.html        |
| Frontend        | http://localhost:5173                            |
| Prometheus      | http://localhost:9090                            |
| Grafana         | http://localhost:3000 (admin / admin)            |
| Actuator health | http://localhost:8080/api/actuator/health        |

### Optional: Kafka UI (debug profile)

```bash
docker compose --profile debug up -d
```

Kafdrop: http://localhost:9000

---

## Quick start — local development (host backend + frontend)

Use this when iterating on Java or React code. Infrastructure (Postgres, Kafka) is still easiest via Docker Compose.

### 1. Start infrastructure

```bash
docker compose up -d postgres zookeeper kafka
```

Optional observability stack:

```bash
docker compose up -d postgres zookeeper kafka prometheus grafana
docker compose --profile debug up -d   # adds Kafdrop on :9000
```

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

On **PowerShell**, quote the `-D` flag as shown — otherwise Maven treats `.run.profiles=dev` as a lifecycle phase and fails.

Backend runs at **http://localhost:8081** (local dev default; Docker Compose uses **8080**).

Verify:

```bash
curl http://localhost:8081/api/actuator/health
```

### 3. Frontend

Start the backend first, then in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** in your browser.

| Page      | URL                                |
|-----------|------------------------------------|
| Dashboard | http://localhost:5173/           |
| Trades    | http://localhost:5173/trades     |
| Add trade | http://localhost:5173/trades/new |
| Login     | http://localhost:5173/login      |

The Vite dev server proxies `/api/*` → **http://localhost:8081/api/** (see `frontend/vite.config.js`). Use `/api`-prefixed paths in frontend code — do not hard-code port 8081.

On the Dashboard, **SSE: connected** means the live feed is linked to `GET /api/v1/trades/stream`. Trade cards appear when events are published to Kafka.

Other frontend commands:

```bash
npm run build    # production bundle
npm test         # Vitest unit tests
npm run lint     # ESLint
```

**Static HTML dashboard (no Node):** open `static-dashboard/dashboard.html` in a browser for a standalone Day 7 demo.

### Useful URLs (local dev)

| What            | URL                                              |
|-----------------|--------------------------------------------------|
| Swagger UI      | http://localhost:8081/api/swagger-ui.html        |
| Frontend        | http://localhost:5173                            |
| Actuator health | http://localhost:8081/api/actuator/health        |
| SSE stream      | http://localhost:8081/api/v1/trades/stream       |
| H2 console      | http://localhost:8081/api/h2 (dev profile only)  |

---

## Default credentials (dev profile)

Liquibase seeds demo users on first boot. Obtain a JWT via `POST /api/auth/login`:

| Role          | Email           | Password     |
|---------------|-----------------|--------------|
| ADMIN         | `admin@db.com`  | `admin123`   |
| TRADER        | `trader@db.com` | `trader123`  |
| VIEWER        | `viewer@db.com` | `viewer123`  |
| RECON_ANALYST | `recon@db.com`  | `recon123`   |

Example (Docker backend on 8080):

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@db.com\",\"password\":\"admin123\"}"
```

For local dev, replace `8080` with `8081`.

JWT is valid for 60 minutes. Refresh tokens live in HttpOnly cookies for 7 days (once implemented).

---

## Deploy to the demo laptop (Day 10)

The deploy story is **GitHub Actions builds + pushes Docker images to GHCR; the demo laptop pulls them and runs the full stack via `docker compose up`.** No cloud hosting — the demo laptop *is* the deploy target.

```bash
# One-time on the demo laptop (GitHub PAT with read:packages scope):
echo "<your-PAT>" | docker login ghcr.io -u <gh-username> --password-stdin

# Each deploy:
docker compose pull
docker compose up -d
```

Full walkthrough: [`student-guides/day10/README.md`](./student-guides/day10/README.md).

---

## How to read the TODOs in this codebase

Every place you must write code has a comment block like this:

```java
// ============================================================================
// TICKET-ADV019 — Build EquityTrade with the Builder pattern
//
// WHAT:    A concrete EquityTrade record/class that extends Trade …
// HOW:     Use a static inner Builder with fluent setters …
// WHY:     Builder pattern keeps the call-site readable …
// OBSERVE: A trade missing required fields throws IllegalStateException …
// HINT:    See ../model/FXTrade.java for the same pattern …
// ============================================================================
```

Below each block the method body is replaced with `// TODO(TICKET-ADVxxx)` and either an `UnsupportedOperationException` or a minimal placeholder. Your job is to remove the TODO and implement the body.

The full ticket text, acceptance criteria, and hints live in the matching day's README under [`./student-guides/`](./student-guides/README.md).

---

## Daily flow

| Day | Theme | New Tickets | Headline new-2026 topic |
|----:|-------|-------------|--------------------------|
| 0   | Introduction & onboarding | — | — |
| 1   | PostgreSQL + Liquibase Deep Dive | ADV001–ADV017 | ★ Liquibase, ★ AI for ADR |
| 2   | Java OOP + sealed classes + SOLID | ADV018–ADV032 | sealed-class trade hierarchy |
| 3   | Functional Java + JUnit 5 + Testcontainers | ADV033–ADV047 | parallel recon with CompletableFuture |
| 4   | Spring Boot enterprise setup | ADV048–ADV062 | multi-module Maven, Hibernate Envers, MapStruct |
| 5   | REST + JWT + RBAC + Testcontainers tests | ADV063–ADV080 | API versioning |
| 6   | Caching + Prometheus + Grafana | ADV081–ADV097 | ★ Observability deep dive |
| 7   | HTML5 + CSS Grid + SSE feed + ARIA | ADV098–ADV110 | ★ live SSE trade feed |
| 8   | JS ES6+ + React patterns (HOC, hooks, RHF) | ADV111–ADV125 (+ ADV127 stretch) | React performance profiling |
| 9   | React Context + Kafka multi-topic + DLQ | ADV128–ADV145 | ★ Kafka deep dive, event sourcing |
| 10  | Docker (7-svc) + GH Actions + load test + demo | ADV146–ADV165 | ★ Liquibase-in-CI, ★ AI in DevOps |

---

## Branching

Use **GitFlow**:

```
main      ← only release tags (v1.0.0 at end of Day 10)
develop   ← integration branch — your team merges here
feature/* ← one branch per ticket (e.g. feature/ADV019-equity-builder)
```

Open a Pull Request from each `feature/*` branch into `develop`. Two approvals required before merge (Advanced track convention).

---

## Environment variables

Docker Compose reads optional overrides from your shell or a `.env` file in the project root:

| Variable         | Default                               | Description        |
|------------------|---------------------------------------|--------------------|
| `JWT_SECRET`     | `dev-secret-change-me-32-bytes-min!!` | JWT signing key    |
| `BACKEND_IMAGE`  | `reconx-backend:latest`               | Backend image tag  |
| `FRONTEND_IMAGE` | `reconx-frontend:latest`              | Frontend image tag |

Database credentials (Postgres) in `docker-compose.yml`:

- Database: `reconx`
- User: `reconx_user`
- Password: `reconx_pass`

---

## View logs

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f          # all services
```

## Stop the project

```bash
docker compose down             # keeps data volumes
docker compose down -v          # fresh database on next start
```

---

## Troubleshooting

**Port already in use** — Stop the process using the port, or change the host port mapping in `docker-compose.yml`. Local dev backend defaults to **8081**; Docker Compose backend uses **8080**.

**Maven fails with `Unknown lifecycle phase ".run.profiles=dev"` (Windows/PowerShell)** — Quote the property:

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**SSE shows disconnected on the Dashboard** — Confirm the backend is running on **8081**, restart the frontend dev server (`npm run dev`), then check:

```bash
curl http://localhost:8081/api/v1/trades/stream
```

You should see `event:connected` followed by `data:ok`.

**Backend not healthy (Docker)** — Wait up to ~60s on first start (migrations + seed data):

```bash
docker compose logs backend
```

**Rebuild after code changes**:

```bash
docker compose up -d --build
```

**Reset everything**:

```bash
docker compose down -v
docker compose up -d --build
```

---

## Final demo (Day 10)

A 20-minute end-to-end walkthrough:

| Minutes | Content |
|--------:|---------|
| 3       | Problem statement + C4 architecture diagram |
| 8       | Live demo: JWT login → post trade → Kafka event → auto-recon → resolve break → Grafana metric ticks |
| 5       | Code walkthrough (one feature each team member is proud of) |
| 4       | Q&A |

---

## Good luck — and ask your instructors anything 🏦

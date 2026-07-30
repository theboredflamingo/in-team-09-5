# ReconX

Enterprise trade reconciliation platform (Spring Boot backend, React frontend, PostgreSQL, Kafka, Prometheus, Grafana).

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose)
- At least **4 GB** of free RAM for the full stack

Verify Docker is running:

```bash
docker --version
docker compose version
```

## Quick start (recommended)

From the project root:

```bash
docker compose up -d --build
```

The first run builds the backend and frontend images and may take a few minutes. Subsequent starts are faster.

### Check that services are up

```bash
docker compose ps
```

All core services should show **healthy** status:

| Service    | Container           | URL                      |
|------------|---------------------|--------------------------|
| Frontend   | `reconx-frontend`   | http://localhost:5173    |
| Backend    | `reconx-backend`    | http://localhost:8080    |
| Grafana    | `reconx-grafana`    | http://localhost:3000    |
| Prometheus | `reconx-prometheus` | http://localhost:9090    |
| PostgreSQL | `reconx-postgres`   | `localhost:5432`         |
| Kafka      | `reconx-kafka`      | `localhost:9092`         |

### Verify the backend

```bash
curl http://localhost:8080/api/actuator/health
```

Expected response: `{"status":"UP"}`

### API login (seeded users)

On first boot, Liquibase seeds demo users. Use these to obtain a JWT via `POST /api/auth/login`:

| Email            | Password   | Role           |
|------------------|------------|----------------|
| `admin@db.com`   | `admin123` | ADMIN          |
| `trader@db.com`  | `trader123`| TRADER         |
| `viewer@db.com`  | `viewer123`| VIEWER         |
| `recon@db.com`   | `recon123` | RECON_ANALYST  |

Example:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@db.com\",\"password\":\"admin123\"}"
```

### Grafana

- URL: http://localhost:3000
- Username: `admin`
- Password: `admin`

## Optional: Kafka UI (debug profile)

Start Kafdrop for browsing Kafka topics:

```bash
docker compose --profile debug up -d
```

Kafdrop: http://localhost:9000

## View logs

Follow logs for a single service:

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

All services:

```bash
docker compose logs -f
```

## Stop the project

Stop containers (keeps data volumes):

```bash
docker compose down
```

Stop and remove volumes (fresh database on next start):

```bash
docker compose down -v
```

## Local development (without Docker)

Use this only if you want to run backend or frontend on the host machine. Infrastructure (Postgres, Kafka) is still easiest via Docker Compose.

### 1. Start infrastructure only

```bash
docker compose up -d postgres zookeeper kafka
```

### 2. Backend

Requirements: **Java 21+**, Maven (or use `./mvnw`).

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend runs at http://localhost:8080

### 3. Frontend

Requirements: **Node.js 22+**

```bash
cd frontend
npm install
npm run dev
```

Vite dev server runs at http://localhost:5173 (see `frontend/vite.config.js` for the API proxy).

## Environment variables

Docker Compose reads optional overrides from your shell or a `.env` file in the project root:

| Variable        | Default                              | Description              |
|-----------------|--------------------------------------|--------------------------|
| `JWT_SECRET`    | `dev-secret-change-me-32-bytes-min!!`| JWT signing key          |
| `BACKEND_IMAGE` | `reconx-backend:latest`              | Backend image tag        |
| `FRONTEND_IMAGE`| `reconx-frontend:latest`             | Frontend image tag       |

Database credentials (Postgres) are set in `docker-compose.yml` for local use:

- Database: `reconx`
- User: `reconx_user`
- Password: `reconx_pass`

## Troubleshooting

**Port already in use** — Stop the process using the port, or change the host port mapping in `docker-compose.yml`.

**Backend not healthy** — Wait up to ~60s on first start (migrations + seed data). Check logs:

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

## Project layout

```
├── backend/          Spring Boot API
├── frontend/         React + Vite SPA
├── db/               SQL scripts and diagrams
├── monitoring/       Prometheus & Grafana config
├── static-dashboard/ Static HTML dashboard
└── docker-compose.yml
```

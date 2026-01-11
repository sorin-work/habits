# habits

A habit tracker: daily, weekday or "N times a week" habits, optional daily targets,
one check-in per day with a note, and streaks that follow the schedule.

## Features

- A today view with the habits due today and one-tap check-ins or counters.
- Daily, weekday and "N times a week" schedules, with optional daily targets.
- Schedule-aware streaks, so days off don't break them.
- Notes on check-ins.

## Running with Docker

```bash
cp .env.example .env    # optional, the defaults work
docker compose up --build
```

Then open http://localhost:5183 and sign in with the demo account:

- **Email:** `demo@example.com`
- **Password:** `demo-password`

The API is on http://localhost:4403 (`/health` checks the database).

## Running without Docker

You need JDK 21, Node 22 and a PostgreSQL database. The easiest way to get the database
is still Docker:

```bash
docker compose up -d db
```

API (http://localhost:8080):

```bash
cd api
SEED_DEMO=true ./gradlew run
```

It connects to `jdbc:postgresql://localhost:5443/habits` as `habits`/`habits` by
default. Override with `DATABASE_URL`, `DATABASE_USER` and `DATABASE_PASSWORD`.

Web (http://localhost:5173):

```bash
cd web
npm install
npm run dev
```

Vite proxies `/api` to `http://localhost:8080`; set `API_URL` to point it elsewhere.

## Tests

```bash
cd api && ./gradlew test
cd web && npm test
```

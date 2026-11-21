# habits

A habit tracker for people who want help building habits and keeping them. Each habit
has a schedule (every day, specific weekdays or a number of times per week), an optional
daily target such as 8 glasses or 30 minutes, and a check-in per day with an optional
note. Streaks follow the schedule, so a Monday/Wednesday/Friday habit doesn't lose its
streak over the weekend, and every day is counted in the user's own time zone.

## Features

- **Today view**: the habits due on a day, a progress bar, one-tap check-ins for simple
  habits and a counter for measured ones (`5 / 8 glasses`), notes per check-in. Habits
  that aren't due that day are listed separately. Step back to earlier days to fill in
  a check-in you forgot.
- **Schedules**: every day, specific weekdays (e.g. Mon/Wed/Fri) or N times per week on
  any days. A weekly goal stays "due" until it has been met for the week.
- **Targets**: an optional daily amount with a unit. Partial progress is saved and shown,
  but only reaching the target completes the day.
- **Streaks**: current and best, schedule-aware. Daily and weekday habits count
  scheduled days in a row; weekly goals count weeks in a row. Today (or the current
  week) never breaks a streak while it is still in progress.
- **Heatmap**: up to a year of check-ins per habit, one square per day, shaded by
  progress towards the target, with off-days marked.
- **Stats**: completion rate per week or month for each habit; an overview across all
  habits with a per-habit table (last 30 days, current and best streak) and which
  weekdays go best.
- **Organising**: reorder habits, archive the ones you've stopped (history is kept and
  they can be restored), or delete them for good.
- **Accounts**: sign up / sign in with email and password, profile with time zone and
  week start (Monday or Sunday), password change and account deletion.

## Stack

| Part     | Tech                                                                              |
| -------- | --------------------------------------------------------------------------------- |
| API      | Kotlin 2.2, Ktor 3 (Netty), Exposed, kotlinx.serialization, JWT (java-jwt), bcrypt |
| Database | PostgreSQL 17, Flyway migrations, HikariCP                                        |
| Web      | React 19, TypeScript, Vite, React Router, TanStack Query                          |
| Tests    | JUnit 5 + kotlin.test, Ktor `testApplication` on H2; Vitest + Testing Library     |
| Tooling  | Gradle (Kotlin DSL, wrapper), ktlint, ESLint, Docker Compose, GitHub Actions      |

## Architecture

```
browser ──> web (nginx :80) ──/api──> api (Ktor :8080) ──> db (Postgres :5432)
             static React app          JWT auth, Flyway on start
```

The browser only talks to the web container: nginx serves the built React app and
forwards `/api` to the API. In development the Vite dev server does the same proxying.

The API is split by feature, each with plain service and repository classes wired
together by hand in `Application.kt`:

| Package    | What lives there                                                       |
| ---------- | ---------------------------------------------------------------------- |
| `users`    | Registration, login, profile, password change, account deletion        |
| `habits`   | Habit CRUD, schedules, validation, archiving and ordering, check-ins   |
| `checkins` | Check-in model and repository                                          |
| `stats`    | Streaks, completion rates, heatmaps and the overview                   |
| `today`    | The today view                                                         |
| `common`   | Error responses, validation, query parameter parsing, date helpers     |
| `seed`     | The demo account                                                       |

The rules that matter most are pure functions with no database access, so they are
easy to test on their own:

| Object               | What it does                                                         |
| -------------------- | -------------------------------------------------------------------- |
| `Schedule`           | Daily / weekdays / times per week, and which days are scheduled      |
| `StreakCalculator`   | Current and best streak in days or weeks                             |
| `CompletionStats`    | Expected vs. completed check-ins for any range, week and month buckets |
| `Heatmap`            | Per-day progress levels                                              |

### How days and streaks work

- A check-in belongs to a calendar date, not an instant. "Today" is computed from the
  user's time zone (set at sign-up from the browser and changeable in Settings), so a
  check-in at 23:30 in Auckland counts for that day in Auckland.
- Check-ins can't be made for future days or before the habit's start date.
- A day is complete when its value reaches the target (or simply when there is a
  check-in for habits without a target).
- Daily and weekday habits: the streak counts scheduled days in a row that were
  completed. Days the habit isn't scheduled on are skipped; extra check-ins on those
  days are kept but don't extend the streak.
- Weekly goals: the streak counts weeks in a row where the goal was met. Weeks start
  on the user's chosen day. In the week a habit starts, the goal is capped at the
  number of days left, so a 3-times-a-week habit started on Saturday needs 2.
- Completion rates leave out days before the start date and after today, and only
  count today once it is done. Weekly goals are pro-rated by the days in the range.

Authentication is stateless: `POST /api/auth/login` returns a signed JWT that the web
app keeps in `localStorage` and sends as `Authorization: Bearer …`. When the API
rejects it (e.g. it expired), the app signs out and shows the login page. Every query
is scoped to the signed-in user. Login and sign-up are rate limited per client IP.

## Running with Docker

```bash
cp .env.example .env    # optional, the defaults work
docker compose up --build
```

Then open http://localhost:5183 and sign in with the demo account:

- **Email:** `demo@example.com`
- **Password:** `demo-password`

On start the API runs the Flyway migrations and, because `SEED_DEMO=true`, creates the
demo user (once) with about half a year of check-ins ending today: water and reading
targets, a Mon/Wed/Fri run, meditation, a 3-times-a-week strength habit, a journal and
an archived habit, with a two-week slump in the middle and a few notes. Set
`SEED_DEMO=false` for an empty database.

| Service | Host port | URL                                                 | Notes                            |
| ------- | --------- | --------------------------------------------------- | -------------------------------- |
| web     | 5183      | http://localhost:5183                               | nginx; proxies `/api` to the API |
| api     | 4403      | http://localhost:4403/health                        | Ktor                             |
| db      | 5443      | `postgresql://habits:habits@localhost:5443/habits`  | Postgres 17                      |

Ports can be changed with `WEB_PORT`, `API_PORT` and `DB_PORT` (in `.env` or inline,
e.g. `WEB_PORT=8081 docker compose up`). Data lives in the `db-data` volume;
`docker compose down -v` removes it.

Quick check from the command line:

```bash
curl http://localhost:4403/health
curl -X POST http://localhost:4403/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"demo-password"}'
```

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

### Configuration

| Variable               | Default                                    | Used by        |
| ---------------------- | ------------------------------------------ | -------------- |
| `DATABASE_URL`         | `jdbc:postgresql://localhost:5443/habits`  | api            |
| `DATABASE_USER`        | `habits`                                   | api            |
| `DATABASE_PASSWORD`    | `habits`                                   | api            |
| `DATABASE_POOL_SIZE`   | `10`                                       | api            |
| `JWT_SECRET`           | a development value (min. 32 characters)   | api            |
| `JWT_TTL`              | `7d` (also accepts `12h`, `30m`, `90s`)    | api            |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` (comma separated)  | api            |
| `LOGIN_RATE_LIMIT`     | `10` attempts per minute per IP            | api            |
| `SEED_DEMO`            | `false` (`true` in docker compose)         | api            |
| `PORT`                 | `8080`                                     | api            |
| `API_URL`              | `http://localhost:8080`                    | web dev server |

## Tests and lint

```bash
cd api && ./gradlew test          # unit tests + Ktor testApplication against H2
cd api && ./gradlew ktlintCheck   # ktlintFormat fixes most findings
cd web && npm test                # Vitest + Testing Library
cd web && npm run lint && npm run typecheck
```

Neither suite needs a running database: the API tests run the real Flyway migrations
against an in-memory H2 database in PostgreSQL mode, with a fixed clock. CI
(`.github/workflows/ci.yml`) runs both suites and the linters, builds the web app and
builds the Docker images.

## API overview

All endpoints except `/api/auth/*` and `/health` need a bearer token. Dates are
`YYYY-MM-DD` in the user's time zone; instants are ISO 8601 in UTC.

| Method & path                                     | Purpose                                            |
| ------------------------------------------------- | -------------------------------------------------- |
| `POST /api/auth/register`, `POST /api/auth/login` | Create an account / sign in, returns a token       |
| `GET/PUT/DELETE /api/me`, `PUT /api/me/password`  | Profile (name, time zone, week start), password, delete account |
| `GET /api/habits?archived=true\|false`            | Habits in display order                            |
| `POST /api/habits`, `PUT/DELETE /api/habits/{id}` | Create, update, delete                             |
| `GET /api/habits/{id}`                            | Habit with streaks, last 30 days and totals        |
| `POST /api/habits/{id}/archive`, `/restore`       | Archive or restore                                 |
| `PUT /api/habits/order`                           | New order: `{ "ids": [3, 1, 2] }`                  |
| `GET /api/habits/{id}/checkins?from=&to=`         | Check-ins with notes, newest first                 |
| `PUT/DELETE /api/habits/{id}/checkins/{date}`     | Check in (`{ "value": 5, "note": "…" }`) or undo   |
| `GET /api/habits/{id}/heatmap?from=&to=`          | One entry per day with a 0–4 level                 |
| `GET /api/habits/{id}/stats?period=week\|month&count=12` | Completion per week or month                |
| `GET /api/stats/overview?period=week\|month`      | All habits combined, per habit, per weekday        |
| `GET /api/today?date=`                            | Habits for a day (default: today) with check-ins   |

Errors share one shape:
`{ "status": 400, "error": "validation_failed", "message": "…", "fields": { "name": "…" } }`.

## Project layout

```
api/
  src/main/kotlin/app/habits/
    Application.kt   wiring, plugins and routes
    config/          environment configuration
    db/              tables, connection pool, migrations runner
    common/          errors, validation, parameters, dates
    users/ habits/ checkins/ stats/ today/ health/ seed/
  src/main/resources/db/migration/   Flyway migrations
  src/test/kotlin/                   unit and route tests
web/
  src/
    api/           typed API client and endpoints
    auth/          auth context and route guards
    components/    form controls, check-in counter, heatmap, chart
    lib/           date, schedule and heatmap helpers
    pages/         one component per route
docker-compose.yml
```

## Assumptions and limits

- One check-in per habit per day. Values are whole numbers; there's no unit conversion.
- Changing a habit's schedule or target re-evaluates its whole history with the new
  rules; there is no schedule history.
- Moving the start date later hides earlier check-ins from streaks and stats but keeps
  them.
- There are no reminders or notifications, and no sharing between accounts.

Ideas for lifting some of these, and for what could come next, are in
[ROADMAP.md](ROADMAP.md).

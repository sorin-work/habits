# habits

A habit tracker: daily, weekday or "N times a week" habits, optional daily targets,
one check-in per day with a note, and streaks that follow the schedule.

## Features

- A today view with the habits due today and one-tap check-ins or counters.
- Daily, weekday and "N times a week" schedules, with optional daily targets.
- Schedule-aware streaks, so days off don't break them.
- Habit pages with a heatmap of the last year and weekly or monthly completion.
- Notes on check-ins.

## How days and streaks work

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

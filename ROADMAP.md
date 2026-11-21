# Roadmap

Things I might add to habits. The app covers the core loop (schedules, daily check-ins with
targets, schedule-aware streaks, the heatmap and stats), so nothing here is promised or
scheduled. These are ideas, written down with enough detail that I (or anyone else) can pick one
up later without starting from zero. Items may change, move between groups or be dropped.

What the app does today is in the [README](README.md), including how days and streaks are
counted and the "Assumptions and limits" that several items below lift.

- **Next**: small or well understood. I would start with these.
- **Later**: worth doing, but bigger or waiting on a decision.
- **Maybe**: not sure yet that they are worth the extra complexity.

Sizes are rough: **S** is an evening or two, **M** a few evenings up to a couple of weekends,
**L** several weeks of spare time.

Each item is meant to fit in one pull request and to be covered by the tests. The API has two
levels: unit tests for the pure rule objects (`Schedule`, `StreakCalculator`, `CompletionStats`,
`Heatmap`), and route tests that run the whole module in Ktor's `testApplication` against H2 in
PostgreSQL mode with the real Flyway migrations and a fixed clock (`TestSupport.kt`). So a rule
worth testing on its own (what counts as done, what breaks a streak) belongs in a pure object
next to those, not in a service or a repository. The web app has Vitest specs with Testing
Library next to the components and pages, and logic there belongs in `web/src/lib`. Most items
add columns, and each brings its own Flyway migration (the next free number is `V8`). Kotlin file
names below are relative to `api/src/main/kotlin/app/habits/`; others start with `api/` or `web/`.

When an item ships, its row in the overview says `Shipped`, its section moves under a
`## Shipped` heading at the end with one paragraph on what was built and what was left out, and
the same pull request updates the README (the feature list, the API overview, "How days and
streaks work" and "Assumptions and limits" where they change). There is no changelog yet: the
first item to ship adds a `CHANGELOG.md` in the Keep a Changelog format with an `## Unreleased`
section, and every later item adds its entry there.

"Depends on" lists real prerequisites only. Items that merely touch the same file are not listed
there; the bigger overlaps are mentioned in the items.

## Overview

| #   | Item                                   | Group | Area        | Size | Depends on |
| --- | -------------------------------------- | ----- | ----------- | ---- | ---------- |
| 1   | A login limit that works behind nginx  | Next  | security    | S    | -          |
| 2   | Typing an amount instead of tapping    | Next  | check-ins   | S    | -          |
| 3   | Schedule and target history            | Next  | streaks     | M    | -          |
| 4   | Skipped days and breaks                | Next  | streaks     | M    | -          |
| 5   | Sessions that end                      | Next  | accounts    | S    | -          |
| 6   | Export and import                      | Later | data        | M    | -          |
| 7   | Reminders by email                     | Later | reminders   | M    | -          |
| 8   | Password reset                         | Later | accounts    | S    | 7          |
| 9   | A day that ends after midnight         | Later | days        | S    | -          |
| 10  | Habits to break                        | Later | habits      | M    | -          |
| 11  | Every few days and monthly schedules   | Later | schedules   | M    | -          |
| 12  | Weekly and monthly totals as goals     | Later | schedules   | M    | -          |
| 13  | Decimal amounts                        | Later | check-ins   | M    | -          |
| 14  | Times of day in the today view         | Later | today       | S    | -          |
| 15  | Areas for habits                       | Later | habits      | S    | -          |
| 16  | Filling in the past from a calendar    | Later | check-ins   | S    | -          |
| 17  | Notes: a timeline and search           | Later | check-ins   | S    | -          |
| 18  | Installable app with offline check-ins | Later | web         | M    | -          |
| 19  | Push reminders                         | Later | reminders   | M    | 18         |
| 20  | Hardening the Docker setup             | Later | security    | S    | -          |
| 21  | Backups and restore                    | Later | ops         | S    | -          |
| 22  | Tests against PostgreSQL               | Later | tests       | M    | -          |
| 23  | Accessibility pass                     | Later | ui          | M    | -          |
| 24  | Tests for the remaining pages          | Later | tests       | S    | -          |
| 25  | Request ids and metrics                | Later | ops         | S    | -          |
| 26  | Import from other habit apps           | Maybe | data        | M    | 6          |
| 27  | An accountability partner              | Maybe | sharing     | L    | -          |
| 28  | Sharing a habit's progress by link     | Maybe | sharing     | S    | -          |
| 29  | Translations                           | Maybe | ui          | M    | -          |
| 30  | Faster stats with years of history     | Maybe | performance | M    | -          |
| 31  | A year in review                       | Maybe | stats       | M    | -          |
| 32  | Streak freezes                         | Maybe | streaks     | S    | 4          |
| 33  | Starter habits                         | Maybe | habits      | S    | -          |

## Next

### 1. A login limit that works behind nginx

Login and sign-up are limited per client IP (`RateLimit` in `Application.kt`, keyed by
`call.request.origin.remoteHost`, `LOGIN_RATE_LIMIT` attempts per minute). In the Docker setup
every request reaches the API from the nginx container, so `remoteHost` is the same address for
everyone: all users share one bucket of ten attempts a minute. One person mistyping their password
a few times, or anyone running a script, locks everyone else out of signing in. The API port is
also published directly (`4403`), where the limit sees the real address, so the bypass and the
lock-out live side by side.

- Install Ktor's `XForwardedHeaders` plugin and trust it only for the nginx container (or a
  configured `TRUSTED_PROXIES` list), so `origin.remoteHost` is the real client. nginx already
  sets `X-Forwarded-For` (`web/nginx.conf`).
- Key login attempts by email as well as by address: a second limiter on the normalised email
  (`normalizeEmail`) stops slow guessing against one account from many addresses.
- Tricky: trusting `X-Forwarded-For` from anyone would let a client pick its own key. The trust
  list is the whole point; a request that does not come from it uses its socket address.
- Tricky: `testApplication` has no real socket. Tests set the header and a fake remote address
  through the test client.
- Tests: in `users/AuthRoutesTest.kt`: two clients behind the same proxy have separate limits, a
  forged header from an untrusted address is ignored, an email is limited across addresses.

### 2. Typing an amount instead of tapping

A measured habit is checked in with the counter in `web/src/components/CheckInControl.tsx`: a
minus and a plus button that change the value by one, and each tap saves and waits for the answer
before the buttons work again (`disabled={busy}` in `TodayPage.tsx`). For "8 glasses" that is
fine. For "30 minutes" of meditation, "20 pages" or "10000 steps" (targets go up to 100,000) it is
unusable, and the README's own examples include minutes.

- Tapping the value opens a small number field (`inputMode="numeric"`), saved on Enter or blur,
  with "Done" setting it to the target in one go.
- Steps that fit the target: a step size per habit (default 1, set in `HabitForm.tsx` next to the
  target), so a 30 minute habit goes up by 5 or 10.
- Tricky: quick taps. Today a tap is blocked until the previous save returns, which is slow on a
  phone. Keep a local value, debounce the save by half a second and send the final value, so five
  quick taps are one request. The API already takes the full value (`PUT .../checkins/{date}`), so
  this is a web change.
- Data: `step` on `habits` (`V8`), validated in `HabitValidation.kt` as at least 1 and at most the
  target.
- Tests: `CheckInControl.test.tsx` for typing a value, the step size and debounced taps;
  `HabitValidationTest.kt` for the step.

### 3. Schedule and target history

"Assumptions and limits" in the README says it: changing a habit's schedule or target re-evaluates
its whole history with the new rules. Someone who ran Monday, Wednesday and Friday for half a year
and moves to every day sees their past turned into a sea of missed days, a best streak that
shrinks and completion rates that collapse. Raising the water target from 6 to 8 glasses turns
months of finished days into partial ones. People change their habits as they get better at them,
so this happens to anyone who uses the app for long.

- Data: `habit_rules` (`habit_id`, `effective_from`, `schedule_type`, `schedule_days`,
  `times_per_week`, `target`). The columns on `habits` stay as the current rule for simple
  reads. A migration writes one row per habit from its `start_date`.
- Saving a habit with a different schedule or target adds a rule from today (or from a date the
  user picks) instead of changing the past.
- The pure objects take a timeline instead of one schedule: a `RuleTimeline` with
  `ruleOn(date)`. `StreakCalculator`, `CompletionStats` and `Heatmap` ask it per day.
  `Schedule.isScheduledOn` stays as it is and is called on the rule in force.
- Tricky: a weekly goal changing in the middle of a week. The week in which the rule changes
  should use the new rule from its first day, the same way the start week is capped today, and
  the README must say so.
- Tricky: a streak across a change from weekdays to times per week, which switches the unit from
  days to weeks. The simplest honest rule: the streak in the new unit starts at the change, the
  best streak keeps the old record.
- Undecided: whether a user can also "apply to the past" on purpose (for fixing a mistake right
  after creating a habit). I lean towards yes, but only when the habit is a few days old.
- Tests: unit tests for the timeline in `StreakCalculatorTest.kt` and `CompletionStatsTest.kt`
  (change in the middle of a week, a target raised, a weekday schedule widened); route tests in
  `habits/HabitRoutesTest.kt` that the stats before the change stay the same.

### 4. Skipped days and breaks

A scheduled day is either done or missed. Being ill for a week, a holiday without a gym, or a
rest day after a long run all count as missed and end the streak. The usual answer in habit
trackers is a skip: the day is neither done nor missed. Without it people either lose streaks they
worked for or fake check-ins, and both make the numbers worth less.

- Skip a day: on the today view, "Skip" next to a habit marks that day as skipped, with an
  optional note. Stored in `check_ins` with a `kind` of `SKIP` and no value (the `value > 0`
  check becomes "value > 0 unless skipped").
- A break: a date range per habit, or for all habits ("on holiday 10 to 20 August"), in a
  `breaks` table. Days in a break are treated like skipped days.
- Streaks and stats: skipped and break days are left out, like unscheduled days today. For weekly
  goals, a skipped day lowers the week's goal by one, as the start week is capped today.
- Heatmap: skipped days get their own look, like off-days.
- Tricky: abuse is the user's own business, but unlimited skips make a streak meaningless. Show
  skips in the stats ("4 skipped") rather than limiting them.
- Tricky: skipping a future day (a known holiday) is useful; checking in for a future day is not
  allowed today (`checkDate` in `HabitService.kt`). Skips and breaks may be in the future.
- Tests: unit tests in `StreakCalculatorTest.kt` and `CompletionStatsTest.kt` for skipped days in
  daily, weekday and weekly habits; route tests for skipping and for a break across all habits.

### 5. Sessions that end

A login is a JWT kept in `localStorage` (`web/src/api/client.ts`) and checked only by its
signature (`configureSecurity` in `auth/Security.kt`). Signing out forgets it in that browser.
Changing the password (`UserService.changePassword`) leaves every other session working until it
expires (`JWT_TTL`, seven days), so a lost phone stays signed in for a week.

- A `token_version` on `users`, put into the token as a claim. The `validate` block in
  `Security.kt` compares it with the user's row; that is one primary key lookup per request.
- A password change and a new "Sign out everywhere" button in Settings raise the version. The
  password change answers with a fresh token so the current tab stays signed in.
- The same check turns away tokens of deleted accounts, which today keep passing `validate` and
  fail later with "not found" errors.
- Undecided: moving the token to an `HttpOnly` cookie so no script can read it. It needs CSRF
  protection and changes the README's curl example; worth doing, but as its own change.
- Tests: `users/AuthRoutesTest.kt`: an old token is refused after a password change and after
  "sign out everywhere"; the token from the change works.

## Later

### 6. Export and import

There is no way to get the data out. Someone who wants to move to another server, keep a copy, or
look at their check-ins in a spreadsheet can only delete their account.

- `GET /api/me/export` as JSON (habits with all fields and rules, check-ins with notes, skips and
  breaks once item 4 exists, the profile) and as CSV (one row per check-in) for spreadsheets.
- `POST /api/me/import` of the JSON into the signed-in account: new habits are created, nothing
  is merged into existing ones.
- Tricky: the 50 active habits limit (`MAX_ACTIVE_HABITS`). Import archived habits as archived, and
  refuse the import with a clear message if the active ones would pass the limit.
- Tricky: the format must survive later migrations. A `version` field and a test that imports the
  output of the current export.
- Tests: a round trip of the demo account (`seed/DemoSeeder.kt`): streaks and stats equal before
  and after.

### 7. Reminders by email

"There are no reminders or notifications" is in the README's limits, and a reminder at the right
time is the feature people ask a habit tracker for first.

- A reminder time per habit (or one daily summary of what is still due), in the user's time zone.
  A job runs every few minutes, finds reminders that are due and not sent today, and skips
  habits that are already done, not scheduled that day, skipped or on a break (item 4).
- Optional SMTP settings (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `MAIL_FROM`);
  without `SMTP_HOST` reminders are not offered. Jakarta Mail or Simple Java Mail, no hosted
  service.
- Each email has a link that turns that reminder off without signing in (a signed token).
- A mailpit service in `docker-compose.yml` behind a profile for development.
- Tricky: time zones and daylight saving changes. A reminder at 08:00 means 08:00 local on every
  date; compute the next instant per day with `java.time`, never by adding 24 hours.
- Tricky: the API has no scheduler yet. A coroutine launched in `module()` with the injected
  `Clock`, and a function the tests can call directly with the fixed clock.
- Tests: unit tests for "which reminders are due at this instant" across time zones and a DST
  change; route tests with a fake mail sender.

### 8. Password reset

A forgotten password means a lost account; there is no admin who could help. With email from item
7:

- `POST /api/auth/forgot` answers the same for known and unknown emails and sends a single-use
  link valid for an hour (token stored hashed); `POST /api/auth/reset` sets the new password and
  ends other sessions (item 5).
- Rate limited like login.
- Without SMTP the link is hidden, and the README documents how whoever runs the server can set a
  password.
- Tests: route tests for unknown emails, expired and reused tokens.

### 9. A day that ends after midnight

A check-in belongs to the calendar date in the user's time zone. Someone who reads in bed at 00:30
or journals after midnight has to step back to "yesterday" in the today view every time, or their
check-in lands on the wrong day and both days look wrong.

- A "my day ends at" setting (midnight by default, up to 04:00) on the profile.
  `Clock.today(zone)` in `common/Time.kt` becomes "today for this user": the date of the current
  time minus that offset.
- Everything that uses today follows: the today view, the future date check, streaks ("today
  never breaks a streak while in progress"), completion rates and reminders (item 7).
- Tricky: there is exactly one place that should compute this, and every caller must use it. A
  test that greps is silly; a route test at 00:30 for each endpoint that defaults to today is not.
- Tests: `users/TimeZoneTest.kt` cases for the offset across a DST change; route tests at 00:30
  with the fixed clock.

### 10. Habits to break

Every habit today is something to do. "No sugar", "no smoking", "under two coffees" are just as
common, and they invert the logic: a day is a success when nothing (or little) was logged.

- A habit `kind`: `BUILD` (today) or `QUIT`. A quit habit is done on a day unless a check-in says
  otherwise; with a target it is done while the logged value stays at or under the target.
- `StreakCalculator` and `CompletionStats` get "is this day done" from one function that knows the
  kind, instead of comparing value and target directly.
- Today view: "I slipped" instead of "Done", and the streak reads "12 days without".
- Tricky: days before the first check-in. For a quit habit every day since the start counts as
  done, which is right, but a new habit created with a start date months ago would show a huge
  streak at once. Default the start date to today for quit habits.
- Tests: unit tests for both kinds in the streak and completion tests; route tests for the today
  view.

### 11. Every few days and monthly schedules

Schedules are every day, chosen weekdays, or N times a week (`Schedule.kt`). "Water the plants
every 3 days", "deep clean on the first Saturday", "pay the bills on the 1st" and "twice a month"
do not fit.

- `EveryNDays(n)` counted from the start date, `MonthlyOnDays(days of the month)` and
  `TimesPerMonth(n)`, the monthly cousin of times per week.
- `Schedule.isScheduledOn` covers the first two. `TimesPerMonth` behaves like `TimesPerWeek` with
  months as the unit, so streaks count months in a row.
- `HabitForm.tsx` and `web/src/lib/schedule.ts` (`describeSchedule`) learn the new types.
- Tricky: monthly days that do not exist (the 31st in April). Use the last day of a short
  month, and say so in the form.
- Tricky: `TimesPerMonth` in the start month and with item 4's skips, like the start week cap.
- Tests: `ScheduleTest.kt`, `StreakCalculatorTest.kt` and `CompletionStatsTest.kt` cases for each
  type; `HabitValidationTest.kt` for limits.

### 12. Weekly and monthly totals as goals

A target is per day ("8 glasses a day"). "Run 20 km a week" or "read 300 pages a month" is a
total over a period: what matters is the sum, on any days.

- A target period: per day (today), per week or per month. With a weekly or monthly target, check-ins
  add up and the period is complete when the total reaches the target; streaks count periods.
- Today view: "12 / 20 km this week" next to today's entry.
- This overlaps with `TimesPerWeek`, which counts days, not amounts. Both stay; the form explains
  the difference with an example each.
- Tests: unit tests for totals across a week start change (`weekStart` is Monday or Sunday) and a
  short start week; route tests for the today view.

### 13. Decimal amounts

Values are whole numbers (README limits; `value INT`). "2.5 km", "7.5 hours of sleep" or "1.5
litres" have to be entered in smaller units with a target like 2500 metres.

- Values and targets as `NUMERIC(10, 2)` and decimals in the API, shown with the user's locale in
  the web app.
- Tricky: comparisons with the target must stay exact; use `BigDecimal` in Kotlin, never `Double`.
  `Heatmap` levels (0 to 4) are ratios and can stay floating point.
- Tricky: the API's JSON for `value` changes from an integer to a decimal. Clients that send
  integers keep working; clients that read it need a note in the README.
- Tests: unit tests for completion and heatmap levels with decimals; route tests for rounding to
  two places.

### 14. Times of day in the today view

The today view is one list in the user's order. Many people think of habits as a morning routine,
things during the day and an evening routine.

- An optional part of the day per habit (morning, afternoon, evening, any time). The today view
  groups by it, and the current part of the day comes first.
- Reminders (item 7) can default to the start of the part of the day.
- Tests: a `TodayPage.test.tsx` case for grouping; a route test that the today response carries
  the field.

### 15. Areas for habits

With fifteen habits the list and the stats get long. Grouping them into areas such as health,
learning and home is the common answer.

- `areas` per user (name, colour) and an optional area per habit. Filters on the habits page and
  the stats overview, and the overview's per-habit table grouped by area.
- Tests: route tests for areas and the filtered overview.

### 16. Filling in the past from a calendar

Earlier days can be filled in by stepping back one day at a time on the today view. A forgotten
week means seven steps and seven check-ins per habit.

- A month calendar on the habit detail page (`HabitDetailPage.tsx`): tap a day to check in,
  change the value or add a note, using the same `PUT .../checkins/{date}`.
- The heatmap cells become buttons that open the same editor.
- Tricky: archived habits refuse changes today (`habit_archived`); keep the calendar read-only
  for them.
- Tests: page tests for editing a past day and for an archived habit.

### 17. Notes: a timeline and search

Check-ins can carry a note, and the habit detail shows them newest first
(`GET /api/habits/{id}/checkins`). A journal habit with a year of notes cannot be searched, and
notes from all habits cannot be read together.

- A notes page across habits, newest first, with a text search and a habit filter, paged.
- Search with `ILIKE` on `check_ins.note` scoped to the user's habits; an index is not needed for
  one person's notes.
- Tests: route tests for search and paging, and that another user's notes never show.

### 18. Installable app with offline check-ins

A habit tracker lives on the phone home screen, and check-ins happen in places with a poor signal
(the gym, the train).

- A web app manifest and a service worker (`vite-plugin-pwa`) so the app installs and opens from
  the cache.
- Offline check-ins: queued in IndexedDB with the date they were made for and sent when back
  online, oldest first. The API already treats a check-in as "set the value for this date"
  (`upsert`), so a replay is safe.
- Tricky: a queued check-in for a habit that was archived or deleted in the meantime. Drop it
  and say so, rather than retrying forever.
- Tricky: "today" offline is the device's date, which is right as long as the device and profile
  time zones agree. Use the profile's zone from the last online answer.
- Tests: unit tests for the queue in `web/src/lib`; the service worker is checked by hand.

### 19. Push reminders

Email reminders (item 7) work, but a phone notification is what most people expect. With item 18
in place, Web Push can send them: the server signs pushes with its own VAPID keys and needs no
account anywhere, though the messages travel through the browser vendor's push service, which the
README should say.

- `push_subscriptions` per user and device, a toggle in Settings, sending from the same reminder
  job as item 7 when it exists, or its own when it does not.
- Tricky: iOS only delivers pushes to installed web apps, which is why this depends on item 18.
- Tests: subscription routes and the payload; delivery is checked by hand.

### 20. Hardening the Docker setup

A few defaults that are fine on a laptop and not on a server:

- `JWT_SECRET` falls back to `local-development-secret-change-me-0123456789` in
  `docker-compose.yml`. A server started with the quick start accepts tokens anyone can sign.
  `AppConfig` should refuse that value unless `SEED_DEMO` is on, and say why.
- The API port is published to the host next to nginx. Publish only the web port by default and
  leave the API port to an override for development (it also matters for item 1).
- nginx (`web/nginx.conf`) sets cache headers only. Add a strict `Content-Security-Policy`,
  `Referrer-Policy` and `frame-ancestors 'none'`; the API already sends `X-Content-Type-Options`.
- Tests: `config/AppConfigTest.kt` for the secret check; the headers can be checked in the Docker
  job of `.github/workflows/ci.yml`.

### 21. Backups and restore

The data lives in the `db-data` volume and nothing copies it. `docker compose down -v` in the
wrong folder ends years of streaks.

- A `backup` service in `docker-compose.yml` (behind a profile) that runs `pg_dump` every night
  into `./backups` and keeps the last 14.
- A short "Backups and upgrades" section in the README: restore a dump into a fresh volume (tried
  for real), and how to upgrade (back up, pull, rebuild; Flyway migrates on start). A PostgreSQL
  major upgrade needs a dump and restore.
- Tests: a script that restores a dump into a throwaway database and counts rows, run by hand.

### 22. Tests against PostgreSQL

The route tests run on H2 in PostgreSQL mode. The migrations never run on PostgreSQL in CI (the
Docker job only builds images), and H2 differs exactly where items here go: `NUMERIC` and rounding
(item 13), `ILIKE` (item 17), partial and expression indexes, and the `TIMESTAMP` without zone
columns that rely on the JVM running in UTC (`main()` in `Application.kt` sets it).

- A Gradle task or property that points the tests at PostgreSQL, through Testcontainers or a
  service container in the `api` job of `.github/workflows/ci.yml`. H2 stays the default locally.
- Tests: the item is the tests; the existing suite on PostgreSQL, plus one test that the timestamp
  columns read back the same instant.

### 23. Accessibility pass

The basics are there (`aria-pressed` on the check button, a labelled counter group, a label per
heatmap cell), but some parts still fail:

- the heatmap uses `role="grid"`, which promises arrow key navigation, and its cells cannot be
  focused, so keyboard users cannot reach a single day. Either make it a real grid (one tab stop,
  arrow keys) or drop the role and offer the same data as a list;
- habits are told apart by their colour dot alone, and an API client can set any hex colour,
  including one that disappears against the background in dark mode;
- after a route change focus stays where it was and nothing is announced;
- the minus button is the character `−`, read out differently by screen readers; its label is
  right, but the visible text should be hidden from them (`aria-hidden`).

- Fix those, then add `axe-core` checks to the existing Testing Library tests for the main pages.
- Tests: the axe checks, plus keyboard tests for the heatmap.

### 24. Tests for the remaining pages

The web app has tests for the today and habits pages, login, the habit form, the counter, notes,
the chart and the heatmap. The habit detail page, the stats page, settings (profile, password,
account deletion) and the signup page have none, and settings holds the only irreversible action
in the app.

- Tests with Testing Library and a mocked API (`web/src/test/utils.tsx` already renders with the
  providers) for those four pages: loading and error states, the delete confirmation, the week
  start setting reaching the stats.
- Tests: the item is the tests.

### 25. Request ids and metrics

`CallLogging` writes a line per API request, and that is all there is. A user's error cannot be
matched to a log line, and nothing shows request rates, latencies or how often logins fail.

- `CallId` with `X-Request-Id` (taken from nginx or generated), in every log line through the MDC
  and in error responses, next to `status`, `error` and `message`.
- Micrometer with the Prometheus registry on `/metrics`, off by default and not forwarded by
  nginx.
- Tests: a route test that an error carries the request id; one that `/metrics` is off by default.

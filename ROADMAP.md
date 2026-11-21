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

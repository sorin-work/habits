package app.habits.common

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** The calendar date it currently is in [zone]. Habit days always follow the user's time zone. */
fun Clock.today(zone: ZoneId): LocalDate = LocalDate.now(withZone(zone))

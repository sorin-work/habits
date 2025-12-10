package app.habits.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun LocalDate.startOfWeek(weekStart: DayOfWeek): LocalDate = with(TemporalAdjusters.previousOrSame(weekStart))

fun LocalDate.endOfWeek(weekStart: DayOfWeek): LocalDate = startOfWeek(weekStart).plusDays(6)

fun latest(
    a: LocalDate,
    b: LocalDate,
): LocalDate = if (a.isAfter(b)) a else b

fun earliest(
    a: LocalDate,
    b: LocalDate,
): LocalDate = if (a.isBefore(b)) a else b

/** Days from [from] to [to], both included. Empty when [to] is before [from]. */
fun daysBetween(
    from: LocalDate,
    to: LocalDate,
): Sequence<LocalDate> = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }

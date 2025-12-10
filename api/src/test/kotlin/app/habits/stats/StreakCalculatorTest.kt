package app.habits.stats

import app.habits.habits.Schedule
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakCalculatorTest {
    // Wednesday
    private val today = LocalDate.of(2026, 3, 18)
    private val start = LocalDate.of(2026, 1, 1)

    private fun daysAgo(vararg n: Long) = n.map { today.minusDays(it) }.toSet()

    private fun daily(
        completed: Set<LocalDate>,
        startDate: LocalDate = start,
    ) = StreakCalculator.calculate(Schedule.Daily, startDate, completed, today, MONDAY)

    @Test
    fun `counts consecutive completed days up to today`() {
        val streaks = daily(daysAgo(0, 1, 2, 3))
        assertEquals(Streaks(4, 4, StreakUnit.DAYS), streaks)
    }

    @Test
    fun `an open today does not break the streak`() {
        assertEquals(3, daily(daysAgo(1, 2, 3)).current)
    }

    @Test
    fun `a missed yesterday resets the current streak but keeps the best`() {
        val streaks = daily(daysAgo(0, 2, 3, 4, 5, 6))
        assertEquals(1, streaks.current)
        assertEquals(5, streaks.best)
    }

    @Test
    fun `days before the start date are ignored`() {
        val streaks = daily(daysAgo(1, 2, 3, 10, 11, 12, 13), startDate = today.minusDays(3))
        assertEquals(Streaks(3, 3, StreakUnit.DAYS), streaks)
    }

    @Test
    fun `weekday habits skip days they are not scheduled on`() {
        val schedule = Schedule.Weekdays(setOf(MONDAY, WEDNESDAY, FRIDAY))
        // Wed 18, Mon 16, Fri 13, Wed 11: the weekend in between doesn't count against the streak.
        val completed = setOf(18, 16, 13, 11).map { LocalDate.of(2026, 3, it) }.toSet()
        val streaks = StreakCalculator.calculate(schedule, start, completed, today, MONDAY)
        assertEquals(4, streaks.current)
    }

    @Test
    fun `check-ins on unscheduled days do not extend a weekday streak`() {
        val schedule = Schedule.Weekdays(setOf(MONDAY, WEDNESDAY, FRIDAY))
        // Sat 14 and Sun 15 are extra; Fri 13 was missed.
        val completed = setOf(16, 15, 14, 11).map { LocalDate.of(2026, 3, it) }.toSet()
        val streaks = StreakCalculator.calculate(schedule, start, completed, today, MONDAY)
        assertEquals(1, streaks.current)
    }

    @Test
    fun `the current week counts once its goal is met`() {
        val schedule = Schedule.TimesPerWeek(2)
        val completed = setOf(2, 4, 16, 17).map { LocalDate.of(2026, 3, it) }.toSet()
        val streaks = StreakCalculator.calculate(schedule, LocalDate.of(2026, 3, 2), completed, today, MONDAY)
        // The week of the 9th was missed, so only the current week counts.
        assertEquals(1, streaks.current)
        assertEquals(1, streaks.best)
    }

    @Test
    fun `weeks follow the user's week start`() {
        val schedule = Schedule.TimesPerWeek(2)
        // Sun 15 + Mon 16 are the same week when weeks start on Sunday, but not when they start on Monday.
        val completed = setOf(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 16))
        val sunday = StreakCalculator.calculate(schedule, LocalDate.of(2026, 3, 1), completed, today, SUNDAY)
        val monday = StreakCalculator.calculate(schedule, LocalDate.of(2026, 3, 1), completed, today, MONDAY)
        assertEquals(1, sunday.current)
        assertEquals(0, monday.current)
    }
}

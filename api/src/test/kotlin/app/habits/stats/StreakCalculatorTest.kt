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
    fun `weekly goals count weeks in a row where the goal was met`() {
        val schedule = Schedule.TimesPerWeek(2)
        // Weeks starting Mon 2, Mon 9 met; week of Mon 16 (current) has one so far.
        val completed = setOf(2, 4, 10, 13, 17).map { LocalDate.of(2026, 3, it) }.toSet()
        val streaks = StreakCalculator.calculate(schedule, LocalDate.of(2026, 3, 2), completed, today, MONDAY)
        assertEquals(Streaks(2, 2, StreakUnit.WEEKS), streaks)
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
    fun `the first week only asks for the days that were left`() {
        // Started on Saturday the 7th with a goal of 3: one check-in that weekend is not enough...
        val schedule = Schedule.TimesPerWeek(3)
        val started = LocalDate.of(2026, 3, 7)
        val oneDay = setOf(LocalDate.of(2026, 3, 8))
        assertEquals(
            0,
            StreakCalculator.calculate(schedule, started, oneDay, LocalDate.of(2026, 3, 10), MONDAY).current,
        )
        // ...but both remaining days are.
        val both = setOf(LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 8))
        assertEquals(1, StreakCalculator.calculate(schedule, started, both, LocalDate.of(2026, 3, 10), MONDAY).current)
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

    @Test
    fun `required days in the first week`() {
        val monday = LocalDate.of(2026, 3, 16)
        assertEquals(3, StreakCalculator.requiredInWeek(3, monday, monday))
        assertEquals(2, StreakCalculator.requiredInWeek(3, monday, LocalDate.of(2026, 3, 21)))
        assertEquals(0, StreakCalculator.requiredInWeek(3, monday, LocalDate.of(2026, 3, 30)))
    }
}

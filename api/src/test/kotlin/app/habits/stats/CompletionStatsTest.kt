package app.habits.stats

import app.habits.habits.Schedule
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompletionStatsTest {
    private val today = LocalDate.of(2026, 3, 18)
    private val start = LocalDate.of(2026, 1, 1)

    private fun march(vararg days: Int) = days.map { LocalDate.of(2026, 3, it) }.toSet()

    @Test
    fun `daily completion over a past week`() {
        val completion =
            CompletionStats.completion(
                Schedule.Daily,
                start,
                march(9, 10, 12),
                march(9).first(),
                march(15).first(),
                today,
            )
        assertEquals(Completion(7, 3), completion)
        assertEquals(3.0 / 7, completion.rate)
    }

    @Test
    fun `today only counts once it is done`() {
        val open = CompletionStats.completion(Schedule.Daily, start, march(16, 17), march(16).first(), today, today)
        assertEquals(Completion(2, 2), open)
        val done = CompletionStats.completion(Schedule.Daily, start, march(16, 17, 18), march(16).first(), today, today)
        assertEquals(Completion(3, 3), done)
    }

    @Test
    fun `days in the future and before the start are not expected`() {
        val completion =
            CompletionStats.completion(
                Schedule.Daily,
                march(14).first(),
                march(14, 15),
                march(9).first(),
                march(22).first(),
                today,
            )
        // 14th to 17th; the 18th is still open and the rest hasn't happened.
        assertEquals(Completion(4, 2), completion)
    }

    @Test
    fun `weekday habits only expect their days`() {
        val schedule = Schedule.Weekdays(setOf(MONDAY, TUESDAY))
        val completion =
            CompletionStats.completion(
                schedule,
                start,
                march(9, 14),
                march(9).first(),
                march(15).first(),
                today,
            )
        // Mon 9 and Tue 10 expected; Sat 14 is an extra that doesn't count.
        assertEquals(Completion(2, 1), completion)
    }

    @Test
    fun `weekly goals are pro-rated by the days in the range`() {
        val schedule = Schedule.TimesPerWeek(3)
        val week =
            CompletionStats.completion(
                schedule,
                start,
                march(9, 11, 13),
                march(9).first(),
                march(15).first(),
                today,
            )
        assertEquals(Completion(3, 3), week)
        val twoWeeks =
            CompletionStats.completion(
                schedule,
                start,
                march(2, 9, 11),
                march(2).first(),
                march(15).first(),
                today,
            )
        assertEquals(Completion(6, 3), twoWeeks)
    }

    @Test
    fun `rate is capped at one and null when nothing was expected`() {
        assertEquals(1.0, Completion(2, 5).rate)
        assertNull(Completion(0, 0).rate)
    }

    @Test
    fun `week buckets end with the current week`() {
        val ranges = CompletionStats.bucketRanges(StatsPeriod.WEEK, 3, today, MONDAY)
        assertEquals(
            listOf(
                march(2).first() to march(8).first(),
                march(9).first() to march(15).first(),
                march(16).first() to march(22).first(),
            ),
            ranges,
        )
        val sundays = CompletionStats.bucketRanges(StatsPeriod.WEEK, 1, today, SUNDAY)
        assertEquals(listOf(march(15).first() to march(21).first()), sundays)
    }

    @Test
    fun `month buckets cover whole calendar months`() {
        val ranges = CompletionStats.bucketRanges(StatsPeriod.MONTH, 2, today, MONDAY)
        assertEquals(
            listOf(
                LocalDate.of(2026, 2, 1) to LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 1) to LocalDate.of(2026, 3, 31),
            ),
            ranges,
        )
    }

    @Test
    fun `the current month only expects days up to today`() {
        val buckets =
            CompletionStats.buckets(Schedule.Daily, start, march(1, 2, 3), StatsPeriod.MONTH, 1, today, MONDAY)
        assertEquals(Completion(17, 3), buckets.single().completion)
    }
}

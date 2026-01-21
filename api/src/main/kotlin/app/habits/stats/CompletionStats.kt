package app.habits.stats

import app.habits.habits.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil

enum class StatsPeriod { WEEK, MONTH }

data class Completion(
    val expected: Int,
    val completed: Int,
) {
    /** Share of expected check-ins that were done, capped at 1. Null when nothing was expected. */
    val rate: Double? get() = if (expected == 0) null else minOf(1.0, completed.toDouble() / expected)

    operator fun plus(other: Completion) = Completion(expected + other.expected, completed + other.completed)

    companion object {
        val NONE = Completion(0, 0)
    }
}

data class Bucket(
    val start: LocalDate,
    val end: LocalDate,
    val completion: Completion,
)

object CompletionStats {
    /**
     * Expected vs. completed check-ins between [from] and [to] (inclusive).
     *
     * Days before the habit's start date and days after today are left out. Today only counts once it is
     * completed, so an open day doesn't drag the rate down. For "N times per week" habits the goal is
     * pro-rated by the number of days in the range.
     */
    fun completion(
        schedule: Schedule,
        startDate: LocalDate,
        completedDays: Set<LocalDate>,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate,
    ): Completion {
        val first = latest(from, startDate)
        val last = to
        if (last.isBefore(first)) return Completion.NONE
        val lastCounted = last

        return when (schedule) {
            is Schedule.TimesPerWeek -> {
                if (lastCounted.isBefore(first)) return Completion.NONE
                val days = ChronoUnit.DAYS.between(first, lastCounted).toInt() + 1
                val expected = minOf(days, ceil(schedule.times * days / 7.0).toInt())
                val completed = completedDays.count { !it.isBefore(first) && !it.isAfter(lastCounted) }
                Completion(expected, completed)
            }
            else -> {
                var expected = 0
                var completed = 0
                for (day in daysBetween(first, lastCounted)) {
                    if (!schedule.isScheduledOn(day)) continue
                    expected++
                    if (day in completedDays) completed++
                }
                Completion(expected, completed)
            }
        }
    }

    /** The last [count] weeks or months up to and including the one that contains [today], oldest first. */
    fun buckets(
        schedule: Schedule,
        startDate: LocalDate,
        completedDays: Set<LocalDate>,
        period: StatsPeriod,
        count: Int,
        today: LocalDate,
        weekStart: DayOfWeek,
    ): List<Bucket> =
        bucketRanges(period, count, today, weekStart).map { (start, end) ->
            Bucket(start, end, completion(schedule, startDate, completedDays, start, end, today))
        }

    fun bucketRanges(
        period: StatsPeriod,
        count: Int,
        today: LocalDate,
        weekStart: DayOfWeek,
    ): List<Pair<LocalDate, LocalDate>> =
        (count - 1 downTo 0).map { back ->
            when (period) {
                StatsPeriod.WEEK -> {
                    val start = today.startOfWeek(weekStart).minusWeeks(back.toLong())
                    start to start.plusDays(6)
                }
                StatsPeriod.MONTH -> {
                    val start = today.withDayOfMonth(1).minusMonths(back.toLong())
                    start to start.with(TemporalAdjusters.lastDayOfMonth())
                }
            }
        }
}

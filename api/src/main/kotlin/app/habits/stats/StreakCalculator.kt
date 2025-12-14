package app.habits.stats

import app.habits.habits.Schedule
import java.time.DayOfWeek
import java.time.LocalDate

enum class StreakUnit { DAYS, WEEKS }

data class Streaks(
    val current: Int,
    val best: Int,
    val unit: StreakUnit,
)

/**
 * Schedule-aware streaks.
 *
 * - Daily and weekday habits count scheduled days in a row that were completed. Days the habit is not
 *   scheduled on are skipped, so a Mon/Wed/Fri habit keeps its streak over the weekend. Check-ins on
 *   unscheduled days are welcome but don't extend the streak.
 * - "N times per week" habits count weeks in a row in which the goal was met.
 * - Today (or the current week) never breaks a streak while it is still in progress; it only adds to
 *   it once completed.
 */
object StreakCalculator {
    fun calculate(
        schedule: Schedule,
        startDate: LocalDate,
        completedDays: Set<LocalDate>,
        today: LocalDate,
        weekStart: DayOfWeek,
    ): Streaks =
        when (schedule) {
            is Schedule.TimesPerWeek -> weekly(schedule.times, startDate, completedDays, today, weekStart)
            else -> daily(schedule, startDate, completedDays, today)
        }

    private fun daily(
        schedule: Schedule,
        startDate: LocalDate,
        completedDays: Set<LocalDate>,
        today: LocalDate,
    ): Streaks {
        var run = 0
        var best = 0
        for (day in daysBetween(startDate, today)) {
            if (!schedule.isScheduledOn(day)) continue
            when {
                day in completedDays -> {
                    run++
                    if (run > best) best = run
                }
                day == today -> Unit
                else -> run = 0
            }
        }
        return Streaks(run, best, StreakUnit.DAYS)
    }

    private fun weekly(
        times: Int,
        startDate: LocalDate,
        completedDays: Set<LocalDate>,
        today: LocalDate,
        weekStart: DayOfWeek,
    ): Streaks {
        if (today.isBefore(startDate)) return Streaks(0, 0, StreakUnit.WEEKS)
        val currentWeek = today.startOfWeek(weekStart)
        var week = startDate.startOfWeek(weekStart)
        var run = 0
        var best = 0
        while (!week.isAfter(currentWeek)) {
            val weekEnd = week.plusDays(6)
            val required = times
            val done = completedDays.count { !it.isBefore(latest(week, startDate)) && !it.isAfter(weekEnd) }
            when {
                done >= required -> {
                    run++
                    if (run > best) best = run
                }
                week == currentWeek -> Unit
                else -> run = 0
            }
            week = week.plusWeeks(1)
        }
        return Streaks(run, best, StreakUnit.WEEKS)
    }
}

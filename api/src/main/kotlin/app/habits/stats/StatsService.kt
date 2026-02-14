package app.habits.stats

import app.habits.checkins.CheckIn
import app.habits.checkins.CheckInRepository
import app.habits.common.DateRange
import app.habits.common.today
import app.habits.habits.Habit
import app.habits.habits.HabitRepository
import app.habits.habits.HabitService
import app.habits.habits.Schedule
import app.habits.habits.toResponse
import app.habits.users.User
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

class StatsService(
    private val habitService: HabitService,
    private val habits: HabitRepository,
    private val checkIns: CheckInRepository,
    private val clock: Clock,
) {
    suspend fun detail(
        user: User,
        id: Long,
    ): HabitDetailResponse {
        val habit = habitService.get(user, id)
        val history = checkIns.all(habit.id)
        return HabitDetailResponse(habit.toResponse(), summary(habit, history, user))
    }

    suspend fun stats(
        user: User,
        id: Long,
        period: StatsPeriod,
        count: Int,
    ): HabitStatsResponse {
        val habit = habitService.get(user, id)
        val completed = completedDays(habit, checkIns.all(habit.id))
        val buckets =
            CompletionStats.buckets(
                habit.schedule,
                habit.startDate,
                completed,
                period,
                count,
                clock.today(user.timeZone),
                DayOfWeek.MONDAY,
            )
        return HabitStatsResponse(period.name.lowercase(), buckets.map { it.toResponse() })
    }

    suspend fun heatmap(
        user: User,
        id: Long,
        range: DateRange,
    ): HeatmapResponse {
        val habit = habitService.get(user, id)
        val values = checkIns.list(habit.id, range).associate { it.day to it.value }
        val days = Heatmap.build(habit, values, range.from, range.to)
        return HeatmapResponse(
            from = range.from,
            to = range.to,
            goal = habit.dailyGoal,
            days = days.map { HeatmapDayResponse(it.date, it.value, it.level, it.scheduled, it.completed) },
        )
    }

    /** Completion across all active habits, plus each habit's streak and which weekdays go best. */
    suspend fun overview(
        user: User,
        period: StatsPeriod,
        count: Int,
    ): OverviewResponse {
        val today = clock.today(user.timeZone)
        val active = habits.list(user.id, archived = false)
        val history = checkIns.forHabits(active.map { it.id }, today)
        val ranges = CompletionStats.bucketRanges(period, count, today, DayOfWeek.MONDAY)
        val totals = MutableList(ranges.size) { Completion.NONE }
        val weekdayTotals = DayOfWeek.entries.associateWith { Completion.NONE }.toMutableMap()
        val weekdayFrom = today.minusWeeks(12)

        val rows =
            active.map { habit ->
                val completed = completedDays(habit, history[habit.id].orEmpty())
                ranges.forEachIndexed { index, (start, end) ->
                    val completion =
                        CompletionStats.completion(
                            habit.schedule,
                            habit.startDate,
                            completed,
                            start,
                            end,
                            today,
                        )
                    totals[index] = totals[index] + completion.capped()
                }
                if (habit.schedule !is Schedule.TimesPerWeek) {
                    var day = latest(weekdayFrom, habit.startDate)
                    while (day.isBefore(today)) {
                        if (habit.schedule.isScheduledOn(day)) {
                            val done = if (day in completed) 1 else 0
                            weekdayTotals[day.dayOfWeek] = weekdayTotals.getValue(day.dayOfWeek) + Completion(1, done)
                        }
                        day = day.plusDays(1)
                    }
                }
                val streaks =
                    StreakCalculator.calculate(habit.schedule, habit.startDate, completed, today, DayOfWeek.MONDAY)
                OverviewHabit(
                    id = habit.id,
                    name = habit.name,
                    color = habit.color,
                    streak = streaks.toResponse(),
                    last30Days = last30(habit, completed, today).toResponse(),
                )
            }

        return OverviewResponse(
            period = period.name.lowercase(),
            buckets = ranges.mapIndexed { index, (start, end) -> Bucket(start, end, totals[index]).toResponse() },
            habits = rows,
            weekdays =
                DayOfWeek.entries.map { day ->
                    val total = weekdayTotals.getValue(day)
                    WeekdayRate(day.value, total.expected, total.completed, total.rate)
                },
        )
    }

    fun summary(
        habit: Habit,
        history: List<CheckIn>,
        user: User,
    ): HabitSummary {
        val today = clock.today(user.timeZone)
        val completed = completedDays(habit, history)
        val streaks = StreakCalculator.calculate(habit.schedule, habit.startDate, completed, today, DayOfWeek.MONDAY)
        return HabitSummary(
            streak = streaks.toResponse(),
            last30Days = last30(habit, completed, today).toResponse(),
            totalCheckIns = history.size,
            thisWeek = weekProgress(habit, completed, today, DayOfWeek.MONDAY),
        )
    }

    companion object {
        fun completedDays(
            habit: Habit,
            history: List<CheckIn>,
        ): Set<LocalDate> = history.filter { habit.isComplete(it.value) }.map { it.day }.toSet()

        fun last30(
            habit: Habit,
            completed: Set<LocalDate>,
            today: LocalDate,
        ): Completion =
            CompletionStats.completion(
                habit.schedule,
                habit.startDate,
                completed,
                today.minusDays(29),
                today,
                today,
            )

        fun weekProgress(
            habit: Habit,
            completed: Set<LocalDate>,
            today: LocalDate,
            weekStart: DayOfWeek,
        ): WeekProgress? {
            val schedule = habit.schedule as? Schedule.TimesPerWeek ?: return null
            val start = today.startOfWeek(weekStart)
            val done = completed.count { !it.isBefore(start) && !it.isAfter(today) }
            return WeekProgress(done, StreakCalculator.requiredInWeek(schedule.times, start, habit.startDate))
        }
    }
}

/** Over-achieving one habit shouldn't hide a missed one in combined numbers. */
private fun Completion.capped() = Completion(expected, minOf(completed, expected))

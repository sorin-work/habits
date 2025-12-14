package app.habits.stats

import app.habits.checkins.CheckIn
import app.habits.checkins.CheckInRepository
import app.habits.common.today
import app.habits.habits.Habit
import app.habits.habits.HabitService
import app.habits.habits.Schedule
import app.habits.habits.toResponse
import app.habits.users.User
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

class StatsService(
    private val habitService: HabitService,
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
            totalCheckIns = history.size,
            thisWeek = weekProgress(habit, completed, today, DayOfWeek.MONDAY),
        )
    }

    companion object {
        fun completedDays(
            habit: Habit,
            history: List<CheckIn>,
        ): Set<LocalDate> = history.filter { habit.isComplete(it.value) }.map { it.day }.toSet()

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

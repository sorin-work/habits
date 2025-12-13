package app.habits.stats

import app.habits.checkins.CheckIn
import app.habits.checkins.CheckInRepository
import app.habits.common.today
import app.habits.habits.Habit
import app.habits.habits.HabitService
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
        )
    }

    companion object {
        fun completedDays(
            habit: Habit,
            history: List<CheckIn>,
        ): Set<LocalDate> = history.filter { habit.isComplete(it.value) }.map { it.day }.toSet()
    }
}

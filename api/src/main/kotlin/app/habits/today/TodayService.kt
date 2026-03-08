package app.habits.today

import app.habits.checkins.CheckInRepository
import app.habits.common.LocalDateSerializer
import app.habits.common.today
import app.habits.habits.HabitRepository
import app.habits.habits.HabitResponse
import app.habits.habits.toResponse
import app.habits.stats.StatsService
import app.habits.stats.StreakCalculator
import app.habits.stats.StreakResponse
import app.habits.stats.WeekProgress
import app.habits.stats.toResponse
import app.habits.users.User
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.LocalDate

@Serializable
data class TodayHabit(
    val habit: HabitResponse,
    val due: Boolean,
    val value: Int,
    val note: String?,
    val completed: Boolean,
    val streak: StreakResponse,
    val week: WeekProgress?,
)

@Serializable
data class TodayResponse(
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val habits: List<TodayHabit>,
    val dueCount: Int,
    val completedCount: Int,
)

class TodayService(
    private val habits: HabitRepository,
    private val checkIns: CheckInRepository,
    private val clock: Clock,
) {
    /** Active habits for today in the user's time zone, each with today's check-in and whether it is due. */
    suspend fun forDay(user: User): TodayResponse {
        val date = clock.today(user.timeZone)

        val active = habits.list(user.id, archived = false).filter { !it.startDate.isAfter(date) }

        val items =
            active.map { habit ->
                val entries = checkIns.all(habit.id)
                val entry = entries.lastOrNull { it.day == date }
                val completed = StatsService.completedDays(habit, entries)
                val doneToday = entry != null && habit.isComplete(entry.value)
                val week = StatsService.weekProgress(habit, completed, date, user.weekStart)
                val due = habit.schedule.isScheduledOn(date)
                TodayHabit(
                    habit = habit.toResponse(),
                    due = due,
                    value = entry?.value ?: 0,
                    note = entry?.note,
                    completed = doneToday,
                    streak =
                        StreakCalculator
                            .calculate(habit.schedule, habit.startDate, completed, date, user.weekStart)
                            .toResponse(),
                    week = week,
                )
            }

        return TodayResponse(
            date = date,
            habits = items,
            dueCount = items.count { it.due },
            completedCount = items.count { it.due && it.completed },
        )
    }
}

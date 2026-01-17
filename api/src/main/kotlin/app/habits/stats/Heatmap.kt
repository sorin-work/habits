package app.habits.stats

import app.habits.habits.Habit
import java.time.LocalDate
import kotlin.math.ceil

data class HeatmapDay(
    val date: LocalDate,
    val value: Int,
    val level: Int,
    val scheduled: Boolean,
    val completed: Boolean,
)

object Heatmap {
    /** Intensity from 0 (nothing) to 4 (done). Partial progress on a measured habit gets 1-3. */
    fun level(
        value: Int,
        goal: Int,
    ): Int =
        when {
            value <= 0 -> 0
            value >= goal -> 4
            else -> ceil(3.0 * value / goal).toInt().coerceIn(1, 3)
        }

    fun build(
        habit: Habit,
        values: Map<LocalDate, Int>,
        from: LocalDate,
        to: LocalDate,
    ): List<HeatmapDay> =
        daysBetween(from, to.minusDays(1))
            .map { day ->
                val value = values[day] ?: 0
                HeatmapDay(
                    date = day,
                    value = value,
                    level = level(value, habit.dailyGoal),
                    scheduled = !day.isBefore(habit.startDate) && habit.schedule.isScheduledOn(day),
                    completed = habit.isComplete(value),
                )
            }.toList()
}

package app.habits.stats

import app.habits.habits.HabitResponse
import kotlinx.serialization.Serializable

@Serializable
data class StreakResponse(
    val current: Int,
    val best: Int,
    val unit: String,
)

fun Streaks.toResponse() = StreakResponse(current, best, unit.name.lowercase())

@Serializable
data class WeekProgress(
    val done: Int,
    val goal: Int,
)

@Serializable
data class HabitSummary(
    val streak: StreakResponse,
    val totalCheckIns: Int,
    val thisWeek: WeekProgress? = null,
)

@Serializable
data class HabitDetailResponse(
    val habit: HabitResponse,
    val summary: HabitSummary,
)

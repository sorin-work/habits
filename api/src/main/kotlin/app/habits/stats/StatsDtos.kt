package app.habits.stats

import app.habits.common.LocalDateSerializer
import app.habits.habits.HabitResponse
import kotlinx.serialization.Serializable
import java.time.LocalDate

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

@Serializable
data class HeatmapDayResponse(
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val value: Int,
    val level: Int,
    val scheduled: Boolean,
    val completed: Boolean,
)

@Serializable
data class HeatmapResponse(
    @Serializable(with = LocalDateSerializer::class) val from: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val to: LocalDate,
    val goal: Int,
    val days: List<HeatmapDayResponse>,
)

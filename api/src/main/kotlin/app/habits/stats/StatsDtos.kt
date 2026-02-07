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
    val last30Days: CompletionResponse,
    val totalCheckIns: Int,
    val thisWeek: WeekProgress? = null,
)

@Serializable
data class HabitDetailResponse(
    val habit: HabitResponse,
    val summary: HabitSummary,
)

@Serializable
data class CompletionResponse(
    val expected: Int,
    val completed: Int,
    val rate: Double?,
)

fun Completion.toResponse() = CompletionResponse(expected, completed, rate)

@Serializable
data class BucketResponse(
    @Serializable(with = LocalDateSerializer::class) val start: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val end: LocalDate,
    val expected: Int,
    val completed: Int,
    val rate: Double?,
)

fun Bucket.toResponse() = BucketResponse(start, end, completion.expected, completion.completed, completion.rate)

@Serializable
data class HabitStatsResponse(
    val period: String,
    val buckets: List<BucketResponse>,
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

@Serializable
data class OverviewHabit(
    val id: Long,
    val name: String,
    val color: String,
    val streak: StreakResponse,
    val last30Days: CompletionResponse,
)

@Serializable
data class OverviewResponse(
    val period: String,
    val buckets: List<BucketResponse>,
    val habits: List<OverviewHabit>,
)

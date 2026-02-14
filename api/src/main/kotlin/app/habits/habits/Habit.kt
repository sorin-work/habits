package app.habits.habits

import app.habits.common.InstantSerializer
import app.habits.common.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

data class Habit(
    val id: Long,
    val userId: Long,
    val name: String,
    val description: String?,
    val color: String,
    val schedule: Schedule,
    val target: Int?,
    val unit: String?,
    val startDate: LocalDate,
    val archivedAt: Instant?,
    val createdAt: Instant,
) {
    /** How much counts as done for one day: the target for measured habits, otherwise a single check-in. */
    val dailyGoal get() = target ?: 1

    fun isComplete(value: Int) = value >= dailyGoal

    val archived get() = archivedAt != null
}

@Serializable
data class HabitResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val color: String,
    val schedule: ScheduleDto,
    val target: Int?,
    val unit: String?,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate,
    val archived: Boolean,
    @Serializable(with = InstantSerializer::class) val archivedAt: Instant?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)

fun Habit.toResponse() =
    HabitResponse(
        id = id,
        name = name,
        description = description,
        color = color,
        schedule = schedule.toDto(),
        target = target,
        unit = unit,
        startDate = startDate,
        archived = archived,
        archivedAt = archivedAt,
        createdAt = createdAt,
    )

@Serializable
data class HabitRequest(
    val name: String = "",
    val description: String? = null,
    val color: String? = null,
    val schedule: ScheduleDto? = null,
    val target: Int? = null,
    val unit: String? = null,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate? = null,
)

/** A validated create or update. */
data class HabitInput(
    val name: String,
    val description: String?,
    val color: String,
    val schedule: Schedule,
    val target: Int?,
    val unit: String?,
    val startDate: LocalDate,
)

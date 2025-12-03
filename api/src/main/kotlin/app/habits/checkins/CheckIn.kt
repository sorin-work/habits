package app.habits.checkins

import java.time.Instant
import java.time.LocalDate

data class CheckIn(
    val habitId: Long,
    val day: LocalDate,
    val value: Int,
    val note: String?,
    val updatedAt: Instant,
)

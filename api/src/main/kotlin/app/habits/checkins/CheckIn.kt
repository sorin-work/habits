package app.habits.checkins

import app.habits.common.InstantSerializer
import app.habits.common.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

data class CheckIn(
    val habitId: Long,
    val day: LocalDate,
    val value: Int,
    val note: String?,
    val updatedAt: Instant,
)

@Serializable
data class CheckInRequest(
    val value: Int? = null,
    val note: String? = null,
)

@Serializable
data class CheckInResponse(
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val value: Int,
    val note: String?,
    val completed: Boolean,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

const val MAX_CHECK_IN_VALUE = 1_000_000
const val MAX_NOTE_LENGTH = 500

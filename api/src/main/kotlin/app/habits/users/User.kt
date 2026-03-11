package app.habits.users

import app.habits.common.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class User(
    val id: Long,
    val email: String,
    val passwordHash: String,
    val name: String,
    val timeZone: ZoneId,
    val weekStart: DayOfWeek,
    val createdAt: Instant,
)

@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val timeZone: String,
    val weekStart: String,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)

fun User.toResponse() =
    UserResponse(
        id = id,
        email = email,
        name = name,
        timeZone = timeZone.id,
        weekStart = weekStart.name,
        createdAt = createdAt,
    )

@Serializable
data class RegisterRequest(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val timeZone: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String = "",
    val password: String = "",
)

@Serializable
data class AuthResponse(
    val token: String,
    @Serializable(with = InstantSerializer::class) val expiresAt: Instant,
    val user: UserResponse,
)

@Serializable
data class UpdateProfileRequest(
    val name: String = "",
    val timeZone: String = "",
    val weekStart: String = "MONDAY",
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String = "",
    val newPassword: String = "",
)

@Serializable
data class DeleteAccountRequest(
    val password: String = "",
)

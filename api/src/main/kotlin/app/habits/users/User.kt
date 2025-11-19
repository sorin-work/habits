package app.habits.users

import app.habits.common.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

data class User(
    val id: Long,
    val email: String,
    val passwordHash: String,
    val name: String,
    val timeZone: ZoneId,
    val createdAt: Instant,
)

@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val timeZone: String,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
)

fun User.toResponse() =
    UserResponse(
        id = id,
        email = email,
        name = name,
        timeZone = timeZone.id,
        createdAt = createdAt,
    )

@Serializable
data class RegisterRequest(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val timeZone: String? = null,
)

package app.habits.users

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

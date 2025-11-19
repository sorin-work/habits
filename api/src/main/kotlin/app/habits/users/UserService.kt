package app.habits.users

import app.habits.auth.PasswordHasher
import app.habits.common.ConflictException
import app.habits.common.Validator
import app.habits.common.isEmail
import app.habits.common.normalizeEmail
import app.habits.common.validate
import java.time.Clock
import java.time.ZoneId

const val MIN_PASSWORD_LENGTH = 8

fun parseTimeZone(value: String): ZoneId? = runCatching { ZoneId.of(value.trim()) }.getOrNull()

class UserService(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val clock: Clock,
) {
    suspend fun register(request: RegisterRequest): User {
        val email = normalizeEmail(request.email)
        val name = request.name.trim()
        val zone = request.timeZone?.let { parseTimeZone(it) }
        validate {
            check("email", isEmail(email)) { "Enter a valid email address" }
            checkPassword("password", request.password)
            check("timeZone", request.timeZone.isNullOrBlank() || zone != null) { "Unknown time zone" }
        }
        if (users.findByEmail(email) != null) {
            throw ConflictException("email_taken", "An account with this email already exists")
        }
        val user =
            users.create(
                email = email,
                passwordHash = hasher.hash(request.password),
                name = name,
                timeZone = zone ?: ZoneId.of("UTC"),
                createdAt = clock.instant(),
            )
        return user
    }
}

private fun Validator.checkPassword(
    field: String,
    password: String,
) {
    check(field, password.length >= MIN_PASSWORD_LENGTH) {
        "Password must be at least $MIN_PASSWORD_LENGTH characters"
    }
}

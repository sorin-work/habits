package app.habits.users

import app.habits.auth.JwtService
import app.habits.auth.PasswordHasher
import app.habits.common.ApiException
import app.habits.common.ConflictException
import app.habits.common.Validator
import app.habits.common.isEmail
import app.habits.common.normalizeEmail
import app.habits.common.validate
import io.ktor.http.HttpStatusCode
import java.time.Clock
import java.time.ZoneId

const val MIN_PASSWORD_LENGTH = 8
const val MAX_PASSWORD_LENGTH = 72

fun parseTimeZone(value: String): ZoneId? = runCatching { ZoneId.of(value.trim()) }.getOrNull()

class UserService(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val jwt: JwtService,
    private val clock: Clock,
) {
    suspend fun register(request: RegisterRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        val name = request.name.trim()
        val zone = request.timeZone?.let { parseTimeZone(it) }
        validate {
            check("email", isEmail(email)) { "Enter a valid email address" }
            checkPassword("password", request.password)
            check("name", name.isNotEmpty()) { "Enter your name" }
            check("name", name.length <= 80) { "Name must be at most 80 characters" }
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
        val issued = jwt.issue(user.id)
        return AuthResponse(issued.token, issued.expiresAt, user.toResponse())
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim()
        validate {
            check("email", email.isNotEmpty()) { "Enter your email" }
            check("password", request.password.isNotEmpty()) { "Enter your password" }
        }
        val user = users.findByEmail(email)
        if (user == null || !hasher.verify(request.password, user.passwordHash)) throw invalidCredentials()
        val issued = jwt.issue(user.id)
        return AuthResponse(issued.token, issued.expiresAt, user.toResponse())
    }

    private fun invalidCredentials() =
        ApiException(HttpStatusCode.Unauthorized, "invalid_credentials", "Email or password is incorrect")
}

private fun Validator.checkPassword(
    field: String,
    password: String,
) {
    check(field, password.length >= MIN_PASSWORD_LENGTH) {
        "Password must be at least $MIN_PASSWORD_LENGTH characters"
    }
    check(field, password.length <= MAX_PASSWORD_LENGTH) {
        "Password must be at most $MAX_PASSWORD_LENGTH characters"
    }
}

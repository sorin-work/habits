package app.habits.config

import java.time.Duration

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
)

data class JwtConfig(
    val secret: String,
    val issuer: String = "habits",
    val audience: String = "habits-web",
    val ttl: Duration = Duration.ofDays(7),
)

data class AppConfig(
    val port: Int,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val corsAllowedOrigins: List<String>,
    val seedDemo: Boolean,
    val bcryptCost: Int = 12,
    val loginRateLimit: Int = 10,
) {
    companion object {
        private const val DEV_SECRET = "local-development-secret-change-me-0123456789"

        fun fromEnv(env: Map<String, String> = System.getenv()): AppConfig {
            val secret = env["JWT_SECRET"]?.takeIf { it.isNotBlank() } ?: DEV_SECRET
            require(secret.length >= 32) { "JWT_SECRET must be at least 32 characters" }
            return AppConfig(
                port = env["PORT"]?.toIntOrNull() ?: 8080,
                database =
                    DatabaseConfig(
                        url = env["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5443/habits",
                        user = env["DATABASE_USER"] ?: "habits",
                        password = env["DATABASE_PASSWORD"] ?: "habits",
                    ),
                jwt = JwtConfig(secret = secret, ttl = parseDuration(env["JWT_TTL"] ?: "7d")),
                corsAllowedOrigins =
                    (env["CORS_ALLOWED_ORIGINS"] ?: "http://localhost:5173")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() },
                seedDemo = env["SEED_DEMO"]?.trim()?.lowercase() in setOf("true", "1", "yes"),
                loginRateLimit = env["LOGIN_RATE_LIMIT"]?.toIntOrNull() ?: 10,
            )
        }

        /** Parses "90s", "15m", "12h" or "7d". */
        fun parseDuration(value: String): Duration {
            val match =
                Regex("""^(\d+)([smhd])$""").matchEntire(value.trim())
                    ?: throw IllegalArgumentException("Invalid duration '$value', expected e.g. 12h or 7d")
            val amount = match.groupValues[1].toLong()
            return when (match.groupValues[2]) {
                "s" -> Duration.ofSeconds(amount)
                "m" -> Duration.ofMinutes(amount)
                "h" -> Duration.ofHours(amount)
                else -> Duration.ofDays(amount)
            }
        }
    }
}

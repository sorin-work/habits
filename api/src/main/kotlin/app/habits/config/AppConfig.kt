package app.habits.config

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
)

data class AppConfig(
    val port: Int,
    val database: DatabaseConfig,
    val bcryptCost: Int = 12,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): AppConfig {
            return AppConfig(
                port = env["PORT"]?.toIntOrNull() ?: 8080,
                database =
                    DatabaseConfig(
                        url = env["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5443/habits",
                        user = env["DATABASE_USER"] ?: "habits",
                        password = env["DATABASE_PASSWORD"] ?: "habits",
                    ),
            )
        }
    }
}

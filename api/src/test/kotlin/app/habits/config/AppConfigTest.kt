package app.habits.config

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppConfigTest {
    @Test
    fun `defaults point at the local compose database`() {
        val config = AppConfig.fromEnv(emptyMap())
        assertEquals(8080, config.port)
        assertEquals("jdbc:postgresql://localhost:5443/habits", config.database.url)
        assertEquals(Duration.ofDays(7), config.jwt.ttl)
    }

    @Test
    fun `reads overrides from the environment`() {
        val config =
            AppConfig.fromEnv(
                mapOf(
                    "PORT" to "9000",
                    "JWT_TTL" to "12h",
                ),
            )
        assertEquals(9000, config.port)
        assertEquals(Duration.ofHours(12), config.jwt.ttl)
    }

    @Test
    fun `rejects short jwt secrets`() {
        assertFailsWith<IllegalArgumentException> { AppConfig.fromEnv(mapOf("JWT_SECRET" to "too-short")) }
    }

    @Test
    fun `parses durations`() {
        assertEquals(Duration.ofSeconds(90), AppConfig.parseDuration("90s"))
        assertEquals(Duration.ofMinutes(15), AppConfig.parseDuration("15m"))
        assertFailsWith<IllegalArgumentException> { AppConfig.parseDuration("7 days") }
    }
}

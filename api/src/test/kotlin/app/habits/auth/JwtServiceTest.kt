package app.habits.auth

import app.habits.config.JwtConfig
import com.auth0.jwt.exceptions.JWTVerificationException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JwtServiceTest {
    private val config = JwtConfig(secret = "a-test-secret-with-at-least-32-characters", ttl = Duration.ofHours(1))

    @Test
    fun `issued tokens verify and carry the user id`() {
        val service = JwtService(config, Clock.systemUTC())
        val issued = service.issue(42)
        assertEquals("42", service.verifier.verify(issued.token).subject)
    }

    @Test
    fun `expired tokens are rejected`() {
        val past = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC)
        val token = JwtService(config, past).issue(1).token
        assertFailsWith<JWTVerificationException> { JwtService(config, Clock.systemUTC()).verifier.verify(token) }
    }

    @Test
    fun `tokens signed with another secret are rejected`() {
        val other = JwtService(config.copy(secret = "another-secret-that-is-also-long-enough"), Clock.systemUTC())
        val token = other.issue(1).token
        assertFailsWith<JWTVerificationException> { JwtService(config, Clock.systemUTC()).verifier.verify(token) }
    }

    @Test
    fun `password hashes verify only the original password`() {
        val hasher = PasswordHasher(cost = 4)
        val hash = hasher.hash("correct-horse")
        assertNotEquals("correct-horse", hash)
        assertTrue(hasher.verify("correct-horse", hash))
        assertEquals(false, hasher.verify("wrong-horse", hash))
    }
}

package app.habits.auth

import app.habits.config.JwtConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.time.Clock
import java.time.Instant
import java.util.Date

data class IssuedToken(
    val token: String,
    val expiresAt: Instant,
)

class JwtService(
    private val config: JwtConfig,
    private val clock: Clock,
) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    // Built with our clock so expiry follows the same time source as issuing.
    val verifier: JWTVerifier =
        (
            JWT
                .require(algorithm)
                .withIssuer(config.issuer)
                .withAudience(config.audience) as JWTVerifier.BaseVerification
        ).build(clock)

    val audience get() = config.audience

    fun issue(userId: Long): IssuedToken {
        val now = clock.instant()
        val expiresAt = now.plus(config.ttl)
        val token =
            JWT
                .create()
                .withIssuer(config.issuer)
                .withAudience(config.audience)
                .withSubject(userId.toString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm)
        return IssuedToken(token, expiresAt)
    }
}

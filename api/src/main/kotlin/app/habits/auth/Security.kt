package app.habits.auth

import app.habits.common.UnauthorizedException
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal

const val AUTH_JWT = "jwt"

fun Application.configureSecurity(jwtService: JwtService) {
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = "habits"
            verifier(jwtService.verifier)
            validate { credential ->
                val subject = credential.payload.subject?.toLongOrNull()
                if (subject != null && credential.payload.audience.contains(jwtService.audience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ -> throw UnauthorizedException("Your session has expired, please sign in again") }
        }
    }
}

/** The signed-in user's id. Only valid inside `authenticate(AUTH_JWT)` routes. */
val ApplicationCall.userId: Long
    get() =
        principal<JWTPrincipal>()?.payload?.subject?.toLongOrNull()
            ?: throw UnauthorizedException()

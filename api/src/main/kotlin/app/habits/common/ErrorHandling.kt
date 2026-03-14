package app.habits.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val fields: Map<String, String>? = null,
)

private val log = LoggerFactory.getLogger("app.habits.Errors")

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(
                cause.status,
                ErrorResponse(cause.status.value, cause.code, cause.message, cause.fields.ifEmpty { null }),
            )
        }
        exception<ContentTransformationException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(400, "invalid_body", "The request body is missing or is not valid JSON"),
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(400, "invalid_body", "The request body is missing or is not valid JSON"),
            )
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled error on ${call.request.local.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(500, "internal_error", "Something went wrong on our side"),
            )
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse(404, "not_found", "No such endpoint"))
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ErrorResponse(429, "rate_limited", "Too many attempts, try again in a minute"))
        }
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ErrorResponse(401, "unauthorized", "Authentication required"))
        }
    }
}
